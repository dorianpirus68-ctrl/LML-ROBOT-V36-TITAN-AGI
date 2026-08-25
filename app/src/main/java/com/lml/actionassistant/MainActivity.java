package com.lml.actionassistant;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Conversation-first assistant. It can analyse text and prepare a small set of
 * user-initiated Android actions. It never controls another application's UI.
 */
public final class MainActivity extends AppCompatActivity {
    private static final long EXPORT_APPROVAL_LIFETIME_MS = 5 * 60 * 1000L;

    private TextInputEditText baseUrlInput;
    private TextInputEditText modelInput;
    private TextInputEditText apiKeyInput;
    private TextInputEditText taskInput;
    private TextView responseText;
    private TextView statusText;
    private TextView plannerText;
    private TextView criticText;
    private TextView safetyText;
    private TextView contextInfoText;
    private LinearLayout chatHistory;
    private ScrollView conversationScroll;
    private View multiBrainDetails;
    private View localActionsPanel;
    private MaterialButton saveButton;
    private MaterialButton testConnectionButton;
    private MaterialButton reviewButton;
    private MaterialButton multiBrainButton;
    private MaterialButton copyButton;
    private MaterialButton shareButton;
    private MaterialButton exportButton;
    private MaterialButton calendarButton;
    private MaterialButton webSearchButton;
    private MaterialButton mapsButton;
    private MaterialButton emailButton;
    private MaterialButton smsButton;
    private MaterialButton newConversationButton;
    private MaterialButton suggestionProjectButton;
    private MaterialButton suggestionMeetingButton;
    private MaterialButton suggestionSummaryButton;
    private MaterialButton clearButton;

    private SecureConfigStore configStore;
    private final OpenAiCompatibleClient aiClient = new OpenAiCompatibleClient();
    private final MultiBrainOrchestrator multiBrainOrchestrator = new MultiBrainOrchestrator(aiClient);
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final List<ChatEntry> conversation = new ArrayList<>();
    private volatile boolean busy = false;
    private String latestSynthesis = "";
    private String approvedExportMarkdown = "";
    private long exportApprovalExpiresAtMs = 0L;
    private final ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleDocumentResult(result.getResultCode(), result.getData()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configStore = new SecureConfigStore(getApplicationContext());
        baseUrlInput = findViewById(R.id.baseUrlInput);
        modelInput = findViewById(R.id.modelInput);
        apiKeyInput = findViewById(R.id.apiKeyInput);
        taskInput = findViewById(R.id.taskInput);
        responseText = findViewById(R.id.responseText);
        statusText = findViewById(R.id.statusText);
        plannerText = findViewById(R.id.plannerText);
        criticText = findViewById(R.id.criticText);
        safetyText = findViewById(R.id.safetyText);
        contextInfoText = findViewById(R.id.contextInfoText);
        chatHistory = findViewById(R.id.chatHistory);
        conversationScroll = findViewById(R.id.conversationScroll);
        multiBrainDetails = findViewById(R.id.multiBrainDetails);
        localActionsPanel = findViewById(R.id.localActionsPanel);
        saveButton = findViewById(R.id.saveButton);
        testConnectionButton = findViewById(R.id.testConnectionButton);
        reviewButton = findViewById(R.id.reviewButton);
        multiBrainButton = findViewById(R.id.multiBrainButton);
        copyButton = findViewById(R.id.copyButton);
        shareButton = findViewById(R.id.shareButton);
        exportButton = findViewById(R.id.exportButton);
        calendarButton = findViewById(R.id.calendarButton);
        webSearchButton = findViewById(R.id.webSearchButton);
        mapsButton = findViewById(R.id.mapsButton);
        emailButton = findViewById(R.id.emailButton);
        smsButton = findViewById(R.id.smsButton);
        newConversationButton = findViewById(R.id.newConversationButton);
        suggestionProjectButton = findViewById(R.id.suggestionProjectButton);
        suggestionMeetingButton = findViewById(R.id.suggestionMeetingButton);
        suggestionSummaryButton = findViewById(R.id.suggestionSummaryButton);
        clearButton = findViewById(R.id.clearButton);

        baseUrlInput.setText(configStore.getBaseUrl());
        modelInput.setText(configStore.getModel());
        if (configStore.hasApiKey()) {
            apiKeyInput.setHint(getString(R.string.api_key_saved_hint));
        }
        appendWelcomeMessage();
        updateConversationInfo();

        saveButton.setOnClickListener(view -> saveConfiguration());
        testConnectionButton.setOnClickListener(view -> testConnection());
        reviewButton.setOnClickListener(view -> sendChatMessage());
        multiBrainButton.setOnClickListener(view -> analyseLastMessageWithCollective());
        copyButton.setOnClickListener(view -> copyResponse());
        shareButton.setOnClickListener(view -> shareResponse());
        exportButton.setOnClickListener(view -> requestMarkdownExport());
        calendarButton.setOnClickListener(view -> requestCalendarDraft());
        webSearchButton.setOnClickListener(view -> requestWebSearch());
        mapsButton.setOnClickListener(view -> requestMapSearch());
        emailButton.setOnClickListener(view -> requestEmailDraft());
        smsButton.setOnClickListener(view -> requestSmsDraft());
        newConversationButton.setOnClickListener(view -> clearConversation());
        suggestionProjectButton.setOnClickListener(view -> useSuggestion(R.string.suggestion_project));
        suggestionMeetingButton.setOnClickListener(view -> useSuggestion(R.string.suggestion_meeting));
        suggestionSummaryButton.setOnClickListener(view -> useSuggestion(R.string.suggestion_summary));
        clearButton.setOnClickListener(view -> clearConversation());
    }

