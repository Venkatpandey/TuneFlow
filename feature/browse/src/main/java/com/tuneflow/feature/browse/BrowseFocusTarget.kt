package com.tuneflow.feature.browse

enum class BrowseFocusTargetKind {
    Album,
    Artist,
    HomeCategory,
    Playlist,
}

data class BrowseFocusTarget(
    val kind: BrowseFocusTargetKind,
    val id: String,
)
