package com.lml.control;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Service de navigation supervisée. Il ne lit pas le contenu des fenêtres.
 * Il exécute uniquement un retour ou un défilement, après une validation
 * de l’utilisateur, dans une application explicitement autorisée.
 */
public class SupervisionAccessibilityService extends AccessibilityService {
    private static SupervisionAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) {
            return;
        }
        String foregroundPackage = event.getPackageName().toString();
        if (!isAllowedPackage(this, foregroundPackage)) {
            return;
        }
        String actionType = PendingApprovedAction.takeIfReady(this, foregroundPackage);
        if (actionType.isEmpty()) {
            return;
        }
        boolean completed = executeNavigation(actionType);
        String result = completed
                ? getString(R.string.approved_actions_navigation_completed, describeAction(actionType))
                : getString(R.string.approved_actions_navigation_failed, describeAction(actionType));
        ActionExecutionLog.append(this, result);
    }

    private boolean executeNavigation(String actionType) {
        if (PendingApprovedAction.ACTION_BACK.equals(actionType)) {
            return performGlobalAction(GLOBAL_ACTION_BACK);
        }
        if (PendingApprovedAction.ACTION_SCROLL_DOWN.equals(actionType)) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            Path path = new Path();
            path.moveTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.72f);
            path.lineTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.28f);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 350))
                    .build();
            return dispatchGesture(gesture, null, null);
        }
        return false;
    }

    private String describeAction(String actionType) {
        if (PendingApprovedAction.ACTION_BACK.equals(actionType)) {
            return getString(R.string.approved_actions_back);
        }
        if (PendingApprovedAction.ACTION_SCROLL_DOWN.equals(actionType)) {
            return getString(R.string.approved_actions_scroll);
        }
        return actionType;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static boolean isAllowedPackage(Context context, String packageName) {
        SharedPreferences preferences = context.getSharedPreferences(
                MainActivity.PREFERENCES_NAME, Context.MODE_PRIVATE);
        Set<String> allowed = preferences.getStringSet(
                AllowedAppsActivity.KEY_ALLOWED_PACKAGES, new HashSet<>());
        return allowed.contains(packageName);
    }

    @Override
    public void onInterrupt() {
        PendingApprovedAction.clear(this);
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }
}
