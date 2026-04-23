package com.tuneflow.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuneflow.core.network.SearchBundle
import com.tuneflow.core.network.SearchHistoryStore
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
    val recentQueries: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val error: String? = null,
)

class SearchViewModel(
    private val repository: BrowseRepository,
    private val historyStore: SearchHistoryStore,
) : ViewModel() {
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyStore.recentQueriesFlow.collect { queries ->
                _uiState.update { it.copy(recentQueries = queries) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    result = SearchBundle(emptyList(), emptyList(), emptyList()),
                    suggestions = emptyList(),
                    isLoading = false,
                )
            }
            return
        }

        searchJob =
            viewModelScope.launch {
                delay(350)
                _uiState.update { it.copy(isLoading = true) }
                val result = repository.search(query)
                _uiState.update {
                    if (result.isSuccess) {
                        val bundle = result.getOrNull() ?: SearchBundle(emptyList(), emptyList(), emptyList())
                        it.copy(
                            isLoading = false,
                            result = bundle,
                            suggestions = suggestionsFor(bundle),
                        )
                    } else {
                        it.copy(isLoading = false, error = result.exceptionOrNull()?.message, suggestions = emptyList())
                    }
                }
                historyStore.record(query)
            }
    }

    fun applySuggestedQuery(query: String) {
        onQueryChanged(query)
    }

    private fun suggestionsFor(bundle: SearchBundle): List<String> {
        return buildList {
            addAll(bundle.artists.map { it.name })
            addAll(bundle.albums.map { it.title })
            addAll(bundle.tracks.map { it.title })
        }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(8)
    }
}
