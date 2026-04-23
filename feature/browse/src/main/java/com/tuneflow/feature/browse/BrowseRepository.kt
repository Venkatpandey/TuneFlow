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
import com.tuneflow.core.network.TrackSummary
import com.tuneflow.core.network.toBundle
import com.tuneflow.core.network.toDetail
import com.tuneflow.core.network.toSummary

class BrowseRepository(
    private val sessionProvider: SessionProvider,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
) {
    constructor(sessionStore: SessionStore) : this(
        sessionProvider = DataStoreSessionProvider(sessionStore),
        clientProvider = DefaultNavidromeClientProvider,
    )

    suspend fun getAlbums(
        size: Int,
        offset: Int,
    ): Result<List<AlbumSummary>> {
        val session = requireSession() ?: return Result.failure(IllegalStateException("Not logged in"))
        val client = clientOrFailure(session).getOrElse { return Result.failure(it) }
        return when (val result = client.getAlbums(size, offset)) {
            is NetworkResult.Success -> Result.success(result.data.map { it.toSummary().withArtwork(session) })
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getAlbumDetail(albumId: String): Result<AlbumDetail> {
        val session = requireSession() ?: return Result.failure(IllegalStateException("Not logged in"))
        val client = clientOrFailure(session).getOrElse { return Result.failure(it) }
        return when (val result = client.getAlbum(albumId)) {
            is NetworkResult.Success -> Result.success(result.data.toDetail().withArtwork(session))
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getPlaylists(): Result<List<PlaylistSummary>> {
        val session = requireSession() ?: return Result.failure(IllegalStateException("Not logged in"))
        val client = clientOrFailure(session).getOrElse { return Result.failure(it) }
        return when (val result = client.getPlaylists()) {
            is NetworkResult.Success -> Result.success(result.data.map { it.toSummary() })
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun getPlaylistDetail(playlistId: String): Result<PlaylistDetail> {
        val session = requireSession() ?: return Result.failure(IllegalStateException("Not logged in"))
        val client = clientOrFailure(session).getOrElse { return Result.failure(it) }
        return when (val result = client.getPlaylist(playlistId)) {
            is NetworkResult.Success -> Result.success(result.data.toDetail().withArtwork(session))
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun search(query: String): Result<SearchBundle> {
        val session = requireSession() ?: return Result.failure(IllegalStateException("Not logged in"))
        val client = clientOrFailure(session).getOrElse { return Result.failure(it) }
        return when (val result = client.search(query)) {
            is NetworkResult.Success -> Result.success(result.data.toBundle().withArtwork(session))
            is NetworkResult.Error -> Result.failure(IllegalStateException(result.message))
        }
    }

    suspend fun streamUrl(trackId: String): String {
        val session = requireSession() ?: return ""
        val client = clientOrFailure(session).getOrElse { return "" }
        return client.streamUrl(trackId)
    }

    suspend fun coverArtUrl(coverArtId: String?): String? {
        val session = requireSession()
        return coverArtUrl(session, coverArtId)
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

    private suspend fun requireSession(): SessionData? = sessionProvider.currentSession()

    private fun coverArtUrl(
        session: SessionData?,
        coverArtId: String?,
    ): String? {
        return if (coverArtId.isNullOrBlank() || session == null) {
            null
        } else {
            "${session.serverUrl}/rest/getCoverArt.view?id=$coverArtId&u=${session.username}&t=${session.token}&s=${session.salt}&v=1.16.1&c=TuneFlow&f=json"
        }
    }

    private fun AlbumSummary.withArtwork(session: SessionData): AlbumSummary {
        return copy(artUrl = coverArtUrl(session, coverArtId))
    }

    private fun AlbumDetail.withArtwork(session: SessionData): AlbumDetail {
        return copy(
            artUrl = coverArtUrl(session, coverArtId),
            tracks = tracks.map { it.withArtwork(session) },
        )
    }

    private fun TrackSummary.withArtwork(session: SessionData): TrackSummary {
        return copy(artUrl = coverArtUrl(session, coverArtId))
    }

    private fun PlaylistDetail.withArtwork(session: SessionData): PlaylistDetail {
        return copy(tracks = tracks.map { it.withArtwork(session) })
    }

    private fun SearchBundle.withArtwork(session: SessionData): SearchBundle {
        return copy(
            albums = albums.map { it.withArtwork(session) },
            tracks = tracks.map { it.withArtwork(session) },
        )
    }
}
