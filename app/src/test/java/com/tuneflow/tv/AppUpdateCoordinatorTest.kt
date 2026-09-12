package com.tuneflow.tv

import com.tuneflow.core.network.AppRelease
import com.tuneflow.core.network.AppUpdateRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppUpdateCoordinatorTest {
    @Test
    fun checkFindsNewerReleaseAndDoesNotDuplicateVisiblePrompt() =
        runTest {
            val repository = FakeUpdateRepository(RELEASE)
            val store = FakePromptStore()
            val coordinator = coordinator(repository, store, nowMs = 1_000L)

            coordinator.checkForUpdate()

            assertTrue(coordinator.state.value is AppUpdateUiState.Available)
            assertEquals(1_000L, store.lastSuccessfulCheckAtMs)
            coordinator.checkForUpdate()
            assertTrue(coordinator.state.value is AppUpdateUiState.Available)
            assertEquals(1, repository.checkCount)
        }

    @Test
    fun forcedStartupCheckIgnoresRecentSuccessfulCheck() =
        runTest {
            val repository = FakeUpdateRepository(RELEASE)
            val store = FakePromptStore().apply { recordSuccessfulCheck(999L) }
            val coordinator = coordinator(repository, store, nowMs = 1_000L)

            coordinator.checkForUpdate(force = true)

            assertEquals(1, repository.checkCount)
            assertTrue(coordinator.state.value is AppUpdateUiState.Available)
        }

    @Test
    fun checkDoesNotPromptForCurrentOrOlderRelease() =
        runTest {
            val repository = FakeUpdateRepository(RELEASE.copy(version = "1.2.0"))
            val coordinator = coordinator(repository, FakePromptStore(), nowMs = 1_000L)

            coordinator.checkForUpdate()

            assertEquals(AppUpdateUiState.Hidden, coordinator.state.value)
        }

    @Test
    fun laterSuppressesChecksFor48Hours() =
        runTest {
            val repository = FakeUpdateRepository(RELEASE)
            val store = FakePromptStore()
            var nowMs = 10_000L
            val coordinator = coordinator(repository, store) { nowMs }
            coordinator.checkForUpdate()

            coordinator.snooze()

            assertEquals(10_000L + UPDATE_SNOOZE_MS, store.snoozeUntilMs)
            coordinator.checkForUpdate()
            assertEquals(1, repository.checkCount)
            nowMs += UPDATE_SNOOZE_MS
            coordinator.checkForUpdate()
            assertEquals(2, repository.checkCount)
            assertTrue(coordinator.state.value is AppUpdateUiState.Available)
        }

    @Test
    fun checkPolicyUses24HourIntervalAndFailureBackoff() {
        assertFalse(
            isUpdateCheckDue(
                nowMs = UPDATE_CHECK_INTERVAL_MS - 1,
                lastSuccessfulCheckAtMs = 1L,
                snoozeUntilMs = 0L,
                lastFailedCheckAtMs = 0L,
            ),
        )
        assertTrue(
            isUpdateCheckDue(
                nowMs = UPDATE_CHECK_INTERVAL_MS + 1L,
                lastSuccessfulCheckAtMs = 1L,
                snoozeUntilMs = 0L,
                lastFailedCheckAtMs = 0L,
            ),
        )
        assertFalse(
            isUpdateCheckDue(
                nowMs = 2_000L,
                lastSuccessfulCheckAtMs = 0L,
                snoozeUntilMs = 0L,
                lastFailedCheckAtMs = 1_000L,
            ),
        )
        assertTrue(
            isUpdateCheckDue(
                nowMs = 2_000L,
                lastSuccessfulCheckAtMs = 1_999L,
                snoozeUntilMs = 0L,
                lastFailedCheckAtMs = 1_999L,
                force = true,
            ),
        )
        assertFalse(
            isUpdateCheckDue(
                nowMs = 2_000L,
                lastSuccessfulCheckAtMs = 0L,
                snoozeUntilMs = 3_000L,
                lastFailedCheckAtMs = 0L,
                force = true,
            ),
        )
    }

    private fun kotlinx.coroutines.test.TestScope.coordinator(
        repository: AppUpdateRepository,
        store: AppUpdatePromptStore,
        nowMs: Long,
    ): AppUpdateCoordinator = coordinator(repository, store) { nowMs }

    private fun kotlinx.coroutines.test.TestScope.coordinator(
        repository: AppUpdateRepository,
        store: AppUpdatePromptStore,
        currentTimeMs: () -> Long,
    ): AppUpdateCoordinator =
        AppUpdateCoordinator(
            repository = repository,
            promptStore = store,
            updateCacheDirectory = File(System.getProperty("java.io.tmpdir"), "tuneflow-update-test"),
            currentVersion = "1.2.0",
            scope = backgroundScope,
            currentTimeMs = currentTimeMs,
        )
}

private class FakeUpdateRepository(private val release: AppRelease) : AppUpdateRepository {
    var checkCount = 0

    override suspend fun latestRelease(): AppRelease {
        checkCount += 1
        return release
    }

    override suspend fun downloadApk(
        release: AppRelease,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ) = Unit
}

private class FakePromptStore : AppUpdatePromptStore {
    override var lastSuccessfulCheckAtMs = 0L
    override var snoozeUntilMs = 0L

    override fun recordSuccessfulCheck(atMs: Long) {
        lastSuccessfulCheckAtMs = atMs
    }

    override fun snooze(untilMs: Long) {
        snoozeUntilMs = untilMs
    }
}

private val RELEASE =
    AppRelease(
        version = "1.3.0",
        apkUrl = "https://github.com/Venkatpandey/TuneFlow/releases/download/v1.3.0/tuneflow-tv.apk",
        apkSizeBytes = 100L,
        sha256 = null,
    )
