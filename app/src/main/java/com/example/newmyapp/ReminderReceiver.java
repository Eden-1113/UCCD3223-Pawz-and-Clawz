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
        // 获取传递的宠物名字、疫苗日期和疫苗时间
        String petName = intent.getStringExtra("petName");
        String vaccineDate = intent.getStringExtra("vaccineDate");
        String vaccineTime = intent.getStringExtra("vaccineTime");

        // 创建通知渠道
        createNotificationChannel(context);

        // 构建通知内容
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Reminder: Your Lover Pets has a vaccination")
                .setContentText("Scheduled on " + vaccineDate + " at " + vaccineTime)
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
