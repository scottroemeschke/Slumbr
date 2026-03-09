package dev.ashera.slumbr.android.system

import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ashera.slumbr.core.system.DndStateProvider
import javax.inject.Inject

class AndroidDndStateProvider
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : DndStateProvider {
        override fun isTotalSilence(): Boolean {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        }
    }
