package com.lml.control;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TaskReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "com.lml.control.REMINDER";
    public static final String EXTRA_OBJECTIVE = "objective";
    private static final String CHANNEL_ID = "lml_task_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_REMINDER.equals(intent.getAction())) {
            return;
        }
        String objective = intent.getStringExtra(EXTRA_OBJECTIVE);
        createChannel(context);
        Intent openIntent = new Intent(context, TrainingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(context.getString(R.string.reminder_notification_text, objective))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify((int) (System.currentTimeMillis() & 0xFFFFFFF), notification);
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
