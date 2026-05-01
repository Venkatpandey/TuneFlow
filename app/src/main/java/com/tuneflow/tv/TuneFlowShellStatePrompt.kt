package com.tuneflow.tv

internal fun TuneFlowShellState.showExitPrompt(timestampMs: Long): TuneFlowShellState =
    copy(showExitPrompt = true, lastExitPromptAt = timestampMs)

internal fun TuneFlowShellState.hideExitPrompt(): TuneFlowShellState = copy(showExitPrompt = false)

internal fun TuneFlowShellState.consumeNowPlayingTransportFocus(): TuneFlowShellState = copy(autoFocusNowPlayingTransport = false)

internal fun TuneFlowShellState.consumePreselectedPlaylist(): TuneFlowShellState = copy(preselectedPlaylistId = null)
