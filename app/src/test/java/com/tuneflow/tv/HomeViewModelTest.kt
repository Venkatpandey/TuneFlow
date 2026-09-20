package com.tuneflow.tv

import com.tuneflow.core.network.AlbumDto
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistDto
import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.PlaylistDetailDto
import com.tuneflow.core.network.PlaylistDto
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.SongDto
import com.tuneflow.core.network.Starred2Dto
import com.tuneflow.feature.browse.BrowseFocusTarget
import com.tuneflow.feature.browse.BrowseFocusTargetKind
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.browse.HomeCategoryKind
import com.tuneflow.feature.video.PreferredVideoLookupResult
import com.tuneflow.feature.video.PreferredVideoStore
import com.tuneflow.feature.video.PreferredVideoTrack
import com.tuneflow.feature.video.VideoCandidate
import com.tuneflow.feature.video.VideoHistoryEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun slowVideoHistoryDoesNotBlockOtherHomeContent() =
        runTest(dispatcher) {
            val client = PagedHomeClient()
            val viewModel = HomeViewModel(browseRepository(client), SlowPreferredVideoStore())
            viewModel.loadRail(HomeCategoryKind.Albums)

            runCurrent()

            assertTrue(viewModel.uiState.value.rail(HomeCategoryKind.Albums).isLoaded)
            assertEquals(5, viewModel.uiState.value.recentAlbums.size)
        }

    @Test
    fun homeDoesNotFetchOffscreenRails() =
        runTest(dispatcher) {
            val client = PagedHomeClient()
            val viewModel = HomeViewModel(browseRepository(client), SlowPreferredVideoStore())
            runCurrent()
            assertEquals(0, client.totalCalls)
            viewModel.loadRail(HomeCategoryKind.Artists)
            viewModel.loadRail(HomeCategoryKind.Artists)
            runCurrent()
            assertEquals(1, client.totalCalls)
            assertEquals(5, viewModel.uiState.value.artists.size)
            viewModel.loadMoreRail(HomeCategoryKind.Artists)
            runCurrent()
            assertEquals(10, viewModel.uiState.value.artists.size)
            assertEquals(1, client.totalCalls)
        }

    @Test
    fun albumsAppendWithoutReplacingExistingItemsAndRetrySameOffset() =
        runTest(dispatcher) {
            val client = PagedHomeClient()
            val viewModel = HomeViewModel(browseRepository(client), SlowPreferredVideoStore())
            viewModel.loadRail(HomeCategoryKind.Albums)
            runCurrent()
            val firstPage = viewModel.uiState.value.recentAlbums
            client.failAlbums = true
            viewModel.loadMoreRail(HomeCategoryKind.Albums)
            runCurrent()
            assertEquals(firstPage, viewModel.uiState.value.recentAlbums)
            assertEquals("Offline", viewModel.uiState.value.rail(HomeCategoryKind.Albums).error)
            client.failAlbums = false
            viewModel.loadMoreRail(HomeCategoryKind.Albums)
            runCurrent()
            assertEquals(firstPage, viewModel.uiState.value.recentAlbums.take(5))
            assertEquals(listOf(0, 5, 5), client.albumOffsets)
            viewModel.loadMoreRail(HomeCategoryKind.Albums)
            runCurrent()
            assertEquals(12, viewModel.uiState.value.recentAlbums.size)
            assertFalse(viewModel.uiState.value.rail(HomeCategoryKind.Albums).hasMore)
            viewModel.loadMoreRail(HomeCategoryKind.Albums)
            runCurrent()
            assertEquals(listOf(0, 5, 5, 10), client.albumOffsets)
        }

    @Test
    fun cancelledRefreshCannotPublishStaleRailResults() =
        runTest(dispatcher) {
            val client = PagedHomeClient()
            val pending = CompletableDeferred<NetworkResult<List<AlbumDto>>>()
            client.pendingAlbums = pending
            val viewModel = HomeViewModel(browseRepository(client), SlowPreferredVideoStore())
            viewModel.loadRail(HomeCategoryKind.Albums)
            runCurrent()
            viewModel.refresh()
            client.pendingAlbums = null
            viewModel.loadRail(HomeCategoryKind.Albums)
            runCurrent()
            pending.complete(NetworkResult.Success(listOf(AlbumDto("stale", "Stale"))))
            runCurrent()
            assertEquals((0 until 5).map { "album-$it" }, viewModel.uiState.value.recentAlbums.map { it.id })
        }

    @Test
    fun favoritesGrowAcrossAlbumsAndTracksWithoutRefetching() =
        runTest(dispatcher) {
            val client = PagedHomeClient()
            val viewModel = HomeViewModel(browseRepository(client), SlowPreferredVideoStore())
            viewModel.loadRail(HomeCategoryKind.Favorites)
            runCurrent()
            assertEquals(3, viewModel.uiState.value.favorites.albums.size)
            assertEquals(2, viewModel.uiState.value.favorites.tracks.size)
            viewModel.loadMoreRail(HomeCategoryKind.Favorites)
            runCurrent()
            assertEquals(7, viewModel.uiState.value.favorites.tracks.size)
            viewModel.loadMoreRail(HomeCategoryKind.Favorites)
            runCurrent()
            assertEquals(9, viewModel.uiState.value.favorites.tracks.size)
            assertFalse(viewModel.uiState.value.rail(HomeCategoryKind.Favorites).hasMore)
            assertEquals(1, client.totalCalls)
        }

    @Test
    fun playlistArtworkIsFetchedOnlyForExposedPage() =
        runTest(dispatcher) {
            val client = PagedHomeClient()
            val viewModel = HomeViewModel(browseRepository(client), SlowPreferredVideoStore())
            viewModel.loadRail(HomeCategoryKind.Playlists)
            runCurrent()
            assertEquals(5, viewModel.uiState.value.playlists.size)
            assertEquals(5, client.detailIds.size)
            viewModel.loadMoreRail(HomeCategoryKind.Playlists)
            runCurrent()
            assertEquals(10, viewModel.uiState.value.playlists.size)
            assertEquals(10, client.detailIds.size)
            assertEquals(10, client.detailIds.distinct().size)
        }

    @Test
    fun focusRestorationUsesAppendedItemsAndStableSectionSlots() {
        val albums = (0 until 10).map { AlbumSummary(id = "album-$it", title = "Album $it", artist = "Artist", coverArtId = null) }
        val state =
            HomeUiState(
                recentAlbums = albums,
                rails = mapOf(HomeCategoryKind.Albums to HomeRailUiState(isLoaded = true, hasMore = true)),
            )
        val appendedItem = state.focusLocation(BrowseFocusTarget(BrowseFocusTargetKind.Album, "album-8"))
        assertEquals(8, appendedItem?.rowItemIndex)
        assertEquals(8, appendedItem?.sectionRowIndex)
        val showAll = state.focusLocation(BrowseFocusTarget(BrowseFocusTargetKind.HomeCategory, HomeCategoryKind.Albums.name))
        assertEquals(11, showAll?.rowItemIndex)
        assertEquals(8, showAll?.sectionRowIndex)
        assertEquals(
            0,
            HomeUiState().focusLocation(
                BrowseFocusTarget(BrowseFocusTargetKind.HomeCategory, HomeCategoryKind.Playlists.name),
            )?.rowItemIndex,
        )
        assertEquals(
            11,
            HomeUiState().focusLocation(
                BrowseFocusTarget(BrowseFocusTargetKind.HomeCategory, HomeCategoryKind.Playlists.name),
            )?.sectionRowIndex,
        )
    }

    @Test
    fun refreshResetsPaginationAndDefersReloadUntilVisible() =
        runTest(dispatcher) {
            val client = PagedHomeClient()
            val viewModel = HomeViewModel(browseRepository(client), SlowPreferredVideoStore())
            viewModel.loadRail(HomeCategoryKind.Albums)
            runCurrent()
            viewModel.refresh()
            runCurrent()
            assertTrue(viewModel.uiState.value.recentAlbums.isEmpty())
            assertEquals(listOf(0), client.albumOffsets)
            viewModel.loadRail(HomeCategoryKind.Albums)
            runCurrent()
            assertEquals(listOf(0, 0), client.albumOffsets)
        }

    private fun browseRepository(client: NavidromeClient): BrowseRepository {
        val session = SessionData("https://demo", "user", "token", "salt")
        return BrowseRepository(
            sessionProvider = SessionProvider { session },
            clientProvider = NavidromeClientProvider { client },
        )
    }
}

