package com.tuneflow.feature.auth

import com.tuneflow.core.network.NavidromeClient
import com.tuneflow.core.network.NavidromeClientProvider
import com.tuneflow.core.network.NetworkResult
import com.tuneflow.core.network.SessionData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun login_savesSession_onSuccessfulPing() =
        runTest {
            var saved: SessionData? = null
            val repository =
                AuthRepository(
                    saveSession = { saved = it },
                    clearSession = {},
                    clientProvider = NavidromeClientProvider { FakeClient(it, NetworkResult.Success(Unit)) },
                )

            val result =
                repository.login(
                    serverUrl = "https://demo.local",
                    username = "demo",
                    password = "secret",
                )

            assertTrue(result.isSuccess)
            assertEquals("demo", saved?.username)
            assertEquals("https://demo.local", saved?.serverUrl)
        }

    @Test
    fun login_normalizesPlainHostOrIp_toHttpUrl() =
        runTest {
            var saved: SessionData? = null
            val repository =
                AuthRepository(
                    saveSession = { saved = it },
                    clearSession = {},
                    clientProvider = NavidromeClientProvider { FakeClient(it, NetworkResult.Success(Unit)) },
                )

            val result =
                repository.login(
                    serverUrl = "192.168.1.10:4533",
                    username = "demo",
                    password = "secret",
                )

            assertTrue(result.isSuccess)
            assertEquals("http://192.168.1.10:4533", saved?.serverUrl)
        }

    @Test
    fun login_returnsFailure_whenPingFails() =
        runTest {
            val repository =
                AuthRepository(
                    saveSession = {},
                    clearSession = {},
                    clientProvider =
                        NavidromeClientProvider {
                            FakeClient(it, NetworkResult.Error("Invalid credentials"))
                        },
                )

            val result =
                repository.login(
                    serverUrl = "https://demo.local",
                    username = "demo",
                    password = "wrong",
                )

            assertTrue(result.isFailure)
            assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
        }

    @Test
    fun login_returnsFailure_whenUrlIsInvalid() =
        runTest {
            val repository =
                AuthRepository(
                    saveSession = {},
                    clearSession = {},
                    clientProvider = NavidromeClientProvider { FakeClient(it, NetworkResult.Success(Unit)) },
                )

            val result =
                repository.login(
                    serverUrl = "http://bad host",
                    username = "demo",
                    password = "secret",
                )

            assertTrue(result.isFailure)
            assertEquals("Enter a valid server URL.", result.exceptionOrNull()?.message)
        }
}

private class FakeClient(
    session: SessionData,
    private val pingResult: NetworkResult<Unit>,
) : NavidromeClient(session) {
    override suspend fun ping(): NetworkResult<Unit> = pingResult
}
