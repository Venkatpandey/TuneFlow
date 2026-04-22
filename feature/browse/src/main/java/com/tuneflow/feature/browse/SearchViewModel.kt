package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.SearchBundle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val result: SearchBundle = SearchBundle(emptyList(), emptyList(), emptyList()),
    val error: String? = null,
)

class SearchViewModel(private val repository: BrowseRepository) : ViewModel() {
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(result = SearchBundle(emptyList(), emptyList(), emptyList()), isLoading = false) }
            return
        }

        searchJob =
            viewModelScope.launch {
                delay(350)
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.search(query)
                _uiState.update {
                    if (result.isSuccess) {
                        it.copy(isLoading = false, result = result.getOrNull() ?: SearchBundle(emptyList(), emptyList(), emptyList()))
                    } else {
                        it.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                    }
                }
            }
    }
}