private class PagedHomeClient : NavidromeClient(SessionData("https://demo", "user", "token", "salt")) {
    var totalCalls = 0
    var failAlbums = false
    var pendingAlbums: CompletableDeferred<NetworkResult<List<AlbumDto>>>? = null
    val albumOffsets = mutableListOf<Int>()
    val detailIds = mutableListOf<String>()

    override suspend fun getAlbums(
        size: Int,
        offset: Int,
    ): NetworkResult<List<AlbumDto>> {
        totalCalls++
        albumOffsets.add(offset)
        val pending = pendingAlbums
        return when {
            pending != null -> withContext(NonCancellable) { pending.await() }
            failAlbums -> NetworkResult.Error("Offline")
            else -> NetworkResult.Success((offset until minOf(offset + size, 12)).map { AlbumDto("album-$it", "Album $it") })
        }
    }

    override suspend fun getArtists(): NetworkResult<List<ArtistDto>> {
        totalCalls++
        return NetworkResult.Success((0 until 12).map { ArtistDto("artist-$it", "Artist $it") })
    }

    override suspend fun getStarred2(): NetworkResult<Starred2Dto> {
        totalCalls++
        return NetworkResult.Success(
            Starred2Dto(
                album = (0 until 3).map { AlbumDto("album-$it", "Album $it") },
                song = (0 until 9).map { SongDto("track-$it", "Track $it") },
            ),
        )
    }

    override suspend fun getPlaylists(): NetworkResult<List<PlaylistDto>> {
        totalCalls++
        return NetworkResult.Success((0 until 12).map { PlaylistDto("playlist-$it", "Playlist $it") })
    }

    override suspend fun getPlaylist(playlistId: String): NetworkResult<PlaylistDetailDto> {
        totalCalls++
        detailIds.add(playlistId)
        return NetworkResult.Success(PlaylistDetailDto(playlistId, playlistId))
    }
}

private class SlowPreferredVideoStore : PreferredVideoStore {
    override val history: StateFlow<List<VideoHistoryEntry>> = MutableStateFlow(emptyList())

    override suspend fun lookup(track: PreferredVideoTrack) = PreferredVideoLookupResult.Missing

    override suspend fun savePreferredVideo(
        track: PreferredVideoTrack,
        candidate: VideoCandidate,
    ) = true

    override suspend fun markPlayed(trackId: String) = true

    override suspend fun deletePreferredVideo(trackId: String) = true

    override suspend fun refreshHistory(limit: Int): Boolean {
        delay(10_000L)
        return false
    }
}
