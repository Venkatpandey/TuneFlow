package com.tuneflow.core.network

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.EOFException
import java.io.IOException

@Suppress("TooManyFunctions")
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

    open suspend fun getOpenSubsonicExtensions(): NetworkResult<List<OpenSubsonicExtensionDto>> {
        return safeCall {
            val response =
                api.getOpenSubsonicExtensions(
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(
                    message = response.error?.message ?: "Failed to discover OpenSubsonic extensions.",
                    code = response.error?.code,
                )
            } else {
                NetworkResult.Success(response.openSubsonicExtensions)
            }
        }
    }

    open suspend fun getLyricsBySongId(songId: String): NetworkResult<List<StructuredLyricsDto>> {
        return safeCall {
            val response =
                api.getLyricsBySongId(
                    songId = songId,
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(
                    message = response.error?.message ?: "Failed to load lyrics.",
                    code = response.error?.code,
                )
            } else {
                NetworkResult.Success(response.lyricsList?.structuredLyrics.orEmpty())
            }
        }
    }

    open suspend fun getLyrics(
        artist: String,
        title: String,
    ): NetworkResult<LegacyLyricsDto?> {
        return safeCall {
            val response =
                api.getLyrics(
                    artist = artist,
                    title = title,
                    username = session.username,
                    token = session.token,
                    salt = session.salt,
                ).response

            if (response.status != "ok") {
                NetworkResult.Error(
                    message = response.error?.message ?: "Failed to load lyrics.",
                    code = response.error?.code,
                )
            } else {
                NetworkResult.Success(response.lyrics)
            }
        }
    }

    open fun streamOptions(trackId: String): TrackStreamOptions {
        val base =
            "${session.serverUrl}/rest/stream.view" +
                "?id=$trackId&u=${session.username}&t=${session.token}&s=${session.salt}&v=1.16.1&c=TuneFlow&f=json"

        return TrackStreamOptions(
            directUrl = "$base&format=raw",
            fallbackMp3Url = "$base&format=mp3&maxBitRate=0",
        )
    }

    private inline fun <T> safeCall(block: () -> NetworkResult<T>): NetworkResult<T> {
        return try {
            block()
        } catch (ex: HttpException) {
            NetworkResult.Error(
                message = ex.message ?: "HTTP error",
                kind = NetworkErrorKind.Http,
                httpCode = ex.code(),
            )
        } catch (ex: JsonParseException) {
            NetworkResult.Error(
                message = ex.message ?: "Malformed server response",
                kind = NetworkErrorKind.Parsing,
            )
        } catch (ex: MalformedJsonException) {
            NetworkResult.Error(
                message = ex.message ?: "Malformed server response",
                kind = NetworkErrorKind.Parsing,
            )
        } catch (ex: EOFException) {
            NetworkResult.Error(
                message = ex.message ?: "Incomplete server response",
                kind = NetworkErrorKind.Parsing,
            )
        } catch (ex: IOException) {
            NetworkResult.Error(
                message = ex.message ?: "Network error",
                kind = NetworkErrorKind.Network,
            )
        }
    }
}
