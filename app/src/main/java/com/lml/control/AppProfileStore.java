package com.lml.control;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** App profiles are explicit user notes. They never inspect an app or collect its screen contents. */
public final class AppProfileStore {
    private static final String STORE = "nexus_app_profiles";
    private static final String KEY_PROFILES = "profiles_v1";
    private static final int LIMIT = 80;

    public static final class Profile {
        public final String packageName;
        public final String label;
        public final String objective;
        public final String notes;
        public final long updatedAt;

        Profile(String packageName, String label, String objective, String notes, long updatedAt) {
            this.packageName = packageName;
            this.label = label;
            this.objective = objective;
            this.notes = notes;
            this.updatedAt = updatedAt;
        }
    }

    private AppProfileStore() { }

    public static synchronized void save(Context context, String packageName, String label, String objective, String notes) {
        JSONArray previous = read(context);
        JSONArray next = new JSONArray();
        Profile profile = new Profile(packageName, label, objective, notes, System.currentTimeMillis());
        next.put(toJson(profile));
        for (int index = 0; index < previous.length() && next.length() < LIMIT; index++) {
            try {
                JSONObject candidate = previous.getJSONObject(index);
                if (!packageName.equals(candidate.optString("packageName"))) next.put(candidate);
            } catch (Exception ignored) { }
        }
        preferences(context).edit().putString(KEY_PROFILES, next.toString()).commit();
        NexusAuditLog.record(context, "app_profile_saved", label + " / " + (objective.isEmpty() ? "profil" : objective));
    }

    public static synchronized int count(Context context) { return read(context).length(); }

    public static synchronized List<Profile> all(Context context) {
        List<Profile> result = new ArrayList<>();
        JSONArray items = read(context);
        for (int index = 0; index < items.length(); index++) {
            try {
                JSONObject item = items.getJSONObject(index);
                result.add(new Profile(item.optString("packageName"), item.optString("label"), item.optString("objective"), item.optString("notes"), item.optLong("updatedAt")));
            } catch (Exception ignored) { }
        }
        return result;
    }

    public static synchronized void clear(Context context) {
        preferences(context).edit().remove(KEY_PROFILES).commit();
        NexusAuditLog.record(context, "app_profiles_cleared", "local");
    }

    private static JSONObject toJson(Profile profile) {
        JSONObject json = new JSONObject();
        try {
            json.put("packageName", profile.packageName); json.put("label", profile.label); json.put("objective", profile.objective); json.put("notes", profile.notes); json.put("updatedAt", profile.updatedAt);
        } catch (Exception ignored) { }
        return json;
    }

    private static JSONArray read(Context context) {
        try { return new JSONArray(preferences(context).getString(KEY_PROFILES, "[]")); } catch (Exception ignored) { return new JSONArray(); }
    }

    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(STORE, Context.MODE_PRIVATE); }
}
