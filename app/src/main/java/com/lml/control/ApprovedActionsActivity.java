package com.lml.control;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApprovedActionsActivity extends Activity {
    private SharedPreferences preferences;
    private TextView activityLog;
    private final List<String> allowedPackages = new ArrayList<>();
    private final List<String> allowedLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText(R.string.approved_actions_title);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(title);

        TextView description = new TextView(this);
        description.setText(R.string.approved_actions_description);
        description.setPadding(0, 20, 0, 20);
        layout.addView(description);

        Button start = new Button(this);
        start.setText(R.string.approved_actions_choose);
        start.setOnClickListener(view -> chooseApplication());
        layout.addView(start, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button accessibilitySettings = new Button(this);
        accessibilitySettings.setText(R.string.approved_actions_open_supervision);
        accessibilitySettings.setOnClickListener(view -> startActivity(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(accessibilitySettings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView logLabel = new TextView(this);
        logLabel.setText(R.string.approved_actions_log_title);
        logLabel.setTextSize(18);
        logLabel.setPadding(0, 28, 0, 8);
        layout.addView(logLabel);

        activityLog = new TextView(this);
        activityLog.setTextSize(14);
        layout.addView(activityLog);

        setContentView(layout);
        refreshLog();
    }

    private void chooseApplication() {
        loadAllowedApplications();
        if (allowedPackages.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.approved_actions_no_apps_title)
                    .setMessage(R.string.approved_actions_no_apps_description)
                    .setPositiveButton(R.string.confirm, null)
                    .show();
            return;
        }
        String[] labels = allowedLabels.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.approved_actions_choose)
                .setItems(labels, (dialog, which) -> chooseNavigation(
                        allowedPackages.get(which), allowedLabels.get(which)))
                .show();
    }

    private void chooseNavigation(String packageName, String label) {
        String[] actionLabels = new String[]{
                getString(R.string.approved_actions_open),
                getString(R.string.approved_actions_back),
                getString(R.string.approved_actions_scroll)
        };
        String[] actionTypes = new String[]{
                PendingApprovedAction.ACTION_OPEN,
                PendingApprovedAction.ACTION_BACK,
                PendingApprovedAction.ACTION_SCROLL_DOWN
        };
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.approved_actions_select_action, label))
                .setItems(actionLabels, (dialog, which) -> confirmAction(
                        packageName, label, actionTypes[which], actionLabels[which]))
                .show();
    }

    private void confirmAction(String packageName, String label, String actionType, String actionLabel) {
        String message = getString(R.string.approved_actions_confirmation_message, actionLabel, label);
        new AlertDialog.Builder(this)
                .setTitle(R.string.approved_actions_confirmation_title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.approved_actions_start, (dialog, which) -> executeApprovedAction(
                        packageName, label, actionType, actionLabel))
                .show();
    }

    private void executeApprovedAction(String packageName, String label, String actionType, String actionLabel) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            Toast.makeText(this, R.string.approved_actions_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        if (!PendingApprovedAction.ACTION_OPEN.equals(actionType) && !SupervisionAccessibilityService.isAvailable()) {
            Toast.makeText(this, R.string.approved_actions_supervision_needed, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        if (PendingApprovedAction.ACTION_OPEN.equals(actionType)) {
            ActionExecutionLog.append(this, getString(R.string.approved_actions_log_entry, actionLabel, label));
        } else {
            PendingApprovedAction.queue(this, packageName, actionType);
            ActionExecutionLog.append(this, getString(R.string.approved_actions_queued_entry, actionLabel, label));
        }
        refreshLog();
        startActivity(launchIntent);
    }

    private void loadAllowedApplications() {
        allowedPackages.clear();
        allowedLabels.clear();
        Set<String> selected = preferences.getStringSet(
                AllowedAppsActivity.KEY_ALLOWED_PACKAGES, new HashSet<>());
        for (String packageName : selected) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                CharSequence label = packageName;
                List<ResolveInfo> infos = getPackageManager().queryIntentActivities(launchIntent, 0);
                if (!infos.isEmpty()) {
                    label = infos.get(0).loadLabel(getPackageManager());
                }
                allowedPackages.add(packageName);
                allowedLabels.add(label.toString());
            }
        }
    }

    private void refreshLog() {
        String log = ActionExecutionLog.read(this);
        activityLog.setText(log.isEmpty() ? getString(R.string.approved_actions_no_log) : log);
    }
}
