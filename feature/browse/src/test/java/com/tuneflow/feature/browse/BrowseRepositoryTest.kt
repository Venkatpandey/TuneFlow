package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumDto
import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.PlaylistDetailDto
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.SongDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseRepositoryTest {
    private val session = SessionData("https://demo", "u", "t", "s")

    @Test
    fun getAlbums_mapsData() =
        runTest {
            val repository =
                BrowseRepository(
                    sessionProvider = SessionProvider { session },
                    clientProvider =
                        NavidromeClientProvider {
                            object : NavidromeClient(it) {
                                override suspend fun getAlbums(
                                    size: Int,
                                    offset: Int,
                                ): NetworkResult<List<AlbumDto>> {
                                    return NetworkResult.Success(listOf(AlbumDto(id = "a1", name = "Album", artist = "Artist")))
                                }
                            }
                        },
                )

            val result = repository.getAlbums(size = 30, offset = 0)
            assertTrue(result.isSuccess)
            assertEquals("Album", result.getOrNull()?.first()?.title)
        }

    @Test
    fun getPlaylistDetail_mapsTracks() =
        runTest {
            val repository =
                BrowseRepository(
                    sessionProvider = SessionProvider { session },
                    clientProvider =
                        NavidromeClientProvider {
                            object : NavidromeClient(it) {
                                override suspend fun getPlaylist(playlistId: String): NetworkResult<PlaylistDetailDto> {
                                    return NetworkResult.Success(
                                        PlaylistDetailDto(
                                            id = "p1",
                                            name = "Fav",
                                            entry = listOf(SongDto(id = "s1", title = "Song A", artist = "A")),
                                        ),
                                    )
                                }
                            }
                        },
                )

            val result = repository.getPlaylistDetail("p1")
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()?.tracks?.size)
        }

    @Test
    fun search_returnsFailure_whenNoSession() =
        runTest {
            val repository =
                BrowseRepository(
                    sessionProvider = SessionProvider { null },
                    clientProvider = NavidromeClientProvider { error("not used") },
                )

            val result = repository.search("abc")
            assertTrue(result.isFailure)
            assertEquals("Not logged in", result.exceptionOrNull()?.message)
        }
}
