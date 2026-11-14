package com.tuneflow.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.browse.HomeCategoryKind
import com.tuneflow.feature.video.PreferredVideoStore
import com.tuneflow.feature.video.VideoHistoryEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val HOME_RAIL_PAGE_SIZE = 5
internal const val HOME_VIDEO_HISTORY_PAGE_SIZE = 10

data class HomeRailUiState(
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

data class HomeUiState(
    val recentAlbums: List<AlbumSummary> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val favorites: FavoritesBundle = FavoritesBundle(emptyList(), emptyList()),
    val artists: List<ArtistSummary> = emptyList(),
    val videoHistory: List<VideoHistoryEntry> = emptyList(),
    val visibleVideoHistoryCount: Int = HOME_VIDEO_HISTORY_PAGE_SIZE,
    val rails: Map<HomeCategoryKind, HomeRailUiState> = emptyMap(),
) {
    fun rail(category: HomeCategoryKind): HomeRailUiState = rails[category] ?: HomeRailUiState()

    val visibleVideoHistory: List<VideoHistoryEntry>
        get() = videoHistory.take(visibleVideoHistoryCount)

    val hasMoreVideoHistory: Boolean
        get() = visibleVideoHistoryCount < videoHistory.size
}

class HomeViewModel(
    private val repository: BrowseRepository,
    private val preferredVideoStore: PreferredVideoStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val railJobs = mutableMapOf<HomeCategoryKind, Job>()
    private var artists = emptyList<ArtistSummary>()
    private var playlists = emptyList<PlaylistSummary>()
    private var favorites = FavoritesBundle(emptyList(), emptyList())
    private var albumOffset = 0

    init {
        viewModelScope.launch {
            preferredVideoStore.history.collect { history ->
                _uiState.update { it.copy(videoHistory = history) }
            }
        }
        refresh()
    }

    fun refresh() {
        railJobs.values.forEach { it.cancel() }
        railJobs.clear()
        artists = emptyList()
        playlists = emptyList()
        favorites = FavoritesBundle(emptyList(), emptyList())
        albumOffset = 0
        _uiState.update { HomeUiState(videoHistory = it.videoHistory, visibleVideoHistoryCount = HOME_VIDEO_HISTORY_PAGE_SIZE) }
        viewModelScope.launch { preferredVideoStore.refreshHistory() }
    }

    fun loadMoreVideoHistory() {
        val current = _uiState.value
        if (!current.hasMoreVideoHistory) return
        _uiState.update {
            it.copy(visibleVideoHistoryCount = it.visibleVideoHistoryCount + HOME_VIDEO_HISTORY_PAGE_SIZE)
        }
    }

    fun loadRail(category: HomeCategoryKind) {
        val rail = _uiState.value.rail(category)
        if (rail.isLoaded || rail.isLoading || rail.error != null) return
        requestRail(category, append = false)
    }

    fun loadMoreRail(category: HomeCategoryKind) {
        val rail = _uiState.value.rail(category)
        if (rail.isLoading || (rail.isLoaded && !rail.hasMore)) return
        requestRail(category, append = rail.isLoaded)
    }

    private fun requestRail(
        category: HomeCategoryKind,
        append: Boolean,
    ) {
        updateRail(category) { it.copy(isLoading = true, error = null) }
        railJobs[category] =
            viewModelScope.launch {
                val result =
                    when (category) {
                        HomeCategoryKind.Albums -> loadAlbums()
                        HomeCategoryKind.Artists -> loadArtists(append)
                        HomeCategoryKind.Favorites -> loadFavorites(append)
                        HomeCategoryKind.Playlists -> loadPlaylists(append)
                    }
                currentCoroutineContext().ensureActive()
                updateRail(category) {
                    if (result.isSuccess) {
                        it.copy(isLoaded = true, isLoading = false, hasMore = result.getOrDefault(false))
                    } else {
                        it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Could not load this rail.")
                    }
                }
            }
    }

    private suspend fun loadAlbums(): Result<Boolean> {
        val result = repository.getAlbums(HOME_RAIL_PAGE_SIZE, albumOffset)
        currentCoroutineContext().ensureActive()
        return result.map { page ->
            albumOffset += page.size
            _uiState.update { it.copy(recentAlbums = (it.recentAlbums + page).distinctBy(AlbumSummary::id)) }
            page.size == HOME_RAIL_PAGE_SIZE
        }
    }

    private suspend fun loadArtists(append: Boolean): Result<Boolean> {
        if (!append) {
            val result = repository.getArtists()
            currentCoroutineContext().ensureActive()
            if (result.isFailure) return Result.failure(requireNotNull(result.exceptionOrNull()))
            artists = result.getOrThrow()
        }
        val limit = _uiState.value.artists.size + HOME_RAIL_PAGE_SIZE
        _uiState.update { it.copy(artists = artists.take(limit)) }
        return Result.success(limit < artists.size)
    }

    private suspend fun loadFavorites(append: Boolean): Result<Boolean> {
        if (!append) {
            val result = repository.getFavorites()
            currentCoroutineContext().ensureActive()
            if (result.isFailure) return Result.failure(requireNotNull(result.exceptionOrNull()))
            favorites = result.getOrThrow()
        }
        val current = _uiState.value.favorites
        val limit = current.albums.size + current.tracks.size + HOME_RAIL_PAGE_SIZE
        val albums = favorites.albums.take(limit)
        val tracks = favorites.tracks.take((limit - albums.size).coerceAtLeast(0))
        _uiState.update { it.copy(favorites = FavoritesBundle(albums, tracks)) }
        return Result.success(limit < favorites.albums.size + favorites.tracks.size)
    }

    private suspend fun loadPlaylists(append: Boolean): Result<Boolean> {
        if (!append) {
            val result = repository.getPlaylists()
            currentCoroutineContext().ensureActive()
            if (result.isFailure) return Result.failure(requireNotNull(result.exceptionOrNull()))
            playlists = result.getOrThrow()
        }
        val offset = _uiState.value.playlists.size
        val page = playlists.drop(offset).take(HOME_RAIL_PAGE_SIZE)
        _uiState.update { it.copy(playlists = it.playlists + page) }
        val hydrated = repository.hydratePlaylistArtwork(page).getOrDefault(page).associateBy(PlaylistSummary::id)
        currentCoroutineContext().ensureActive()
        _uiState.update { state ->
            state.copy(playlists = state.playlists.map { hydrated[it.id] ?: it })
        }
        return Result.success(offset + page.size < playlists.size)
    }

    private fun updateRail(
        category: HomeCategoryKind,
        update: (HomeRailUiState) -> HomeRailUiState,
    ) {
        _uiState.update { it.copy(rails = it.rails + (category to update(it.rail(category)))) }
    }
}
