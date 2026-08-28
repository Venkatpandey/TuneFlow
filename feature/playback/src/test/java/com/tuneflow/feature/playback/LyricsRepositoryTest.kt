package com.tuneflow.feature.playback

import com.tuneflow.core.network.LegacyLyricsDto
import com.tuneflow.core.network.LyricsLineDto
import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkErrorKind
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.OpenSubsonicExtensionDto
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.StructuredLyricsDto
import com.tuneflow.core.player.QueueItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRepositoryTest {
    private val session = SessionData("https://music.example", "user", "token", "salt")
    private val track = QueueItem("song-1", "Title", "Artist", "Album", streamUrl = "stream")

    @Test
    fun extensionSupported_usesSongIdAndCachesSuccessfulResult() =
        runTest {
            val client = FakeLyricsClient(session)
            val repository = repository(client)

            val first = repository.load(track)
            val second = repository.load(track)

            assertTrue(first is LyricsLoadResult.Available)
            assertEquals(first, second)
            assertEquals(1, client.bySongIdCalls)
            assertEquals(0, client.legacyCalls)
        }

    @Test
    fun extensionMissing_usesLegacyArtistAndTitle() =
        runTest {
            val client = FakeLyricsClient(session, extensions = NetworkResult.Success(emptyList()))
            val result = repository(client).load(track)

            assertTrue(result is LyricsLoadResult.Available)
            assertEquals(0, client.bySongIdCalls)
            assertEquals(1, client.legacyCalls)
            assertEquals("Artist" to "Title", client.lastLegacyQuery)
        }

    @Test
    fun unsupportedSongIdEndpoint_fallsBackToLegacy() =
        runTest {
            val client =
                FakeLyricsClient(
                    session = session,
                    structured = NetworkResult.Error("unknown endpoint"),
                )

            val result = repository(client).load(track)

            assertTrue(result is LyricsLoadResult.Available)
            assertEquals(1, client.bySongIdCalls)
            assertEquals(1, client.legacyCalls)
        }

    @Test
    fun confirmedEmpty_isCachedButFailuresAreNot() =
        runTest {
            val emptyClient =
                FakeLyricsClient(
                    session = session,
                    structured = NetworkResult.Error("Lyrics not found", code = 70),
                )
            val emptyRepository = repository(emptyClient)
            assertEquals(LyricsLoadResult.Empty, emptyRepository.load(track))
            assertEquals(LyricsLoadResult.Empty, emptyRepository.load(track))
            assertEquals(1, emptyClient.bySongIdCalls)

            val failedClient =
                FakeLyricsClient(
                    session = session,
                    structured =
                        NetworkResult.Error(
                            "connection lost",
                            kind = NetworkErrorKind.Network,
                        ),
                )
            val failedRepository = repository(failedClient)
            assertTrue(failedRepository.load(track) is LyricsLoadResult.NetworkFailure)
            assertTrue(failedRepository.load(track) is LyricsLoadResult.NetworkFailure)
            assertEquals(2, failedClient.bySongIdCalls)
        }

    @Test
    fun unsupportedAndParsingFailures_areDistinct() =
        runTest {
            val unsupported =
                FakeLyricsClient(
                    session = session,
                    extensions = NetworkResult.Success(emptyList()),
                    legacy = NetworkResult.Error("unsupported endpoint"),
                )
            assertEquals(LyricsLoadResult.Unsupported, repository(unsupported).load(track))

            val malformed =
                FakeLyricsClient(
                    session = session,
                    structured = NetworkResult.Error("bad json", kind = NetworkErrorKind.Parsing),
                )
            assertTrue(repository(malformed).load(track) is LyricsLoadResult.ParsingFailure)
        }

    private fun repository(client: NavidromeClient): LyricsRepository =
        LyricsRepository(
            sessionProvider = SessionProvider { session },
            clientProvider = NavidromeClientProvider { client },
        )
}

private class FakeLyricsClient(
    session: SessionData,
    private val extensions: NetworkResult<List<OpenSubsonicExtensionDto>> =
        NetworkResult.Success(listOf(OpenSubsonicExtensionDto("songLyrics", listOf(1)))),
    private val structured: NetworkResult<List<StructuredLyricsDto>> =
        NetworkResult.Success(
            listOf(
                StructuredLyricsDto(
                    synced = true,
                    line = listOf(LyricsLineDto(start = 1_000L, value = "Timed lyric")),
                ),
            ),
        ),
    private val legacy: NetworkResult<LegacyLyricsDto?> =
        NetworkResult.Success(LegacyLyricsDto(value = "Legacy lyric")),
) : NavidromeClient(session) {
    var bySongIdCalls = 0
    var legacyCalls = 0
    var lastLegacyQuery: Pair<String, String>? = null

    override suspend fun getOpenSubsonicExtensions(): NetworkResult<List<OpenSubsonicExtensionDto>> = extensions

    override suspend fun getLyricsBySongId(songId: String): NetworkResult<List<StructuredLyricsDto>> {
        bySongIdCalls += 1
        return structured
    }

    override suspend fun getLyrics(
        artist: String,
        title: String,
    ): NetworkResult<LegacyLyricsDto?> {
        legacyCalls += 1
        lastLegacyQuery = artist to title
        return legacy
    }
}
