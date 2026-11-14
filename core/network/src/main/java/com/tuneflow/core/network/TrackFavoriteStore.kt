package com.tuneflow.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TrackFavoriteState(
    val isFavorite: Boolean,
    val isPending: Boolean = false,
)

data class FavoriteFeedbackError(
    val id: Long,
    val message: String,
)

sealed interface FavoriteToggleResult {
    data object Success : FavoriteToggleResult

    data object AlreadyPending : FavoriteToggleResult

    data object IgnoredAfterAccountChange : FavoriteToggleResult

    data class Failure(val message: String) : FavoriteToggleResult
}

class TrackFavoriteStore(
    private val sessionProvider: SessionProvider,
    private val clientProvider: NavidromeClientProvider = DefaultNavidromeClientProvider,
) {
    private val mutex = Mutex()
    private var activeAccount: FavoriteAccount? = null
    private var generation = 0L
    private var nextErrorId = 0L
    private val _states = MutableStateFlow<Map<String, TrackFavoriteState>>(emptyMap())
    private val _error = MutableStateFlow<FavoriteFeedbackError?>(null)

    val states: StateFlow<Map<String, TrackFavoriteState>> = _states.asStateFlow()
    val error: StateFlow<FavoriteFeedbackError?> = _error.asStateFlow()

    suspend fun synchronizeSession(session: SessionData?) {
        val account = session?.favoriteAccount()
        mutex.withLock {
            if (account == activeAccount) return
            activateAccount(account)
        }
    }

    suspend fun seed(
        session: SessionData,
        tracks: Iterable<TrackSummary>,
    ) {
        val account = session.favoriteAccount()
        if (sessionProvider.currentSession()?.favoriteAccount() != account) return

        mutex.withLock {
            if (activeAccount == null) activateAccount(account)
            if (activeAccount != account) return

            val updated = _states.value.toMutableMap()
            tracks.forEach { track ->
                val current = updated[track.id]
                if (current?.isPending != true) {
                    updated[track.id] = TrackFavoriteState(isFavorite = track.isFavorite)
                }
            }
            _states.value = updated
        }
    }

    suspend fun seedMissing(
        session: SessionData,
        tracks: Iterable<TrackSummary>,
    ) {
        val account = session.favoriteAccount()
        if (sessionProvider.currentSession()?.favoriteAccount() != account) return

        mutex.withLock {
            if (activeAccount == null) activateAccount(account)
            if (activeAccount != account) return

            val updated = _states.value.toMutableMap()
            tracks.forEach { track ->
                if (track.id !in updated) {
                    updated[track.id] = TrackFavoriteState(isFavorite = track.isFavorite)
                }
            }
            _states.value = updated
        }
    }

    suspend fun seedFavoritesSnapshot(
        session: SessionData,
        favoriteTracks: Iterable<TrackSummary>,
    ) {
        val account = session.favoriteAccount()
        if (sessionProvider.currentSession()?.favoriteAccount() != account) return
        val tracks = favoriteTracks.toList()
        val favoriteIds = tracks.mapTo(mutableSetOf()) { it.id }

        mutex.withLock {
            if (activeAccount == null) activateAccount(account)
            if (activeAccount != account) return

            val updated =
                _states.value.mapValuesTo(mutableMapOf()) { (trackId, current) ->
                    if (current.isPending) {
                        current
                    } else {
                        TrackFavoriteState(isFavorite = trackId in favoriteIds)
                    }
                }
            tracks.forEach { track ->
                val current = updated[track.id]
                if (current?.isPending != true) {
                    updated[track.id] = TrackFavoriteState(isFavorite = true)
                }
            }
            _states.value = updated
        }
    }

    suspend fun toggle(trackId: String): FavoriteToggleResult {
        val session = sessionProvider.currentSession()
        return if (session == null) {
            FavoriteToggleResult.Failure("Not logged in")
        } else {
            toggleForSession(trackId, session)
        }
    }

    private suspend fun toggleForSession(
        trackId: String,
        session: SessionData,
    ): FavoriteToggleResult {
        synchronizeSession(session)
        val account = session.favoriteAccount()
        return when (val start = beginMutation(trackId, account)) {
            is FavoriteMutationStart.Rejected -> start.result
            is FavoriteMutationStart.Ready -> executeMutation(trackId, session, start.mutation)
        }
    }

    private suspend fun beginMutation(
        trackId: String,
        account: FavoriteAccount,
    ): FavoriteMutationStart =
        mutex.withLock {
            val current = _states.value[trackId]
            when {
                activeAccount != account ->
                    FavoriteMutationStart.Rejected(FavoriteToggleResult.IgnoredAfterAccountChange)
                current?.isPending == true ->
                    FavoriteMutationStart.Rejected(FavoriteToggleResult.AlreadyPending)
                else -> {
                    val previous = current?.isFavorite ?: false
                    _states.value =
                        _states.value +
                        (trackId to TrackFavoriteState(isFavorite = !previous, isPending = true))
                    FavoriteMutationStart.Ready(
                        FavoriteMutation(
                            previous = previous,
                            desired = !previous,
                            account = account,
                            generation = generation,
                        ),
                    )
                }
            }
        }

    private suspend fun executeMutation(
        trackId: String,
        session: SessionData,
        mutation: FavoriteMutation,
    ): FavoriteToggleResult {
        val result = requestMutation(trackId, session, mutation.desired)
        val currentSession = sessionProvider.currentSession()

        return if (currentSession?.favoriteAccount() != mutation.account) {
            synchronizeSession(currentSession)
            FavoriteToggleResult.IgnoredAfterAccountChange
        } else {
            completeMutation(trackId, mutation, result)
        }
    }

    private suspend fun requestMutation(
        trackId: String,
        session: SessionData,
        favorite: Boolean,
    ): NetworkResult<Unit> =
        runCatching { clientProvider.create(session) }
            .fold(
                onSuccess = { client ->
                    if (favorite) client.star(trackId) else client.unstar(trackId)
                },
                onFailure = { error -> NetworkResult.Error(error.message ?: "Invalid server URL.") },
            )

    private suspend fun completeMutation(
        trackId: String,
        mutation: FavoriteMutation,
        result: NetworkResult<Unit>,
    ): FavoriteToggleResult =
        mutex.withLock {
            if (generation != mutation.generation || activeAccount != mutation.account) {
                return@withLock FavoriteToggleResult.IgnoredAfterAccountChange
            }
            when (result) {
                is NetworkResult.Success -> {
                    _states.value =
                        _states.value +
                        (trackId to TrackFavoriteState(isFavorite = mutation.desired))
                    FavoriteToggleResult.Success
                }
                is NetworkResult.Error -> {
                    _states.value =
                        _states.value +
                        (trackId to TrackFavoriteState(isFavorite = mutation.previous))
                    nextErrorId += 1
                    _error.value =
                        FavoriteFeedbackError(
                            id = nextErrorId,
                            message = "Favorite update failed: ${result.message}",
                        )
                    FavoriteToggleResult.Failure(result.message)
                }
            }
        }

    fun clearError(errorId: Long) {
        if (_error.value?.id == errorId) _error.value = null
    }

    private fun activateAccount(account: FavoriteAccount?) {
        activeAccount = account
        generation += 1
        _states.value = emptyMap()
        _error.value = null
    }
}

private data class FavoriteMutation(
    val previous: Boolean,
    val desired: Boolean,
    val account: FavoriteAccount,
    val generation: Long,
)

private sealed interface FavoriteMutationStart {
    data class Ready(val mutation: FavoriteMutation) : FavoriteMutationStart

    data class Rejected(val result: FavoriteToggleResult) : FavoriteMutationStart
}

private data class FavoriteAccount(
    val serverUrl: String,
    val username: String,
)

private fun SessionData.favoriteAccount(): FavoriteAccount =
    FavoriteAccount(
        serverUrl = serverUrl.trimEnd('/'),
        username = username,
    )
