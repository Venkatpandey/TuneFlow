package com.tuneflow.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.player.TvPlayerManager
import com.tuneflow.feature.auth.AuthRepository
import com.tuneflow.feature.auth.AuthViewModel
import com.tuneflow.feature.browse.AlbumDetailViewModel
import com.tuneflow.feature.browse.AlbumsViewModel
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.browse.PlaylistsViewModel
import com.tuneflow.feature.browse.SearchViewModel
import com.tuneflow.feature.playback.PlaybackViewModel

class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AuthViewModel(repository, sessionStore) as T
    }
}

class HomeViewModelFactory(private val repository: BrowseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository) as T
    }
}

class AlbumsViewModelFactory(private val repository: BrowseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumsViewModel(repository) as T
    }
}

class AlbumDetailViewModelFactory(private val repository: BrowseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AlbumDetailViewModel(repository) as T
    }
}

class PlaylistsViewModelFactory(private val repository: BrowseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaylistsViewModel(repository) as T
    }
}

class SearchViewModelFactory(private val repository: BrowseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(repository) as T
    }
}

class PlaybackViewModelFactory(private val manager: TvPlayerManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlaybackViewModel(manager) as T
    }
}
