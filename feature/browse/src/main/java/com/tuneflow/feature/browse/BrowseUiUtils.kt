package com.tuneflow.feature.browse

internal fun formatTrackDuration(durationSec: Int): String {
    if (durationSec <= 0) return "--:--"
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun formatTotalDuration(durationSec: Int): String {
    if (durationSec <= 0) return "0m"
    val hours = durationSec / 3600
    val minutes = (durationSec % 3600) / 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        append("${minutes}m")
    }
}