    private void appendWelcomeMessage() {
        appendChatBubble(getString(R.string.chat_welcome), false, false);
    }

    private void saveConfiguration() {
        ProviderConfig config = validateProvider();
        if (config == null) return;
        try {
            String newKey = fieldValue(apiKeyInput);
            boolean providerChanged = !config.baseUrl.equals(configStore.getBaseUrl());
            configStore.save(config.baseUrl, config.model, newKey, !newKey.isEmpty() || providerChanged);
            if (!newKey.isEmpty()) {
                apiKeyInput.setText("");
                apiKeyInput.setHint(getString(R.string.api_key_saved_hint));
            }
            setStatus(getString(R.string.status_saved));
            Toast.makeText(this, R.string.status_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            showFailure(getString(R.string.error_store_key));
        }
    }

    private void testConnection() {
        ProviderConfig config = validateProvider();
        if (config == null || busy) return;
        runNetworkTask(getString(R.string.status_testing), () -> {
            String apiKey = resolveApiKey(config.baseUrl);
            aiClient.test(config.baseUrl, config.model, apiKey, getString(R.string.system_instruction));
            return getString(R.string.connection_success) + " Modèle : " + config.model;
        }, false);
    }

    private void sendChatMessage() {
        ProviderConfig config = validateProvider();
        String message = validatedTask();
        if (config == null || message == null || busy) return;

        appendConversationEntry(message, true);
        taskInput.setText("");
        runNetworkTask(getString(R.string.status_thinking), () -> {
            String apiKey = resolveApiKey(config.baseUrl);
            return aiClient.review(config.baseUrl, config.model, apiKey,
                    getString(R.string.chat_system_instruction), buildConversationPrompt());
        }, true);
    }

    private void analyseLastMessageWithCollective() {
        ProviderConfig config = validateProvider();
        String task = latestUserMessage();
        if (config == null || task.isEmpty() || busy) {
            if (task.isEmpty()) taskInput.setError(getString(R.string.error_need_message_for_collective));
            return;
        }
        busy = true;
        setBusy(true);
        setStatus(getString(R.string.status_collective));
        networkExecutor.execute(() -> {
            try {
                String apiKey = resolveApiKey(config.baseUrl);
                MultiBrainResult result = multiBrainOrchestrator.review(config.baseUrl, config.model, apiKey, task);
                runOnUiThread(() -> showCollectiveResult(result));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    showFailure(messageFor(exception));
                    busy = false;
                    setBusy(false);
                });
            }
        });
    }

    private void showCollectiveResult(MultiBrainResult result) {
        plannerText.setText(result.getOpinion(AgentRole.PLANNER));
        criticText.setText(result.getOpinion(AgentRole.CRITIC));
        safetyText.setText(result.getOpinion(AgentRole.SAFETY));
        latestSynthesis = result.getSynthesis();
        responseText.setText(latestSynthesis);
        appendConversationEntry(latestSynthesis, false);
        multiBrainDetails.setVisibility(View.VISIBLE);
        localActionsPanel.setVisibility(View.VISIBLE);
        setStatus(result.isHumanConfirmationRecommended()
                ? getString(R.string.status_confirmation_recommended)
                : getString(R.string.status_collective_ready));
        busy = false;
        setBusy(false);
    }

    private void runNetworkTask(String status, NetworkTask task, boolean appendAssistantReply) {
        busy = true;
        setBusy(true);
        setStatus(status);
        networkExecutor.execute(() -> {
            try {
                String result = task.run();
                runOnUiThread(() -> {
                    latestSynthesis = result;
                    responseText.setText(result);
                    if (appendAssistantReply) appendConversationEntry(result, false);
                    localActionsPanel.setVisibility(View.VISIBLE);
                    setStatus(getString(R.string.status_ready));
                    busy = false;
                    setBusy(false);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    String message = messageFor(exception);
                    if (appendAssistantReply) appendChatBubble(message, false, true);
                    showFailure(message);
                    busy = false;
                    setBusy(false);
                });
            }
        });
    }

    private void copyResponse() {
        if (latestSynthesis.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.response_section), latestSynthesis));
        Toast.makeText(this, R.string.action_copied, Toast.LENGTH_SHORT).show();
    }

    private void shareResponse() {
        if (latestSynthesis.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, latestSynthesis);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_chooser)));
    }

    /** HITL: preview is shown before a user approves the one-time local export. */
    private void requestMarkdownExport() {
        if (latestSynthesis.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        String markdown = "# Conversation LML Action Assistant\n\n" + latestSynthesis + "\n";
        String preview = markdown.length() > 900 ? markdown.substring(0, 900) + "\n…" : markdown;
        new AlertDialog.Builder(this)
                .setTitle(R.string.export_preview_title)
                .setMessage(getString(R.string.export_preview_message, preview))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> setStatus(getString(R.string.status_export_cancelled)))
                .setPositiveButton(R.string.action_authorize_export, (dialog, which) -> {
                    approvedExportMarkdown = markdown;
                    exportApprovalExpiresAtMs = System.currentTimeMillis() + EXPORT_APPROVAL_LIFETIME_MS;
                    Intent createDocument = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    createDocument.setType("text/markdown");
                    createDocument.addCategory(Intent.CATEGORY_OPENABLE);
                    createDocument.putExtra(Intent.EXTRA_TITLE, "lml-chat-" + UUID.randomUUID() + ".md");
                    createDocumentLauncher.launch(createDocument);
                })
                .show();
    }

    private void requestWebSearch() {
        String query = actionContext();
        if (query.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
        confirmExternalIntent(getString(R.string.web_search_preview_title),
                getString(R.string.web_search_preview_message, shortPreview(query)),
                getString(R.string.action_open_search), intent);
    }

    private void requestMapSearch() {
        String query = actionContext();
        if (query.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query)));
        confirmExternalIntent(getString(R.string.maps_preview_title),
                getString(R.string.maps_preview_message, shortPreview(query)),
                getString(R.string.action_open_maps), intent);
    }

    private void requestEmailDraft() {
        String content = actionContext();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_draft_subject));
        intent.putExtra(Intent.EXTRA_TEXT, content);
        confirmExternalIntent(getString(R.string.email_preview_title),
                getString(R.string.email_preview_message, shortPreview(content)),
                getString(R.string.action_open_email), intent);
    }

    private void requestSmsDraft() {
        String content = actionContext();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        intent.putExtra("sms_body", content);
        confirmExternalIntent(getString(R.string.sms_preview_title),
                getString(R.string.sms_preview_message, shortPreview(content)),
                getString(R.string.action_open_sms), intent);
    }

    private void confirmExternalIntent(String title, String message, String positiveLabel, Intent intent) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(positiveLabel, (dialog, which) -> openExternalIntent(intent))
                .show();
    }

    private void openExternalIntent(Intent intent) {
        try {
            startActivity(intent);
            setStatus(getString(R.string.status_external_action_opened));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.error_action_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private String actionContext() {
        return !latestSynthesis.isEmpty() ? latestSynthesis : latestUserMessage();
    }

    private String shortPreview(String value) {
        return value.length() > 360 ? value.substring(0, 360) + "…" : value;
    }

    /** HITL: the user confirms, then the Calendar app presents its own editable event screen. */
    private void requestCalendarDraft() {
        String title = latestUserMessage();
        if (title.isEmpty()) title = latestSynthesis;
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        String finalTitle = title.length() > 120 ? title.substring(0, 120) : title;
        new AlertDialog.Builder(this)
                .setTitle(R.string.calendar_preview_title)
                .setMessage(getString(R.string.calendar_preview_message, finalTitle))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_open_calendar, (dialog, which) -> openCalendarDraft(finalTitle))
                .show();
    }

    private void openCalendarDraft(String title) {
        long start = System.currentTimeMillis() + 60 * 60 * 1000L;
        Intent calendar = new Intent(Intent.ACTION_INSERT);
        calendar.setData(CalendarContract.Events.CONTENT_URI);
        calendar.putExtra(CalendarContract.Events.TITLE, title);
        calendar.putExtra(CalendarContract.Events.DESCRIPTION, latestSynthesis);
        calendar.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start);
        calendar.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 30 * 60 * 1000L);
        try {
            startActivity(calendar);
            setStatus(getString(R.string.status_calendar_opened));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.error_calendar_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDocumentResult(int resultCode, Intent data) {
        Uri uri = data == null ? null : data.getData();
        if (resultCode != RESULT_OK || uri == null) {
            invalidateExportApproval();
            setStatus(getString(R.string.status_export_cancelled));
            return;
        }
        if (approvedExportMarkdown.isEmpty() || System.currentTimeMillis() > exportApprovalExpiresAtMs) {
            invalidateExportApproval();
            Toast.makeText(this, R.string.error_export_expired, Toast.LENGTH_LONG).show();
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new IllegalStateException("Sortie de document indisponible.");
            output.write(approvedExportMarkdown.getBytes(StandardCharsets.UTF_8));
            setStatus(getString(R.string.status_exported));
            Toast.makeText(this, R.string.status_exported, Toast.LENGTH_SHORT).show();
        } catch (Exception exception) {
            Toast.makeText(this, getString(R.string.error_export_failed), Toast.LENGTH_LONG).show();
        } finally {
            invalidateExportApproval();
        }
    }

    private void invalidateExportApproval() {
        approvedExportMarkdown = "";
        exportApprovalExpiresAtMs = 0L;
    }

    private void clearConversation() {
        conversation.clear();
        chatHistory.removeAllViews();
        taskInput.setText("");
        appendWelcomeMessage();
        updateConversationInfo();
        latestSynthesis = "";
        invalidateExportApproval();
        responseText.setText(R.string.response_placeholder);
        plannerText.setText(R.string.role_placeholder);
        criticText.setText(R.string.role_placeholder);
        safetyText.setText(R.string.role_placeholder);
        multiBrainDetails.setVisibility(View.GONE);
        localActionsPanel.setVisibility(View.GONE);
        setStatus(getString(R.string.status_ready));
    }

    private void appendConversationEntry(String text, boolean isUser) {
        conversation.add(new ChatEntry(text, isUser));
        appendChatBubble(text, isUser, false);
        updateConversationInfo();
    }

    private void appendChatBubble(String text, boolean isUser, boolean isError) {
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(15);
        bubble.setTextColor(Color.rgb(15, 23, 42));
        bubble.setTextIsSelectable(true);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(18));
        background.setColor(isError ? Color.rgb(254, 242, 242)
                : isUser ? Color.rgb(219, 234, 254) : Color.WHITE);
        background.setStroke(dp(1), isUser ? Color.rgb(147, 197, 253) : Color.rgb(226, 232, 240));
        bubble.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.width = LinearLayout.LayoutParams.MATCH_PARENT;
        params.setMargins(isUser ? dp(44) : 0, dp(6), isUser ? 0 : dp(44), dp(6));
        bubble.setLayoutParams(params);
        bubble.setGravity(isUser ? Gravity.END : Gravity.START);
        chatHistory.addView(bubble);
        conversationScroll.post(() -> conversationScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void useSuggestion(int stringId) {
        taskInput.setText(getString(stringId));
        taskInput.requestFocus();
        taskInput.setSelection(taskInput.length());
    }

    private void updateConversationInfo() {
        int messageCount = conversation.size();
        contextInfoText.setText(getResources().getQuantityString(
                R.plurals.context_counter, messageCount, messageCount));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String buildConversationPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Conversation récente. Répondez au dernier message de manière utile et concise. ");
        prompt.append("N’affirmez pas avoir effectué d’action sur le téléphone. Si une action locale est utile, ");
        prompt.append("proposez-la en texte et indiquez que l’utilisateur devra la valider.\n\n");
        int first = Math.max(0, conversation.size() - 8);
        for (int index = first; index < conversation.size(); index++) {
            ChatEntry entry = conversation.get(index);
            prompt.append(entry.user ? "Utilisateur : " : "Assistant : ")
                    .append(entry.text).append("\n\n");
        }
        return prompt.toString();
    }

    private String latestUserMessage() {
        for (int index = conversation.size() - 1; index >= 0; index--) {
            if (conversation.get(index).user) return conversation.get(index).text;
        }
        return "";
    }

    private String resolveApiKey(String requestedBaseUrl) throws Exception {
        String visibleKey = fieldValue(apiKeyInput);
        if (!visibleKey.isEmpty()) return visibleKey;
        return requestedBaseUrl.equals(configStore.getBaseUrl()) ? configStore.getApiKey() : "";
    }

    private ProviderConfig validateProvider() {
        String baseUrl = fieldValue(baseUrlInput);
        String model = fieldValue(modelInput);
        boolean valid = true;
        if (!OpenAiCompatibleClient.isValidBaseUrl(baseUrl)) {
            baseUrlInput.setError(getString(R.string.error_invalid_url));
            valid = false;
        } else {
            baseUrlInput.setError(null);
        }
        if (model.isEmpty()) {
            modelInput.setError(getString(R.string.error_missing_model));
            valid = false;
        } else {
            modelInput.setError(null);
        }
        return valid ? new ProviderConfig(baseUrl, model) : null;
    }

    private String validatedTask() {
        String task = fieldValue(taskInput);
        if (task.isEmpty()) {
            taskInput.setError(getString(R.string.error_missing_task));
            taskInput.requestFocus();
            return null;
        }
        taskInput.setError(null);
        return task;
    }

    private void setBusy(boolean isBusy) {
        saveButton.setEnabled(!isBusy);
        testConnectionButton.setEnabled(!isBusy);
        reviewButton.setEnabled(!isBusy);
        multiBrainButton.setEnabled(!isBusy);
        copyButton.setEnabled(!isBusy);
        shareButton.setEnabled(!isBusy);
        exportButton.setEnabled(!isBusy);
        calendarButton.setEnabled(!isBusy);
        webSearchButton.setEnabled(!isBusy);
        mapsButton.setEnabled(!isBusy);
        emailButton.setEnabled(!isBusy);
        smsButton.setEnabled(!isBusy);
        newConversationButton.setEnabled(!isBusy);
        suggestionProjectButton.setEnabled(!isBusy);
        suggestionMeetingButton.setEnabled(!isBusy);
        suggestionSummaryButton.setEnabled(!isBusy);
        clearButton.setEnabled(!isBusy);
    }

    private void setStatus(String value) {
        statusText.setText(value);
    }

    private void showFailure(String detail) {
        responseText.setText(detail);
        setStatus(getString(R.string.status_ready));
    }

    private String messageFor(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? getString(R.string.error_network) : message.trim();
    }

    private String fieldValue(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
        super.onDestroy();
    }

    private interface NetworkTask {
        String run() throws Exception;
    }

    private static final class ChatEntry {
        private final String text;
        private final boolean user;

        private ChatEntry(String text, boolean user) {
            this.text = text;
            this.user = user;
        }
    }

    private static final class ProviderConfig {
        private final String baseUrl;
        private final String model;

        private ProviderConfig(@NonNull String baseUrl, @NonNull String model) {
            this.baseUrl = baseUrl;
            this.model = model;
        }
    }
}
