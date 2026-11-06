package com.fiesta.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class MediaListenerService extends NotificationListenerService {
    public static void requestRescan(Context context) {
        MediaHub.refresh(context);
    }

    public static boolean enabled(Context context) {
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (flat == null || flat.isEmpty()) {
            return false;
        }
        ComponentName me = new ComponentName(context, MediaListenerService.class);
        return flat.contains(me.flattenToString()) || flat.contains(me.flattenToShortString());
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        MediaHub.refresh(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        MediaHub.refresh(this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        MediaHub.refresh(this);
    }
}
