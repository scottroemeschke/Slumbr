package dev.ashera.slumbr.android.service

import android.app.Notification

interface ForegroundHost {
    fun promoteForeground(notification: Notification)

    fun demoteForeground()
}
