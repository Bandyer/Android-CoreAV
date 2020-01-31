package com.bandyer.demo_core_av.room.utils

import android.content.Context
import android.content.Intent

/**
 * @author kristiyan
 */
object ScreenSharingUtils {
    fun showScreenShareNotification(context: Context) {
        val intent = Intent(context, ScreenShareForegroundService::class.java)
        intent.action = ScreenShareForegroundService.ACTION_START_FOREGROUND_SERVICE
        context.startService(intent)
    }

    fun hideScreenShareNotification() {
        ScreenShareForegroundService.instance?.dispose()
    }
}