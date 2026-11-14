package com.tuneflow.tv

import android.content.Context
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tuneflow.core.network.SearchHistoryStore
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.player.ScrobbleReporter
import com.tuneflow.core.player.TvPlayerManager
import com.tuneflow.feature.auth.AuthRepository
import com.tuneflow.feature.auth.AuthViewModel
import com.tuneflow.feature.browse.AlbumDetailViewModel
import com.tuneflow.feature.browse.AlbumsViewModel
import com.tuneflow.feature.browse.ArtistDetailViewModel
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.browse.HomeCategoryViewModel
import com.tuneflow.feature.browse.PlaylistsViewModel
import com.tuneflow.feature.browse.SearchViewModel
import com.tuneflow.feature.playback.LyricsProvider
import com.tuneflow.feature.playback.PlaybackViewModel
import com.tuneflow.feature.video.PreferredVideoStore
import com.tuneflow.feature.video.SharedPreferencesVideoConsentStore
import com.tuneflow.feature.video.VideoViewModel

fun authViewModelFactory(
    repository: AuthRepository,
    sessionStore: SessionStore,
) = viewModelFactory {
    initializer { AuthViewModel(repository, sessionStore) }
}

fun homeViewModelFactory(
    repository: BrowseRepository,
    preferredVideoStore: PreferredVideoStore,
) = viewModelFactory {
    initializer { HomeViewModel(repository, preferredVideoStore) }
}

fun albumsViewModelFactory(repository: BrowseRepository) =
    viewModelFactory {
        initializer { AlbumsViewModel(repository) }
    }

fun homeCategoryViewModelFactory(repository: BrowseRepository) =
    viewModelFactory {
        initializer { HomeCategoryViewModel(repository) }
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

fun playbackViewModelFactory(
    manager: TvPlayerManager,
    lyricsProvider: LyricsProvider,
) = viewModelFactory {
    initializer { PlaybackViewModel(manager, lyricsProvider) }
}

fun videoViewModelFactory(
    context: Context,
    manager: TvPlayerManager,
    preferredVideoStore: PreferredVideoStore,
    scrobbleReporter: ScrobbleReporter,
) = viewModelFactory {
    initializer {
        VideoViewModel(
            audio = manager,
            consentStore = SharedPreferencesVideoConsentStore(context),
            nativeBackend = createNativeVideoBackend(context),
            preferredVideoStore = preferredVideoStore,
            scrobbleReporter = scrobbleReporter,
        )
    }
}
