package com.example.newmyapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "VACCINE_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取传递的宠物名字
        String petName = intent.getStringExtra("petName");

        // 创建通知渠道（只需一次，通常在 app 启动时做）
        createNotificationChannel(context);

        // 构建通知内容
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Vaccine Reminder for") // 显示宠物名字
                .setContentText("It's almost time for your pets vaccination!")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // 发送通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(1001, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Vaccine Reminder Channel";
            String description = "Channel for vaccine reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
