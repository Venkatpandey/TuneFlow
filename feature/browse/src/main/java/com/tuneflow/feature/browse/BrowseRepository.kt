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
import com.tuneflow.core.network.toBundle
import com.tuneflow.core.network.toDetail
import com.tuneflow.core.network.toFavoritesBundle
import com.tuneflow.core.network.toSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class BrowseRepository(
    private val sessionProvider: SessionProvider,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
) {
    private data class SessionClient(
        val session: SessionData,
        val client: NavidromeClient,
    )

    private val playlistArtCache = mutableMapOf<String, List<String>>()
    private val artistArtCache = mutableMapOf<String, String?>()

    constructor(sessionStore: SessionStore) : this(
        sessionProvider = DataStoreSessionProvider(sessionStore),
        clientProvider = DefaultNavidromeClientProvider,
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
        return when (val result = sessionClient.client.getAlbum(albumId)) {
            is NetworkResult.Success -> Result.success(result.data.toDetail().withArtwork(sessionClient.session))
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getArtists(): Result<List<ArtistSummary>> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getArtists()) {
            is NetworkResult.Success ->
                Result.success(
                    result.data
                        .map { artist ->
                            artist.toSummary().withArtwork(artistArtCache[artist.id])
                        }
                        .sortedBy { it.name.lowercase() },
                )
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getArtistDetail(artistId: String): Result<ArtistDetail> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getArtist(artistId)) {
            is NetworkResult.Success -> {
                val detail = result.data.toDetail().withArtwork(sessionClient.session)
                artistArtCache[detail.id] = detail.artUrl
                Result.success(detail)
            }
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getPlaylists(): Result<List<PlaylistSummary>> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getPlaylists()) {
            is NetworkResult.Success ->
                Result.success(
                    result.data.map { playlist ->
                        val summary = playlist.toSummary()
                        summary.withArtwork(playlistArtCache[summary.id].orEmpty())
                    },
                )
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
        return when (val result = sessionClient.client.getPlaylist(playlistId)) {
            is NetworkResult.Success -> Result.success(result.data.toDetail().withArtwork(sessionClient.session))
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getFavorites(): Result<FavoritesBundle> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getStarred2()) {
            is NetworkResult.Success -> Result.success(result.data.toFavoritesBundle().withArtwork(sessionClient.session))
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun search(query: String): Result<SearchBundle> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.search(query)) {
            is NetworkResult.Success -> Result.success(result.data.toBundle().withArtwork(sessionClient.session))
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun streamUrl(trackId: String): String {
        val sessionClient = requireSessionClient().getOrElse { return "" }
        return sessionClient.client.streamUrl(trackId)
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
        val session = requireSession() ?: return Result.failure(IllegalStateException("Not logged in"))
        return clientOrFailure(session).map { client ->
            SessionClient(session = session, client = client)
        }
    }

    private suspend fun requireSession(): SessionData? = sessionProvider.currentSession()

    private suspend fun resolvePlaylistArtUrls(
        playlistId: String,
        sessionClient: SessionClient,
    ): List<String> {
        playlistArtCache[playlistId]?.let { return it }

        val artUrls =
            when (val result = sessionClient.client.getPlaylist(playlistId)) {
                is NetworkResult.Success ->
                    result.data.entry
                        .mapNotNull { track -> coverArtUrl(sessionClient.session, track.coverArt) }
                        .distinct()
                        .take(4)
                is NetworkResult.Error -> emptyList()
            }

        playlistArtCache[playlistId] = artUrls
        return artUrls
    }
}
