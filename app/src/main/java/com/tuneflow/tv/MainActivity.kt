restick with os verspackage com.tuneflow.tv

import android.content.Intent
import android.os.Bundle
iimport androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.tuneflow.core.network.PlaybackPreferencesStore
import com.tuneflow.core.network.SearchHistoryStore
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.network.TrackStreamOptions
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlayerGraph
import com.tuneflow.core.player.QueueItem
import com.tuneflow.core.player.TuneFlowPlaybackService
import com.tuneflow.feature.auth.AuthRepository
import com.tuneflow.feature.auth.LoginScreen
import com.tuneflow.feature.browse.AlbumDetailScreen
import com.tuneflow.feature.browse.AlbumsScreen
import com.tuneflow.feature.browse.ArtistDetailScreen
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.browse.PlaylistsScreen
import com.tuneflow.feature.browse.SearchScreen
import com.tuneflow.feature.playback.NowPlayingScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(applicationContext)
        val searchHistoryStore = SearchHistoryStore(applicationContext)
        val playbackPreferencesStore = PlaybackPreferencesStore(applicationContext)
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
                        sessionStore = sessionStore,
                        playbackPreferencesStore = playbackPreferencesStore,
                        searchHistoryStore = searchHistoryStore,
                        onLogout = {
                            playerManager.stopAndClear()
                            authViewModel.logout()
                        },
                    )
                }
            }
        }
    }
}

private fun com.tuneflow.core.network.TrackSummary.toQueueItem(
    streamOptions: TrackStreamOptions,
    preferDirectWithFallback: Boolean,
): QueueItem {
    return QueueItem(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artUrl = artUrl,
        streamUrl = if (preferDirectWithFallback) streamOptions.directUrl else streamOptions.fallbackMp3Url,
        fallbackStreamUrl = if (preferDirectWithFallback) streamOptions.fallbackMp3Url else null,
        streamFormatLabel = if (preferDirectWithFallback) "FLAC" else "MP3",
        streamBitrateLabel = if (preferDirectWithFallback) "Original" else "Max",
        durationMs = durationSec * 1000L,
    )
}

private enum class NavSection { Home, Albums, Playlists, Search }

private const val NOW_PLAYING_SCREEN_KEY = "nowPlaying"

