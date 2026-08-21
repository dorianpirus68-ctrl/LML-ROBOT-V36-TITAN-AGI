package com.lml.control;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/** Atomic, local ticket gate for a canonical plan. It does not grant critical execution. */
public final class DualApprovalGate {
    private static final String PREFERENCES = "lml_agent_approval";
    private static final String KEY_HASH = "plan_hash";
    private static final String KEY_RISK = "risk";
    private static final String KEY_NONCE = "nonce";
    private static final String KEY_EXPIRES = "expires_at";
    private static final String KEY_USED = "used";
    private static final long MIN_TTL = 15_000L;
    private static final long MAX_TTL = 90_000L;

    private DualApprovalGate() { }

    public static long approveStageOne(Context context, AgentPolicy.Plan plan, long requestedTtlMillis) {
        long ttl = Math.max(MIN_TTL, Math.min(MAX_TTL, requestedTtlMillis));
        long expires = System.currentTimeMillis() + ttl;
        String nonce = UUID.randomUUID().toString();
        preferences(context).edit().putString(KEY_HASH, plan.hash).putString(KEY_RISK, plan.risk.name()).putString(KEY_NONCE, nonce).putLong(KEY_EXPIRES, expires).putBoolean(KEY_USED, false).commit();
        NexusAuditLog.record(context, "ticket_approved", plan.actionType.name() + " / " + nonce.substring(0, 8));
        return expires;
    }

    public static synchronized boolean consumeStageTwo(Context context, AgentPolicy.Plan plan) {
        SharedPreferences preferences = preferences(context);
        boolean valid = plan.hash.equals(preferences.getString(KEY_HASH, ""))
                && plan.risk.name().equals(preferences.getString(KEY_RISK, ""))
                && !preferences.getBoolean(KEY_USED, true)
                && System.currentTimeMillis() <= preferences.getLong(KEY_EXPIRES, 0L)
                && !preferences.getString(KEY_NONCE, "").isEmpty();
        if (valid) {
            preferences.edit().putBoolean(KEY_USED, true).remove(KEY_HASH).remove(KEY_RISK).remove(KEY_NONCE).remove(KEY_EXPIRES).commit();
            NexusAuditLog.record(context, "ticket_consumed", plan.actionType.name());
        } else NexusAuditLog.record(context, "ticket_rejected", plan.actionType.name());
        return valid;
    }

    private static SharedPreferences preferences(Context context) { return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE); }
}
