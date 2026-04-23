package com.tuneflow.feature.browse

internal fun formatTrackDuration(durationSec: Int): String {
    if (durationSec <= 0) return "--:--"
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    return "%d:%02d".format(minutes, seconds)
}
