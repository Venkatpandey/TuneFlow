package com.tuneflow.feature.browse

import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.PlaylistDetailDto
import com.tuneflow.core.network.PlaylistDto
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.SongDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {
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
    fun clearSelection_cancelsPendingDetailAndPreventsStalePanel() =
        runTest(dispatcher) {
            val detailResponse = CompletableDeferred<NetworkResult<PlaylistDetailDto>>()
            val repository = playlistRepository(detailResponse)
            val viewModel = PlaylistsViewModel(repository)
            runCurrent()

            viewModel.loadPlaylistDetail("playlist-1")
            runCurrent()
            assertEquals("playlist-1", viewModel.uiState.value.selectedPlaylistId)

            viewModel.clearSelection()
            detailResponse.complete(NetworkResult.Success(PlaylistDetailDto(id = "playlist-1", name = "Favorites")))
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedPlaylistId)
            assertNull(viewModel.uiState.value.selected)
        }

    @Test
    fun tracksAppendInBoundedPagesWhilePlaybackRetainsWholePlaylist() =
        runTest(dispatcher) {
            val response = CompletableDeferred<NetworkResult<PlaylistDetailDto>>()
            val viewModel = PlaylistsViewModel(playlistRepository(response))
            response.complete(
                NetworkResult.Success(
                    PlaylistDetailDto(
                        "playlist-1",
                        "Large",
                        (0 until 123).map {
                            SongDto("track-${it % 10}", "Track $it", duration = 60)
                        },
                    ),
                ),
            )
            viewModel.loadPlaylistDetail("playlist-1")
            runCurrent()
            val firstPage = viewModel.uiState.value.visibleTracks
            assertEquals(50, firstPage.size)
            assertEquals(123, viewModel.uiState.value.selected?.tracks?.size)
            assertEquals(123 * 60, viewModel.uiState.value.selectedDurationSec)
            assertTrue(viewModel.uiState.value.hasMoreTracks)

            viewModel.loadMoreTracks()
            assertEquals(100, viewModel.uiState.value.visibleTracks.size)
            assertEquals(firstPage, viewModel.uiState.value.visibleTracks.take(50))
            viewModel.loadMoreTracks()
            assertEquals(123, viewModel.uiState.value.visibleTracks.size)
            assertFalse(viewModel.uiState.value.hasMoreTracks)
            viewModel.loadMoreTracks()
            assertEquals(123, viewModel.uiState.value.visibleTrackCount)
            viewModel.clearSelection()
            assertEquals(0, viewModel.uiState.value.visibleTrackCount)
            assertEquals(0, viewModel.uiState.value.selectedDurationSec)
            assertTrue(viewModel.uiState.value.visibleTracks.isEmpty())
        }

    @Test
    fun selectingPlaylistAgainStartsWithFirstPage() =
        runTest(dispatcher) {
            val response = CompletableDeferred<NetworkResult<PlaylistDetailDto>>()
            val viewModel = PlaylistsViewModel(playlistRepository(response))
            response.complete(
                NetworkResult.Success(PlaylistDetailDto("playlist-1", "Large", (0 until 100).map { SongDto("track-$it", "Track $it") })),
            )
            viewModel.loadPlaylistDetail("playlist-1")
            runCurrent()
            viewModel.loadMoreTracks()
            viewModel.loadPlaylistDetail("playlist-1")
            assertEquals(0, viewModel.uiState.value.visibleTrackCount)
            runCurrent()
            assertEquals(50, viewModel.uiState.value.visibleTrackCount)
        }

    @Test
    fun artworkLoadsOnlyVisiblePlaylistsAndDoesNotRefetchCompletedOnes() =
        runTest(dispatcher) {
            val detailIds = mutableListOf<String>()
            val session = SessionData("https://demo", "user", "token", "salt")
            val repository =
                BrowseRepository(
                    sessionProvider = SessionProvider { session },
                    clientProvider =
                        NavidromeClientProvider {
                            object : NavidromeClient(it) {
                                override suspend fun getPlaylists(): NetworkResult<List<PlaylistDto>> =
                                    NetworkResult.Success((0 until 100).map { PlaylistDto("playlist-$it", "Playlist $it") })

                                override suspend fun getPlaylist(playlistId: String): NetworkResult<PlaylistDetailDto> {
                                    detailIds.add(playlistId)
                                    return NetworkResult.Success(PlaylistDetailDto(playlistId, playlistId))
                                }
                            }
                        },
                )
            val viewModel = PlaylistsViewModel(repository)
            runCurrent()
            assertTrue(detailIds.isEmpty())
            viewModel.loadVisiblePlaylistArtwork(listOf("playlist-0", "playlist-1"))
            runCurrent()
            assertEquals(listOf("playlist-0", "playlist-1"), detailIds)
            viewModel.loadVisiblePlaylistArtwork(listOf("playlist-1", "playlist-2"))
            runCurrent()
            assertEquals(listOf("playlist-0", "playlist-1", "playlist-2"), detailIds)
        }

    @Test
    fun playlistRowsForDisplay_filtersByNameAndFavorites() {
        val playlists = playlists()

        val result =
            playlistRowsForDisplay(
                playlists = playlists,
                query = "focus",
                favoritePlaylistIds = setOf("playlist-2", "playlist-3"),
                favoritesOnly = true,
                currentPlaylistId = null,
            )

        assertEquals(listOf("playlist-3"), result.map { it.id })
    }

    @Test
    fun playlistRowsForDisplay_putsCurrentPlaylistFirst() {
        val result =
            playlistRowsForDisplay(
                playlists = playlists(),
                query = "",
                favoritePlaylistIds = emptySet(),
                favoritesOnly = false,
                currentPlaylistId = "playlist-3",
            )

        assertEquals(listOf("playlist-3", "playlist-1", "playlist-2"), result.map { it.id })
    }

    @Test
    fun playlistRowsForDisplay_sortsAllUsedPlaylistsByRecency() {
        val result =
            playlistRowsForDisplay(
                playlists = playlists(),
                query = "",
                favoritePlaylistIds = emptySet(),
                favoritesOnly = false,
                currentPlaylistId = null,
                recentPlaylistIds = listOf("playlist-2", "playlist-3", "playlist-1"),
            )

        assertEquals(listOf("playlist-2", "playlist-3", "playlist-1"), result.map { it.id })
    }

    @Test
    fun resolveCurrentPlaylistId_fallsBackToLegacyPlaylistName() {
        val result =
            resolveCurrentPlaylistId(
                playlists = playlists(),
                currentPlaylistId = null,
                currentPlaylistName = " deep focus ",
            )

        assertEquals("playlist-3", result)
    }

    private fun playlists(): List<PlaylistSummary> =
        listOf(
            PlaylistSummary(id = "playlist-1", name = "Morning Mix", songCount = 10, durationSec = 1_800),
            PlaylistSummary(id = "playlist-2", name = "Workout", songCount = 20, durationSec = 3_600),
            PlaylistSummary(id = "playlist-3", name = "Deep Focus", songCount = 30, durationSec = 5_400),
        )

    private fun playlistRepository(detailResponse: CompletableDeferred<NetworkResult<PlaylistDetailDto>>): BrowseRepository {
        val session = SessionData("https://demo", "user", "token", "salt")
        return BrowseRepository(
            sessionProvider = SessionProvider { session },
            clientProvider =
                NavidromeClientProvider {
                    object : NavidromeClient(it) {
                        override suspend fun getPlaylists(): NetworkResult<List<PlaylistDto>> = NetworkResult.Success(emptyList())

                        override suspend fun getPlaylist(playlistId: String): NetworkResult<PlaylistDetailDto> = detailResponse.await()
                    }
                },
        )
    }
}
