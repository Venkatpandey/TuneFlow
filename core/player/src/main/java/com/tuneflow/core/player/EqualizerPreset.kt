package com.tuneflow.core.player

enum class EqualizerPreset(
    val displayName: String,
) {
    Original("Original"),
    Bass("Bass"),
    Jazz("Jazz"),
    Vocal("Vocal"),
}

data class EqualizerState(
    val selectedPreset: EqualizerPreset = EqualizerPreset.Original,
    val isSupported: Boolean = false,
    val displayLabel: String = "N/A",
)

internal fun EqualizerPreset.next(): EqualizerPreset {
    return when (this) {
        EqualizerPreset.Original -> EqualizerPreset.Bass
        EqualizerPreset.Bass -> EqualizerPreset.Jazz
        EqualizerPreset.Jazz -> EqualizerPreset.Vocal
        EqualizerPreset.Vocal -> EqualizerPreset.Original
    }
}
