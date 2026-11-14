package com.tuneflow.tv

import android.content.Context
import com.tuneflow.core.network.AppRelease
import com.tuneflow.core.network.AppUpdateException
import com.tuneflow.core.network.AppUpdateRepository
import com.tuneflow.core.network.isNewerAppVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import kotlin.math.max

internal sealed interface AppUpdateUiState {
    data object Hidden : AppUpdateUiState

    data class Available(val release: AppRelease) : AppUpdateUiState

    data class PermissionRequired(
        val release: AppRelease,
        val permissionWasDenied: Boolean = false,
    ) : AppUpdateUiState

    data class Downloading(
        val release: AppRelease,
        val progress: Float,
    ) : AppUpdateUiState

    data class ReadyToInstall(
        val release: AppRelease,
        val apkFile: File,
    ) : AppUpdateUiState

    data class Failed(
        val release: AppRelease,
        val message: String,
        val retryAvailable: Boolean = true,
    ) : AppUpdateUiState
}

internal sealed interface AppUpdateCheckUiState {
    data object Idle : AppUpdateCheckUiState

    data object Checking : AppUpdateCheckUiState

    data class Complete(
        val release: AppRelease,
        val updateAvailable: Boolean,
    ) : AppUpdateCheckUiState

    data class Failed(val message: String) : AppUpdateCheckUiState
}

internal interface AppUpdatePromptStore {
    val lastSuccessfulCheckAtMs: Long
    val snoozeUntilMs: Long

    fun recordSuccessfulCheck(atMs: Long)

    fun snooze(untilMs: Long)
}

internal class SharedPreferencesAppUpdatePromptStore(context: Context) : AppUpdatePromptStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override val lastSuccessfulCheckAtMs: Long
        get() = preferences.getLong(LAST_SUCCESSFUL_CHECK_KEY, 0L)

    override val snoozeUntilMs: Long
        get() = preferences.getLong(SNOOZE_UNTIL_KEY, 0L)

    override fun recordSuccessfulCheck(atMs: Long) {
        preferences.edit().putLong(LAST_SUCCESSFUL_CHECK_KEY, atMs).apply()
    }

    override fun snooze(untilMs: Long) {
        preferences.edit().putLong(SNOOZE_UNTIL_KEY, untilMs).apply()
    }
}

