@file:Suppress("MatchingDeclarationName")

package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.tuneflow.core.design.TuneFlowActionSurface
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.design.TuneFlowThemeDefinition
import com.tuneflow.core.design.TuneFlowThemes
import com.tuneflow.core.network.ScreenScaleOption

@Composable
internal fun AppSettingsDialog(
    currentThemeId: String,
    onPreviewTheme: (String) -> Unit,
    onSaveTheme: (String) -> Unit,
    onCancelTheme: () -> Unit,
    currentScreenScale: ScreenScaleOption = ScreenScaleOption.Compact,
    onScreenScaleSelected: ((ScreenScaleOption) -> Unit)? = null,
) {
    var selectedThemeId by rememberSaveable { mutableStateOf(currentThemeId) }
    var selectedScale by rememberSaveable { mutableStateOf(currentScreenScale) }
    val initialFocusRequester = remember { FocusRequester() }
    val initialIndex =
        remember {
            val idx = TuneFlowThemes.all.indexOfFirst { it.id.rawId.equals(currentThemeId, ignoreCase = true) }
            if (idx >= 0) idx else 0
        }
    var hasRequestedInitialFocus by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onCancelTheme,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f)),
            contentAlignment = Alignment.Center,
        ) {
            TuneFlowScaledContent(scaleFactor = ScreenScaleOption.Compact.factor) {
                Column(
                    modifier =
                        Modifier
                            .width(760.dp)
                            .clip(TuneFlowShapes.panel)
                            .background(MaterialTheme.colorScheme.surface)
                            .verticalScroll(rememberScrollState())
                            .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        text = "Navigate with cursor up/down to preview. Click a theme to check it, then press Save.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TuneFlowThemes.all.forEachIndexed { index, theme ->
                            ThemeSelectionCard(
                                theme = theme,
                                isChecked = theme.id.rawId.equals(selectedThemeId, ignoreCase = true),
                                isApplied = theme.id.rawId.equals(currentThemeId, ignoreCase = true),
                                onClick = {
                                    selectedThemeId = theme.id.rawId
                                    onPreviewTheme(theme.id.rawId)
                                },
                                onFocus = {
                                    onPreviewTheme(theme.id.rawId)
                                },
                                modifier =
                                    if (index == initialIndex) {
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
                                    onFocus = {
                                        onPreviewTheme(selectedThemeId)
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TuneFlowActionSurface(
                            onClick = onCancelTheme,
                            shape = TuneFlowShapes.button,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            modifier =
                                Modifier
                                    .onFocusChanged { if (it.isFocused) onPreviewTheme(selectedThemeId) }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Cancel and discard theme preview"
                                    },
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        TuneFlowActionSurface(
                            onClick = { onSaveTheme(selectedThemeId) },
                            accent = true,
                            shape = TuneFlowShapes.button,
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                            modifier =
                                Modifier
                                    .onFocusChanged { if (it.isFocused) onPreviewTheme(selectedThemeId) }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Save and apply selected theme"
                                    },
                        ) {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedInitialFocus) {
            hasRequestedInitialFocus = true
            initialFocusRequester.requestFocus()
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun ThemeSelectionCard(
    theme: TuneFlowThemeDefinition,
    isChecked: Boolean,
    isApplied: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
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
                        isChecked -> MaterialTheme.colorScheme.surfaceContainerHigh
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        when {
                            focused -> MaterialTheme.colorScheme.onSurface
                            isChecked -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                        },
                    shape = TuneFlowShapes.row,
                )
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    if (focusState.isFocused) {
                        onFocus()
                    }
                }
                .clickable(
                    role = Role.Checkbox,
                    onClick = onClick,
                )
                .semantics {
                    role = Role.Checkbox
                    selected = isChecked
                    contentDescription =
                        "${theme.displayName} theme. ${theme.description}. " +
                        (if (isChecked) "Checked. " else "Unchecked. ") +
                        (if (isApplied) "Currently applied." else "")
                }
                .focusable()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeCheckbox(isChecked = isChecked)

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
                ThemeStatusBadge(isApplied = isApplied, isChecked = isChecked)
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
private fun ThemeCheckbox(
    isChecked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isChecked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                )
                .border(
                    width = 2.dp,
                    color =
                        if (isChecked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    shape = RoundedCornerShape(6.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (isChecked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ThemeStatusBadge(
    isApplied: Boolean,
    isChecked: Boolean,
) {
    if (isApplied) {
        Box(
            modifier =
                Modifier
                    .clip(TuneFlowShapes.avatar)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "Active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium,
            )
        }
    } else if (isChecked) {
        Box(
            modifier =
                Modifier
                    .clip(TuneFlowShapes.avatar)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "Checked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
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
    onFocus: () -> Unit = {},
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
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) {
                        onFocus()
                    }
                }
                .clickable(role = Role.RadioButton, onClick = onSelect)
                .semantics {
                    role = Role.RadioButton
                    selected = isSelected
                    contentDescription = "Display scale ${option.name} ${option.percent}%. ${if (isSelected) "Selected." else ""}"
                }
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
