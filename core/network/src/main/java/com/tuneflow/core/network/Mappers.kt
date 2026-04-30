package com.tuneflow.core.network

data class AlbumSummary(
    val id: String,
    val title: String,
    val artist: String,
    val coverArtId: String?,
    val artUrl: String? = null,
)

data class AlbumDetail(
    val id: String,
    val title: String,
    val artist: String,
    val coverArtId: String?,
    val artUrl: String? = null,
    val tracks: List<TrackSummary>,
)

data class TrackSummary(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val coverArtId: String?,
    val artUrl: String? = null,
)

data class PlaylistSummary(
    val id: String,
    val name: String,
    val songCount: Int,
    val durationSec: Int,
    val artUrls: List<String> = emptyList(),
)

data class PlaylistDetail(
    val id: String,
    val name: String,
    val tracks: List<TrackSummary>,
)

data class ArtistSummary(
    val id: String,
    val name: String,
    val albumCount: Int,
    val artUrl: String? = null,
)

data class ArtistDetail(
    val id: String,
    val name: String,
    val albumCount: Int,
    val albums: List<AlbumSummary>,
    val artUrl: String? = null,
)

data class FavoritesBundle(
    val albums: List<AlbumSummary>,
    val tracks: List<TrackSummary>,
)

data class SearchBundle(
    val artists: List<ArtistSummary>,
    val albums: List<AlbumSummary>,
    val tracks: List<TrackSummary>,
)

fun AlbumDto.toSummary(): AlbumSummary {
    return AlbumSummary(
        id = id,
        title = name,
        artist = artist.orEmpty().ifBlank { "Unknown Artist" },
        coverArtId = coverArt,
    )
}

fun AlbumDetailDto.toDetail(): AlbumDetail {
    return AlbumDetail(
        id = id,
        title = name,
        artist = artist.orEmpty().ifBlank { "Unknown Artist" },
        coverArtId = coverArt,
        tracks = song.map { it.toTrack() },
    )
}

fun SongDto.toTrack(): TrackSummary {
    return TrackSummary(
        id = id,
        title = title,
        artist = artist.orEmpty().ifBlank { "Unknown Artist" },
        album = album.orEmpty().ifBlank { "Unknown Album" },
        durationSec = duration ?: 0,
        coverArtId = coverArt,
    )
}

fun PlaylistDto.toSummary(): PlaylistSummary {
    return PlaylistSummary(
        id = id,
        name = name,
        songCount = songCount ?: 0,
        durationSec = duration ?: 0,
    )
}

fun PlaylistDetailDto.toDetail(): PlaylistDetail {
    return PlaylistDetail(
        id = id,
        name = name,
        tracks = entry.map { it.toTrack() },
    )
}

fun ArtistDto.toSummary(): ArtistSummary {
    return ArtistSummary(
        id = id,
        name = name,
        albumCount = albumCount ?: 0,
    )
}

fun ArtistDetailDto.toDetail(): ArtistDetail {
    val albums = album.map { it.toSummary() }
    return ArtistDetail(
        id = id,
        name = name,
        albumCount = albumCount ?: albums.size,
        albums = albums,
    )
}

fun Starred2Dto.toFavoritesBundle(): FavoritesBundle {
    return FavoritesBundle(
        albums = album.map { it.toSummary() },
        tracks = song.map { it.toTrack() },
    )
}

fun SearchResult3Dto.toBundle(): SearchBundle {
    return SearchBundle(
        artists = artist.map { it.toSummary() },
        albums = album.map { it.toSummary() },
        tracks = song.map { it.toTrack() },
    )
}
