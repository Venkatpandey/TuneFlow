package com.tuneflow.feature.browse

import com.google.gson.Gson
import com.tuneflow.core.network.AlbumDetail
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistDetail
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.PlaylistDetail
import com.tuneflow.core.network.SearchBundle
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.TrackSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private const val SEARCH_CACHE_SIZE = 20
private const val DETAIL_CACHE_SIZE = 30
private const val ARTWORK_CACHE_SIZE = 30
private const val CACHE_FORMAT_VERSION = 1
internal const val BROWSE_CACHE_MAX_AGE_MILLIS = 14L * 24L * 60L * 60L * 1_000L

internal class BrowseRequestCache(
    private val storage: BrowseCacheStorage = NoOpBrowseCacheStorage,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val sessionMutex = Mutex()
    private val storageMutex = Mutex()
    private val gson = Gson()
    private var activeSession: BrowseSessionKey? = null
    private var sessionInitialized = false
    private var generation = 0L
    private var revision = 0L

    private val searches = RequestBucket<SearchBundle>(SEARCH_CACHE_SIZE)
    private val albums = RequestBucket<AlbumDetail>(DETAIL_CACHE_SIZE)
    private val artists = RequestBucket<ArtistDetail>(DETAIL_CACHE_SIZE)
    private val playlists = RequestBucket<PlaylistDetail>(DETAIL_CACHE_SIZE)
    private val playlistArtwork = LinkedHashMap<String, List<String>>(ARTWORK_CACHE_SIZE, 0.75f, true)
    private val artistArtwork = LinkedHashMap<String, String?>(ARTWORK_CACHE_SIZE, 0.75f, true)

    suspend fun synchronizeSession(session: SessionData?) {
        sessionMutex.withLock {
            val sessionKey = session?.toBrowseSessionKey()
            val changed =
                mutex.withLock {
                    if (sessionInitialized && activeSession == sessionKey) {
                        false
                    } else {
                        activateSession(sessionKey)
                        true
                    }
                }

            if (changed) {
                synchronizeStorage(session, sessionKey)
            }
        }
    }

    suspend fun search(
        session: SessionData,
        query: String,
        load: suspend () -> Result<SearchBundle>,
    ): Result<SearchBundle> = getOrLoad(session, query, searches, load)

    suspend fun album(
        session: SessionData,
        albumId: String,
        load: suspend () -> Result<AlbumDetail>,
    ): Result<AlbumDetail> = getOrLoad(session, albumId, albums, load)

    suspend fun artist(
        session: SessionData,
        artistId: String,
        load: suspend () -> Result<ArtistDetail>,
    ): Result<ArtistDetail> = getOrLoad(session, artistId, artists, load)

    suspend fun playlist(
        session: SessionData,
        playlistId: String,
        load: suspend () -> Result<PlaylistDetail>,
    ): Result<PlaylistDetail> = getOrLoad(session, playlistId, playlists, load)

    suspend fun artistArtwork(session: SessionData): Map<String, String?> =
        mutex.withLock {
            if (isActive(session)) artistArtwork.toMap() else emptyMap()
        }

    suspend fun putArtistArtwork(
        session: SessionData,
        artistId: String,
        artUrl: String?,
    ) {
        mutex.withLock {
            if (isActive(session)) artistArtwork.putBounded(artistId, artUrl, ARTWORK_CACHE_SIZE)
        }
    }

    suspend fun playlistArtwork(
        session: SessionData,
        playlistId: String,
    ): List<String>? =
        mutex.withLock {
            if (isActive(session)) playlistArtwork[playlistId] else null
        }

    suspend fun putPlaylistArtwork(
        session: SessionData,
        playlistId: String,
        artUrls: List<String>,
    ) {
        mutex.withLock {
            if (isActive(session)) playlistArtwork.putBounded(playlistId, artUrls, ARTWORK_CACHE_SIZE)
        }
    }

    private suspend fun <Value : Any> getOrLoad(
        session: SessionData,
        key: String,
        bucket: RequestBucket<Value>,
        load: suspend () -> Result<Value>,
    ): Result<Value> {
        val lookup =
            mutex.withLock {
                if (!isActive(session)) {
                    CacheLookup.SessionChanged
                } else {
                    bucket.cached(key, currentTimeMillis())?.let { return@withLock CacheLookup.Cached(it) }
                    bucket.inFlight[key]?.let { return@withLock CacheLookup.Pending(it) }

                    val deferred = CompletableDeferred<Result<Value>>()
                    bucket.inFlight[key] = deferred
                    CacheLookup.Load(generation, deferred)
                }
            }

        return when (lookup) {
            is CacheLookup.Cached -> Result.success(lookup.value)
            is CacheLookup.Pending -> lookup.deferred.await()
            is CacheLookup.Load -> loadAndShare(key, bucket, lookup, load)
            CacheLookup.SessionChanged -> sessionChangedFailure()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <Value : Any> loadAndShare(
        key: String,
        bucket: RequestBucket<Value>,
        lookup: CacheLookup.Load<Value>,
        load: suspend () -> Result<Value>,
    ): Result<Value> {
        val result =
            try {
                load()
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    cancelLoad(key, bucket, lookup, cancelled)
                }
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }

        val completion =
            mutex.withLock {
                if (generation != lookup.generation || bucket.inFlight[key] !== lookup.deferred) {
                    lookup.deferred.cancel()
                    CacheCompletion(sessionChangedFailure(), null)
                } else {
                    result.getOrNull()?.let { bucket.cache(key, it, currentTimeMillis()) }
                    bucket.inFlight.remove(key)
                    lookup.deferred.complete(result)
                    val snapshot = if (result.isSuccess) createSnapshot() else null
                    CacheCompletion(result, snapshot)
                }
            }

        completion.snapshot?.let { persist(it) }
        return completion.result
    }

    private suspend fun <Value : Any> cancelLoad(
        key: String,
        bucket: RequestBucket<Value>,
        lookup: CacheLookup.Load<Value>,
        cancelled: CancellationException,
    ) {
        mutex.withLock {
            if (bucket.inFlight[key] === lookup.deferred) {
                bucket.inFlight.remove(key)
                lookup.deferred.cancel(cancelled)
            }
        }
    }

    private fun activateSession(sessionKey: BrowseSessionKey?) {
        sessionInitialized = true
        activeSession = sessionKey
        generation += 1
        revision += 1
        searches.clear()
        albums.clear()
        artists.clear()
        playlists.clear()
        playlistArtwork.clear()
        artistArtwork.clear()
    }

    private suspend fun synchronizeStorage(
        session: SessionData?,
        sessionKey: BrowseSessionKey?,
    ) {
        val expectedGeneration = mutex.withLock { generation }
        storageMutex.withLock {
            if (!isCurrentGeneration(expectedGeneration)) return

            if (session == null || sessionKey == null) {
                clearStorage()
            } else {
                restoreStorage(session, sessionKey, expectedGeneration)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun restoreStorage(
        session: SessionData,
        sessionKey: BrowseSessionKey,
        expectedGeneration: Long,
    ) {
        try {
            val encoded = storage.read() ?: return
            val persisted = gson.fromJson(encoded, PersistedBrowseCache::class.java)
            if (persisted.version != CACHE_FORMAT_VERSION || persisted.sessionFingerprint != sessionKey.fingerprint()) {
                storage.clear()
                return
            }

            mutex.withLock {
                if (generation == expectedGeneration && activeSession == sessionKey) {
                    restoreSnapshot(persisted, session)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            clearStorage()
        }
    }

    private fun restoreSnapshot(
        persisted: PersistedBrowseCache,
        session: SessionData,
    ) {
        val now = currentTimeMillis()
        searches.restore(persisted.searches, now) { it.withArtwork(session) }
        albums.restore(persisted.albums, now) { it.withArtwork(session) }
        artists.restore(persisted.artists, now) { it.withArtwork(session) }
        playlists.restore(persisted.playlists, now) { it.withArtwork(session) }

        artists.freshValues(now).forEach { artist ->
            artistArtwork.putBounded(artist.id, artist.artUrl, ARTWORK_CACHE_SIZE)
        }
        playlists.freshValues(now).forEach { playlist ->
            val artUrls = playlist.tracks.mapNotNull { it.artUrl }.distinct().take(4)
            playlistArtwork.putBounded(playlist.id, artUrls, ARTWORK_CACHE_SIZE)
        }
        revision += 1
    }

    private fun createSnapshot(): CacheSnapshot {
        val now = currentTimeMillis()
        revision += 1
        return CacheSnapshot(
            generation = generation,
            revision = revision,
            persisted =
                PersistedBrowseCache(
                    version = CACHE_FORMAT_VERSION,
                    sessionFingerprint = checkNotNull(activeSession).fingerprint(),
                    searches = searches.snapshot(now) { it.withoutArtwork() },
                    albums = albums.snapshot(now) { it.withoutArtwork() },
                    artists = artists.snapshot(now) { it.withoutArtwork() },
                    playlists = playlists.snapshot(now) { it.withoutArtwork() },
                ),
        )
    }

    private suspend fun persist(snapshot: CacheSnapshot) {
        storageMutex.withLock {
            val current =
                mutex.withLock {
                    generation == snapshot.generation && revision == snapshot.revision && activeSession != null
                }
            if (!current) return

            try {
                storage.write(gson.toJson(snapshot.persisted))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Cache persistence is best effort; network result remains usable.
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun clearStorage() {
        try {
            storage.clear()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Nothing else can safely recover a failed cache deletion.
        }
    }

    private suspend fun isCurrentGeneration(expectedGeneration: Long): Boolean = mutex.withLock { generation == expectedGeneration }

    private fun isActive(session: SessionData): Boolean = sessionInitialized && activeSession == session.toBrowseSessionKey()
}

private class RequestBucket<Value : Any>(private val maximumSize: Int) {
    private val values = LinkedHashMap<String, CacheEntry<Value>>(maximumSize, 0.75f, true)
    val inFlight = mutableMapOf<String, CompletableDeferred<Result<Value>>>()

    fun cached(
        key: String,
        now: Long,
    ): Value? {
        val entry = values[key] ?: return null
        return if (entry.isFresh(now)) {
            entry.value
        } else {
            values.remove(key)
            null
        }
    }

    fun cache(
        key: String,
        value: Value,
        now: Long,
    ) {
        values.putBounded(key, CacheEntry(value, now), maximumSize)
    }

    fun clear() {
        values.clear()
        inFlight.values.forEach { it.cancel() }
        inFlight.clear()
    }

    fun restore(
        restored: Map<String, CacheEntry<Value>>,
        now: Long,
        transform: (Value) -> Value,
    ) {
        restored
            .filterValues { it.isFresh(now) }
            .forEach { (key, entry) ->
                values.putBounded(key, entry.copy(value = transform(entry.value)), maximumSize)
            }
    }

    fun freshValues(now: Long): List<Value> = values.values.filter { it.isFresh(now) }.map { it.value }

    fun <StoredValue : Any> snapshot(
        now: Long,
        transform: (Value) -> StoredValue,
    ): Map<String, CacheEntry<StoredValue>> =
        values
            .filterValues { it.isFresh(now) }
            .mapValues { (_, entry) -> CacheEntry(transform(entry.value), entry.cachedAtMillis) }
}

private fun <Key, Value> LinkedHashMap<Key, Value>.putBounded(
    key: Key,
    value: Value,
    maximumSize: Int,
) {
    this[key] = value
    while (size > maximumSize) {
        val iterator = entries.iterator()
        iterator.next()
        iterator.remove()
    }
}

private sealed interface CacheLookup<out Value> {
    data class Cached<Value>(val value: Value) : CacheLookup<Value>

    data class Pending<Value>(val deferred: CompletableDeferred<Result<Value>>) : CacheLookup<Value>

    data class Load<Value>(
        val generation: Long,
        val deferred: CompletableDeferred<Result<Value>>,
    ) : CacheLookup<Value>

    data object SessionChanged : CacheLookup<Nothing>
}

private data class CacheCompletion<Value>(
    val result: Result<Value>,
    val snapshot: CacheSnapshot?,
)

private data class CacheSnapshot(
    val generation: Long,
    val revision: Long,
    val persisted: PersistedBrowseCache,
)

private data class CacheEntry<Value>(
    val value: Value,
    val cachedAtMillis: Long,
) {
    fun isFresh(now: Long): Boolean = now - cachedAtMillis <= BROWSE_CACHE_MAX_AGE_MILLIS
}

private data class PersistedBrowseCache(
    val version: Int,
    val sessionFingerprint: String,
    val searches: Map<String, CacheEntry<SearchBundle>>,
    val albums: Map<String, CacheEntry<AlbumDetail>>,
    val artists: Map<String, CacheEntry<ArtistDetail>>,
    val playlists: Map<String, CacheEntry<PlaylistDetail>>,
)

private data class BrowseSessionKey(
    val serverUrl: String,
    val username: String,
    val token: String,
    val salt: String,
) {
    fun fingerprint(): String {
        val source = listOf(serverUrl, username, token, salt).joinToString("\u0000")
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}

private fun SessionData.toBrowseSessionKey(): BrowseSessionKey =
    BrowseSessionKey(
        serverUrl = serverUrl.trimEnd('/'),
        username = username,
        token = token,
        salt = salt,
    )

private fun SearchBundle.withoutArtwork(): SearchBundle =
    copy(
        artists = artists.map(ArtistSummary::withoutArtwork),
        albums = albums.map(AlbumSummary::withoutArtwork),
        tracks = tracks.map(TrackSummary::withoutArtwork),
    )

private fun AlbumDetail.withoutArtwork(): AlbumDetail =
    copy(
        artUrl = null,
        tracks = tracks.map(TrackSummary::withoutArtwork),
    )

private fun ArtistDetail.withoutArtwork(): ArtistDetail =
    copy(
        artUrl = null,
        albums = albums.map(AlbumSummary::withoutArtwork),
    )

private fun PlaylistDetail.withoutArtwork(): PlaylistDetail = copy(tracks = tracks.map(TrackSummary::withoutArtwork))

private fun ArtistSummary.withoutArtwork(): ArtistSummary = copy(artUrl = null)

private fun AlbumSummary.withoutArtwork(): AlbumSummary = copy(artUrl = null)

private fun TrackSummary.withoutArtwork(): TrackSummary = copy(artUrl = null)

private fun <Value> sessionChangedFailure(): Result<Value> = Result.failure(IllegalStateException("Session changed"))
