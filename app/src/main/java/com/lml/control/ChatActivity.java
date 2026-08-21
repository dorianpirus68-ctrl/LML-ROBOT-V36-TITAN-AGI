package com.lml.control;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChatActivity extends Activity {
    private TextView transcript;
    private EditText input;
    private ScrollView scrollView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24, 24, 24, 24); root.setBackgroundColor(Color.rgb(6, 15, 29));
        TextView title = new TextView(this); title.setText("CHAT LOCAL / AGENT"); title.setTextSize(24); title.setTextColor(Color.rgb(91, 245, 255)); root.addView(title);
        TextView model = new TextView(this); model.setText(ModelDownloadManager.isInstalled(this) ? "Moteur Qwen3 local disponible. Les réponses restent des propositions sous supervision." : "Mode de secours local actif. Installez le moteur dans Modèle local pour une conversation générative hors ligne."); model.setTextColor(Color.rgb(255, 205, 90)); model.setPadding(0, 12, 0, 12); root.addView(model);
        scrollView = new ScrollView(this); transcript = new TextView(this); transcript.setText(getString(R.string.chat_welcome)); transcript.setTextSize(16); transcript.setTextColor(Color.WHITE); transcript.setPadding(0, 20, 0, 20); scrollView.addView(transcript); root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        input = new EditText(this); input.setHint(R.string.chat_hint); input.setTextColor(Color.WHITE); input.setHintTextColor(Color.LTGRAY); root.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button send = new Button(this); send.setText(R.string.chat_send); send.setOnClickListener(v -> sendMessage()); root.addView(send, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); setContentView(root);
    }
    private void sendMessage() { String message = input.getText().toString().trim(); if (message.isEmpty()) return; transcript.append("\n\nVous : " + message + "\nLML : …"); input.setText(""); scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN)); LocalAgentRuntime.replyAsync(this, message, AllowedAppsActivity.getAllowedCount(this), TrainingActivity.getScenarioCount(this), new LocalAgentRuntime.JavaCallback() { @Override public void onReply(String response) { runOnUiThread(() -> replaceLastAnswer(response)); } @Override public void onFailure(String error) { String fallback = LocalAssistant.reply(message, AllowedAppsActivity.getAllowedCount(ChatActivity.this), TrainingActivity.getScenarioCount(ChatActivity.this)); runOnUiThread(() -> replaceLastAnswer(fallback + "\n\n[Le moteur local a signalé : " + error + "]")); } }); }
    private void replaceLastAnswer(String answer) { String current = transcript.getText().toString(); int marker = current.lastIndexOf("\nLML : …"); if (marker >= 0) transcript.setText(current.substring(0, marker) + "\nLML : " + answer); else transcript.append("\nLML : " + answer); scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN)); }
}
