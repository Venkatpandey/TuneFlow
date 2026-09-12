package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumDetail
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.SearchBundle
import com.tuneflow.core.network.SessionData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class BrowseRequestCacheTest {
    private val session = SessionData("https://demo", "user", "secret-token", "secret-salt")

    @Test
    fun persistedCache_survivesRepositoryRecreationWithoutStoringCredentials() =
        runTest {
            val storage = MemoryBrowseCacheStorage()
            val first = BrowseRequestCache(storage) { 1_000L }
            first.synchronizeSession(session)
            val expected = searchBundle(session)
            val expectedAlbum = albumDetail(session)
            assertEquals(expected, first.search(session, "query") { Result.success(expected) }.getOrThrow())
            assertEquals(expectedAlbum, first.album(session, "album") { Result.success(expectedAlbum) }.getOrThrow())
            assertFalse(storage.value.orEmpty().contains(session.token))
            assertFalse(storage.value.orEmpty().contains(session.salt))

            val searchCalls = AtomicInteger()
            val albumCalls = AtomicInteger()
            val restored = BrowseRequestCache(storage) { 2_000L }
            restored.synchronizeSession(session)
            val restoredSearch =
                restored.search(session, "query") {
                    searchCalls.incrementAndGet()
                    Result.success(SearchBundle(emptyList(), emptyList(), emptyList()))
                }
            val restoredAlbum =
                restored.album(session, "album") {
                    albumCalls.incrementAndGet()
                    Result.success(expectedAlbum.copy(title = "Network"))
                }

            assertEquals(expected, restoredSearch.getOrThrow())
            assertEquals(expectedAlbum, restoredAlbum.getOrThrow())
            assertEquals(0, searchCalls.get())
            assertEquals(0, albumCalls.get())
        }

    @Test
    fun persistedCache_expiresAfterFourteenDays() =
        runTest {
            val storage = MemoryBrowseCacheStorage()
            var now = 1_000L
            val expected = searchBundle(session)
            BrowseRequestCache(storage) { now }.also { cache ->
                cache.synchronizeSession(session)
                cache.search(session, "query") { Result.success(expected) }
            }

            now += BROWSE_CACHE_MAX_AGE_MILLIS
            val atBoundaryCalls = AtomicInteger()
            BrowseRequestCache(storage) { now }.also { cache ->
                cache.synchronizeSession(session)
                assertTrue(
                    cache.search(session, "query") {
                        atBoundaryCalls.incrementAndGet()
                        Result.success(expected)
                    }.isSuccess,
                )
            }
            assertEquals(0, atBoundaryCalls.get())

            now += 1L
            val expiredCalls = AtomicInteger()
            BrowseRequestCache(storage) { now }.also { cache ->
                cache.synchronizeSession(session)
                assertTrue(
                    cache.search(session, "query") {
                        expiredCalls.incrementAndGet()
                        Result.success(expected)
                    }.isSuccess,
                )
            }
            assertEquals(1, expiredCalls.get())
        }

    @Test
    fun logout_clearsPersistedCache() =
        runTest {
            val storage = MemoryBrowseCacheStorage()
            val cache = BrowseRequestCache(storage)
            cache.synchronizeSession(session)
            cache.search(session, "query") { Result.success(searchBundle(session)) }
            assertTrue(storage.value != null)

            cache.synchronizeSession(null)

            assertNull(storage.value)
        }

    private fun searchBundle(session: SessionData): SearchBundle =
        SearchBundle(
            artists = emptyList(),
            albums =
                listOf(
                    AlbumSummary(
                        id = "album",
                        title = "Album",
                        artist = "Artist",
                        coverArtId = "cover",
                        artUrl = coverArtUrl(session, "cover"),
                    ),
                ),
            tracks = emptyList(),
        )

    private fun albumDetail(session: SessionData): AlbumDetail =
        AlbumDetail(
            id = "album",
            title = "Album",
            artist = "Artist",
            coverArtId = "cover",
            artUrl = coverArtUrl(session, "cover"),
            tracks = emptyList(),
        )
}

private class MemoryBrowseCacheStorage : BrowseCacheStorage {
    var value: String? = null

    override suspend fun read(): String? = value

    override suspend fun write(value: String) {
        this.value = value
    }

    override suspend fun clear() {
        value = null
    }
}
