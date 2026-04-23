package com.tuneflow.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSession = sessionStore.sessionFlow.first() != null
            if (hasSession) {
                _uiState.update { it.copy(isLoggedIn = true) }
            }
        }
    }

    fun updateServerUrl(value: String) = _uiState.update { it.copy(serverUrl = value, error = null) }

    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, error = null) }

    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun login() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result =
                try {
                    repository.login(
                        serverUrl = current.serverUrl,
                        username = current.username,
                        password = current.password,
                    )
                } catch (ex: Exception) {
                    Result.failure(ex)
                }

            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, isLoggedIn = true, password = "", error = null)
                } else {
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Login failed")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState()
        }
    }
}
