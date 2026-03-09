package dev.ashera.slumbr.android.service

import dev.ashera.slumbr.core.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class ServicePlaybackObserver
    @Inject
    constructor(
        private val playbackController: PlaybackController,
        private val notifier: PlaybackNotifier,
        private val mediaSessionController: MediaSessionController,
    ) {
        private var isForeground = false

        fun observe(
            scope: CoroutineScope,
            host: ForegroundHost,
        ) {
            scope.launch {
                playbackController.playbackState
                    .map { it.currentNoise to it.isPlaying }
                    .distinctUntilChanged()
                    .collect { (noiseType, isPlaying) ->
                        when {
                            isPlaying && noiseType != null -> {
                                mediaSessionController.updatePlaying(noiseType)
                                if (!isForeground) {
                                    isForeground = true
                                    host.promoteForeground(notifier.buildNotification(noiseType))
                                } else {
                                    notifier.updateNotification(noiseType)
                                }
                            }
                            !isPlaying && isForeground -> {
                                mediaSessionController.updateStopped()
                                isForeground = false
                                host.demoteForeground()
                            }
                        }
                    }
            }
        }
    }
