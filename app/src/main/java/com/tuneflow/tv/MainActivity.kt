package com.tuneflow.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.player.PlaybackQueue
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
                    LoginScreen(
                        viewModel = authViewModel,
                        logoResId = R.drawable.ic_tuneflow_brand,
                        backgroundResId = R.drawable.login_background,
                    )
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

private fun com.tuneflow.core.network.TrackSummary.toQueueItem(streamUrl: String): QueueItem {
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

private enum class NavSection { Home, Albums, Playlists, Search }

private const val NOW_PLAYING_SCREEN_KEY = "nowPlaying"

@Composable
private fun TuneFlowShell(
    browseRepository: BrowseRepository,
    playerManager: com.tuneflow.core.player.TvPlayerManager,
) {
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(browseRepository))
    val albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel = viewModel(factory = AlbumsViewModelFactory(browseRepository))
    val albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel =
        viewModel(factory = AlbumDetailViewModelFactory(browseRepository))
    val playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel =
        viewModel(factory = PlaylistsViewModelFactory(browseRepository))
    val searchViewModel: com.tuneflow.feature.browse.SearchViewModel = viewModel(factory = SearchViewModelFactory(browseRepository))
    val playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel = viewModel(factory = PlaybackViewModelFactory(playerManager))
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()

    var currentSection by rememberSaveable { mutableStateOf(NavSection.Home) }
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var albumSourceSection by rememberSaveable { mutableStateOf(NavSection.Home) }
    var preselectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }

    fun openSection(section: NavSection) {
        currentSection = section
        selectedAlbumId = null
        if (section != NavSection.Playlists) {
            preselectedPlaylistId = null
        }
        showNowPlaying = false
    }

    fun openAlbum(
        albumId: String,
        source: NavSection,
    ) {
        currentSection = source
        albumSourceSection = source
        selectedAlbumId = albumId
        showNowPlaying = false
    }

    fun openNowPlaying() {
        showNowPlaying = true
    }

    fun playTracks(
        tracks: List<com.tuneflow.core.network.TrackSummary>,
        index: Int,
    ) {
        scope.launch {
            val queue =
                tracks.map { track ->
                    track.toQueueItem(
                        streamUrl = browseRepository.streamUrl(track.id),
                    )
                }
            playerManager.playQueue(queue, index)
            openNowPlaying()
        }
    }

    ShellBackHandler(
        showNowPlaying = showNowPlaying,
        selectedAlbumId = selectedAlbumId,
        currentSection = currentSection,
        onCloseNowPlaying = { showNowPlaying = false },
        onCloseAlbum = {
            selectedAlbumId = null
            currentSection = albumSourceSection
        },
        onGoHome = { currentSection = NavSection.Home },
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.16f,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0x802A4C66), Color(0xF2071019)),
                        ),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(26.dp),
        ) {
            NavRail(
                currentSection = currentSection,
                onSectionSelected = ::openSection,
                onNowPlaying = ::openNowPlaying,
                isNowPlayingActive = showNowPlaying,
            )

            Spacer(Modifier.width(22.dp))

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(34.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(34.dp),
                        )
                        .padding(26.dp),
            ) {
                ShellContent(
                    currentSection = currentSection,
                    selectedAlbumId = selectedAlbumId,
                    showNowPlaying = showNowPlaying,
                    preselectedPlaylistId = preselectedPlaylistId,
                    playbackQueue = playbackState.queue,
                    homeViewModel = homeViewModel,
                    albumsViewModel = albumsViewModel,
                    albumDetailViewModel = albumDetailViewModel,
                    playlistsViewModel = playlistsViewModel,
                    searchViewModel = searchViewModel,
                    playbackViewModel = playbackViewModel,
                    onOpenAlbum = ::openAlbum,
                    onOpenSection = ::openSection,
                    onOpenPlaylist = {
                        currentSection = NavSection.Playlists
                        preselectedPlaylistId = it
                        showNowPlaying = false
                    },
                    onPreselectedPlaylistConsumed = { preselectedPlaylistId = null },
                    onOpenNowPlaying = ::openNowPlaying,
                    onPlayTracks = ::playTracks,
                )
            }
        }
    }
}

@Composable
private fun ShellBackHandler(
    showNowPlaying: Boolean,
    selectedAlbumId: String?,
    currentSection: NavSection,
    onCloseNowPlaying: () -> Unit,
    onCloseAlbum: () -> Unit,
    onGoHome: () -> Unit,
) {
    BackHandler(enabled = showNowPlaying || selectedAlbumId != null || currentSection != NavSection.Home) {
        when {
            showNowPlaying -> onCloseNowPlaying()
            selectedAlbumId != null -> onCloseAlbum()
            currentSection != NavSection.Home -> onGoHome()
        }
    }
}

