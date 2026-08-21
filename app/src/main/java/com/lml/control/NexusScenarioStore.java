package com.lml.control;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Structured, local scenario memory; scenarios are instructions for a human, not autonomous workflows. */
public final class NexusScenarioStore {
    private static final String STORE = "nexus_scenarios";
    private static final String KEY_SCENARIOS = "scenarios_v1";
    private static final String KEY_MIGRATED = "legacy_migrated";
    private static final int LIMIT = 40;

    public static final class Scenario {
        public final String id;
        public final String application;
        public final String objective;
        public final String steps;
        public final long createdAt;

        Scenario(String id, String application, String objective, String steps, long createdAt) {
            this.id = id;
            this.application = application;
            this.objective = objective;
            this.steps = steps;
            this.createdAt = createdAt;
        }
    }

    private NexusScenarioStore() { }

    public static synchronized Scenario add(Context context, String application, String objective, String steps) {
        migrateLegacyIfNeeded(context);
        Scenario scenario = new Scenario(UUID.randomUUID().toString(), application.trim(), objective.trim(), steps.trim(), System.currentTimeMillis());
        JSONArray next = new JSONArray();
        next.put(toJson(scenario));
        JSONArray previous = read(context);
        for (int index = 0; index < previous.length() && index < LIMIT - 1; index++) {
            try { next.put(previous.getJSONObject(index)); } catch (Exception ignored) { }
        }
        preferences(context).edit().putString(KEY_SCENARIOS, next.toString()).commit();
        NexusAuditLog.record(context, "scenario_saved", scenario.application + " / " + scenario.objective);
        return scenario;
    }

    public static synchronized int count(Context context) {
        migrateLegacyIfNeeded(context);
        return read(context).length();
    }

    public static synchronized List<Scenario> recent(Context context) {
        migrateLegacyIfNeeded(context);
        List<Scenario> result = new ArrayList<>();
        JSONArray items = read(context);
        for (int index = 0; index < items.length(); index++) {
            try {
                JSONObject item = items.getJSONObject(index);
                result.add(new Scenario(item.optString("id"), item.optString("application"), item.optString("objective"), item.optString("steps"), item.optLong("createdAt")));
            } catch (Exception ignored) { }
        }
        return result;
    }

    private static void migrateLegacyIfNeeded(Context context) {
        if (preferences(context).getBoolean(KEY_MIGRATED, false)) return;
        JSONArray next = read(context);
        String legacy = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(TrainingActivity.KEY_SCENARIOS, "");
        if (next.length() == 0 && legacy != null && !legacy.trim().isEmpty()) {
            String[] entries = legacy.split("\\n\\n");
            for (String entry : entries) {
                String[] parts = entry.split(" \\| ", 3);
                String application = parts.length > 1 ? parts[1] : "Scénario importé";
                String objective = parts.length > 2 ? parts[2].split("\\n", 2)[0] : entry;
                String steps = parts.length > 2 && parts[2].contains("\n") ? parts[2].substring(parts[2].indexOf('\n') + 1) : "À relire et valider manuellement.";
                next.put(toJson(new Scenario(UUID.randomUUID().toString(), application, objective, steps, System.currentTimeMillis())));
            }
        }
        preferences(context).edit().putString(KEY_SCENARIOS, next.toString()).putBoolean(KEY_MIGRATED, true).commit();
    }

    private static JSONObject toJson(Scenario scenario) {
        JSONObject value = new JSONObject();
        try {
            value.put("id", scenario.id); value.put("application", scenario.application); value.put("objective", scenario.objective); value.put("steps", scenario.steps); value.put("createdAt", scenario.createdAt);
        } catch (Exception ignored) { }
        return value;
    }

    private static JSONArray read(Context context) {
        try { return new JSONArray(preferences(context).getString(KEY_SCENARIOS, "[]")); } catch (Exception ignored) { return new JSONArray(); }
    }

    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(STORE, Context.MODE_PRIVATE); }
}
