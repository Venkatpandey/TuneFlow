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

internal fun resolveArtistArtUrl(
    session: SessionData?,
    coverArtId: String?,
    artistImageUrl: String?,
): String? =
    when {
        artistImageUrl.isNullOrBlank() -> coverArtUrl(session, coverArtId)
        artistImageUrl.startsWith("http://") || artistImageUrl.startsWith("https://") -> artistImageUrl
        session == null -> coverArtUrl(session, coverArtId)
        artistImageUrl.startsWith("/") -> "${session.serverUrl.trimEnd('/')}$artistImageUrl"
        else -> coverArtUrl(session, artistImageUrl)
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

internal fun ArtistSummary.withArtwork(
    session: SessionData,
    fallbackUrl: String? = null,
): ArtistSummary {
    val resolved = resolveArtistArtUrl(session, coverArtId, artistImageUrl) ?: fallbackUrl
    return copy(artUrl = resolved)
}

internal fun ArtistSummary.withArtwork(artUrl: String?): ArtistSummary {
    return copy(artUrl = artUrl)
}

internal fun ArtistDetail.withArtwork(
    session: SessionData,
    fallbackUrl: String? = null,
): ArtistDetail {
    val resolvedArtUrl =
        resolveArtistArtUrl(session, coverArtId, artistImageUrl)
            ?: albums.firstNotNullOfOrNull { it.coverArtId?.let { id -> coverArtUrl(session, id) } }
            ?: fallbackUrl
    return copy(
        artUrl = resolvedArtUrl,
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
                val matchedAlbum =
                    albums.firstOrNull { it.artist.equals(artist.name, ignoreCase = true) }
                val fallbackCover = matchedAlbum?.coverArtId?.let { coverArtUrl(session, it) }
                artist.withArtwork(session, fallbackUrl = fallbackCover)
            },
        albums = albums.map { it.withArtwork(session) },
        tracks = tracks.map { it.withArtwork(session) },
    )
}
