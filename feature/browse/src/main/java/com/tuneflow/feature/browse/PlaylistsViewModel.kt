package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.PlaylistDetail
import com.tuneflow.core.network.PlaylistSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val PLAYLIST_TRACK_PAGE_SIZE = 50
private const val PLAYLIST_ARTWORK_BATCH_SIZE = 4

data class PlaylistsUiState(
    val isLoading: Boolean = false,
    val playlists: List<PlaylistSummary> = emptyList(),
    val selectedPlaylistId: String? = null,
    val selected: PlaylistDetail? = null,
    val visibleTrackCount: Int = 0,
    val selectedDurationSec: Int = 0,
    val error: String? = null,
) {
    // Subsonic returns the full playlist; only the displayed prefix grows. Playback uses selected.tracks.
    val visibleTracks get() = selected?.tracks.orEmpty().take(visibleTrackCount)
    val hasMoreTracks get() = visibleTrackCount < selected?.tracks.orEmpty().size
}

class PlaylistsViewModel(private val repository: BrowseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()
    private var playlistDetailJob: Job? = null
    private var playlistListJob: Job? = null
    private var artworkJob: Job? = null
    private val hydratedPlaylistIds = mutableSetOf<String>()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        playlistListJob?.cancel()
        artworkJob?.cancel()
        hydratedPlaylistIds.clear()
        _uiState.update { it.copy(isLoading = true, error = null) }
        playlistListJob =
            viewModelScope.launch {
                val result = repository.getPlaylists()
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    if (result.isSuccess) {
                        it.copy(isLoading = false, playlists = result.getOrNull().orEmpty())
                    } else {
                        it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                    }
                }
            }
    }

    fun loadVisiblePlaylistArtwork(playlistIds: List<String>) {
        artworkJob?.cancel()
        val requested = _uiState.value.playlists.filter { it.id in playlistIds && it.id !in hydratedPlaylistIds }
        artworkJob =
            viewModelScope.launch {
                requested.chunked(PLAYLIST_ARTWORK_BATCH_SIZE).forEach { batch ->
                    val hydrated = repository.hydratePlaylistArtwork(batch).getOrElse { return@launch }
                    currentCoroutineContext().ensureActive()
                    hydratedPlaylistIds.addAll(hydrated.map { it.id })
                    val artworkById = hydrated.associateBy { it.id }
                    _uiState.update { state ->
                        state.copy(playlists = state.playlists.map { artworkById[it.id] ?: it })
                    }
                }
            }
    }

    fun loadPlaylistDetail(playlistId: String) {
        playlistDetailJob?.cancel()
        _uiState.update {
            it.copy(
                selectedPlaylistId = playlistId,
                selected = null,
                visibleTrackCount = 0,
                selectedDurationSec = 0,
                error = null,
            )
        }
        playlistDetailJob =
            viewModelScope.launch {
                val result = repository.getPlaylistDetail(playlistId)
                currentCoroutineContext().ensureActive()
                _uiState.update {
                    if (it.selectedPlaylistId != playlistId) return@update it
                    if (result.isSuccess) {
                        val detail = result.getOrThrow()
                        it.copy(
                            selected = detail,
                            visibleTrackCount = minOf(PLAYLIST_TRACK_PAGE_SIZE, detail.tracks.size),
                            selectedDurationSec = detail.tracks.sumOf { track -> track.durationSec },
                            error = null,
                        )
                    } else {
                        it.copy(selectedPlaylistId = null, selected = null, error = result.exceptionOrNull()?.message)
                    }
                }
            }
    }

    fun loadMoreTracks() {
        _uiState.update {
            it.copy(visibleTrackCount = minOf(it.visibleTrackCount + PLAYLIST_TRACK_PAGE_SIZE, it.selected?.tracks.orEmpty().size))
        }
    }

    fun clearSelection() {
        playlistDetailJob?.cancel()
        playlistDetailJob = null
        _uiState.update {
            it.copy(
                selectedPlaylistId = null,
                selected = null,
                visibleTrackCount = 0,
                selectedDurationSec = 0,
                error = null,
            )
        }
    }
}
