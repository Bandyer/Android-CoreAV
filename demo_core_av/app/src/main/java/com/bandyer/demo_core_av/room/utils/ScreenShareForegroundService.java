package com.bandyer.demo_core_av.room.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bandyer.demo_core_av.R;

public class ScreenShareForegroundService extends Service {

    private static ScreenShareForegroundService instance = null;

    public static ScreenShareForegroundService getInstance() {
        return instance;
    }

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.getAction().equals(ACTION_START_FOREGROUND_SERVICE))
            return super.onStartCommand(intent, flags, startId);

        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "122";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Configure the notification channel.
            NotificationChannel channel = new NotificationChannel(channelId, "ScreenSharing", NotificationManager.IMPORTANCE_LOW);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.enableLights(true);
            channel.enableVibration(false);
            channel.setBypassDnd(true);
            notificationManager.createNotificationChannel(channel);

        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId);

        notificationBuilder
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_videocam)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("ScreenSharing")
                .setContentText("ScreenSharing ongoing")
                .setContentInfo("");

        startForeground(123, notificationBuilder.build());

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    void clearCurrentNotification() {
        stopForeground(true);
    }

    public void dispose() {
        clearCurrentNotification();
        stopSelf();
    }
}