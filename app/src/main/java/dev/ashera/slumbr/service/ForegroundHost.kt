package dev.ashera.slumbr.service

import android.app.Notification

interface ForegroundHost {
    fun promoteForeground(notification: Notification)

    fun demoteForeground()
}
