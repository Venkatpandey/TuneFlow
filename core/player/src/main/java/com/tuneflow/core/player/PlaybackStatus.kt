package com.tuneflow.core.player

enum class PlaybackPhase {
    Idle,
    Buffering,
    Ready,
    Ended,
}

data class PlaybackStatus(
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val currentIndex: Int = 0,
    val expectedToPlay: Boolean = false,
    val errorCategory: String? = null,
    val errorMessage: String? = null,
)
