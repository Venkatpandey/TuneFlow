package com.tuneflow.feature.browse

import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.PlaylistDetailDto
import com.tuneflow.core.network.PlaylistDto
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
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
import org.junit.Assert.assertNull
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
