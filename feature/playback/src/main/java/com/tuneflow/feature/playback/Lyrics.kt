package com.tuneflow.feature.playback

import com.tuneflow.core.network.DataStoreSessionProvider
import com.tuneflow.core.network.DefaultNavidromeClientProvider
import com.tuneflow.core.network.LegacyLyricsDto
import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkErrorKind
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.SessionData
import com.tuneflow.core.network.SessionProvider
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.network.StructuredLyricsDto
import com.tuneflow.core.player.QueueItem

data class LyricLine(
    val text: String,
    val startMs: Long? = null,
)

data class Lyrics(
    val synchronized: Boolean,
    val lines: List<LyricLine>,
    val offsetMs: Long = 0L,
)

sealed interface LyricsLoadResult {
    data class Available(val lyrics: Lyrics) : LyricsLoadResult

    data object Empty : LyricsLoadResult

    data object Unsupported : LyricsLoadResult

    data class NetworkFailure(val message: String) : LyricsLoadResult

    data class ParsingFailure(val message: String) : LyricsLoadResult
}

sealed interface LyricsUiState {
    data object Idle : LyricsUiState

    data class Loading(val trackId: String) : LyricsUiState

    data class Available(
        val trackId: String,
        val lyrics: Lyrics,
    ) : LyricsUiState

    data class Empty(val trackId: String) : LyricsUiState

    data class Unsupported(val trackId: String) : LyricsUiState

    data class NetworkFailure(
        val trackId: String,
        val message: String,
    ) : LyricsUiState

    data class ParsingFailure(
        val trackId: String,
        val message: String,
    ) : LyricsUiState
}

fun interface LyricsProvider {
    suspend fun load(track: QueueItem): LyricsLoadResult
}

object EmptyLyricsProvider : LyricsProvider {
    override suspend fun load(track: QueueItem): LyricsLoadResult = LyricsLoadResult.Empty
}

class LyricsRepository(
    private val sessionProvider: SessionProvider,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
) : LyricsProvider {
    private enum class SongLyricsSupport {
        Unknown,
        Supported,
        LegacyOnly,
    }

    private data class SessionClient(
        val session: SessionData,
        val client: NavidromeClient,
    )

    private var support = SongLyricsSupport.Unknown
    private var client: SessionClient? = null
    private val cache = mutableMapOf<String, LyricsLoadResult>()

    constructor(sessionStore: SessionStore) : this(
        sessionProvider = DataStoreSessionProvider(sessionStore),
        clientProvider = DefaultNavidromeClientProvider,
    )

    override suspend fun load(track: QueueItem): LyricsLoadResult {
        val cached = cache[track.id]
        return if (cached != null) {
            cached
        } else {
            val sessionClient = requireClient()
            if (sessionClient == null) {
                LyricsLoadResult.NetworkFailure("Not logged in")
            } else {
                val result =
                    when (discoverSupport(sessionClient.client)) {
                        SongLyricsSupport.Supported -> loadStructured(sessionClient.client, track)
                        SongLyricsSupport.LegacyOnly,
                        SongLyricsSupport.Unknown,
                        -> loadLegacy(sessionClient.client, track)
                    }

                if (result is LyricsLoadResult.Available || result == LyricsLoadResult.Empty) {
                    cache[track.id] = result
                }
                result
            }
        }
    }

    private suspend fun discoverSupport(client: NavidromeClient): SongLyricsSupport {
        if (support != SongLyricsSupport.Unknown) return support
        support =
            when (val result = client.getOpenSubsonicExtensions()) {
                is NetworkResult.Success -> {
                    if (result.data.any { it.name.equals("songLyrics", ignoreCase = true) }) {
                        SongLyricsSupport.Supported
                    } else {
                        SongLyricsSupport.LegacyOnly
                    }
                }
                is NetworkResult.Error -> SongLyricsSupport.LegacyOnly
            }
        return support
    }

    private suspend fun loadStructured(
        client: NavidromeClient,
        track: QueueItem,
    ): LyricsLoadResult =
        when (val result = client.getLyricsBySongId(track.id)) {
            is NetworkResult.Success -> selectStructuredLyrics(result.data)
            is NetworkResult.Error -> {
                when {
                    result.isConfirmedEmpty() -> LyricsLoadResult.Empty
                    result.isUnsupportedEndpoint() -> {
                        support = SongLyricsSupport.LegacyOnly
                        loadLegacy(client, track)
                    }
                    result.kind == NetworkErrorKind.Parsing ->
                        LyricsLoadResult.ParsingFailure(result.message)
                    else -> LyricsLoadResult.NetworkFailure(result.message)
                }
            }
        }

    private suspend fun loadLegacy(
        client: NavidromeClient,
        track: QueueItem,
    ): LyricsLoadResult =
        when (val result = client.getLyrics(track.artist, track.title)) {
            is NetworkResult.Success -> parseLegacyLyrics(result.data)
            is NetworkResult.Error -> {
                when {
                    result.isConfirmedEmpty() -> LyricsLoadResult.Empty
                    result.isUnsupportedEndpoint() -> LyricsLoadResult.Unsupported
                    result.kind == NetworkErrorKind.Parsing -> LyricsLoadResult.ParsingFailure(result.message)
                    else -> LyricsLoadResult.NetworkFailure(result.message)
                }
            }
        }

    private suspend fun requireClient(): SessionClient? {
        val session = sessionProvider.currentSession()
        return if (session == null) {
            null
        } else {
            client?.takeIf { it.session == session }
                ?: run {
                    support = SongLyricsSupport.Unknown
                    cache.clear()
                    runCatching { SessionClient(session, clientProvider.create(session)) }
                        .getOrNull()
                        ?.also { client = it }
                }
        }
    }
}

