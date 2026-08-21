package com.lml.control;

import android.content.Context;
import android.content.SharedPreferences;

public final class PendingApprovedAction {
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_BACK = "back";
    public static final String ACTION_SCROLL_DOWN = "scroll_down";

    private static final String KEY_TARGET_PACKAGE = "pending_action_target_package";
    private static final String KEY_ACTION_TYPE = "pending_action_type";
    private static final String KEY_EXPIRY = "pending_action_expiry";
    private static final long EXPIRY_MILLIS = 45_000L;

    private PendingApprovedAction() {
    }

    public static void queue(Context context, String targetPackage, String actionType) {
        preferences(context).edit()
                .putString(KEY_TARGET_PACKAGE, targetPackage)
                .putString(KEY_ACTION_TYPE, actionType)
                .putLong(KEY_EXPIRY, System.currentTimeMillis() + EXPIRY_MILLIS)
                .apply();
    }

    public static String takeIfReady(Context context, String foregroundPackage) {
        SharedPreferences preferences = preferences(context);
        long expiry = preferences.getLong(KEY_EXPIRY, 0L);
        String targetPackage = preferences.getString(KEY_TARGET_PACKAGE, "");
        String actionType = preferences.getString(KEY_ACTION_TYPE, "");
        if (expiry < System.currentTimeMillis()) {
            clear(context);
            return "";
        }
        if (!foregroundPackage.equals(targetPackage) || actionType.isEmpty()) {
            return "";
        }
        clear(context);
        return actionType;
    }

    public static void clear(Context context) {
        preferences(context).edit()
                .remove(KEY_TARGET_PACKAGE)
                .remove(KEY_ACTION_TYPE)
                .remove(KEY_EXPIRY)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}
