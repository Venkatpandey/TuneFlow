package com.tuneflow.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.player.PlayerGraph
import com.tuneflow.core.player.QueueItem
import com.tuneflow.core.player.TuneFlowPlaybackService
import com.tuneflow.feature.auth.AuthRepository
import com.tuneflow.feature.auth.LoginScreen
import com.tuneflow.feature.browse.AlbumDetailScreen
import com.tuneflow.feature.browse.AlbumsScreen
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.browse.PlaylistsScreen
import com.tuneflow.feature.browse.SearchScreen
import com.tuneflow.feature.playback.NowPlayingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(applicationContext)
        val authRepository = AuthRepository(sessionStore)
        val browseRepository = BrowseRepository(sessionStore)
        val playerManager = PlayerGraph.get(applicationContext)
        startService(Intent(this, TuneFlowPlaybackService::class.java))

        setContent {
            TuneFlowTheme {
                val authViewModel: com.tuneflow.feature.auth.AuthViewModel =
                    viewModel(
                        factory = AuthViewModelFactory(authRepository, sessionStore),
                    )
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                if (!authState.isLoggedIn) {
                    LoginScreen(viewModel = authViewModel)
                } else {
                    TuneFlowShell(
                        browseRepository = browseRepository,
                        playerManager = playerManager,
                    )
                }
            }
        }
    }
}

private fun com.tuneflow.core.network.TrackSummary.toQueueItem(
    streamUrl: String,
    artUrl: String?,
): QueueItem {
    return QueueItem(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artUrl = artUrl,
        streamUrl = streamUrl,
        durationMs = durationSec * 1000L,
    )
}

private enum class NavSection { Albums, Playlists, Search }

@Composable
private fun TuneFlowShell(
    browseRepository: BrowseRepository,
    playerManager: com.tuneflow.core.player.TvPlayerManager,
) {
    val scope = rememberCoroutineScope()
    val albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel = viewModel(factory = AlbumsViewModelFactory(browseRepository))
    val albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel =
        viewModel(factory = AlbumDetailViewModelFactory(browseRepository))
    val playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel =
        viewModel(factory = PlaylistsViewModelFactory(browseRepository))
    val searchViewModel: com.tuneflow.feature.browse.SearchViewModel = viewModel(factory = SearchViewModelFactory(browseRepository))
    val playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel = viewModel(factory = PlaybackViewModelFactory(playerManager))

    var nav by remember { mutableStateOf(NavSection.Albums) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var showNowPlaying by remember { mutableStateOf(false) }

    fun playTracks(
        tracks: List<com.tuneflow.core.network.TrackSummary>,
        index: Int,
    ) {
        scope.launch {
            val queue =
                tracks.map { track ->
                    track.toQueueItem(
                        streamUrl = browseRepository.streamUrl(track.id),
                        artUrl = browseRepository.coverArtUrl(track.coverArtId),
                    )
                }
            playerManager.playQueue(queue, index)
            showNowPlaying = true
        }
    }

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SidebarItem("Albums", selected = nav == NavSection.Albums) {
                nav = NavSection.Albums
                selectedAlbumId = null
                showNowPlaying = false
            }
            SidebarItem("Playlists", selected = nav == NavSection.Playlists) {
                nav = NavSection.Playlists
                showNowPlaying = false
            }
            SidebarItem("Search", selected = nav == NavSection.Search) {
                nav = NavSection.Search
                showNowPlaying = false
            }
        }

        Spacer(Modifier.width(20.dp))

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(20.dp),
        ) {
            when {
                showNowPlaying -> {
                    NowPlayingScreen(viewModel = playbackViewModel)
                }
                nav == NavSection.Albums && selectedAlbumId != null -> {
                    AlbumDetailScreen(
                        albumId = selectedAlbumId.orEmpty(),
                        viewModel = albumDetailViewModel,
                        onPlayAlbum = ::playTracks,
                    )
                }
                nav == NavSection.Albums -> {
                    AlbumsScreen(
                        viewModel = albumsViewModel,
                        onAlbumSelected = { selectedAlbumId = it },
                    )
                }
                nav == NavSection.Playlists -> {
                    PlaylistsScreen(
                        viewModel = playlistsViewModel,
                        onPlayTracks = ::playTracks,
                    )
                }
                nav == NavSection.Search -> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        onPlayTracks = ::playTracks,
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(if (focused) 1.04f else 1f)
                .background(
                    color =
                        if (selected || focused) {
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.22f,
                            )
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = if (focused) 2.dp else 0.dp,
                    color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label)
    }
}