internal fun selectStructuredLyrics(results: List<StructuredLyricsDto>): LyricsLoadResult {
    val parsed = results.map(::parseStructuredCandidate)
    val available = parsed.filterIsInstance<StructuredCandidate.Available>()
    val selected =
        available.firstOrNull { it.isMain && it.lyrics.synchronized }
            ?: available.firstOrNull { it.isMain && !it.lyrics.synchronized }
            ?: available.firstOrNull()

    return when {
        selected != null -> LyricsLoadResult.Available(selected.lyrics)
        parsed.any { it is StructuredCandidate.Malformed } ->
            LyricsLoadResult.ParsingFailure("Lyrics response contains invalid synchronized lines.")
        else -> LyricsLoadResult.Empty
    }
}

private sealed interface StructuredCandidate {
    data class Available(
        val lyrics: Lyrics,
        val isMain: Boolean,
    ) : StructuredCandidate

    data object Empty : StructuredCandidate

    data object Malformed : StructuredCandidate
}

private fun parseStructuredCandidate(dto: StructuredLyricsDto): StructuredCandidate {
    val nonEmptyLines = dto.line.filter { !it.value.isNullOrBlank() }
    return when {
        nonEmptyLines.isEmpty() -> StructuredCandidate.Empty
        dto.synced && nonEmptyLines.any { line -> line.start?.let { it < 0L } != false } ->
            StructuredCandidate.Malformed
        else -> {
            val lines =
                nonEmptyLines.map { line ->
                    LyricLine(
                        text = line.value.orEmpty().trim(),
                        startMs = line.start?.takeIf { dto.synced },
                    )
                }
            StructuredCandidate.Available(
                lyrics =
                    Lyrics(
                        synchronized = dto.synced,
                        lines = if (dto.synced) lines.sortedBy { it.startMs } else lines,
                        offsetMs = dto.offset ?: 0L,
                    ),
                isMain = dto.kind.isNullOrBlank() || dto.kind.equals("main", ignoreCase = true),
            )
        }
    }
}

internal fun parseLegacyLyrics(dto: LegacyLyricsDto?): LyricsLoadResult {
    val lines =
        dto?.value
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.map { LyricLine(text = it) }
            ?.toList()
            .orEmpty()
    return if (lines.isEmpty()) {
        LyricsLoadResult.Empty
    } else {
        LyricsLoadResult.Available(Lyrics(synchronized = false, lines = lines))
    }
}

internal fun resolveActiveLyricLine(
    lyrics: Lyrics,
    positionMs: Long,
): Int? {
    if (!lyrics.synchronized || lyrics.lines.isEmpty()) return null
    val adjustedPosition = positionMs.coerceAtLeast(0L) + lyrics.offsetMs
    val index = lyrics.lines.indexOfLast { (it.startMs ?: Long.MAX_VALUE) <= adjustedPosition }
    return index.takeIf { it >= 0 }
}

private fun NetworkResult.Error.isConfirmedEmpty(): Boolean = code == 70

private fun NetworkResult.Error.isUnsupportedEndpoint(): Boolean {
    if (httpCode == 404 || httpCode == 405 || httpCode == 501) return true
    val normalized = message.lowercase()
    return normalized.contains("unsupported endpoint") ||
        normalized.contains("unknown endpoint") ||
        normalized.contains("not implemented")
}
