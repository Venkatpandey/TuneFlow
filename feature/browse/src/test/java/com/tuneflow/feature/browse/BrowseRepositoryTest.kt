package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumDetailDto
import com.tuneflow.core.network.AlbumDto
import com.tuneflow.core.network.ArtistDetailDto
import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.PlaylistDetailDto
import com.tuneflow.core.network.SearchResult3Dto
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.SongDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

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

    @Test
    fun search_cachesNormalizedQuery() =
        runTest {
            val calls = AtomicInteger()
            val repository =
                repositoryWith(
                    object : NavidromeClient(session) {
                        override suspend fun search(query: String): NetworkResult<SearchResult3Dto> {
                            calls.incrementAndGet()
                            return NetworkResult.Success(SearchResult3Dto(song = listOf(SongDto(id = "s1", title = query))))
                        }
                    },
                )

            val first = repository.search("  Bowie ")
            val second = repository.search("bowie")

            assertTrue(first.isSuccess)
            assertEquals(first, second)
            assertEquals(1, calls.get())
        }

    @Test
    fun search_coalescesMatchingInFlightRequests() =
        runTest {
            val calls = AtomicInteger()
            val requestStarted = CompletableDeferred<Unit>()
            val releaseRequest = CompletableDeferred<Unit>()
            val repository =
                repositoryWith(
                    object : NavidromeClient(session) {
                        override suspend fun search(query: String): NetworkResult<SearchResult3Dto> {
                            calls.incrementAndGet()
                            requestStarted.complete(Unit)
                            releaseRequest.await()
                            return NetworkResult.Success(SearchResult3Dto())
                        }
                    },
                )

            val first = async { repository.search("Bowie") }
            requestStarted.await()
            val second = async { repository.search(" bowie ") }

            testScheduler.runCurrent()
            assertEquals(1, calls.get())
            assertFalse(second.isCompleted)

            releaseRequest.complete(Unit)
            assertTrue(first.await().isSuccess)
            assertTrue(second.await().isSuccess)
            assertEquals(1, calls.get())
        }

    @Test
    fun detailRequests_cacheAlbumArtistAndPlaylist() =
        runTest {
            val albumCalls = AtomicInteger()
            val artistCalls = AtomicInteger()
            val playlistCalls = AtomicInteger()
            val repository =
                repositoryWith(
                    object : NavidromeClient(session) {
                        override suspend fun getAlbum(albumId: String): NetworkResult<AlbumDetailDto> {
                            albumCalls.incrementAndGet()
                            return NetworkResult.Success(AlbumDetailDto(id = albumId, name = "Album"))
                        }

                        override suspend fun getArtist(artistId: String): NetworkResult<ArtistDetailDto> {
                            artistCalls.incrementAndGet()
                            return NetworkResult.Success(ArtistDetailDto(id = artistId, name = "Artist"))
                        }

                        override suspend fun getPlaylist(playlistId: String): NetworkResult<PlaylistDetailDto> {
                            playlistCalls.incrementAndGet()
                            return NetworkResult.Success(PlaylistDetailDto(id = playlistId, name = "Playlist"))
                        }
                    },
                )

            repeat(2) {
                assertTrue(repository.getAlbumDetail("album-1").isSuccess)
                assertTrue(repository.getArtistDetail("artist-1").isSuccess)
                assertTrue(repository.getPlaylistDetail("playlist-1").isSuccess)
            }

            assertEquals(1, albumCalls.get())
            assertEquals(1, artistCalls.get())
            assertEquals(1, playlistCalls.get())
        }

    @Test
    fun canceledRequest_canBeRetried() =
        runTest {
            val calls = AtomicInteger()
            val requestStarted = CompletableDeferred<Unit>()
            val repository =
                repositoryWith(
                    object : NavidromeClient(session) {
                        override suspend fun search(query: String): NetworkResult<SearchResult3Dto> {
                            return if (calls.incrementAndGet() == 1) {
                                requestStarted.complete(Unit)
                                awaitCancellation()
                            } else {
                                NetworkResult.Success(SearchResult3Dto())
                            }
                        }
                    },
                )

            val canceled = async { repository.search("retry") }
            requestStarted.await()
            canceled.cancelAndJoin()

            assertTrue(repository.search("retry").isSuccess)
            assertEquals(2, calls.get())
        }

    @Test
    fun caches_clearWhenSessionChanges() =
        runTest {
            var activeSession = session
            val calls = AtomicInteger()
            val repository =
                BrowseRepository(
                    sessionProvider = SessionProvider { activeSession },
                    clientProvider =
                        NavidromeClientProvider { requestedSession ->
                            object : NavidromeClient(requestedSession) {
                                override suspend fun search(query: String): NetworkResult<SearchResult3Dto> {
                                    calls.incrementAndGet()
                                    return NetworkResult.Success(
                                        SearchResult3Dto(
                                            album =
                                                listOf(
                                                    AlbumDto(
                                                        id = requestedSession.username,
                                                        name = requestedSession.username,
                                                    ),
                                                ),
                                        ),
                                    )
                                }
                            }
                        },
                )

            assertEquals("u", repository.search("same").getOrThrow().albums.single().title)
            assertEquals("u", repository.search("same").getOrThrow().albums.single().title)
            activeSession = session.copy(username = "other-user")
            assertEquals("other-user", repository.search("same").getOrThrow().albums.single().title)
            repository.synchronizeSession(null)
            activeSession = session
            assertEquals("u", repository.search("same").getOrThrow().albums.single().title)

            assertEquals(3, calls.get())
        }

    @Test
    fun failedRequests_areNotCached() =
        runTest {
            val calls = AtomicInteger()
            val repository =
                repositoryWith(
                    object : NavidromeClient(session) {
                        override suspend fun search(query: String): NetworkResult<SearchResult3Dto> {
                            return if (calls.incrementAndGet() == 1) {
                                NetworkResult.Error("temporary")
                            } else {
                                NetworkResult.Success(SearchResult3Dto())
                            }
                        }
                    },
                )

            assertTrue(repository.search("retry").isFailure)
            assertTrue(repository.search("retry").isSuccess)
            assertEquals(2, calls.get())
        }

    private fun repositoryWith(client: NavidromeClient): BrowseRepository =
        BrowseRepository(
            sessionProvider = SessionProvider { session },
            clientProvider = NavidromeClientProvider { client },
        )
}
