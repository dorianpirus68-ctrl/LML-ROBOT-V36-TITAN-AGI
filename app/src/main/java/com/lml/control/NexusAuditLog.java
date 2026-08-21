package com.lml.control;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Local-only audit trail. It contains no screen content and no credentials. */
public final class NexusAuditLog {
    private static final String STORE = "nexus_audit";
    private static final String KEY_ENTRIES = "entries_v1";
    private static final int LIMIT = 50;

    public static final class Entry {
        public final long timestamp;
        public final String category;
        public final String detail;

        Entry(long timestamp, String category, String detail) {
            this.timestamp = timestamp;
            this.category = category;
            this.detail = detail;
        }
    }

    private NexusAuditLog() { }

    public static synchronized void record(Context context, String category, String detail) {
        try {
            JSONArray previous = readArray(context);
            JSONArray next = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("at", System.currentTimeMillis());
            item.put("category", category);
            item.put("detail", detail);
            next.put(item);
            for (int index = 0; index < previous.length() && index < LIMIT - 1; index++) next.put(previous.getJSONObject(index));
            preferences(context).edit().putString(KEY_ENTRIES, next.toString()).commit();
        } catch (Exception ignored) {
            // Audit failures must never turn into an execution permission.
        }
    }

    public static synchronized List<Entry> recent(Context context) {
        List<Entry> result = new ArrayList<>();
        try {
            JSONArray items = readArray(context);
            for (int index = 0; index < items.length(); index++) {
                JSONObject item = items.getJSONObject(index);
                result.add(new Entry(item.optLong("at"), item.optString("category"), item.optString("detail")));
            }
        } catch (Exception ignored) { }
        return result;
    }

    public static synchronized void clear(Context context) {
        preferences(context).edit().remove(KEY_ENTRIES).commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
    }

    private static JSONArray readArray(Context context) {
        String value = preferences(context).getString(KEY_ENTRIES, "[]");
        try { return new JSONArray(value); } catch (Exception ignored) { return new JSONArray(); }
    }
}
