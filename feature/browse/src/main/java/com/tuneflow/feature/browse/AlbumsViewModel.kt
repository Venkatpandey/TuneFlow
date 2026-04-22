package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.AlbumSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumsUiState(
    val items: List<AlbumSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
)

class AlbumsViewModel(private val repository: BrowseRepository) : ViewModel() {
    private val pageSize = 30
    private var offset = 0

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        offset = 0
        _uiState.value = AlbumsUiState(isLoading = true)
        viewModelScope.launch {
            val result = repository.getAlbums(pageSize, offset)
            _uiState.update {
                if (result.isSuccess) {
                    val albums = result.getOrNull().orEmpty()
                    it.copy(
                        items = albums,
                        isLoading = false,
                        hasMore = albums.size == pageSize,
                        error = null,
                    )
                } else {
                    it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                }
            }
            offset += _uiState.value.items.size
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isLoadingMore || !current.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val result = repository.getAlbums(pageSize, offset)
            _uiState.update {
                if (result.isSuccess) {
                    val next = result.getOrNull().orEmpty()
                    it.copy(
                        items = it.items + next,
                        isLoadingMore = false,
                        hasMore = next.size == pageSize,
                        error = null,
                    )
                } else {
                    it.copy(isLoadingMore = false, error = result.exceptionOrNull()?.message)
                }
            }
            if (result.isSuccess) {
                offset += result.getOrNull().orEmpty().size
            }
        }
    }
}
