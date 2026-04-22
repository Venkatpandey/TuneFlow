package com.tuneflow.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "TuneFlow", style = MaterialTheme.typography.displaySmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = viewModel::updateServerUrl,
            label = { Text("Navidrome URL") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::updateUsername,
            label = { Text("Username") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
        )

        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = viewModel::login,
            enabled = !state.isLoading,
        ) {
            Text(if (state.isLoading) "Signing in..." else "Login")
        }
    }
}
