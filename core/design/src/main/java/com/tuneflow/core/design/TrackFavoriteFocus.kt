package com.tuneflow.core.design

enum class TrackRowFocusTarget {
    RowBody,
    FavoriteButton,
}

enum class HorizontalFocusDirection {
    Left,
    Right,
}

fun trackRowFocusDestination(
    current: TrackRowFocusTarget,
    direction: HorizontalFocusDirection,
): TrackRowFocusTarget? =
    when (current to direction) {
        TrackRowFocusTarget.RowBody to HorizontalFocusDirection.Right -> TrackRowFocusTarget.FavoriteButton
        TrackRowFocusTarget.FavoriteButton to HorizontalFocusDirection.Left -> TrackRowFocusTarget.RowBody
        else -> null
    }
