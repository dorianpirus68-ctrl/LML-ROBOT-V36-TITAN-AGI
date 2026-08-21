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
    private boolean assistantEnabled;
    private TextView status, sessionCount, activityLog, permissionsState, capabilitySummary, modelState;
    private Button toggleAssistant, toggleBubble;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        status = findViewById(R.id.status); sessionCount = findViewById(R.id.session_count); activityLog = findViewById(R.id.activity_log);
        permissionsState = findViewById(R.id.permissions_state); capabilitySummary = findViewById(R.id.capability_summary); modelState = findViewById(R.id.model_state);
        toggleAssistant = findViewById(R.id.toggle_assistant); toggleBubble = findViewById(R.id.toggle_bubble);
        toggleAssistant.setOnClickListener(v -> toggleAssistant()); toggleBubble.setOnClickListener(v -> toggleBubble());
        findViewById(R.id.permission_center).setOnClickListener(v -> showPermissionCenter());
        findViewById(R.id.allowed_apps).setOnClickListener(v -> startActivity(new Intent(this, AllowedAppsActivity.class)));
        findViewById(R.id.training).setOnClickListener(v -> startActivity(new Intent(this, TrainingActivity.class)));
        findViewById(R.id.chat).setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        findViewById(R.id.approved_actions).setOnClickListener(v -> startActivity(new Intent(this, ApprovedActionsActivity.class)));
        findViewById(R.id.model_manager).setOnClickListener(v -> startActivity(new Intent(this, ModelManagerActivity.class)));
        findViewById(R.id.mission_control).setOnClickListener(v -> startActivity(new Intent(this, MissionControlActivity.class)));
        findViewById(R.id.assistant_info).setOnClickListener(v -> showInfo(getString(R.string.assistant_title), getString(R.string.assistant_description)));
        findViewById(R.id.history_info).setOnClickListener(v -> showInfo(getString(R.string.history_title), getActivityLog()));
        findViewById(R.id.copy_summary).setOnClickListener(v -> copySummary()); findViewById(R.id.reset_data).setOnClickListener(v -> confirmReset());
        findViewById(R.id.safety_info).setOnClickListener(v -> showInfo(getString(R.string.safety_title), getString(R.string.safety_description)));
        updateDashboard();
    }
    @Override protected void onResume() { super.onResume(); if (preferences != null) updateDashboard(); }
    private void toggleAssistant() { assistantEnabled = !assistantEnabled; if (assistantEnabled) { preferences.edit().putInt(KEY_SESSION_COUNT, preferences.getInt(KEY_SESSION_COUNT, 0) + 1).apply(); appendActivity(getString(R.string.activity_session_started)); } else appendActivity(getString(R.string.activity_session_stopped)); updateDashboard(); }
    private void toggleBubble() { if (!Settings.canDrawOverlays(this)) { Toast.makeText(this, R.string.overlay_needed, Toast.LENGTH_LONG).show(); requestOverlayPermission(); return; } Intent intent = new Intent(this, FloatingAssistantService.class); if (FloatingAssistantService.isRunning) { stopService(intent); appendActivity(getString(R.string.activity_bubble_stopped)); } else { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent); appendActivity(getString(R.string.activity_bubble_started)); } updateDashboard(); }
    private void showPermissionCenter() { String[] items = {getString(R.string.permission_overlay), getString(R.string.permission_accessibility), getString(R.string.permission_notifications), getString(R.string.permission_limits)}; new AlertDialog.Builder(this).setTitle(R.string.permission_center_title).setItems(items, (d, n) -> { if (n == 0) requestOverlayPermission(); else if (n == 1) startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); else if (n == 2) requestNotificationPermission(); else showInfo(getString(R.string.permission_limits), getString(R.string.permission_limits_description)); }).show(); }
    private void requestOverlayPermission() { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
    private void requestNotificationPermission() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST); else Toast.makeText(this, R.string.notifications_ready, Toast.LENGTH_SHORT).show(); }
    private void updateDashboard() { status.setText(assistantEnabled ? R.string.status_on : R.string.status_off); toggleAssistant.setText(assistantEnabled ? R.string.toggle_off : R.string.toggle_on); toggleBubble.setText(FloatingAssistantService.isRunning ? R.string.bubble_stop : R.string.bubble_start); sessionCount.setText(getString(R.string.sessions_count, preferences.getInt(KEY_SESSION_COUNT, 0))); activityLog.setText(getActivityLog()); permissionsState.setText(getString(R.string.permission_state, Settings.canDrawOverlays(this) ? getString(R.string.permission_ready) : getString(R.string.permission_missing), isAccessibilityEnabled() ? getString(R.string.permission_ready) : getString(R.string.permission_missing))); capabilitySummary.setText(getString(R.string.capability_summary, AllowedAppsActivity.getAllowedCount(this), TrainingActivity.getScenarioCount(this))); modelState.setText(ModelDownloadManager.isInstalled(this) ? R.string.model_ready : R.string.model_missing); }
    private boolean isAccessibilityEnabled() { String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES); String id = getPackageName() + "/" + SupervisionAccessibilityService.class.getName(); return enabled != null && enabled.contains(id); }
    private void appendActivity(String message) { String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()); String previous = preferences.getString(KEY_ACTIVITY_LOG, ""); String log = time + " — " + message + (previous.isEmpty() ? "" : "\n" + previous); preferences.edit().putString(KEY_ACTIVITY_LOG, keepRecentEntries(log)).apply(); }
    private String keepRecentEntries(String log) { String[] entries = log.split("\\n"); StringBuilder out = new StringBuilder(); for (int i = 0; i < Math.min(entries.length, 6); i++) { if (i > 0) out.append("\n"); out.append(entries[i]); } return out.toString(); }
    private String getActivityLog() { String log = preferences.getString(KEY_ACTIVITY_LOG, ""); return log.isEmpty() ? getString(R.string.no_activity) : log; }
    private void copySummary() { String summary = getString(R.string.summary_template, assistantEnabled ? getString(R.string.status_on) : getString(R.string.status_off), preferences.getInt(KEY_SESSION_COUNT, 0), getActivityLog()); ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); if (clipboard != null) { clipboard.setPrimaryClip(ClipData.newPlainText("LML Control summary", summary)); Toast.makeText(this, R.string.summary_copied, Toast.LENGTH_SHORT).show(); } }
    private void confirmReset() { new AlertDialog.Builder(this).setTitle(R.string.reset_title).setMessage(R.string.reset_description).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.reset_confirm, (d, w) -> { assistantEnabled = false; preferences.edit().clear().apply(); updateDashboard(); Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show(); }).show(); }
    private void showInfo(String title, String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(R.string.confirm, null).show(); }
}
