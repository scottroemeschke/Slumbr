package dev.ashera.slumbr.playback

import dev.ashera.slumbr.system.DndStateProvider

class FakeDndStateProvider : DndStateProvider {
    var totalSilence = false

    override fun isTotalSilence(): Boolean = totalSilence
}
