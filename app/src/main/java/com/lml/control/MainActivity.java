package com.lml.control;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private boolean assistantEnabled = false;
    private TextView status;
    private Button toggleAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        toggleAssistant = findViewById(R.id.toggle_assistant);
        Button assistantInfo = findViewById(R.id.assistant_info);
        Button safetyInfo = findViewById(R.id.safety_info);

        toggleAssistant.setOnClickListener(view -> toggleAssistant());
        assistantInfo.setOnClickListener(view -> showInfo(
                getString(R.string.assistant_title),
                getString(R.string.assistant_description)));
        safetyInfo.setOnClickListener(view -> showInfo(
                getString(R.string.safety_title),
                getString(R.string.safety_description)));
    }

    private void toggleAssistant() {
        assistantEnabled = !assistantEnabled;
        status.setText(assistantEnabled ? R.string.status_on : R.string.status_off);
        toggleAssistant.setText(assistantEnabled ? R.string.toggle_off : R.string.toggle_on);
    }

    private void showInfo(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }
}
