@file:Suppress("MatchingDeclarationName")

package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuneflow.core.design.ThemePreviewColors
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.design.TuneFlowThemeDefinition
import com.tuneflow.core.design.TuneFlowThemes
import com.tuneflow.core.network.ScreenScaleOption

@Composable
internal fun AppSettingsDialog(
    currentThemeId: String,
    onPreviewTheme: (String) -> Unit,
    onApplyTheme: (String) -> Unit,
    onDismiss: () -> Unit,
    currentScreenScale: ScreenScaleOption = ScreenScaleOption.Compact,
    onScreenScaleSelected: ((ScreenScaleOption) -> Unit)? = null,
) {
    var selectedThemeId by remember { mutableStateOf(currentThemeId) }
    var selectedScale by remember { mutableStateOf(currentScreenScale) }
    val initialFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = {
            onPreviewTheme(currentThemeId)
            onDismiss()
        },
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
                        .width(760.dp)
                        .clip(TuneFlowShapes.panel)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Personalize colors while keeping high contrast and obvious TV focus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TuneFlowThemes.all.forEachIndexed { index, theme ->
                        val isInitial = theme.id.rawId.equals(currentThemeId, ignoreCase = true)
                        ThemeSelectionCard(
                            theme = theme,
                            isSelected = theme.id.rawId.equals(selectedThemeId, ignoreCase = true),
                            isApplied = theme.id.rawId.equals(currentThemeId, ignoreCase = true),
                            onSelect = {
                                selectedThemeId = theme.id.rawId
                                onPreviewTheme(theme.id.rawId)
                            },
                            modifier =
                                if (isInitial || index == 0) {
                                    Modifier.focusRequester(initialFocusRequester)
                                } else {
                                    Modifier
                                },
                        )
                    }
                }

                if (onScreenScaleSelected != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Display Scale",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ScreenScaleOption.entries.forEach { option ->
                            ScaleSelectionChip(
                                option = option,
                                isSelected = option == selectedScale,
                                onSelect = {
                                    selectedScale = option
                                    onScreenScaleSelected(option)
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            onPreviewTheme(currentThemeId)
                            onDismiss()
                        },
                        shape = TuneFlowShapes.row,
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onApplyTheme(selectedThemeId)
                            onDismiss()
                        },
                        shape = TuneFlowShapes.row,
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        initialFocusRequester.requestFocus()
    }
}

@Composable
private fun ThemeSelectionCard(
    theme: TuneFlowThemeDefinition,
    isSelected: Boolean,
    isApplied: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(TuneFlowShapes.row)
                .background(
                    when {
                        focused -> MaterialTheme.colorScheme.surfaceVariant
                        isSelected -> MaterialTheme.colorScheme.surfaceContainerHigh
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        when {
                            focused -> MaterialTheme.colorScheme.onSurface
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                        },
                    shape = TuneFlowShapes.row,
                )
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    if (focusState.isFocused) {
                        onSelect()
                    }
                }
                .clickable(
                    role = Role.RadioButton,
                    onClick = onSelect,
                )
                .semantics {
                    role = Role.RadioButton
                    selected = isSelected
                    contentDescription =
                        "${theme.displayName} theme. ${theme.description}. " +
                        if (isApplied) "Currently applied." else ""
                }
                .focusable()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeColorSwatches(previewColors = theme.previewColors)

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isApplied) {
                    Box(
                        modifier =
                            Modifier
                                .clip(TuneFlowShapes.avatar)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Applied",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeColorSwatches(
    previewColors: ThemePreviewColors,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwatchCircle(color = previewColors.background, label = "Background")
        SwatchCircle(color = previewColors.highlight, label = "Highlight")
        SwatchCircle(color = previewColors.text, label = "Text")
    }
}

@Composable
private fun SwatchCircle(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                .semantics { contentDescription = label },
    )
}

@Composable
private fun ScaleSelectionChip(
    option: ScreenScaleOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .clip(TuneFlowShapes.row)
                .background(
                    when {
                        focused -> MaterialTheme.colorScheme.surfaceVariant
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        when {
                            focused -> MaterialTheme.colorScheme.onSurface
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                        },
                    shape = TuneFlowShapes.row,
                )
                .onFocusChanged { focused = it.isFocused }
                .clickable(role = Role.RadioButton, onClick = onSelect)
                .focusable()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${option.name} (${option.percent}%)",
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}
