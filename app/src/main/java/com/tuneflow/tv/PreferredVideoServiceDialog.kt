package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.feature.video.normalizePreferredVideoServiceUrl

@Composable
internal fun PreferredVideoServiceDialog(
    currentUrl: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draftUrl by remember(currentUrl) { mutableStateOf(currentUrl) }
    var error by remember { mutableStateOf<String?>(null) }
    val urlFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .width(720.dp)
                        .clip(TuneFlowShapes.panel)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Preferred video service",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Enter the LAN address of the TuneFlow Docker service. Example: http://192.168.0.128:8090",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = draftUrl,
                    onValueChange = {
                        draftUrl = it
                        error = null
                    },
                    label = { Text("Service URL") },
                    placeholder = { Text("http://192.168.0.128:8090") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = error != null,
                    supportingText = error?.let { message -> ({ Text(message) }) },
                    shape = TuneFlowShapes.row,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(urlFocusRequester),
                )
                Text(
                    text = "No authentication. Use only on a trusted LAN. Leaving it disabled keeps normal video search available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = TuneFlowShapes.row,
                    ) {
                        Text("Cancel")
                    }
                    OutlinedButton(
                        onClick = {
                            onSave("")
                            onDismiss()
                        },
                        shape = TuneFlowShapes.row,
                    ) {
                        Text("Disable")
                    }
                    Button(
                        onClick = {
                            val normalized = normalizePreferredVideoServiceUrl(draftUrl)
                            if (normalized.isNullOrEmpty()) {
                                error = "Enter a valid HTTP or HTTPS service URL."
                            } else {
                                onSave(normalized)
                                onDismiss()
                            }
                        },
                        shape = TuneFlowShapes.row,
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        urlFocusRequester.requestFocus()
    }
}
