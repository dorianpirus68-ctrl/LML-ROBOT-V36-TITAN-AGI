package com.lml.actionassistant;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private TextInputEditText baseUrlInput;
    private TextInputEditText modelInput;
    private TextInputEditText apiKeyInput;
    private TextInputEditText taskInput;
    private TextView responseText;
    private TextView statusText;
    private TextView plannerText;
    private TextView criticText;
    private TextView safetyText;
    private View multiBrainDetails;
    private View localActionsPanel;
    private MaterialButton saveButton;
    private MaterialButton testConnectionButton;
    private MaterialButton reviewButton;
    private MaterialButton multiBrainButton;
    private MaterialButton copyButton;
    private MaterialButton shareButton;
    private MaterialButton confirmReviewButton;
    private MaterialButton clearButton;

    private SecureConfigStore configStore;
    private final OpenAiCompatibleClient aiClient = new OpenAiCompatibleClient();
    private final MultiBrainOrchestrator multiBrainOrchestrator = new MultiBrainOrchestrator(aiClient);
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean busy = false;
    private String latestSynthesis = "";

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
        multiBrainDetails = findViewById(R.id.multiBrainDetails);
        localActionsPanel = findViewById(R.id.localActionsPanel);
        saveButton = findViewById(R.id.saveButton);
        testConnectionButton = findViewById(R.id.testConnectionButton);
        reviewButton = findViewById(R.id.reviewButton);
        multiBrainButton = findViewById(R.id.multiBrainButton);
        copyButton = findViewById(R.id.copyButton);
        shareButton = findViewById(R.id.shareButton);
        confirmReviewButton = findViewById(R.id.confirmReviewButton);
        clearButton = findViewById(R.id.clearButton);

        baseUrlInput.setText(configStore.getBaseUrl());
        modelInput.setText(configStore.getModel());
        if (configStore.hasApiKey()) {
            apiKeyInput.setHint(getString(R.string.api_key_saved_hint));
        }

        saveButton.setOnClickListener(view -> saveConfiguration());
        testConnectionButton.setOnClickListener(view -> testConnection());
        reviewButton.setOnClickListener(view -> reviewTask());
        multiBrainButton.setOnClickListener(view -> reviewWithCollective());
        copyButton.setOnClickListener(view -> copySynthesis());
        shareButton.setOnClickListener(view -> shareSynthesis());
        confirmReviewButton.setOnClickListener(view -> confirmReview());
        clearButton.setOnClickListener(view -> clearReview());
    }

    private void saveConfiguration() {
        ProviderConfig config = validateProvider();
        if (config == null) {
            return;
        }
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
        if (config == null || busy) {
            return;
        }
        runNetworkTask(getString(R.string.status_testing), () -> {
            String apiKey = resolveApiKey(config.baseUrl);
            aiClient.test(config.baseUrl, config.model, apiKey, getString(R.string.system_instruction));
            return getString(R.string.connection_success) + " Modèle : " + config.model;
        });
    }

    private void reviewTask() {
        ProviderConfig config = validateProvider();
        String task = validatedTask();
        if (config == null || task == null || busy) {
            return;
        }
        runNetworkTask(getString(R.string.status_thinking), () -> {
            String apiKey = resolveApiKey(config.baseUrl);
            return aiClient.review(config.baseUrl, config.model, apiKey,
                    getString(R.string.system_instruction), task);
        });
    }

    private void reviewWithCollective() {
        ProviderConfig config = validateProvider();
        String task = validatedTask();
        if (config == null || task == null || busy) {
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
                    String message = exception.getMessage();
                    showFailure(message == null || message.trim().isEmpty()
                            ? getString(R.string.error_network) : message.trim());
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
        multiBrainDetails.setVisibility(View.VISIBLE);
        localActionsPanel.setVisibility(View.VISIBLE);
        setStatus(result.isHumanConfirmationRecommended()
                ? getString(R.string.status_confirmation_recommended)
                : getString(R.string.status_collective_ready));
        busy = false;
        setBusy(false);
    }

    private void runNetworkTask(String status, NetworkTask task) {
        busy = true;
        setBusy(true);
        setStatus(status);
        networkExecutor.execute(() -> {
            try {
                String result = task.run();
                runOnUiThread(() -> {
                    latestSynthesis = result;
                    responseText.setText(result);
                    localActionsPanel.setVisibility(View.VISIBLE);
                    setStatus(getString(R.string.status_ready));
                    busy = false;
                    setBusy(false);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    String message = exception.getMessage();
                    showFailure(message == null || message.trim().isEmpty()
                            ? getString(R.string.error_network) : message.trim());
                    busy = false;
                    setBusy(false);
                });
            }
        });
    }

    private void copySynthesis() {
        if (latestSynthesis.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.response_section), latestSynthesis));
        Toast.makeText(this, R.string.action_copied, Toast.LENGTH_SHORT).show();
    }

    private void shareSynthesis() {
        if (latestSynthesis.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, latestSynthesis);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_chooser)));
    }

    private void confirmReview() {
        if (latestSynthesis.isEmpty()) {
            Toast.makeText(this, R.string.error_no_result, Toast.LENGTH_SHORT).show();
            return;
        }
        setStatus(getString(R.string.status_review_confirmed));
        Toast.makeText(this, R.string.status_review_confirmed, Toast.LENGTH_SHORT).show();
    }

    private void clearReview() {
        latestSynthesis = "";
        responseText.setText(R.string.response_placeholder);
        plannerText.setText(R.string.role_placeholder);
        criticText.setText(R.string.role_placeholder);
        safetyText.setText(R.string.role_placeholder);
        multiBrainDetails.setVisibility(View.GONE);
        localActionsPanel.setVisibility(View.GONE);
        setStatus(getString(R.string.status_ready));
    }

    private String resolveApiKey(String requestedBaseUrl) throws Exception {
        String visibleKey = fieldValue(apiKeyInput);
        if (!visibleKey.isEmpty()) {
            return visibleKey;
        }
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
        confirmReviewButton.setEnabled(!isBusy);
        clearButton.setEnabled(!isBusy);
    }

    private void setStatus(String value) {
        statusText.setText(value);
    }

    private void showFailure(String detail) {
        responseText.setText(detail);
        setStatus(getString(R.string.status_ready));
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

    private static final class ProviderConfig {
        private final String baseUrl;
        private final String model;

        private ProviderConfig(@NonNull String baseUrl, @NonNull String model) {
            this.baseUrl = baseUrl;
            this.model = model;
        }
    }
}
