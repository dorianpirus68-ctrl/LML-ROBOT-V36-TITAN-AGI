package com.lml.control;

import android.app.Activity;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText(R.string.chat_title);
        title.setTextSize(24);
        root.addView(title);

        scrollView = new ScrollView(this);
        transcript = new TextView(this);
        transcript.setText(getString(R.string.chat_welcome));
        transcript.setTextSize(16);
        transcript.setPadding(0, 20, 0, 20);
        scrollView.addView(transcript);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        input = new EditText(this);
        input.setHint(R.string.chat_hint);
        root.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button send = new Button(this);
        send.setText(R.string.chat_send);
        send.setOnClickListener(view -> sendMessage());
        root.addView(send, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }
        String answer = LocalAssistant.reply(message,
                AllowedAppsActivity.getAllowedCount(this), TrainingActivity.getScenarioCount(this));
        transcript.append("\n\nVous : " + message + "\nLML : " + answer);
        input.setText("");
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
