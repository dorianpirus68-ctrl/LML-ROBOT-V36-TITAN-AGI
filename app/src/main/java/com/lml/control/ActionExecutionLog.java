package com.lml.control;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DateFormat;
import java.util.Date;

public final class ActionExecutionLog {
    private static final String KEY_EXECUTION_LOG = "approved_action_execution_log";

    private ActionExecutionLog() {
    }

    public static void append(Context context, String entry) {
        SharedPreferences preferences = context.getSharedPreferences(
                MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        String timestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date());
        String existing = preferences.getString(KEY_EXECUTION_LOG, "");
        String updated = timestamp + " — " + entry;
        if (!existing.isEmpty()) {
            updated += "\n" + existing;
        }
        preferences.edit().putString(KEY_EXECUTION_LOG, keepLatest(updated)).apply();
    }

    public static String read(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_EXECUTION_LOG, "");
    }

    private static String keepLatest(String value) {
        String[] lines = value.split("\\n");
        StringBuilder result = new StringBuilder();
        int limit = Math.min(lines.length, 8);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                result.append("\n");
            }
            result.append(lines[i]);
        }
        return result.toString();
    }
}
