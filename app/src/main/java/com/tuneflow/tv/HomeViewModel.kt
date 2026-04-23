package com.tuneflow.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.feature.browse.BrowseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val recentAlbums: List<AlbumSummary> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(private val repository: BrowseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val albumsResult = repository.getAlbums(size = 12, offset = 0)
            val playlistsResult = repository.getPlaylists()

            val errors =
                listOfNotNull(
                    albumsResult.exceptionOrNull()?.message,
                    playlistsResult.exceptionOrNull()?.message,
                )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    recentAlbums = albumsResult.getOrNull().orEmpty(),
                    playlists = playlistsResult.getOrNull().orEmpty(),
                    error = errors.firstOrNull(),
                )
            }
        }
    }
}
