package btm.m.todaywallpaper.service

import android.service.notification.NotificationListenerService

/** Grants MediaSessionManager access without reading or retaining notification contents. */
class ClockMediaNotificationListenerService : NotificationListenerService()