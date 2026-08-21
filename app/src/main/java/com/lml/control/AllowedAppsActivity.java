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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
                    AppProfileStore.clear(this);
                    updateSummary();
                })
                .show());
        layout.addView(clear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button explore = new Button(this);
        explore.setText(R.string.allowed_apps_explore);
        explore.setOnClickListener(view -> showProfileChooser());
        layout.addView(explore, new LinearLayout.LayoutParams(
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
        selectionSummary.setText(getString(R.string.allowed_apps_count, getAllowedCount(this)) + "\n" + getString(R.string.allowed_apps_profiles_count, AppProfileStore.count(this)));
    }

    private void showProfileChooser() {
        Set<String> allowed = new HashSet<>(preferences.getStringSet(KEY_ALLOWED_PACKAGES, new HashSet<>()));
        List<ResolveInfo> choices = new ArrayList<>();
        for (ResolveInfo info : launchableApps) if (allowed.contains(info.activityInfo.packageName)) choices.add(info);
        if (choices.isEmpty()) {
            Toast.makeText(this, R.string.allowed_apps_explore_need_selection, Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[choices.size()];
        for (int index = 0; index < choices.size(); index++) labels[index] = choices.get(index).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle(R.string.allowed_apps_explore).setItems(labels, (dialog, which) -> showProfileEditor(choices.get(which))).show();
    }

    private void showProfileEditor(ResolveInfo info) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = 28;
        form.setPadding(padding, padding, padding, 0);
        TextView note = new TextView(this);
        note.setText(R.string.allowed_apps_explore_description);
        form.addView(note);
        EditText objective = new EditText(this);
        objective.setHint(R.string.allowed_apps_profile_objective_hint);
        form.addView(objective);
        EditText observations = new EditText(this);
        observations.setHint(R.string.allowed_apps_profile_notes_hint);
        observations.setMinLines(3);
        form.addView(observations);
        String label = info.loadLabel(getPackageManager()).toString();
        String packageName = info.activityInfo.packageName;
        new AlertDialog.Builder(this).setTitle(getString(R.string.allowed_apps_profile_title, label)).setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    AppProfileStore.save(this, packageName, label, objective.getText().toString().trim(), observations.getText().toString().trim());
                    updateSummary();
                    Toast.makeText(this, R.string.allowed_apps_profile_saved, Toast.LENGTH_SHORT).show();
                }).show();
    }

    public static int getAllowedCount(android.content.Context context) {
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, MODE_PRIVATE);
        return preferences.getStringSet(KEY_ALLOWED_PACKAGES, new HashSet<>()).size();
    }
}
