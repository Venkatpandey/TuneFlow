package com.tuneflow.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.feature.browse.BrowseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val recentAlbums: List<AlbumSummary> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val favorites: FavoritesBundle = FavoritesBundle(emptyList(), emptyList()),
    val artists: List<ArtistSummary> = emptyList(),
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
            coroutineScope {
                val albumsDeferred = async { repository.getAlbums(size = 12, offset = 0) }
                val playlistsDeferred = async { repository.getPlaylists() }
                val favoritesDeferred = async { repository.getFavorites() }
                val artistsDeferred = async { repository.getArtists() }

                val albumsResult = albumsDeferred.await()
                val playlistsResult = playlistsDeferred.await()
                val favoritesResult = favoritesDeferred.await()
                val artistsResult = artistsDeferred.await()

                val errors =
                    listOfNotNull(
                        albumsResult.exceptionOrNull()?.message,
                        playlistsResult.exceptionOrNull()?.message,
                        favoritesResult.exceptionOrNull()?.message,
                        artistsResult.exceptionOrNull()?.message,
                    )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recentAlbums = albumsResult.getOrNull().orEmpty(),
                        playlists = playlistsResult.getOrNull().orEmpty(),
                        favorites = favoritesResult.getOrNull() ?: FavoritesBundle(emptyList(), emptyList()),
                        artists = artistsResult.getOrNull().orEmpty(),
                        error = errors.firstOrNull(),
                    )
                }
            }

            val hydratedPlaylists = repository.hydratePlaylistArtwork(_uiState.value.playlists)
            if (hydratedPlaylists.isSuccess) {
                _uiState.update { it.copy(playlists = hydratedPlaylists.getOrNull().orEmpty()) }
            }
        }
    }
}
