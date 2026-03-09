package dev.ashera.slumbr.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ashera.slumbr.audio.AudioEngine
import dev.ashera.slumbr.audio.AudioEngineContract
import dev.ashera.slumbr.system.AndroidDndStateProvider
import dev.ashera.slumbr.system.DndStateProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAudioEngine(impl: AudioEngine): AudioEngineContract

    @Binds
    abstract fun bindDndStateProvider(impl: AndroidDndStateProvider): DndStateProvider
}
