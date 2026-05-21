package com.tuneflow.core.player

import android.media.audiofx.Equalizer

internal class AndroidEqualizerEffect(
    audioSessionId: Int,
) : EqualizerEffect {
    private val equalizer = Equalizer(0, audioSessionId)

    override fun getBandCount(): Short = equalizer.numberOfBands

    override fun getBandLevelRange(): Pair<Short, Short> {
        val range = equalizer.bandLevelRange
        return range[0] to range[1]
    }

    override fun setBandLevel(
        band: Short,
        level: Short,
    ) {
        equalizer.setBandLevel(band, level)
    }

    override fun getPresetCount(): Short = equalizer.numberOfPresets

    override fun getPresetName(preset: Short): String = equalizer.getPresetName(preset)

    override fun usePreset(preset: Short) {
        equalizer.usePreset(preset)
    }

    override fun setEnabled(enabled: Boolean) {
        equalizer.enabled = enabled
    }

    override fun release() {
        equalizer.release()
    }
}
