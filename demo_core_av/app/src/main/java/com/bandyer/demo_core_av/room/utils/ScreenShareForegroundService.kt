package com.bandyer.demo_core_av.room.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bandyer.demo_core_av.R

class ScreenShareForegroundService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action != ACTION_START_FOREGROUND_SERVICE) return super.onStartCommand(intent, flags, startId)
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "122"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Configure the notification channel.
            val channel = NotificationChannel(channelId, "ScreenSharing", NotificationManager.IMPORTANCE_LOW)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.enableLights(true)
            channel.enableVibration(false)
            channel.setBypassDnd(true)
            notificationManager.createNotificationChannel(channel)
        }
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
        notificationBuilder
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_videocam)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle("ScreenSharing")
                .setContentText("ScreenSharing ongoing")
                .setContentInfo("")
        startForeground(123, notificationBuilder.build())
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun clearCurrentNotification() {
        stopForeground(true)
    }

    fun dispose() {
        clearCurrentNotification()
        stopSelf()
    }

    companion object {
        var instance: ScreenShareForegroundService? = null
            private set

        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
    }
}