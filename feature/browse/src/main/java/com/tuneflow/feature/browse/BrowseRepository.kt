package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumDetail
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.DataStoreSessionProvider
import com.tuneflow.core.network.DefaultNavidromeClientProvider
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
import com.tuneflow.core.network.toSummary

class BrowseRepository(
    private val sessionProvider: SessionProvider,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
) {
    private data class SessionClient(
        val session: SessionData,
        val client: NavidromeClient,
    )

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

    suspend fun getPlaylists(): Result<List<PlaylistSummary>> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getPlaylists()) {
            is NetworkResult.Success -> Result.success(result.data.map { it.toSummary() })
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getPlaylistDetail(playlistId: String): Result<PlaylistDetail> {
        val sessionClient = requireSessionClient().getOrElse { return Result.failure(it) }
        return when (val result = sessionClient.client.getPlaylist(playlistId)) {
            is NetworkResult.Success -> Result.success(result.data.toDetail().withArtwork(sessionClient.session))
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
}
