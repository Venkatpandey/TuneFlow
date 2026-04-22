package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.AlbumDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumDetailUiState(
    val isLoading: Boolean = false,
    val album: AlbumDetail? = null,
    val error: String? = null,
)

class AlbumDetailViewModel(private val repository: BrowseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    fun load(albumId: String) {
        _uiState.value = AlbumDetailUiState(isLoading = true)
        viewModelScope.launch {
            val result = repository.getAlbumDetail(albumId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, album = result.getOrNull(), error = null)
                } else {
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}
