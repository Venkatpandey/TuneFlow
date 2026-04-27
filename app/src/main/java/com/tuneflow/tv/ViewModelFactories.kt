package com.tuneflow.tv

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuneflow.core.network.SearchHistoryStore
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.player.TvPlayerManager
import com.tuneflow.feature.auth.AuthRepository
import com.tuneflow.feature.auth.AuthViewModel
import com.tuneflow.feature.browse.AlbumDetailViewModel
import com.tuneflow.feature.browse.AlbumsViewModel
import com.tuneflow.feature.browse.ArtistDetailViewModel
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.browse.PlaylistsViewModel
import com.tuneflow.feature.browse.SearchViewModel
import com.tuneflow.feature.playback.PlaybackViewModel

fun authViewModelFactory(
    repository: AuthRepository,
    sessionStore: SessionStore,
) = viewModelFactory {
    initializer { AuthViewModel(repository, sessionStore) }
}

fun homeViewModelFactory(repository: BrowseRepository) =
    viewModelFactory {
        initializer { HomeViewModel(repository) }
    }

fun albumsViewModelFactory(repository: BrowseRepository) =
    viewModelFactory {
        initializer { AlbumsViewModel(repository) }
    }

fun albumDetailViewModelFactory(repository: BrowseRepository) =
    viewModelFactory {
        initializer { AlbumDetailViewModel(repository) }
    }

fun artistDetailViewModelFactory(repository: BrowseRepository) =
    viewModelFactory {
        initializer { ArtistDetailViewModel(repository) }
    }

fun playlistsViewModelFactory(repository: BrowseRepository) =
    viewModelFactory {
        initializer { PlaylistsViewModel(repository) }
    }

fun searchViewModelFactory(
    repository: BrowseRepository,
    historyStore: SearchHistoryStore,
) = viewModelFactory {
    initializer { SearchViewModel(repository, historyStore) }
}

fun playbackViewModelFactory(manager: TvPlayerManager) =
    viewModelFactory {
        initializer { PlaybackViewModel(manager) }
    }
