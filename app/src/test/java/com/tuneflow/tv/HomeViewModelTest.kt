package com.tuneflow.tv

import com.tuneflow.core.network.ArtistDto
import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.PlaylistDto
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.Starred2Dto
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.video.PreferredVideoLookupResult
import com.tuneflow.feature.video.PreferredVideoStore
import com.tuneflow.feature.video.VideoCandidate
import com.tuneflow.feature.video.VideoHistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            val viewModel = HomeViewModel(successfulBrowseRepository(), SlowPreferredVideoStore())

            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    private fun successfulBrowseRepository(): BrowseRepository {
        val session = SessionData("https://demo", "user", "token", "salt")
        return BrowseRepository(
            sessionProvider = SessionProvider { session },
            clientProvider =
                NavidromeClientProvider {
                    object : NavidromeClient(it) {
                        override suspend fun getAlbums(
                            size: Int,
                            offset: Int,
                        ) = NetworkResult.Success(emptyList<com.tuneflow.core.network.AlbumDto>())

                        override suspend fun getPlaylists() = NetworkResult.Success(emptyList<PlaylistDto>())

                        override suspend fun getStarred2() = NetworkResult.Success(Starred2Dto())

                        override suspend fun getArtists() = NetworkResult.Success(emptyList<ArtistDto>())
                    }
                },
        )
    }
}

private class SlowPreferredVideoStore : PreferredVideoStore {
    override val history: StateFlow<List<VideoHistoryEntry>> = MutableStateFlow(emptyList())

    override suspend fun lookup(trackId: String) = PreferredVideoLookupResult.Missing

    override suspend fun savePreferredVideo(
        trackId: String,
        candidate: VideoCandidate,
    ) = true

    override suspend fun markPlayed(trackId: String) = true

    override suspend fun deletePreferredVideo(trackId: String) = true

    override suspend fun refreshHistory(limit: Int): Boolean {
        delay(10_000L)
        return false
    }
}
