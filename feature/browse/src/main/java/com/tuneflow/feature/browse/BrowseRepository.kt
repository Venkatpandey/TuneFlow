@file:Suppress("TooManyFunctions")

package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumDetail
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistDetail
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.DataStoreSessionProvider
import com.tuneflow.core.network.DefaultNavidromeClientProvider
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.PlaylistDetail
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.SearchBundle
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.network.TrackFavoriteStore
import com.tuneflow.core.network.TrackStreamOptions
import com.tuneflow.core.network.toBundle
import com.tuneflow.core.network.toDetail
import com.tuneflow.core.network.toFavoritesBundle
import com.tuneflow.core.network.toSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale

class BrowseRepository(
    private val sessionProvider: SessionProvider,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
    val favoriteStore: TrackFavoriteStore = TrackFavoriteStore(sessionProvider, clientProvider),
    cacheStorage: BrowseCacheStorage = NoOpBrowseCacheStorage,
) {
    private data class SessionClient(
        val session: SessionData,
        val client: NavidromeClient,
    )

    private val requestCache = BrowseRequestCache(cacheStorage)

    constructor(
        sessionStore: SessionStore,
        favoriteStore: TrackFavoriteStore? = null,
        cacheStorage: BrowseCacheStorage = NoOpBrowseCacheStorage,
    ) : this(
        sessionProvider = DataStoreSessionProvider(sessionStore),
        clientProvider = DefaultNavidromeClientProvider,
        favoriteStore =
            favoriteStore ?: TrackFavoriteStore(
                DataStoreSessionProvider(sessionStore),
                DefaultNavidromeClientProvider,
            ),
        cacheStorage = cacheStorage,
    )

    suspend fun getAlbums(
        size: Int,
        offset: Int,
    ): Result<List<AlbumSummary>> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getAlbums(size, offset)) {
            is NetworkResult.Success -> Result.success(result.data.map { it.toSummary().withArtwork(sessionClient.session) })
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getAlbumDetail(albumId: String): Result<AlbumDetail> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        val detail =
            requestCache.album(sessionClient.session, albumId) {
                when (val result = sessionClient.client.getAlbum(albumId)) {
                    is NetworkResult.Success -> {
                        val loaded = result.data.toDetail().withArtwork(sessionClient.session)
                        favoriteStore.seed(sessionClient.session, loaded.tracks)
                        Result.success(loaded)
                    }
                    is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
                }
            }
        detail.getOrNull()?.let { favoriteStore.seedMissing(sessionClient.session, it.tracks) }
        return detail
    }

    suspend fun getArtists(): Result<List<ArtistSummary>> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        val artwork = requestCache.artistArtwork(sessionClient.session)
        return when (val result = sessionClient.client.getArtists()) {
            is NetworkResult.Success ->
                Result.success(
                    result.data
                        .map { artist ->
                            artist.toSummary().withArtwork(artwork[artist.id])
                        }
                        .sortedBy { it.name.lowercase() },
                )
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getArtistDetail(artistId: String): Result<ArtistDetail> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return requestCache.artist(sessionClient.session, artistId) {
            when (val result = sessionClient.client.getArtist(artistId)) {
                is NetworkResult.Success -> {
                    val detail = result.data.toDetail().withArtwork(sessionClient.session)
                    requestCache.putArtistArtwork(sessionClient.session, detail.id, detail.artUrl)
                    Result.success(detail)
                }
                is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
            }
        }
    }

    suspend fun getPlaylists(): Result<List<PlaylistSummary>> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getPlaylists()) {
            is NetworkResult.Success -> {
                val playlists =
                    result.data.map { playlist ->
                        val summary = playlist.toSummary()
                        val artUrls = requestCache.playlistArtwork(sessionClient.session, summary.id).orEmpty()
                        summary.withArtwork(artUrls)
                    }
                Result.success(playlists)
            }
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun hydratePlaylistArtwork(playlists: List<PlaylistSummary>): Result<List<PlaylistSummary>> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return coroutineScope {
            Result.success(
                playlists
                    .map { playlist ->
                        async {
                            playlist.withArtwork(resolvePlaylistArtUrls(playlist.id, sessionClient))
                        }
                    }.awaitAll(),
            )
        }
    }

    suspend fun getPlaylistDetail(playlistId: String): Result<PlaylistDetail> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return loadPlaylistDetail(playlistId, sessionClient)
    }

    suspend fun getFavorites(): Result<FavoritesBundle> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getStarred2()) {
            is NetworkResult.Success -> {
                val favorites = result.data.toFavoritesBundle().withArtwork(sessionClient.session)
                favoriteStore.seedFavoritesSnapshot(sessionClient.session, favorites.tracks)
                Result.success(favorites)
            }
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun search(query: String): Result<SearchBundle> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        val requestQuery = query.trim()
        val cacheKey = requestQuery.lowercase(Locale.ROOT)
        val search =
            requestCache.search(sessionClient.session, cacheKey) {
                when (val result = sessionClient.client.search(requestQuery)) {
                    is NetworkResult.Success -> {
                        val bundle = result.data.toBundle().withArtwork(sessionClient.session)
                        favoriteStore.seed(sessionClient.session, bundle.tracks)
                        Result.success(bundle)
                    }
                    is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
                }
            }
        search.getOrNull()?.let { favoriteStore.seedMissing(sessionClient.session, it.tracks) }
        return search
    }

    suspend fun synchronizeSession(session: SessionData?) {
        requestCache.synchronizeSession(session)
        favoriteStore.synchronizeSession(session)
    }

    suspend fun streamOptions(trackId: String): TrackStreamOptions {
        val sessionClient =
            requireSessionClient().getOrElse {
                return TrackStreamOptions(
                    directUrl = "",
                    fallbackMp3Url = "",
                )
            }
        return sessionClient.client.streamOptions(trackId)
    }

    private fun requireClient(session: SessionData): NavidromeClient {
        return clientProvider.create(session)
    }

    private fun clientOrFailure(session: SessionData): Result<NavidromeClient> {
        return runCatching { requireClient(session) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = {
                Result.failure(
                    IllegalStateException(it.message ?: "Invalid server URL.", it),
                )
            },
        )
    }

    private suspend fun requireSessionClient(): Result<SessionClient> {
        val session = requireSession()
        synchronizeSession(session)
        session ?: return Result.failure(IllegalStateException("Not logged in"))
        return clientOrFailure(session).map { client ->
            SessionClient(session = session, client = client)
        }
    }

    private suspend fun requireSession(): SessionData? = sessionProvider.currentSession()

    private suspend fun resolvePlaylistArtUrls(
        playlistId: String,
        sessionClient: SessionClient,
    ): List<String> {
        requestCache.playlistArtwork(sessionClient.session, playlistId)?.let { return it }

        val detail = loadPlaylistDetail(playlistId, sessionClient)
        val artUrls =
            detail.fold(
                onSuccess = { playlist -> playlist.tracks.mapNotNull { it.artUrl }.distinct().take(4) },
                onFailure = { emptyList() },
            )

        if (detail.isSuccess) {
            requestCache.putPlaylistArtwork(sessionClient.session, playlistId, artUrls)
        }
        return artUrls
    }

    private suspend fun loadPlaylistDetail(
        playlistId: String,
        sessionClient: SessionClient,
    ): Result<PlaylistDetail> {
        val detail =
            requestCache.playlist(sessionClient.session, playlistId) {
                when (val result = sessionClient.client.getPlaylist(playlistId)) {
                    is NetworkResult.Success -> {
                        val loaded = result.data.toDetail().withArtwork(sessionClient.session)
                        favoriteStore.seed(sessionClient.session, loaded.tracks)
                        Result.success(loaded)
                    }
                    is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
                }
            }
        detail.getOrNull()?.let { favoriteStore.seedMissing(sessionClient.session, it.tracks) }
        return detail
    }
}
