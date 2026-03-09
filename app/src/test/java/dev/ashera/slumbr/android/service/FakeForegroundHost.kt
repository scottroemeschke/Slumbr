package dev.ashera.slumbr.android.service

import android.app.Notification

class FakeForegroundHost : ForegroundHost {
    var promotedCount = 0
        private set
    var lastNotification: Notification? = null
        private set
    var demotedCount = 0
        private set

    override fun promoteForeground(notification: Notification) {
        promotedCount++
        lastNotification = notification
    }

    override fun demoteForeground() {
        demotedCount++
    }
}
