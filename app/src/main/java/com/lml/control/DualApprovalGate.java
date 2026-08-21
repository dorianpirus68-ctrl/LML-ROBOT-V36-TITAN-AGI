package com.lml.control;

import android.content.Context;
import android.content.SharedPreferences;

public final class DualApprovalGate {
    private static final String PREFERENCES = "lml_agent_approval";
    private static final String KEY_HASH = "plan_hash";
    private static final String KEY_EXPIRES = "expires_at";
    private static final String KEY_USED = "used";

    private DualApprovalGate() { }

    public static long approveStageOne(Context context, AgentPolicy.Plan plan, long ttlMillis) {
        long expires = System.currentTimeMillis() + ttlMillis;
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString(KEY_HASH, plan.hash)
                .putLong(KEY_EXPIRES, expires)
                .putBoolean(KEY_USED, false)
                .apply();
        return expires;
    }

    public static boolean consumeStageTwo(Context context, AgentPolicy.Plan plan) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        boolean valid = plan.hash.equals(preferences.getString(KEY_HASH, ""))
                && !preferences.getBoolean(KEY_USED, true)
                && System.currentTimeMillis() <= preferences.getLong(KEY_EXPIRES, 0L);
        if (valid) preferences.edit().putBoolean(KEY_USED, true).apply();
        return valid;
    }
}
