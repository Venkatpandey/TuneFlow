package com.tuneflow.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

private const val API_VERSION = "1.16.1"
private const val CLIENT_NAME = "TuneFlow"
private const val FORMAT = "json"

data class SubsonicEnvelope<T>(
    @SerializedName("subsonic-response") val response: T,
)

data class SubsonicError(
    val code: Int,
    val message: String,
)

data class BaseResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
)

data class AlbumListResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val albumList: AlbumListContainer? = null,
)

data class AlbumListContainer(
    val album: List<AlbumDto> = emptyList(),
)

data class AlbumDto(
    val id: String,
    val name: String,
    val artist: String? = null,
    val year: Int? = null,
    @SerializedName("coverArt") val coverArt: String? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
)

data class AlbumResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val album: AlbumDetailDto? = null,
)

data class AlbumDetailDto(
    val id: String,
    val name: String,
    val artist: String? = null,
    @SerializedName("coverArt") val coverArt: String? = null,
    val song: List<SongDto> = emptyList(),
)

data class SongDto(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val duration: Int? = null,
    val track: Int? = null,
    @SerializedName("coverArt") val coverArt: String? = null,
)

data class PlaylistsResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val playlists: PlaylistListContainer? = null,
)

data class PlaylistListContainer(
    val playlist: List<PlaylistDto> = emptyList(),
)

data class PlaylistDto(
    val id: String,
    val name: String,
    val songCount: Int? = null,
    val duration: Int? = null,
    val changed: String? = null,
)

data class PlaylistResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val playlist: PlaylistDetailDto? = null,
)

data class PlaylistDetailDto(
    val id: String,
    val name: String,
    val entry: List<SongDto> = emptyList(),
)

data class SearchResponse(
    val status: String,
    val version: String,
    val error: SubsonicError? = null,
    val searchResult3: SearchResult3Dto? = null,
)

data class SearchResult3Dto(
    val artist: List<ArtistDto> = emptyList(),
    val album: List<AlbumDto> = emptyList(),
    val song: List<SongDto> = emptyList(),
)

data class ArtistDto(
    val id: String,
    val name: String,
    val albumCount: Int? = null,
)

interface NavidromeApi {
    @GET("rest/ping.view")
    suspend fun ping(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = API_VERSION,
        @Query("c") client: String = CLIENT_NAME,
        @Query("f") format: String = FORMAT,
    ): SubsonicEnvelope<BaseResponse>

    @GET("rest/getAlbumList.view")
    suspend fun getAlbumList(
        @Query("type") type: String = "newest",
        @Query("size") size: Int,
        @Query("offset") offset: Int,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = API_VERSION,
        @Query("c") client: String = CLIENT_NAME,
        @Query("f") format: String = FORMAT,
    ): SubsonicEnvelope<AlbumListResponse>

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") albumId: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = API_VERSION,
        @Query("c") client: String = CLIENT_NAME,
        @Query("f") format: String = FORMAT,
    ): SubsonicEnvelope<AlbumResponse>

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = API_VERSION,
        @Query("c") client: String = CLIENT_NAME,
        @Query("f") format: String = FORMAT,
    ): SubsonicEnvelope<PlaylistsResponse>

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(
        @Query("id") playlistId: String,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = API_VERSION,
        @Query("c") client: String = CLIENT_NAME,
        @Query("f") format: String = FORMAT,
    ): SubsonicEnvelope<PlaylistResponse>

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 40,
        @Query("u") username: String,
        @Query("t") token: String,
        @Query("s") salt: String,
        @Query("v") version: String = API_VERSION,
        @Query("c") client: String = CLIENT_NAME,
        @Query("f") format: String = FORMAT,
    ): SubsonicEnvelope<SearchResponse>
}
