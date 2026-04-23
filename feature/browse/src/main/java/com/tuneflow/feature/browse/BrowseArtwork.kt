package com.tuneflow.feature.browse

import com.tuneflow.core.network.AlbumDetail
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistDetail
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistDetail
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.SearchBundle
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.TrackSummary

internal fun coverArtUrl(
    session: SessionData?,
    coverArtId: String?,
): String? {
    return if (coverArtId.isNullOrBlank() || session == null) {
        null
    } else {
        "${session.serverUrl}/rest/getCoverArt.view?id=$coverArtId&u=${session.username}&t=${session.token}&s=${session.salt}&v=1.16.1&c=TuneFlow&f=json"
    }
}

internal fun AlbumSummary.withArtwork(session: SessionData): AlbumSummary {
    return copy(artUrl = coverArtUrl(session, coverArtId))
}

internal fun AlbumDetail.withArtwork(session: SessionData): AlbumDetail {
    return copy(
        artUrl = coverArtUrl(session, coverArtId),
        tracks = tracks.map { it.withArtwork(session) },
    )
}

internal fun TrackSummary.withArtwork(session: SessionData): TrackSummary {
    return copy(artUrl = coverArtUrl(session, coverArtId))
}

internal fun PlaylistSummary.withArtwork(artUrls: List<String>): PlaylistSummary {
    return copy(artUrls = artUrls)
}

internal fun PlaylistDetail.withArtwork(session: SessionData): PlaylistDetail {
    return copy(tracks = tracks.map { it.withArtwork(session) })
}

internal fun ArtistSummary.withArtwork(artUrl: String?): ArtistSummary {
    return copy(artUrl = artUrl)
}

internal fun ArtistDetail.withArtwork(session: SessionData): ArtistDetail {
    val artUrl = albums.firstNotNullOfOrNull { it.coverArtId?.let { coverArtUrl(session, it) } }
    return copy(
        artUrl = artUrl,
        albums = albums.map { it.withArtwork(session) },
    )
}

internal fun FavoritesBundle.withArtwork(session: SessionData): FavoritesBundle {
    return copy(
        albums = albums.map { it.withArtwork(session) },
        tracks = tracks.map { it.withArtwork(session) },
    )
}

internal fun SearchBundle.withArtwork(session: SessionData): SearchBundle {
    return copy(
        artists =
            artists.map { artist ->
                val matchedAlbum = albums.firstOrNull { it.artist == artist.name }
                artist.withArtwork(
                    matchedAlbum?.coverArtId?.let { coverArtUrl(session, it) },
                )
            },
        albums = albums.map { it.withArtwork(session) },
        tracks = tracks.map { it.withArtwork(session) },
    )
}
