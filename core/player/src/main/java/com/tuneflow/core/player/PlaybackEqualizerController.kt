package com.tuneflow.core.player

import kotlin.math.abs
import kotlin.math.roundToInt

internal class PlaybackEqualizerController(
    private val effectFactory: EqualizerEffectFactory,
    private val onStateChanged: (EqualizerState) -> Unit,
    private val onPresetSelected: (EqualizerPreset) -> Unit,
) {
    private var selectedPreset = EqualizerPreset.Original
    private var attachedSessionId: Int? = null
    private var effect: EqualizerEffect? = null
    private var isSupported = false

    init {
        publishState()
    }

    fun restorePreset(preset: EqualizerPreset) {
        selectedPreset = preset
        applyCurrentPreset()
        publishState()
    }

    fun cyclePreset() {
        selectedPreset = selectedPreset.next()
        onPresetSelected(selectedPreset)
        applyCurrentPreset()
        publishState()
    }

    fun attachToSession(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            releaseEffect()
            isSupported = false
            publishState()
            return
        }

        if (attachedSessionId == audioSessionId && effect != null) {
            applyCurrentPreset()
            publishState()
            return
        }

        releaseEffect()

        val createdEffect =
            runCatching { effectFactory.create(audioSessionId) }
                .getOrNull()

        if (createdEffect == null || !createdEffect.isUsable()) {
            createdEffect?.release()
            isSupported = false
            publishState()
            return
        }

        attachedSessionId = audioSessionId
        effect = createdEffect
        isSupported = true
        applyCurrentPreset()
        publishState()
    }

    fun release() {
        releaseEffect()
        isSupported = false
        publishState()
    }

    private fun applyCurrentPreset() {
        val activeEffect = effect ?: return

        val applied =
            runCatching {
                when (selectedPreset) {
                    EqualizerPreset.Original -> {
                        activeEffect.setEnabled(false)
                    }

                    else -> {
                        activeEffect.setEnabled(true)
                        if (!applyNamedPreset(activeEffect, selectedPreset)) {
                            applyBandProfile(activeEffect, selectedPreset)
                        }
                    }
                }
            }.isSuccess

        if (!applied) {
            isSupported = false
            releaseEffect()
        }
    }

    private fun applyNamedPreset(
        activeEffect: EqualizerEffect,
        preset: EqualizerPreset,
    ): Boolean {
        val targetName =
            when (preset) {
                EqualizerPreset.Original -> return false
                EqualizerPreset.Bass -> "bass"
                EqualizerPreset.Jazz -> "jazz"
                EqualizerPreset.Vocal -> "vocal"
            }

        val presetCount = activeEffect.getPresetCount().toInt()
        for (index in 0 until presetCount) {
            val name = activeEffect.getPresetName(index.toShort()).trim().lowercase()
            if (name.contains(targetName)) {
                activeEffect.usePreset(index.toShort())
                return true
            }
        }
        return false
    }

    private fun applyBandProfile(
        activeEffect: EqualizerEffect,
        preset: EqualizerPreset,
    ) {
        val bandCount = activeEffect.getBandCount().toInt()
        if (bandCount <= 0) {
            error("Equalizer reports no bands")
        }

        val levelRange = activeEffect.getBandLevelRange()
        val minLevel = levelRange.first
        val maxLevel = levelRange.second
        val symmetricLimit = minOf(abs(minLevel.toInt()), abs(maxLevel.toInt())).toShort()
        val profile = preset.bandProfile()

        for (bandIndex in 0 until bandCount) {
            val profileIndex =
                ((bandIndex.toFloat() / (bandCount - 1).coerceAtLeast(1)) * (profile.lastIndex)).roundToInt()
            val scaledLevel = (profile[profileIndex] * symmetricLimit).roundToInt().toShort()
            activeEffect.setBandLevel(
                bandIndex.toShort(),
                scaledLevel.coerceIn(minLevel, maxLevel),
            )
        }
    }

    private fun EqualizerEffect.isUsable(): Boolean {
        return runCatching {
            getBandCount() > 0 && getBandLevelRange().first < getBandLevelRange().second
        }.getOrDefault(false)
    }

    private fun EqualizerPreset.bandProfile(): FloatArray {
        return when (this) {
            EqualizerPreset.Original -> floatArrayOf(0f, 0f, 0f, 0f, 0f)
            EqualizerPreset.Bass -> floatArrayOf(1f, 0.65f, 0.2f, -0.1f, -0.2f)
            EqualizerPreset.Jazz -> floatArrayOf(0.4f, 0.15f, -0.15f, 0.35f, 0.55f)
            EqualizerPreset.Vocal -> floatArrayOf(-0.2f, 0.1f, 0.65f, 0.45f, 0.05f)
        }
    }

    private fun publishState() {
        onStateChanged(
            EqualizerState(
                selectedPreset = selectedPreset,
                isSupported = isSupported,
                displayLabel = if (isSupported) selectedPreset.displayName else "N/A",
            ),
        )
    }

    private fun releaseEffect() {
        effect?.release()
        effect = null
        attachedSessionId = null
    }
}

internal fun interface EqualizerEffectFactory {
    fun create(audioSessionId: Int): EqualizerEffect
}

internal interface EqualizerEffect {
    fun getBandCount(): Short

    fun getBandLevelRange(): Pair<Short, Short>

    fun setBandLevel(
        band: Short,
        level: Short,
    )

    fun getPresetCount(): Short

    fun getPresetName(preset: Short): String

    fun usePreset(preset: Short)

    fun setEnabled(enabled: Boolean)

    fun release()
}
