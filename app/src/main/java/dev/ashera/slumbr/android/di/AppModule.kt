package dev.ashera.slumbr.android.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ashera.slumbr.android.audio.AudioTrackAudioEngine
import dev.ashera.slumbr.android.service.AndroidMediaSessionController
import dev.ashera.slumbr.android.service.AndroidPlaybackNotifier
import dev.ashera.slumbr.android.service.MediaSessionController
import dev.ashera.slumbr.android.service.PlaybackNotifier
import dev.ashera.slumbr.android.system.AndroidDndStateProvider
import dev.ashera.slumbr.core.audio.AudioEngine
import dev.ashera.slumbr.core.system.DndStateProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAudioEngine(impl: AudioTrackAudioEngine): AudioEngine

    @Binds
    @Singleton
    abstract fun bindDndStateProvider(impl: AndroidDndStateProvider): DndStateProvider

    @Binds
    @Singleton
    abstract fun bindPlaybackNotifier(impl: AndroidPlaybackNotifier): PlaybackNotifier

    @Binds
    @Singleton
    abstract fun bindMediaSessionController(impl: AndroidMediaSessionController): MediaSessionController
}
