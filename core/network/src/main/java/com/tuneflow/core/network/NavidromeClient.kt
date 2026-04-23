package com.tuneflow.core.network

import retrofit2.HttpException
import java.io.IOException

open class NavidromeClient(private val session: SessionData) {
    private val api: NavidromeApi = NetworkFactory.createApi(session.serverUrl)

    open suspend fun ping(): NetworkResult<Unit> {
        return safeCall {
            val response =
                api.ping(
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Server rejected login.")
            } else {
                NetworkResult.Success(Unit)
            }
        }
    }

    open suspend fun getAlbums(
        size: Int,
        offset: Int,
    ): NetworkResult<List<AlbumDto>> {
        return safeCall {
            val response =
                api.getAlbumList(
                    size = size,
                    offset = offset,
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Failed to load albums.")
            } else {
                NetworkResult.Success(response.albumList?.album.orEmpty())
            }
        }
    }

    open suspend fun getAlbum(albumId: String): NetworkResult<AlbumDetailDto> {
        return safeCall {
            val response =
                api.getAlbum(
                    albumId = albumId,
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Failed to load album.")
            } else {
                val album = response.album ?: return@safeCall NetworkResult.Error("Album not found.")
                NetworkResult.Success(album)
            }
        }
    }

    open suspend fun getArtists(): NetworkResult<List<ArtistDto>> {
        return safeCall {
            val response =
                api.getArtists(
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Failed to load artists.")
            } else {
                NetworkResult.Success(
                    response.artists
                        ?.index
                        .orEmpty()
                        .flatMap { it.artist },
                )
            }
        }
    }

    open suspend fun getArtist(artistId: String): NetworkResult<ArtistDetailDto> {
        return safeCall {
            val response =
                api.getArtist(
                    artistId = artistId,
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Failed to load artist.")
            } else {
                val artist = response.artist ?: return@safeCall NetworkResult.Error("Artist not found.")
                NetworkResult.Success(artist)
            }
        }
    }

    open suspend fun getPlaylists(): NetworkResult<List<PlaylistDto>> {
        return safeCall {
            val response =
                api.getPlaylists(
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Failed to load playlists.")
            } else {
                NetworkResult.Success(response.playlists?.playlist.orEmpty())
            }
        }
    }

    open suspend fun getStarred2(): NetworkResult<Starred2Dto> {
        return safeCall {
            val response =
                api.getStarred2(
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Failed to load favorites.")
            } else {
                NetworkResult.Success(response.starred2 ?: Starred2Dto())
            }
        }
    }

    open suspend fun getPlaylist(playlistId: String): NetworkResult<PlaylistDetailDto> {
        return safeCall {
            val response =
                api.getPlaylist(
                    playlistId = playlistId,
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Failed to load playlist.")
            } else {
                val playlist = response.playlist ?: return@safeCall NetworkResult.Error("Playlist not found.")
                NetworkResult.Success(playlist)
            }
        }
    }

    open suspend fun search(query: String): NetworkResult<SearchResult3Dto> {
        return safeCall {
            val response =
                api.search3(
                    query = query,
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(response.error?.message ?: "Search failed.")
            } else {
                NetworkResult.Success(response.searchResult3 ?: SearchResult3Dto())
            }
        }
    }

    open fun streamUrl(trackId: String): String {
        return "${session.serverUrl}/rest/stream.view" +
            "?id=$trackId&u=${session.username}&t=${session.token}&s=${session.salt}&v=1.16.1&c=TuneFlow&f=json"
    }

    private inline fun <T> safeCall(block: () -> NetworkResult<T>): NetworkResult<T> {
        return try {
            block()
        } catch (ex: HttpException) {
            NetworkResult.Error(ex.message ?: "HTTP error")
        } catch (ex: IOException) {
            NetworkResult.Error(ex.message ?: "Network error")
        }
    }
}
