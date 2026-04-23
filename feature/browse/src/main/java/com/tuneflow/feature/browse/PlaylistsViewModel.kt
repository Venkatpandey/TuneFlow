package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.PlaylistDetail
import com.tuneflow.core.network.PlaylistSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistsUiState(
    val isLoading: Boolean = false,
    val playlists: List<PlaylistSummary> = emptyList(),
    val selected: PlaylistDetail? = null,
    val error: String? = null,
)

class PlaylistsViewModel(private val repository: BrowseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistsUiState())
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = repository.getPlaylists()
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, playlists = result.getOrNull().orEmpty())
                } else {
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                }
            }

            val playlists = result.getOrNull().orEmpty()
            if (playlists.isNotEmpty()) {
                val hydrated = repository.hydratePlaylistArtwork(playlists)
                if (hydrated.isSuccess) {
                    _uiState.update { it.copy(playlists = hydrated.getOrNull().orEmpty()) }
                }
            }
        }
    }

    fun loadPlaylistDetail(playlistId: String) {
        viewModelScope.launch {
            val result = repository.getPlaylistDetail(playlistId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(selected = result.getOrNull(), error = null)
                } else {
                    it.copy(error = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}
