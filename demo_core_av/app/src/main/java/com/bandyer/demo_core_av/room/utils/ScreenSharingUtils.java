package com.bandyer.demo_core_av.room.utils;

import android.content.Context;
import android.content.Intent;

import static com.bandyer.demo_core_av.room.utils.ScreenShareForegroundService.ACTION_START_FOREGROUND_SERVICE;

/**
 * @author kristiyan
 */
public class ScreenSharingUtils {

    public static void showScreenShareNotification(Context context) {
        Intent intent = new Intent(context, ScreenShareForegroundService.class);
        intent.setAction(ACTION_START_FOREGROUND_SERVICE);
        context.startService(intent);

    }

    public static void hideScreenShareNotification() {
        if (ScreenShareForegroundService.getInstance() == null) return;
        ScreenShareForegroundService.getInstance().dispose();
    }

}
