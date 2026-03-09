package dev.ashera.slumbr.core.playback

import dev.ashera.slumbr.core.system.DndStateProvider

class FakeDndStateProvider : DndStateProvider {
    var totalSilence = false

    override fun isTotalSilence(): Boolean = totalSilence
}