internal class AppUpdateCoordinator(
    private val repository: AppUpdateRepository,
    private val promptStore: AppUpdatePromptStore,
    private val updateCacheDirectory: File,
    private val currentVersion: String,
    private val scope: CoroutineScope,
    private val updatesEnabled: Boolean = true,
    private val currentTimeMs: () -> Long = System::currentTimeMillis,
) {
    private val mutableState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Hidden)
    private val mutableCheckState = MutableStateFlow<AppUpdateCheckUiState>(AppUpdateCheckUiState.Idle)
    private val checkMutex = Mutex()
    private val promptedVersions = mutableSetOf<String>()
    private var lastFailedCheckAtMs = 0L
    private var manualCheckJob: Job? = null
    private var downloadJob: Job? = null
    private var startupCheckPerformed = false

    val state: StateFlow<AppUpdateUiState> = mutableState.asStateFlow()
    val checkState: StateFlow<AppUpdateCheckUiState> = mutableCheckState.asStateFlow()

    suspend fun monitor() {
        while (currentCoroutineContext().isActive) {
            val forceCheck = !startupCheckPerformed
            startupCheckPerformed = true
            checkForUpdate(forceCheck)
            delay(delayUntilNextCheck())
        }
    }

    suspend fun checkForUpdate(force: Boolean = false) {
        if (mutableState.value != AppUpdateUiState.Hidden) return
        val now = currentTimeMs()
        if (!isUpdateCheckDue(
                nowMs = now,
                lastSuccessfulCheckAtMs = promptStore.lastSuccessfulCheckAtMs,
                snoozeUntilMs = promptStore.snoozeUntilMs,
                lastFailedCheckAtMs = lastFailedCheckAtMs,
                force = force,
            )
        ) {
            return
        }

        checkLatestRelease(now, userInitiated = false)
    }

    fun checkNow() {
        if (manualCheckJob?.isActive == true) return
        manualCheckJob =
            scope.launch {
                checkLatestRelease(currentTimeMs(), userInitiated = true)
            }
    }

    private suspend fun checkLatestRelease(
        now: Long,
        userInitiated: Boolean,
    ) {
        checkMutex.withLock {
            val previousCheckState = mutableCheckState.value
            mutableCheckState.value = AppUpdateCheckUiState.Checking
            try {
                val release = repository.latestRelease()
                val updateAvailable = isNewerAppVersion(release.version, currentVersion)
                promptStore.recordSuccessfulCheck(now)
                mutableCheckState.value = AppUpdateCheckUiState.Complete(release, updateAvailable)
                offerUpdateIfAvailable(release, updateAvailable, userInitiated)
            } catch (error: CancellationException) {
                mutableCheckState.value = previousCheckState
                throw error
            } catch (_: Exception) {
                lastFailedCheckAtMs = now
                mutableCheckState.value =
                    if (userInitiated) {
                        AppUpdateCheckUiState.Failed(MANUAL_CHECK_ERROR)
                    } else {
                        previousCheckState
                    }
            }
        }
    }

    private fun offerUpdateIfAvailable(
        release: AppRelease,
        updateAvailable: Boolean,
        userInitiated: Boolean,
    ) {
        if (
            updatesEnabled &&
            updateAvailable &&
            mutableState.value == AppUpdateUiState.Hidden &&
            (userInitiated || release.version !in promptedVersions)
        ) {
            promptedVersions += release.version
            mutableState.value = AppUpdateUiState.Available(release)
        }
    }

    fun requireInstallPermission() {
        val release = mutableState.value.releaseOrNull() ?: return
        mutableState.value = AppUpdateUiState.PermissionRequired(release)
    }

    fun onInstallPermissionResult(granted: Boolean) {
        val state = mutableState.value as? AppUpdateUiState.PermissionRequired ?: return
        if (granted) {
            startDownload(state.release)
        } else {
            mutableState.value = state.copy(permissionWasDenied = true)
        }
    }

    fun startDownload() {
        val release = mutableState.value.releaseOrNull() ?: return
        startDownload(release)
    }

    fun retryDownload() {
        val failure = mutableState.value as? AppUpdateUiState.Failed ?: return
        startDownload(failure.release)
    }

    fun snooze() {
        val release = mutableState.value.releaseOrNull() ?: return
        promptedVersions -= release.version
        promptStore.snooze(safeAdd(currentTimeMs(), UPDATE_SNOOZE_MS))
        mutableState.value = AppUpdateUiState.Hidden
    }

    fun installerLaunched() {
        if (mutableState.value is AppUpdateUiState.ReadyToInstall) {
            mutableState.value = AppUpdateUiState.Hidden
        }
    }

    fun installPreparationFailed(message: String) {
        val ready = mutableState.value as? AppUpdateUiState.ReadyToInstall ?: return
        ready.apkFile.delete()
        mutableState.value = AppUpdateUiState.Failed(ready.release, message, retryAvailable = false)
    }

    fun permissionSettingsUnavailable() {
        val permission = mutableState.value as? AppUpdateUiState.PermissionRequired ?: return
        mutableState.value =
            AppUpdateUiState.Failed(
                permission.release,
                "Install permission settings are unavailable on this device.",
                retryAvailable = false,
            )
    }

    internal fun delayUntilNextCheck(): Long {
        if (mutableState.value != AppUpdateUiState.Hidden) return UPDATE_RETRY_MS
        val now = currentTimeMs()
        val successfulCheckDueAt = safeAdd(promptStore.lastSuccessfulCheckAtMs, UPDATE_CHECK_INTERVAL_MS)
        val failedCheckDueAt = safeAdd(lastFailedCheckAtMs, UPDATE_RETRY_MS)
        val dueAt = max(max(successfulCheckDueAt, promptStore.snoozeUntilMs), failedCheckDueAt)
        return (dueAt - now).coerceIn(MINIMUM_MONITOR_DELAY_MS, UPDATE_CHECK_INTERVAL_MS)
    }

    private fun startDownload(release: AppRelease) {
        if (downloadJob?.isActive == true) return
        mutableState.value = AppUpdateUiState.Downloading(release, progress = 0f)
        downloadJob =
            scope.launch {
                val apkFile = File(updateCacheDirectory, "tuneflow-${release.version.toSafeFilePart()}.apk")
                try {
                    repository.downloadApk(release, apkFile) { downloadedBytes, totalBytes ->
                        val progress =
                            if (totalBytes > 0L) {
                                (downloadedBytes.toDouble() / totalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        val previousProgress =
                            (mutableState.value as? AppUpdateUiState.Downloading)?.progress ?: 0f
                        if (progress >= 1f || progress - previousProgress >= PROGRESS_UPDATE_STEP) {
                            mutableState.value = AppUpdateUiState.Downloading(release, progress)
                        }
                    }
                    mutableState.value = AppUpdateUiState.ReadyToInstall(release, apkFile)
                } catch (error: CancellationException) {
                    apkFile.delete()
                    throw error
                } catch (error: AppUpdateException) {
                    failDownload(release, apkFile, error.message ?: GENERIC_DOWNLOAD_ERROR)
                } catch (_: IOException) {
                    failDownload(release, apkFile, GENERIC_DOWNLOAD_ERROR)
                } catch (_: SecurityException) {
                    failDownload(release, apkFile, GENERIC_DOWNLOAD_ERROR)
                }
            }
    }

    private fun failDownload(
        release: AppRelease,
        apkFile: File,
        message: String,
    ) {
        apkFile.delete()
        mutableState.value = AppUpdateUiState.Failed(release, message)
    }
}

internal fun isUpdateCheckDue(
    nowMs: Long,
    lastSuccessfulCheckAtMs: Long,
    snoozeUntilMs: Long,
    lastFailedCheckAtMs: Long,
    force: Boolean = false,
): Boolean {
    val snoozeExpired = nowMs >= snoozeUntilMs
    val successfulCheckDue =
        lastSuccessfulCheckAtMs <= 0L || nowMs >= safeAdd(lastSuccessfulCheckAtMs, UPDATE_CHECK_INTERVAL_MS)
    val failedCheckDue = lastFailedCheckAtMs <= 0L || nowMs >= safeAdd(lastFailedCheckAtMs, UPDATE_RETRY_MS)
    return snoozeExpired && (force || successfulCheckDue && failedCheckDue)
}

private fun AppUpdateUiState.releaseOrNull(): AppRelease? =
    when (this) {
        AppUpdateUiState.Hidden -> null
        is AppUpdateUiState.Available -> release
        is AppUpdateUiState.PermissionRequired -> release
        is AppUpdateUiState.Downloading -> release
        is AppUpdateUiState.ReadyToInstall -> release
        is AppUpdateUiState.Failed -> release
    }

private fun String.toSafeFilePart(): String = filter { character -> character.isLetterOrDigit() || character in ".-_" }

private fun safeAdd(
    value: Long,
    increment: Long,
): Long = if (value > Long.MAX_VALUE - increment) Long.MAX_VALUE else value + increment

internal const val UPDATE_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
internal const val UPDATE_SNOOZE_MS = 48L * 60L * 60L * 1000L
private const val UPDATE_RETRY_MS = 60L * 60L * 1000L
private const val MINIMUM_MONITOR_DELAY_MS = 1_000L
private const val PROGRESS_UPDATE_STEP = 0.01f
private const val PREFERENCES_NAME = "app_update_preferences"
private const val LAST_SUCCESSFUL_CHECK_KEY = "last_successful_check_at"
private const val SNOOZE_UNTIL_KEY = "update_prompt_snooze_until"
private const val GENERIC_DOWNLOAD_ERROR = "Download failed. Check the connection and try again."
private const val MANUAL_CHECK_ERROR = "Could not check for updates. Check the connection and try again."
