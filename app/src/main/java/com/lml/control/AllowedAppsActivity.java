package com.lml.control;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllowedAppsActivity extends Activity {
    public static final String KEY_ALLOWED_PACKAGES = "allowed_packages";
    private SharedPreferences preferences;
    private TextView selectionSummary;
    private List<ResolveInfo> launchableApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText(getString(R.string.allowed_apps_title));
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.addView(title);

        TextView description = new TextView(this);
        description.setText(getString(R.string.allowed_apps_description));
        description.setPadding(0, 20, 0, 20);
        layout.addView(description);

        selectionSummary = new TextView(this);
        selectionSummary.setTextSize(16);
        layout.addView(selectionSummary);

        Button choose = new Button(this);
        choose.setText(R.string.allowed_apps_choose);
        choose.setOnClickListener(view -> showAppChooser());
        layout.addView(choose, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button clear = new Button(this);
        clear.setText(R.string.allowed_apps_clear);
        clear.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.allowed_apps_clear)
                .setMessage(R.string.allowed_apps_clear_description)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    preferences.edit().remove(KEY_ALLOWED_PACKAGES).apply();
                    updateSummary();
                })
                .show());
        layout.addView(clear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(layout);
        loadLaunchableApps();
        updateSummary();
    }

    private void loadLaunchableApps() {
        Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchableApps = getPackageManager().queryIntentActivities(queryIntent, 0);
        Collections.sort(launchableApps, Comparator.comparing(
                info -> info.loadLabel(getPackageManager()).toString(), String.CASE_INSENSITIVE_ORDER));
    }

    private void showAppChooser() {
        if (launchableApps.isEmpty()) {
            loadLaunchableApps();
        }
        String[] labels = new String[launchableApps.size()];
        boolean[] checked = new boolean[launchableApps.size()];
        Set<String> selected = new HashSet<>(preferences.getStringSet(KEY_ALLOWED_PACKAGES, new HashSet<>()));
        for (int i = 0; i < launchableApps.size(); i++) {
            ResolveInfo info = launchableApps.get(i);
            labels[i] = info.loadLabel(getPackageManager()).toString();
            checked[i] = selected.contains(info.activityInfo.packageName);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.allowed_apps_choose)
                .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    Set<String> updatedSelection = new HashSet<>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            updatedSelection.add(launchableApps.get(i).activityInfo.packageName);
                        }
                    }
                    preferences.edit().putStringSet(KEY_ALLOWED_PACKAGES, updatedSelection).apply();
                    updateSummary();
                })
                .show();
    }

    private void updateSummary() {
        selectionSummary.setText(getString(R.string.allowed_apps_count, getAllowedCount(this)));
    }

    public static int getAllowedCount(Activity activity) {
        SharedPreferences preferences = activity.getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        return preferences.getStringSet(KEY_ALLOWED_PACKAGES, new HashSet<>()).size();
    }
}
