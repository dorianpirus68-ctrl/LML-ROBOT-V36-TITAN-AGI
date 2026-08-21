package com.lml.control;

import android.content.Context;
import android.provider.Settings;

/** Capability registry for the local, supervised subset of the LML Omega roadmap. */
public final class NexusCore {
    public enum Capability { LOCAL_DIALOGUE, SCENARIO_MEMORY, GUIDED_APP_PROFILES, APPROVED_NAVIGATION, ACCESSIBILITY_GATE, LOCAL_MODEL, CRITICAL_EXECUTION }

    private NexusCore() { }

    public static boolean isAvailable(Context context, Capability capability) {
        switch (capability) {
            case LOCAL_DIALOGUE: return true;
            case SCENARIO_MEMORY: return NexusScenarioStore.count(context) > 0;
            case GUIDED_APP_PROFILES: return AppProfileStore.count(context) > 0;
            case APPROVED_NAVIGATION: return AllowedAppsActivity.getAllowedCount(context) > 0;
            case ACCESSIBILITY_GATE: return isAccessibilityEnabled(context);
            case LOCAL_MODEL: return ModelDownloadManager.isInstalled(context);
            case CRITICAL_EXECUTION: return false;
            default: return false;
        }
    }

    public static String summary(Context context) {
        int available = 0;
        for (Capability capability : Capability.values()) if (isAvailable(context, capability)) available++;
        return available + "/" + Capability.values().length + " capacités prêtes — actions critiques bloquées";
    }

    private static boolean isAccessibilityEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String id = context.getPackageName() + "/" + SupervisionAccessibilityService.class.getName();
        return enabled != null && enabled.contains(id);
    }
}
