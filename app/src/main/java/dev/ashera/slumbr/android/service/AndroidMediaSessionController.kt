package dev.ashera.slumbr.android.service

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ashera.slumbr.R
import dev.ashera.slumbr.core.audio.NoiseType
import javax.inject.Inject

class AndroidMediaSessionController
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : MediaSessionController {
        private var mediaSession: MediaSessionCompat? = null

        override val sessionToken: MediaSessionCompat.Token?
            get() = mediaSession?.sessionToken

        override fun initialize(onStop: () -> Unit) {
            mediaSession =
                MediaSessionCompat(context, "SlumbrSession").apply {
                    setCallback(
                        object : MediaSessionCompat.Callback() {
                            override fun onStop() {
                                onStop()
                            }
                        },
                    )
                    isActive = true
                }
        }

        override fun updatePlaying(noiseType: NoiseType) {
            val playbackState =
                PlaybackStateCompat
                    .Builder()
                    .setActions(PlaybackStateCompat.ACTION_STOP)
                    .setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1f,
                    ).build()
            mediaSession?.setPlaybackState(playbackState)

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

        override fun updateStopped() {
            val playbackState =
                PlaybackStateCompat
                    .Builder()
                    .setActions(PlaybackStateCompat.ACTION_STOP)
                    .setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        0f,
                    ).build()
            mediaSession?.setPlaybackState(playbackState)
        }

        override fun release() {
            mediaSession?.release()
            mediaSession = null
        }
    }
