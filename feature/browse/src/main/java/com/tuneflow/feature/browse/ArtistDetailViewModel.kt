package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.ArtistDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArtistDetailUiState(
    val isLoading: Boolean = false,
    val artist: ArtistDetail? = null,
    val error: String? = null,
)

class ArtistDetailViewModel(private val repository: BrowseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    fun load(artistId: String) {
        _uiState.value = ArtistDetailUiState(isLoading = true)
        viewModelScope.launch {
            val result = repository.getArtistDetail(artistId)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, artist = result.getOrNull(), error = null)
                } else {
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}
