package dev.ashera.slumbr.service

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ashera.slumbr.R
import dev.ashera.slumbr.audio.NoiseType
import dev.ashera.slumbr.playback.PlaybackController
import javax.inject.Inject

class PlaybackMediaSessionManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private var mediaSession: MediaSessionCompat? = null

        val sessionToken: MediaSessionCompat.Token?
            get() = mediaSession?.sessionToken

        fun init(playbackController: PlaybackController) {
            mediaSession =
                MediaSessionCompat(context, "SlumbrSession").apply {
                    setCallback(
                        object : MediaSessionCompat.Callback() {
                            override fun onStop() {
                                playbackController.hardStop()
                            }
                        },
                    )
                    isActive = true
                }
        }

        fun updateState(state: Int) {
            val speed =
                if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f
            val playbackState =
                PlaybackStateCompat
                    .Builder()
                    .setActions(PlaybackStateCompat.ACTION_STOP)
                    .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, speed)
                    .build()
            mediaSession?.setPlaybackState(playbackState)
        }

        fun updateMetadata(noiseType: NoiseType) {
            val metadata =
                MediaMetadataCompat
                    .Builder()
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        context.getString(R.string.notification_title),
                    ).putString(MediaMetadataCompat.METADATA_KEY_ARTIST, noiseType.displayName)
                    .build()
            mediaSession?.setMetadata(metadata)
        }

        fun release() {
            mediaSession?.release()
            mediaSession = null
        }
    }
