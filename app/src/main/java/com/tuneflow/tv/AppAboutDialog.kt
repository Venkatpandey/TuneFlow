@file:Suppress("MatchingDeclarationName")

package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuneflow.core.design.TuneFlowShapes

internal data class AppBuildInfo(
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val applicationId: String,
    val channel: String,
)

@Composable
internal fun AppAboutDialog(
    buildInfo: AppBuildInfo,
    updateCheckState: AppUpdateCheckUiState,
    updatesEnabled: Boolean,
    onCheckForUpdates: () -> Unit,
    onDismiss: () -> Unit,
    accountUsername: String? = null,
    accountServerUrl: String? = null,
    onLogout: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val checking = updateCheckState == AppUpdateCheckUiState.Checking
    val primaryFocusRequester = remember { FocusRequester() }
    val cancelLogoutFocusRequester = remember { FocusRequester() }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .width(700.dp)
                        .clip(TuneFlowShapes.panel)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (showLogoutConfirmation) {
                    Text(
                        text = "Log Out of TuneFlow?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = aboutLogoutConfirmationMessage(accountUsername),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutConfirmation = false },
                            shape = TuneFlowShapes.row,
                            modifier = Modifier.focusRequester(cancelLogoutFocusRequester),
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onLogout,
                            shape = TuneFlowShapes.row,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                        ) {
                            Text("Log Out")
                        }
                    }
                } else {
                    Text(
                        text = "About ${buildInfo.appName}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    AppBuildDetails(buildInfo)
                    if (!accountUsername.isNullOrBlank() || !accountServerUrl.isNullOrBlank()) {
                        AppAccountDetails(accountUsername, accountServerUrl)
                    }
                    Text(
                        text = aboutUpdateStatus(updateCheckState, updatesEnabled),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (checking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    LatestReleaseNotes(updateCheckState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutConfirmation = true },
                            shape = TuneFlowShapes.row,
                        ) {
                            Text("Log Out")
                        }
                        if (onOpenSettings != null) {
                            OutlinedButton(
                                onClick = onOpenSettings,
                                shape = TuneFlowShapes.row,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Settings")
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = TuneFlowShapes.row,
                            modifier = if (checking) Modifier.focusRequester(primaryFocusRequester) else Modifier,
                        ) {
                            Text("Close")
                        }
                        Button(
                            onClick = onCheckForUpdates,
                            enabled = !checking,
                            shape = TuneFlowShapes.row,
                            modifier = if (checking) Modifier else Modifier.focusRequester(primaryFocusRequester),
                        ) {
                            Text(if (checking) "Checking…" else "Check for Updates")
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(checking, showLogoutConfirmation) {
        if (showLogoutConfirmation) {
            cancelLogoutFocusRequester.requestFocus()
        } else {
            primaryFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun AppAccountDetails(
    username: String?,
    serverUrl: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!username.isNullOrBlank()) {
            AppBuildDetailRow("Account", username)
        }
        if (!serverUrl.isNullOrBlank()) {
            AppBuildDetailRow("Server", serverUrl)
        }
    }
}

@Composable
private fun AppBuildDetails(buildInfo: AppBuildInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppBuildDetailRow("Version", buildInfo.versionName)
        AppBuildDetailRow("Version code", buildInfo.versionCode.toString())
        AppBuildDetailRow("Channel", buildInfo.channel)
        AppBuildDetailRow("Package", buildInfo.applicationId)
    }
}

@Composable
private fun AppBuildDetailRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun LatestReleaseNotes(updateCheckState: AppUpdateCheckUiState) {
    val release = (updateCheckState as? AppUpdateCheckUiState.Complete)?.release ?: return
    val notes = release.releaseNotes
    if (notes.isNullOrBlank()) {
        Text(
            text = "No changelog was published for the latest stable release.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        AppReleaseNotes(notes)
    }
}

internal fun aboutLogoutConfirmationMessage(accountUsername: String?): String =
    if (!accountUsername.isNullOrBlank()) {
        "Are you sure you want to log out of $accountUsername? Playback will stop and you will return to the sign-in screen."
    } else {
        "Are you sure you want to log out? Playback will stop and you will return to the sign-in screen."
    }

internal fun aboutUpdateStatus(
    state: AppUpdateCheckUiState,
    updatesEnabled: Boolean,
): String =
    when (state) {
        AppUpdateCheckUiState.Idle -> "Latest release has not been checked yet."
        AppUpdateCheckUiState.Checking -> "Checking the latest stable GitHub release…"
        is AppUpdateCheckUiState.Failed -> state.message
        is AppUpdateCheckUiState.Complete ->
            when {
                !updatesEnabled -> "Latest stable release: ${state.release.version}. Beta builds update manually."
                state.updateAvailable -> "Update ${state.release.version} is available."
                else -> "TuneFlow is up to date. Latest stable release: ${state.release.version}."
            }
    }
