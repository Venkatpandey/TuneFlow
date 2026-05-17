package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeCategoryKind {
    Favorites,
    Artists,
    Albums,
    Playlists,
}

data class HomeCategoryUiState(
    val category: HomeCategoryKind = HomeCategoryKind.Albums,
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val favorites: FavoritesBundle = FavoritesBundle(emptyList(), emptyList()),
    val artists: List<ArtistSummary> = emptyList(),
    val albums: List<AlbumSummary> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
) {
    val title: String
        get() =
            when (category) {
                HomeCategoryKind.Favorites -> "Favorites"
                HomeCategoryKind.Artists -> "Artists"
                HomeCategoryKind.Albums -> "Albums"
                HomeCategoryKind.Playlists -> "Playlists"
            }

    val filteredFavorites: FavoritesBundle
        get() {
            if (query.isBlank()) return favorites
            val normalizedQuery = query.trim().lowercase()
            return FavoritesBundle(
                albums =
                    favorites.albums.filter { album ->
                        album.title.contains(normalizedQuery, ignoreCase = true) ||
                            album.artist.contains(normalizedQuery, ignoreCase = true)
                    },
                tracks =
                    favorites.tracks.filter { track ->
                        track.title.contains(normalizedQuery, ignoreCase = true) ||
                            track.artist.contains(normalizedQuery, ignoreCase = true) ||
                            track.album.contains(normalizedQuery, ignoreCase = true)
                    },
            )
        }

    val filteredArtists: List<ArtistSummary>
        get() = artists.filterByQuery(query) { listOf(it.name) }

    val filteredAlbums: List<AlbumSummary>
        get() = albums.filterByQuery(query) { listOf(it.title, it.artist) }

    val filteredPlaylists: List<PlaylistSummary>
        get() = playlists.filterByQuery(query) { listOf(it.name) }
}

class HomeCategoryViewModel(
    private val repository: BrowseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeCategoryUiState())
    val uiState: StateFlow<HomeCategoryUiState> = _uiState.asStateFlow()

    fun load(category: HomeCategoryKind) {
        val currentState = _uiState.value
        if (currentState.category == category && hasLoadedContent(currentState)) {
            _uiState.update { it.copy(query = "", error = null) }
            return
        }

        _uiState.update {
            it.copy(
                category = category,
                query = "",
                isLoading = true,
                error = null,
                favorites = FavoritesBundle(emptyList(), emptyList()),
                artists = emptyList(),
                albums = emptyList(),
                playlists = emptyList(),
            )
        }

        viewModelScope.launch {
            when (category) {
                HomeCategoryKind.Favorites -> loadFavorites(category)
                HomeCategoryKind.Artists -> loadArtists(category)
                HomeCategoryKind.Albums -> loadAlbums(category)
                HomeCategoryKind.Playlists -> loadPlaylists(category)
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    private suspend fun loadFavorites(category: HomeCategoryKind) {
        val result = repository.getFavorites()
        _uiState.update {
            if (result.isSuccess) {
                it.copy(
                    category = category,
                    isLoading = false,
                    favorites = result.getOrNull() ?: FavoritesBundle(emptyList(), emptyList()),
                    error = null,
                )
            } else {
                it.copy(
                    category = category,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private suspend fun loadArtists(category: HomeCategoryKind) {
        val result = repository.getArtists()
        _uiState.update {
            if (result.isSuccess) {
                it.copy(
                    category = category,
                    isLoading = false,
                    artists = result.getOrNull().orEmpty(),
                    error = null,
                )
            } else {
                it.copy(
                    category = category,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private suspend fun loadAlbums(category: HomeCategoryKind) {
        val albums = mutableListOf<AlbumSummary>()
        var offset = 0
        val pageSize = 100

        while (true) {
            val result = repository.getAlbums(size = pageSize, offset = offset)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        category = category,
                        isLoading = false,
                        error = result.exceptionOrNull()?.message,
                    )
                }
                return
            }

            val page = result.getOrNull().orEmpty()
            albums += page
            if (page.size < pageSize) break
            offset += pageSize
        }

        _uiState.update {
            it.copy(
                category = category,
                isLoading = false,
                albums = albums,
                error = null,
            )
        }
    }

    private suspend fun loadPlaylists(category: HomeCategoryKind) {
        val result = repository.getPlaylists()
        if (result.isFailure) {
            _uiState.update {
                it.copy(
                    category = category,
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
            return
        }

        val hydrated = repository.hydratePlaylistArtwork(result.getOrNull().orEmpty())
        _uiState.update {
            if (hydrated.isSuccess) {
                it.copy(
                    category = category,
                    isLoading = false,
                    playlists = hydrated.getOrNull().orEmpty(),
                    error = null,
                )
            } else {
                it.copy(
                    category = category,
                    isLoading = false,
                    error = hydrated.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun hasLoadedContent(state: HomeCategoryUiState): Boolean {
        return state.favorites.albums.isNotEmpty() ||
            state.favorites.tracks.isNotEmpty() ||
            state.artists.isNotEmpty() ||
            state.albums.isNotEmpty() ||
            state.playlists.isNotEmpty() ||
            state.error != null
    }
}

private fun <T> List<T>.filterByQuery(
    query: String,
    fields: (T) -> List<String>,
): List<T> {
    if (query.isBlank()) return this
    val normalizedQuery = query.trim().lowercase()
    return filter { item ->
        fields(item).any { value -> value.contains(normalizedQuery, ignoreCase = true) }
    }
}