private fun shellScreenKey(
    currentSection: NavSection,
    selectedAlbumId: String?,
    showNowPlaying: Boolean,
): String {
    return when {
        showNowPlaying -> NOW_PLAYING_SCREEN_KEY
        selectedAlbumId != null -> "album:$selectedAlbumId"
        else -> currentSection.name
    }
}

@Composable
private fun ShellContent(
    currentSection: NavSection,
    selectedAlbumId: String?,
    showNowPlaying: Boolean,
    preselectedPlaylistId: String?,
    playbackQueue: PlaybackQueue,
    homeViewModel: HomeViewModel,
    albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel,
    albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel,
    playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel,
    searchViewModel: com.tuneflow.feature.browse.SearchViewModel,
    playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel,
    onOpenAlbum: (String, NavSection) -> Unit,
    onOpenSection: (NavSection) -> Unit,
    onOpenPlaylist: (String?) -> Unit,
    onPreselectedPlaylistConsumed: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onPlayTracks: (List<com.tuneflow.core.network.TrackSummary>, Int) -> Unit,
) {
    val screenKey = shellScreenKey(currentSection, selectedAlbumId, showNowPlaying)

    Crossfade(targetState = screenKey, label = "shell-content") { targetScreen ->
        when {
            targetScreen == NOW_PLAYING_SCREEN_KEY -> {
                NowPlayingScreen(viewModel = playbackViewModel)
            }
            targetScreen.startsWith("album:") -> {
                AlbumDetailScreen(
                    albumId = targetScreen.removePrefix("album:"),
                    viewModel = albumDetailViewModel,
                    onPlayAlbum = onPlayTracks,
                )
            }
            targetScreen == NavSection.Home.name -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    playbackQueue = playbackQueue,
                    onOpenAlbum = { onOpenAlbum(it, NavSection.Home) },
                    onOpenAlbums = { onOpenSection(NavSection.Albums) },
                    onOpenPlaylists = onOpenPlaylist,
                    onOpenSearch = { onOpenSection(NavSection.Search) },
                    onOpenNowPlaying = onOpenNowPlaying,
                )
            }
            targetScreen == NavSection.Albums.name -> {
                AlbumsScreen(
                    viewModel = albumsViewModel,
                    onAlbumSelected = { onOpenAlbum(it, NavSection.Albums) },
                )
            }
            targetScreen == NavSection.Playlists.name -> {
                PlaylistsScreen(
                    viewModel = playlistsViewModel,
                    preselectedPlaylistId = preselectedPlaylistId,
                    onPreselectedPlaylistConsumed = onPreselectedPlaylistConsumed,
                    onPlayTracks = onPlayTracks,
                )
            }
            targetScreen == NavSection.Search.name -> {
                SearchScreen(
                    viewModel = searchViewModel,
                    onOpenAlbum = { onOpenAlbum(it, NavSection.Search) },
                    onPlayTracks = onPlayTracks,
                )
            }
        }
    }
}

@Composable
private fun NavRail(
    currentSection: NavSection,
    onSectionSelected: (NavSection) -> Unit,
    onNowPlaying: () -> Unit,
    isNowPlayingActive: Boolean,
) {
    Column(
        modifier =
            Modifier
                .width(236.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(30.dp),
                )
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_tuneflow_brand),
                contentDescription = "TuneFlow",
                modifier =
                    Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp)),
            )
            Text(
                text = "TuneFlow",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Music, tuned for the biggest screen in the room.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RailItem(
            label = "Home",
            selected = currentSection == NavSection.Home && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Home) },
        )
        RailItem(
            label = "Albums",
            selected = currentSection == NavSection.Albums && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Albums) },
        )
        RailItem(
            label = "Playlists",
            selected = currentSection == NavSection.Playlists && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Playlists) },
        )
        RailItem(
            label = "Search",
            selected = currentSection == NavSection.Search && !isNowPlayingActive,
            onClick = { onSectionSelected(NavSection.Search) },
        )

        Spacer(modifier = Modifier.weight(1f))

        RailItem(
            label = "Now Playing",
            selected = isNowPlayingActive,
            onClick = onNowPlaying,
        )
    }
}

@Composable
private fun RailItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val active = selected || focused

    Row(
        modifier =
            Modifier
                .width(196.dp)
                .scale(if (focused) 1.03f else 1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                    },
                )
                .border(
                    width = if (active) 2.dp else 1.dp,
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        },
                    shape = RoundedCornerShape(20.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color =
                if (active) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}
