package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuneflow.core.design.TuneFlowShapes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun AppUpdateDialog(
    state: AppUpdateUiState,
    onUpdateNow: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onRetry: () -> Unit,
    onLater: () -> Unit,
) {
    if (state == AppUpdateUiState.Hidden) return
    val primaryFocusRequester = remember { FocusRequester() }
    val canDismiss = state !is AppUpdateUiState.Downloading && state !is AppUpdateUiState.ReadyToInstall

    Dialog(
        onDismissRequest = { if (canDismiss) onLater() },
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
                        .width(660.dp)
                        .clip(TuneFlowShapes.panel)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = state.title(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.message(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                (state as? AppUpdateUiState.Available)?.release?.releaseNotes?.let { releaseNotes ->
                    ReleaseNotes(releaseNotes)
                }

                UpdateDialogControls(
                    state = state,
                    primaryFocusRequester = primaryFocusRequester,
                    onUpdateNow = onUpdateNow,
                    onOpenInstallSettings = onOpenInstallSettings,
                    onRetry = onRetry,
                    onLater = onLater,
                )
            }
        }
    }

    LaunchedEffect(state::class) {
        if (canDismiss) primaryFocusRequester.requestFocus()
    }
}

@Composable
private fun ReleaseNotes(releaseNotes: String) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    Text(
        text = "What's new",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = releaseNotes,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = RELEASE_NOTES_MAX_HEIGHT)
                .verticalScroll(scrollState)
                .focusable()
                .onKeyEvent { event ->
                    val scrollDelta =
                        when {
                            event.type != KeyEventType.KeyDown -> null
                            event.key == Key.DirectionDown && scrollState.canScrollForward -> RELEASE_NOTES_SCROLL_STEP
                            event.key == Key.DirectionUp && scrollState.canScrollBackward -> -RELEASE_NOTES_SCROLL_STEP
                            else -> null
                        }
                    scrollDelta?.let {
                        scope.launch {
                            scrollState.animateScrollTo(
                                (scrollState.value + it).coerceIn(0, scrollState.maxValue),
                            )
                        }
                        true
                    } ?: false
                },
    )
}

@Composable
private fun UpdateDialogControls(
    state: AppUpdateUiState,
    primaryFocusRequester: FocusRequester,
    onUpdateNow: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onRetry: () -> Unit,
    onLater: () -> Unit,
) {
    when (state) {
        is AppUpdateUiState.Downloading -> DownloadProgress(state.progress)
        is AppUpdateUiState.ReadyToInstall -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        else ->
            UpdateDialogActions(
                action = state.primaryAction(),
                primaryFocusRequester = primaryFocusRequester,
                onUpdateNow = onUpdateNow,
                onOpenInstallSettings = onOpenInstallSettings,
                onRetry = onRetry,
                onLater = onLater,
            )
    }
}

@Composable
private fun DownloadProgress(progress: Float) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = "${(progress * 100).roundToInt()}%",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UpdateDialogActions(
    action: UpdatePrimaryAction?,
    primaryFocusRequester: FocusRequester,
    onUpdateNow: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onRetry: () -> Unit,
    onLater: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
    ) {
        OutlinedButton(
            onClick = onLater,
            shape = TuneFlowShapes.row,
            modifier = if (action == null) Modifier.focusRequester(primaryFocusRequester) else Modifier,
        ) {
            Text("Later")
        }
        if (action != null) {
            Button(
                onClick =
                    when (action) {
                        UpdatePrimaryAction.Update -> onUpdateNow
                        UpdatePrimaryAction.OpenSettings -> onOpenInstallSettings
                        UpdatePrimaryAction.Retry -> onRetry
                    },
                shape = TuneFlowShapes.row,
                modifier = Modifier.focusRequester(primaryFocusRequester),
            ) {
                Text(action.label)
            }
        }
    }
}

private fun AppUpdateUiState.primaryAction(): UpdatePrimaryAction? =
    when (this) {
        is AppUpdateUiState.Available -> UpdatePrimaryAction.Update
        is AppUpdateUiState.PermissionRequired -> UpdatePrimaryAction.OpenSettings
        is AppUpdateUiState.Failed -> UpdatePrimaryAction.Retry.takeIf { retryAvailable }
        is AppUpdateUiState.Downloading,
        is AppUpdateUiState.ReadyToInstall,
        AppUpdateUiState.Hidden,
        -> null
    }

private enum class UpdatePrimaryAction(val label: String) {
    Update("Update Now"),
    OpenSettings("Open Settings"),
    Retry("Retry"),
}

private fun AppUpdateUiState.title(): String =
    when (this) {
        is AppUpdateUiState.Available -> "Update Available"
        is AppUpdateUiState.PermissionRequired -> "Allow TuneFlow Updates"
        is AppUpdateUiState.Downloading -> "Downloading TuneFlow ${release.version}"
        is AppUpdateUiState.ReadyToInstall -> "Preparing Update"
        is AppUpdateUiState.Failed -> "Update Failed"
        AppUpdateUiState.Hidden -> ""
    }

private fun AppUpdateUiState.message(): String =
    when (this) {
        is AppUpdateUiState.Available ->
            "TuneFlow ${release.version} is available. Would you like to update now?"
        is AppUpdateUiState.PermissionRequired ->
            if (permissionWasDenied) {
                "TuneFlow still needs permission to install apps. Enable it in device settings, then return."
            } else {
                "Allow TuneFlow to install apps from this source, then return to continue the update."
            }
        is AppUpdateUiState.Downloading -> "Keep TuneFlow open while the signed release APK downloads."
        is AppUpdateUiState.ReadyToInstall -> "Opening the Android package installer."
        is AppUpdateUiState.Failed -> message
        AppUpdateUiState.Hidden -> ""
    }

private val RELEASE_NOTES_MAX_HEIGHT = 180.dp
private const val RELEASE_NOTES_SCROLL_STEP = 96
