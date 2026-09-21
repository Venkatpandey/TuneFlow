package com.tuneflow.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuneflow.core.design.InitialFocusEffect
import com.tuneflow.core.design.LocalTuneFlowMotion
import com.tuneflow.core.design.TuneFlowActionSurface
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.design.animateTuneFlowFocusScale

private enum class LoginFieldKey { ServerUrl, Username, Password }

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    logoResId: Int,
    backgroundResId: Int,
    screenScaleFactor: Float,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingField by remember { mutableStateOf<LoginFieldKey?>(null) }
    val initialFocusRequester = remember { FocusRequester() }

    InitialFocusEffect(
        focusRequester = initialFocusRequester,
        targetAvailable = true,
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.42f,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.30f)),
        )

        LoginSafeArea {
            LoginScaledContent(scaleFactor = screenScaleFactor) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1.25f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(
                            painter = painterResource(id = logoResId),
                            contentDescription = "TuneFlow logo",
                            modifier =
                                Modifier
                                    .size(112.dp)
                                    .clip(TuneFlowShapes.artwork),
                        )
                        Text(
                            text = "TuneFlow",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.width(72.dp))

                    Column(
                        modifier =
                            Modifier
                                .width(452.dp)
                                .clip(TuneFlowShapes.panel)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                                    shape = TuneFlowShapes.panel,
                                )
                                .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Sign in",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        LoginField(
                            value = state.serverUrl,
                            onValueChange = viewModel::updateServerUrl,
                            label = "Navidrome URL",
                            placeholder = "http://192.168.1.10:4533",
                            editing = editingField == LoginFieldKey.ServerUrl,
                            onEditingChange = { isEditing ->
                                editingField = if (isEditing) LoginFieldKey.ServerUrl else null
                            },
                            displayFocusRequesterOverride = initialFocusRequester,
                        )

                        LoginField(
                            value = state.username,
                            onValueChange = viewModel::updateUsername,
                            label = "Username",
                            placeholder = "Your Navidrome user",
                            editing = editingField == LoginFieldKey.Username,
                            onEditingChange = { isEditing ->
                                editingField = if (isEditing) LoginFieldKey.Username else null
                            },
                        )

                        LoginField(
                            value = state.password,
                            onValueChange = viewModel::updatePassword,
                            label = "Password",
                            placeholder = "Password",
                            keyboardType = KeyboardType.Password,
                            obscure = true,
                            editing = editingField == LoginFieldKey.Password,
                            onEditingChange = { isEditing ->
                                editingField = if (isEditing) LoginFieldKey.Password else null
                            },
                        )

                        if (state.error != null) {
                            Text(
                                text = state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        LoginActionButton(
                            onClick = viewModel::login,
                            enabled = !state.isLoading,
                        ) {
                            Text(if (state.isLoading) "Signing in..." else "Login")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginScaledContent(
    scaleFactor: Float,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaledDensity =
        Density(
            density = density.density * scaleFactor,
            fontScale = density.fontScale * scaleFactor,
        )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        content()
    }
}

@Composable
private fun LoginSafeArea(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp),
        content = content,
    )
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    obscure: Boolean = false,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    displayFocusRequesterOverride: FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val editFocusRequester = remember { FocusRequester() }
    val defaultDisplayFocusRequester = remember { FocusRequester() }
    val displayFocusRequester = displayFocusRequesterOverride ?: defaultDisplayFocusRequester
    var focused by remember { mutableStateOf(false) }
    var restoreDisplayFocus by remember { mutableStateOf(false) }
    var pendingExitDirection by remember { mutableStateOf<FocusDirection?>(null) }

    fun stopEditing(direction: FocusDirection? = null) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        pendingExitDirection = direction
        restoreDisplayFocus = true
        onEditingChange(false)
    }

    LaunchedEffect(editing, restoreDisplayFocus) {
        when {
            editing -> {
                editFocusRequester.requestFocus()
                keyboardController?.show()
            }
            restoreDisplayFocus -> {
                displayFocusRequester.requestFocus()
                pendingExitDirection?.let { focusManager.moveFocus(it) }
                pendingExitDirection = null
                restoreDisplayFocus = false
            }
        }
    }

    if (editing) {
        EditingLoginField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardType = keyboardType,
            focusRequester = editFocusRequester,
            onKeyExit = ::stopEditing,
        )
    } else {
        DisplayLoginField(
            value = value,
            label = label,
            placeholder = placeholder,
            obscure = obscure,
            focused = focused,
            focusRequester = displayFocusRequester,
            onFocusedChange = { focused = it },
            onClick = { onEditingChange(true) },
        )
    }
}

@Composable
private fun EditingLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    visualTransformation: VisualTransformation,
    keyboardType: KeyboardType,
    focusRequester: FocusRequester,
    onKeyExit: (FocusDirection?) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        shape = TuneFlowShapes.field,
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                    when {
                        it.key == Key.Back -> {
                            onKeyExit(null)
                            true
                        }
                        keyToFocusDirection(it.key) != null -> {
                            onKeyExit(keyToFocusDirection(it.key))
                            true
                        }
                        else -> false
                    }
                },
        colors = loginFieldColors(),
    )
}

private fun keyToFocusDirection(key: Key): FocusDirection? {
    return when (key) {
        Key.DirectionUp -> FocusDirection.Up
        Key.DirectionDown -> FocusDirection.Down
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionRight -> FocusDirection.Right
        else -> null
    }
}

@Composable
private fun DisplayLoginField(
    value: String,
    label: String,
    placeholder: String,
    obscure: Boolean,
    focused: Boolean,
    focusRequester: FocusRequester,
    onFocusedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val scale =
        animateTuneFlowFocusScale(
            focused = focused,
            focusedScale = LocalTuneFlowMotion.current.fieldFocusScale,
            label = "login-field-focus",
        )
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(TuneFlowShapes.field)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    shape = TuneFlowShapes.field,
                )
                .onFocusChanged { onFocusedChange(it.hasFocus) }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    when {
                        value.isNotBlank() && obscure -> "•".repeat(value.length.coerceAtMost(16))
                        value.isNotBlank() -> value
                        else -> placeholder
                    },
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (value.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun loginFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

@Composable
private fun LoginActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    TuneFlowActionSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        accent = true,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.ProvideTextStyle(
                    value = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    content = content,
                )
            }
        }
    }
}