@Composable
private fun TuneFlowShell(
    browseRepository: BrowseRepository,
    playerManager: com.tuneflow.core.player.TvPlayerManager,
    sessionStore: SessionStore,
    playbackPreferencesStore: PlaybackPreferencesStore,
    searchHistoryStore: SearchHistoryStore,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(browseRepository))
    val albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel = viewModel(factory = AlbumsViewModelFactory(browseRepository))
    val albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel =
        viewModel(factory = AlbumDetailViewModelFactory(browseRepository))
    val artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel =
        viewModel(factory = ArtistDetailViewModelFactory(browseRepository))
    val playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel =
        viewModel(factory = PlaylistsViewModelFactory(browseRepository))
    val searchViewModel: com.tuneflow.feature.browse.SearchViewModel =
        viewModel(factory = SearchViewModelFactory(browseRepository, searchHistoryStore))
    val playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel = viewModel(factory = PlaybackViewModelFactory(playerManager))
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val session by sessionStore.sessionFlow.collectAsStateWithLifecycle(initialValue = null)
    val preferDirectWithFallback by playbackPreferencesStore.preferDirectWithFallbackFlow.collectAsStateWithLifecycle(initialValue = true)

    var currentSection by rememberSaveable { mutableStateOf(NavSection.Home) }
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedArtistId by rememberSaveable { mutableStateOf<String?>(null) }
    var albumSourceSection by rememberSaveable { mutableStateOf(NavSection.Home) }
    var artistSourceSection by rememberSaveable { mutableStateOf(NavSection.Home) }
    var preselectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var autoFocusNowPlayingTransport by rememberSaveable { mutableStateOf(false) }

    fun openSection(section: NavSection) {
        currentSection = section
        selectedAlbumId = null
        selectedArtistId = null
        if (section != NavSection.Playlists) {
            preselectedPlaylistId = null
        }
        showNowPlaying = false
        autoFocusNowPlayingTransport = false
    }

    fun openAlbum(
        albumId: String,
        source: NavSection,
    ) {
        currentSection = source
        albumSourceSection = source
        selectedAlbumId = albumId
        selectedArtistId = null
        showNowPlaying = false
        autoFocusNowPlayingTransport = false
    }

    fun openArtist(
        artistId: String,
        source: NavSection,
    ) {
        currentSection = source
        artistSourceSection = source
        selectedArtistId = artistId
        selectedAlbumId = null
        showNowPlaying = false
        autoFocusNowPlayingTransport = false
    }

    fun openNowPlaying() {
        showNowPlaying = true
        autoFocusNowPlayingTransport = false
    }

    fun playTracks(
        tracks: List<com.tuneflow.core.network.TrackSummary>,
        index: Int,
    ) {
        scope.launch {
            val queue =
                tracks.map { track ->
                    track.toQueueItem(
                        streamOptions = browseRepository.streamOptions(track.id),
                        preferDirectWithFallback = preferDirectWithFallback,
                    )
                }
            playerManager.playQueue(queue, index)
            openNowPlaying()
            autoFocusNowPlayingTransport = true
        }
    }

    ShellBackHandler(
        showNowPlaying = showNowPlaying,
        selectedAlbumId = selectedAlbumId,
        selectedArtistId = selectedArtistId,
        currentSection = currentSection,
        onCloseNowPlaying = { showNowPlaying = false },
        onCloseAlbum = {
            selectedAlbumId = null
            currentSection = albumSourceSection
        },
        onCloseArtist = {
            selectedArtistId = null
            currentSection = artistSourceSection
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
                    .padding(22.dp),
        ) {
            NavRail(
                currentSection = currentSection,
                onSectionSelected = ::openSection,
                onNowPlaying = ::openNowPlaying,
                isNowPlayingActive = showNowPlaying,
                username = session?.username.orEmpty(),
                preferDirectWithFallback = preferDirectWithFallback,
                onTogglePreferDirectWithFallback = {
                    scope.launch {
                        playbackPreferencesStore.setPreferDirectWithFallback(it)
                    }
                },
                onLogout = onLogout,
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
                        .padding(22.dp),
            ) {
                ShellContent(
                    currentSection = currentSection,
                    selectedAlbumId = selectedAlbumId,
                    selectedArtistId = selectedArtistId,
                    showNowPlaying = showNowPlaying,
                    preselectedPlaylistId = preselectedPlaylistId,
                    playbackQueue = playbackState.queue,
                    homeViewModel = homeViewModel,
                    albumsViewModel = albumsViewModel,
                    albumDetailViewModel = albumDetailViewModel,
                    artistDetailViewModel = artistDetailViewModel,
                    playlistsViewModel = playlistsViewModel,
                    searchViewModel = searchViewModel,
                    playbackViewModel = playbackViewModel,
                    autoFocusNowPlayingTransport = autoFocusNowPlayingTransport,
                    onNowPlayingAutoFocusConsumed = { autoFocusNowPlayingTransport = false },
                    onOpenAlbum = ::openAlbum,
                    onOpenArtist = ::openArtist,
                    onOpenSection = ::openSection,
                    onOpenPlaylist = {
                        currentSection = NavSection.Playlists
                        preselectedPlaylistId = it
                        selectedAlbumId = null
                        selectedArtistId = null
                        showNowPlaying = false
                        autoFocusNowPlayingTransport = false
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
    selectedArtistId: String?,
    currentSection: NavSection,
    onCloseNowPlaying: () -> Unit,
    onCloseAlbum: () -> Unit,
    onCloseArtist: () -> Unit,
    onGoHome: () -> Unit,
) {
    BackHandler(enabled = showNowPlaying || selectedAlbumId != null || selectedArtistId != null || currentSection != NavSection.Home) {
        when {
            showNowPlaying -> onCloseNowPlaying()
            selectedAlbumId != null -> onCloseAlbum()
            selectedArtistId != null -> onCloseArtist()
            currentSection != NavSection.Home -> onGoHome()
        }
    }
}

private fun shellScreenKey(
    currentSection: NavSection,
    selectedAlbumId: String?,
    selectedArtistId: String?,
    showNowPlaying: Boolean,
): String {
    return when {
        showNowPlaying -> NOW_PLAYING_SCREEN_KEY
        selectedAlbumId != null -> "album:$selectedAlbumId"
        selectedArtistId != null -> "artist:$selectedArtistId"
        else -> currentSection.name
    }
}

@Composable
private fun ShellContent(
    currentSection: NavSection,
    selectedAlbumId: String?,
    selectedArtistId: String?,
    showNowPlaying: Boolean,
    preselectedPlaylistId: String?,
    playbackQueue: PlaybackQueue,
    homeViewModel: HomeViewModel,
    albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel,
    albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel,
    artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel,
    playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel,
    searchViewModel: com.tuneflow.feature.browse.SearchViewModel,
    playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel,
    autoFocusNowPlayingTransport: Boolean,
    onNowPlayingAutoFocusConsumed: () -> Unit,
    onOpenAlbum: (String, NavSection) -> Unit,
    onOpenArtist: (String, NavSection) -> Unit,
    onOpenSection: (NavSection) -> Unit,
    onOpenPlaylist: (String?) -> Unit,
    onPreselectedPlaylistConsumed: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onPlayTracks: (List<com.tuneflow.core.network.TrackSummary>, Int) -> Unit,
) {
    val screenKey = shellScreenKey(currentSection, selectedAlbumId, selectedArtistId, showNowPlaying)

    Crossfade(targetState = screenKey, label = "shell-content") { targetScreen ->
        when {
            targetScreen == NOW_PLAYING_SCREEN_KEY -> {
                NowPlayingScreen(
                    viewModel = playbackViewModel,
                    autoFocusTransport = autoFocusNowPlayingTransport,
                    onAutoFocusConsumed = onNowPlayingAutoFocusConsumed,
                )
            }
            targetScreen.startsWith("album:") -> {
                AlbumDetailScreen(
                    albumId = targetScreen.removePrefix("album:"),
                    viewModel = albumDetailViewModel,
                    onPlayAlbum = onPlayTracks,
                )
            }
            targetScreen.startsWith("artist:") -> {
                ArtistDetailScreen(
                    artistId = targetScreen.removePrefix("artist:"),
                    viewModel = artistDetailViewModel,
                    onOpenAlbum = { onOpenAlbum(it, currentSection) },
                )
            }
            targetScreen == NavSection.Home.name -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    playbackQueue = playbackQueue,
                    onOpenAlbum = { onOpenAlbum(it, NavSection.Home) },
                    onOpenArtist = { onOpenArtist(it, NavSection.Home) },
                    onOpenAlbums = { onOpenSection(NavSection.Albums) },
                    onOpenPlaylists = onOpenPlaylist,
                    onOpenSearch = { onOpenSection(NavSection.Search) },
                    onOpenNowPlaying = onOpenNowPlaying,
                    onPlayTracks = onPlayTracks,
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
                    onOpenArtist = { onOpenArtist(it, NavSection.Search) },
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
    username: String,
    preferDirectWithFallback: Boolean,
    onTogglePreferDirectWithFallback: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    var accountExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .width(236.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(26.dp),
                )
                .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_tuneflow_brand),
                contentDescription = "TuneFlow",
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp)),
            )
            Text(
                text = "TuneFlow",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Let the music flow",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RailItem(
            label = "Home",
            selected = currentSection == NavSection.Home && !isNowPlayingActive,
            onClick = {
                accountExpanded = false
                onSectionSelected(NavSection.Home)
            },
        )
        RailItem(
            label = "Albums",
            selected = currentSection == NavSection.Albums && !isNowPlayingActive,
            onClick = {
                accountExpanded = false
                onSectionSelected(NavSection.Albums)
            },
        )
        RailItem(
            label = "Playlists",
            selected = currentSection == NavSection.Playlists && !isNowPlayingActive,
            onClick = {
                accountExpanded = false
                onSectionSelected(NavSection.Playlists)
            },
        )
        RailItem(
            label = "Search",
            selected = currentSection == NavSection.Search && !isNowPlayingActive,
            onClick = {
                accountExpanded = false
                onSectionSelected(NavSection.Search)
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        RailItem(
            label = "Now Playing",
            selected = isNowPlayingActive,
            onClick = {
                accountExpanded = false
                onNowPlaying()
            },
        )

        AccountRailSection(
            username = username.ifBlank { "Account" },
            preferDirectWithFallback = preferDirectWithFallback,
            onTogglePreferDirectWithFallback = onTogglePreferDirectWithFallback,
            expanded = accountExpanded,
            onToggleExpanded = { accountExpanded = !accountExpanded },
            onExpand = { accountExpanded = true },
            onLogout = onLogout,
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
                .fillMaxWidth()
                .scale(if (focused) 1.01f else 1f)
                .clip(RoundedCornerShape(18.dp))
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
                    shape = RoundedCornerShape(18.dp),
                )
                .onFocusChanged { focusState -> focused = focusState.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 11.dp),
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

@Composable
private fun AccountRailSection(
    username: String,
    preferDirectWithFallback: Boolean,
    onTogglePreferDirectWithFallback: (Boolean) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onExpand: () -> Unit,
    onLogout: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .scale(if (focused) 1.01f else 1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (focused || expanded) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                        },
                    )
                    .border(
                        width = if (focused || expanded) 2.dp else 1.dp,
                        color =
                            if (focused || expanded) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                            },
                        shape = RoundedCornerShape(18.dp),
                    )
                    .onFocusChanged { focusState ->
                        focused = focusState.hasFocus
                        if (focusState.hasFocus) onExpand()
                    }
                    .focusable()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (expanded) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(18.dp),
                        )
                        .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "App Info",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "TuneFlow for TV",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AccountToggleRow(
                    title = "FLAC Fallback",
                    subtitle = if (preferDirectWithFallback) "Try FLAC, fall back to MP3" else "Always use MP3 max bitrate",
                    checked = preferDirectWithFallback,
                    onToggle = onTogglePreferDirectWithFallback,
                )
                RailItem(
                    label = "Logout",
                    selected = false,
                    onClick = onLogout,
                )
            }
        }
    }
}

@Composable
private fun AccountToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(if (focused) 1.01f else 1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                        },
                    shape = RoundedCornerShape(16.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable { onToggle(!checked) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}
