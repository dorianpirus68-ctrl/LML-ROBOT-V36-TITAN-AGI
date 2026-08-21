package com.lml.control;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    public static final String PREFERENCES_NAME = "lml_control_preferences";
    public static final String KEY_SESSION_COUNT = "session_count";
    public static final String KEY_ACTIVITY_LOG = "activity_log";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 100;

    private SharedPreferences preferences;
    private boolean assistantEnabled = false;
    private TextView status;
    private TextView sessionCount;
    private TextView activityLog;
    private TextView permissionsState;
    private TextView capabilitySummary;
    private Button toggleAssistant;
    private Button toggleBubble;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        status = findViewById(R.id.status);
        sessionCount = findViewById(R.id.session_count);
        activityLog = findViewById(R.id.activity_log);
        permissionsState = findViewById(R.id.permissions_state);
        capabilitySummary = findViewById(R.id.capability_summary);
        toggleAssistant = findViewById(R.id.toggle_assistant);
        toggleBubble = findViewById(R.id.toggle_bubble);
        Button permissionCenter = findViewById(R.id.permission_center);
        Button allowedApps = findViewById(R.id.allowed_apps);
        Button training = findViewById(R.id.training);
        Button chat = findViewById(R.id.chat);
        Button approvedActions = findViewById(R.id.approved_actions);
        Button assistantInfo = findViewById(R.id.assistant_info);
        Button historyInfo = findViewById(R.id.history_info);
        Button copySummary = findViewById(R.id.copy_summary);
        Button resetData = findViewById(R.id.reset_data);
        Button safetyInfo = findViewById(R.id.safety_info);

        toggleAssistant.setOnClickListener(view -> toggleAssistant());
        toggleBubble.setOnClickListener(view -> toggleBubble());
        permissionCenter.setOnClickListener(view -> showPermissionCenter());
        allowedApps.setOnClickListener(view -> startActivity(new Intent(this, AllowedAppsActivity.class)));
        training.setOnClickListener(view -> startActivity(new Intent(this, TrainingActivity.class)));
        chat.setOnClickListener(view -> startActivity(new Intent(this, ChatActivity.class)));
        approvedActions.setOnClickListener(view -> startActivity(new Intent(this, ApprovedActionsActivity.class)));
        assistantInfo.setOnClickListener(view -> showInfo(
                getString(R.string.assistant_title),
                getString(R.string.assistant_description)));
        historyInfo.setOnClickListener(view -> showInfo(
                getString(R.string.history_title),
                getActivityLog()));
        copySummary.setOnClickListener(view -> copySummary());
        resetData.setOnClickListener(view -> confirmReset());
        safetyInfo.setOnClickListener(view -> showInfo(
                getString(R.string.safety_title),
                getString(R.string.safety_description)));

        updateDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null) {
            updateDashboard();
        }
    }

    private void toggleAssistant() {
        assistantEnabled = !assistantEnabled;
        if (assistantEnabled) {
            int sessions = preferences.getInt(KEY_SESSION_COUNT, 0) + 1;
            preferences.edit().putInt(KEY_SESSION_COUNT, sessions).apply();
            appendActivity(getString(R.string.activity_session_started));
        } else {
            appendActivity(getString(R.string.activity_session_stopped));
        }
        updateDashboard();
    }

    private void toggleBubble() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_needed, Toast.LENGTH_LONG).show();
            requestOverlayPermission();
            return;
        }
        Intent serviceIntent = new Intent(this, FloatingAssistantService.class);
        if (FloatingAssistantService.isRunning) {
            stopService(serviceIntent);
            appendActivity(getString(R.string.activity_bubble_stopped));
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            appendActivity(getString(R.string.activity_bubble_started));
        }
        updateDashboard();
    }

    private void showPermissionCenter() {
        String[] actions = new String[]{
                getString(R.string.permission_overlay),
                getString(R.string.permission_accessibility),
                getString(R.string.permission_notifications),
                getString(R.string.permission_limits)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_center_title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        requestOverlayPermission();
                    } else if (which == 1) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    } else if (which == 2) {
                        requestNotificationPermission();
                    } else {
                        showInfo(getString(R.string.permission_limits), getString(R.string.permission_limits_description));
                    }
                })
                .show();
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        } else {
            Toast.makeText(this, R.string.notifications_ready, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDashboard() {
        status.setText(assistantEnabled ? R.string.status_on : R.string.status_off);
        toggleAssistant.setText(assistantEnabled ? R.string.toggle_off : R.string.toggle_on);
        toggleBubble.setText(FloatingAssistantService.isRunning
                ? R.string.bubble_stop : R.string.bubble_start);
        sessionCount.setText(getString(
                R.string.sessions_count,
                preferences.getInt(KEY_SESSION_COUNT, 0)));
        activityLog.setText(getActivityLog());
        permissionsState.setText(getString(
                R.string.permission_state,
                Settings.canDrawOverlays(this) ? getString(R.string.permission_ready) : getString(R.string.permission_missing),
                isAccessibilityEnabled() ? getString(R.string.permission_ready) : getString(R.string.permission_missing)));
        capabilitySummary.setText(getString(
                R.string.capability_summary,
                AllowedAppsActivity.getAllowedCount(this),
                TrainingActivity.getScenarioCount(this)));
    }

    private boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String serviceId = getPackageName() + "/" + SupervisionAccessibilityService.class.getName();
        return enabledServices != null && enabledServices.contains(serviceId);
    }

    private void appendActivity(String message) {
        String timestamp = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        String previousLog = preferences.getString(KEY_ACTIVITY_LOG, "");
        String updatedLog = timestamp + " — " + message;
        if (!previousLog.isEmpty()) {
            updatedLog += "\n" + previousLog;
        }
        preferences.edit().putString(KEY_ACTIVITY_LOG, keepRecentEntries(updatedLog)).apply();
    }

    private String keepRecentEntries(String log) {
        String[] entries = log.split("\\n");
        StringBuilder result = new StringBuilder();
        int limit = Math.min(entries.length, 6);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append("\n");
            }
            result.append(entries[i]);
        }
        return result.toString();
    }

    private String getActivityLog() {
        String savedLog = preferences.getString(KEY_ACTIVITY_LOG, "");
        return savedLog.isEmpty() ? getString(R.string.no_activity) : savedLog;
    }

    private void copySummary() {
        String summary = getString(
                R.string.summary_template,
                assistantEnabled ? getString(R.string.status_on) : getString(R.string.status_off),
                preferences.getInt(KEY_SESSION_COUNT, 0),
                getActivityLog());
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("LML Control summary", summary));
            Toast.makeText(this, R.string.summary_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_description)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset_confirm, (dialog, which) -> {
                    assistantEnabled = false;
                    preferences.edit().clear().apply();
                    updateDashboard();
                    Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showInfo(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.confirm, null)
                .show();
    }
}
