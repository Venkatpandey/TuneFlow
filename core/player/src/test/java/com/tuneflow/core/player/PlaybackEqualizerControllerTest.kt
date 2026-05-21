package com.tuneflow.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackEqualizerControllerTest {
    @Test
    fun cyclePreset_advancesThroughTuneflowOrder() {
        val recordedPresets = mutableListOf<EqualizerPreset>()
        val controller =
            PlaybackEqualizerController(
                effectFactory = EqualizerEffectFactory { FakeEqualizerEffect() },
                onStateChanged = {},
                onPresetSelected = recordedPresets::add,
            )

        controller.cyclePreset()
        controller.cyclePreset()
        controller.cyclePreset()
        controller.cyclePreset()

        assertEquals(
            listOf(
                EqualizerPreset.Bass,
                EqualizerPreset.Jazz,
                EqualizerPreset.Vocal,
                EqualizerPreset.Original,
            ),
            recordedPresets,
        )
    }

    @Test
    fun attachToSession_marksUnsupportedWhenFactoryFails() {
        var latestState = EqualizerState()
        val controller =
            PlaybackEqualizerController(
                effectFactory = EqualizerEffectFactory { error("boom") },
                onStateChanged = { latestState = it },
                onPresetSelected = {},
            )

        controller.attachToSession(9)

        assertFalse(latestState.isSupported)
        assertEquals("N/A", latestState.displayLabel)
    }

    @Test
    fun attachToSession_appliesNamedPresetWhenAvailable() {
        val effect = FakeEqualizerEffect(presetNames = listOf("Flat", "Jazz", "Rock"))
        val controller =
            PlaybackEqualizerController(
                effectFactory = EqualizerEffectFactory { effect },
                onStateChanged = {},
                onPresetSelected = {},
            )

        controller.restorePreset(EqualizerPreset.Jazz)
        controller.attachToSession(11)

        assertEquals(1.toShort(), effect.usedPreset)
        assertTrue(effect.enabledHistory.last())
    }

    @Test
    fun attachToSession_fallsBackToManualBandProfileWhenNamedPresetMissing() {
        val effect = FakeEqualizerEffect(presetNames = listOf("Flat", "Pop"))
        val controller =
            PlaybackEqualizerController(
                effectFactory = EqualizerEffectFactory { effect },
                onStateChanged = {},
                onPresetSelected = {},
            )

        controller.restorePreset(EqualizerPreset.Bass)
        controller.attachToSession(7)

        assertNull(effect.usedPreset)
        assertTrue(effect.bandLevels.values.any { it > 0 })
        assertTrue(effect.enabledHistory.last())
    }

    @Test
    fun release_disablesSupportState() {
        var latestState = EqualizerState()
        val effect = FakeEqualizerEffect()
        val controller =
            PlaybackEqualizerController(
                effectFactory = EqualizerEffectFactory { effect },
                onStateChanged = { latestState = it },
                onPresetSelected = {},
            )

        controller.attachToSession(5)
        controller.release()

        assertFalse(latestState.isSupported)
        assertTrue(effect.released)
    }
}

private class FakeEqualizerEffect(
    private val bandCount: Short = 5,
    private val bandLevelRange: Pair<Short, Short> = (-1500).toShort() to 1500.toShort(),
    private val presetNames: List<String> = listOf("Flat", "Jazz"),
) : EqualizerEffect {
    val enabledHistory = mutableListOf<Boolean>()
    val bandLevels = linkedMapOf<Short, Short>()
    var usedPreset: Short? = null
    var released = false

    override fun getBandCount(): Short = bandCount

    override fun getBandLevelRange(): Pair<Short, Short> = bandLevelRange

    override fun setBandLevel(
        band: Short,
        level: Short,
    ) {
        bandLevels[band] = level
    }

    override fun getPresetCount(): Short = presetNames.size.toShort()

    override fun getPresetName(preset: Short): String = presetNames[preset.toInt()]

    override fun usePreset(preset: Short) {
        usedPreset = preset
    }

    override fun setEnabled(enabled: Boolean) {
        enabledHistory += enabled
    }

    override fun release() {
        released = true
    }
}
