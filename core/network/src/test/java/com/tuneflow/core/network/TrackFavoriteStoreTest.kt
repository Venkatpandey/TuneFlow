package com.tuneflow.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackFavoriteStoreTest {
    private val firstSession = SessionData("https://server-a", "first-user", "token", "salt")

    @Test
    fun toggle_isOptimisticAndLocksTrackUntilSuccess() =
        runTest {
            var session: SessionData? = firstSession
            val response = CompletableDeferred<NetworkResult<Unit>>()
            var starCalls = 0
            val store =
                favoriteStore(
                    session = { session },
                    star = {
                        starCalls += 1
                        response.await()
                    },
                )
            store.synchronizeSession(firstSession)
            store.seed(firstSession, listOf(track(isFavorite = false)))

            val firstToggle = async { store.toggle(TRACK_ID) }
            runCurrent()

            assertEquals(TrackFavoriteState(isFavorite = true, isPending = true), store.states.value[TRACK_ID])
            assertEquals(FavoriteToggleResult.AlreadyPending, store.toggle(TRACK_ID))
            assertEquals(1, starCalls)

            response.complete(NetworkResult.Success(Unit))
            assertEquals(FavoriteToggleResult.Success, firstToggle.await())
            assertEquals(TrackFavoriteState(isFavorite = true), store.states.value[TRACK_ID])
        }

    @Test
    fun failedMutation_rollsBackAndPublishesError() =
        runTest {
            val store =
                favoriteStore(
                    session = { firstSession },
                    star = { NetworkResult.Error("Server refused") },
                )
            store.synchronizeSession(firstSession)
            store.seed(firstSession, listOf(track(isFavorite = false)))

            val result = store.toggle(TRACK_ID)

            assertEquals(FavoriteToggleResult.Failure("Server refused"), result)
            assertEquals(TrackFavoriteState(isFavorite = false), store.states.value[TRACK_ID])
            assertEquals("Favorite update failed: Server refused", store.error.value?.message)
        }

    @Test
    fun successfulUnstar_usesUnstarAndKeepsNewValue() =
        runTest {
            var unstarCalls = 0
            val store =
                favoriteStore(
                    session = { firstSession },
                    unstar = {
                        unstarCalls += 1
                        NetworkResult.Success(Unit)
                    },
                )
            store.synchronizeSession(firstSession)
            store.seed(firstSession, listOf(track(isFavorite = true)))

            assertEquals(FavoriteToggleResult.Success, store.toggle(TRACK_ID))
            assertEquals(1, unstarCalls)
            assertEquals(TrackFavoriteState(isFavorite = false), store.states.value[TRACK_ID])
        }

    @Test
    fun responseAfterAccountChange_isIgnored() =
        runTest {
            var session: SessionData? = firstSession
            val response = CompletableDeferred<NetworkResult<Unit>>()
            val store = favoriteStore(session = { session }, star = { response.await() })
            store.synchronizeSession(firstSession)
            store.seed(firstSession, listOf(track(isFavorite = false)))
            val toggle = async { store.toggle(TRACK_ID) }
            runCurrent()

            session = firstSession.copy(username = "second-user")
            store.synchronizeSession(session)
            response.complete(NetworkResult.Success(Unit))

            assertEquals(FavoriteToggleResult.IgnoredAfterAccountChange, toggle.await())
            assertTrue(store.states.value.isEmpty())
            assertEquals(null, store.error.value)
        }

    @Test
    fun serverChange_clearsStore() =
        runTest {
            var session: SessionData? = firstSession
            val store = favoriteStore(session = { session })
            store.synchronizeSession(firstSession)
            store.seed(firstSession, listOf(track(isFavorite = true)))
            assertFalse(store.states.value.isEmpty())

            session = firstSession.copy(serverUrl = "https://server-b")
            store.synchronizeSession(session)

            assertTrue(store.states.value.isEmpty())
        }

    @Test
    fun favoritesSnapshot_clearsFavoriteMissingFromServerResponse() =
        runTest {
            val store = favoriteStore(session = { firstSession })
            store.synchronizeSession(firstSession)
            store.seed(firstSession, listOf(track(isFavorite = true)))

            store.seedFavoritesSnapshot(firstSession, emptyList())

            assertEquals(TrackFavoriteState(isFavorite = false), store.states.value[TRACK_ID])
        }

    @Test
    fun seedMissing_preservesCurrentStateAndAddsUnknownTrack() =
        runTest {
            val store = favoriteStore(session = { firstSession })
            store.synchronizeSession(firstSession)
            store.seed(firstSession, listOf(track(isFavorite = false)))

            store.seedMissing(
                firstSession,
                listOf(
                    track(isFavorite = true),
                    track(isFavorite = true).copy(id = "other-track"),
                ),
            )

            assertEquals(TrackFavoriteState(isFavorite = false), store.states.value[TRACK_ID])
            assertEquals(TrackFavoriteState(isFavorite = true), store.states.value["other-track"])
        }

    private fun favoriteStore(
        session: suspend () -> SessionData?,
        star: suspend (String) -> NetworkResult<Unit> = { NetworkResult.Success(Unit) },
        unstar: suspend (String) -> NetworkResult<Unit> = { NetworkResult.Success(Unit) },
    ): TrackFavoriteStore =
        TrackFavoriteStore(
            sessionProvider = SessionProvider { session() },
            clientProvider =
                NavidromeClientProvider { activeSession ->
                    object : NavidromeClient(activeSession) {
                        override suspend fun star(trackId: String): NetworkResult<Unit> = star(trackId)

                        override suspend fun unstar(trackId: String): NetworkResult<Unit> = unstar(trackId)
                    }
                },
        )

    private fun track(isFavorite: Boolean): TrackSummary =
        TrackSummary(
            id = TRACK_ID,
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationSec = 180,
            coverArtId = null,
            isFavorite = isFavorite,
        )

    private companion object {
        const val TRACK_ID = "track/string-id"
    }
}
