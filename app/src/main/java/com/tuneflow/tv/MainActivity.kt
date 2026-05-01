package com.tuneflow.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuneflow.core.network.PlaybackPreferencesStore
import com.tuneflow.core.network.ScreenScaleOption
import com.tuneflow.core.network.SearchHistoryStore
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.network.TrackStreamOptions
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlayerGraph
import com.tuneflow.core.player.QueueItem
import com.tuneflow.core.player.TuneFlowPlaybackService
import com.tuneflow.feature.auth.AuthRepository
import com.tuneflow.feature.auth.LoginScreen
import com.tuneflow.feature.browse.BrowseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: com.tuneflow.core.player.TvPlayerManager
    private lateinit var playbackServiceIntent: Intent
    private var isAppExitInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(applicationContext)
        val searchHistoryStore = SearchHistoryStore(applicationContext)
        val playbackPreferencesStore = PlaybackPreferencesStore(applicationContext)
        val authRepository = AuthRepository(sessionStore)
        val browseRepository = BrowseRepository(sessionStore)
        playerManager = PlayerGraph.get(applicationContext)
        playbackServiceIntent = Intent(this, TuneFlowPlaybackService::class.java)
        startService(playbackServiceIntent)

        setContent {
            TuneFlowTheme {
                val authViewModel: com.tuneflow.feature.auth.AuthViewModel =
                    viewModel(
                        factory = authViewModelFactory(authRepository, sessionStore),
                    )
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val screenScaleOption = ScreenScaleOption.Compact

                if (!authState.isLoggedIn) {
                    LoginScreen(
                        viewModel = authViewModel,
                        logoResId = R.drawable.ic_tuneflow_brand,
                        backgroundResId = R.drawable.login_background,
                        screenScaleFactor = screenScaleOption.factor,
                    )
                } else {
                    TuneFlowShell(
                        browseRepository = browseRepository,
                        playerManager = playerManager,
                        sessionStore = sessionStore,
                        playbackPreferencesStore = playbackPreferencesStore,
                        searchHistoryStore = searchHistoryStore,
                        onExitApp = ::closeAppToSystem,
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        closeAppToSystem()
    }

    private fun closeAppToSystem() {
        if (isAppExitInProgress) return
        isAppExitInProgress = true
        playerManager.stopAndClear()
        stopService(playbackServiceIntent)
        PlayerGraph.release()
        finishAffinity()
        finishAndRemoveTask()
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

private suspend fun buildQueueItems(
    tracks: List<com.tuneflow.core.network.TrackSummary>,
    browseRepository: BrowseRepository,
    preferDirectWithFallback: Boolean,
): List<QueueItem> {
    return tracks.map { track ->
        track.toQueueItem(
            streamOptions = browseRepository.streamOptions(track.id),
            preferDirectWithFallback = preferDirectWithFallback,
        )
    }
}

private suspend fun cyclePlaybackStreamMode(
    queue: PlaybackQueue,
    browseRepository: BrowseRepository,
    playerManager: com.tuneflow.core.player.TvPlayerManager,
    playbackPreferencesStore: PlaybackPreferencesStore,
    currentPreferDirectWithFallback: Boolean,
    wasPlaying: Boolean,
    positionMs: Long,
) {
    val nextPreferDirectWithFallback = !currentPreferDirectWithFallback
    playbackPreferencesStore.setPreferDirectWithFallback(nextPreferDirectWithFallback)
    if (queue.items.isEmpty()) return

    val updatedItems =
        queue.items.map { item ->
            val streamOptions = browseRepository.streamOptions(item.id)
            item.copy(
                streamUrl = if (nextPreferDirectWithFallback) streamOptions.directUrl else streamOptions.fallbackMp3Url,
                fallbackStreamUrl = if (nextPreferDirectWithFallback) streamOptions.fallbackMp3Url else null,
                streamFormatLabel = if (nextPreferDirectWithFallback) "FLAC" else "MP3",
                streamBitrateLabel = if (nextPreferDirectWithFallback) "Original" else "Max",
            )
        }

    playerManager.playQueue(updatedItems, queue.currentIndex)
    playerManager.seekTo(positionMs)
    if (!wasPlaying) {
        playerManager.pause()
    }
}

@Composable
private fun TuneFlowShell(
    browseRepository: BrowseRepository,
    playerManager: com.tuneflow.core.player.TvPlayerManager,
    sessionStore: SessionStore,
    playbackPreferencesStore: PlaybackPreferencesStore,
    searchHistoryStore: SearchHistoryStore,
    onExitApp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory(browseRepository))
    val albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel = viewModel(factory = albumsViewModelFactory(browseRepository))
    val albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel =
        viewModel(factory = albumDetailViewModelFactory(browseRepository))
    val artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel =
        viewModel(factory = artistDetailViewModelFactory(browseRepository))
    val playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel =
        viewModel(factory = playlistsViewModelFactory(browseRepository))
    val searchViewModel: com.tuneflow.feature.browse.SearchViewModel =
        viewModel(factory = searchViewModelFactory(browseRepository, searchHistoryStore))
    val playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel = viewModel(factory = playbackViewModelFactory(playerManager))
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val session by sessionStore.sessionFlow.collectAsStateWithLifecycle(initialValue = null)
    val preferDirectWithFallback by playbackPreferencesStore.preferDirectWithFallbackFlow.collectAsStateWithLifecycle(initialValue = false)

    var shellState by rememberSaveable(stateSaver = TuneFlowShellState.Saver) {
        mutableStateOf(TuneFlowShellState())
    }

    fun updateShellState(transform: (TuneFlowShellState) -> TuneFlowShellState) {
        shellState = transform(shellState)
    }

    val navigationActions =
        remember(playlistsViewModel) {
            TuneFlowNavigationActions(
                clearPlaylistSelection = playlistsViewModel::clearSelection,
                updateShellState = { transform -> updateShellState(transform) },
            )
        }

    fun playTracks(
        tracks: List<com.tuneflow.core.network.TrackSummary>,
        index: Int,
    ) {
        scope.launch {
            val queue = buildQueueItems(tracks, browseRepository, preferDirectWithFallback)
            playerManager.playQueue(queue, index)
            navigationActions.openNowPlaying()
            updateShellState { it.enableNowPlayingTransportFocus() }
        }
    }

    fun shuffleTracks(tracks: List<com.tuneflow.core.network.TrackSummary>) {
        scope.launch {
            if (tracks.isEmpty()) return@launch
            val shuffledTracks = tracks.shuffled()
            val queue = buildQueueItems(shuffledTracks, browseRepository, preferDirectWithFallback)
            playerManager.playQueue(queue, 0)
            navigationActions.openNowPlaying()
            updateShellState { it.enableNowPlayingTransportFocus() }
        }
    }

    fun cycleStreamMode() {
        scope.launch {
            cyclePlaybackStreamMode(
                queue = playbackState.queue,
                browseRepository = browseRepository,
                playerManager = playerManager,
                playbackPreferencesStore = playbackPreferencesStore,
                currentPreferDirectWithFallback = preferDirectWithFallback,
                wasPlaying = playbackState.isPlaying,
                positionMs = playbackState.positionMs,
            )
        }
    }

    fun requestAppExit() {
        val now = System.currentTimeMillis()
        val confirmed = shellState.showExitPrompt && now - shellState.lastExitPromptAt <= EXIT_CONFIRM_TIMEOUT_MS
        if (confirmed) {
            onExitApp()
            return
        }
        updateShellState { it.showExitPrompt(now) }
    }

    androidx.compose.runtime.LaunchedEffect(shellState.showExitPrompt, shellState.lastExitPromptAt) {
        if (!shellState.showExitPrompt) return@LaunchedEffect
        delay(EXIT_CONFIRM_TIMEOUT_MS)
        if (System.currentTimeMillis() - shellState.lastExitPromptAt >= EXIT_CONFIRM_TIMEOUT_MS) {
            updateShellState { it.hideExitPrompt() }
        }
    }

    ShellBackHandler(
        showNowPlaying = shellState.showNowPlaying,
        selectedAlbumId = shellState.selectedAlbumId,
        selectedArtistId = shellState.selectedArtistId,
        currentSection = shellState.currentSection,
        onCloseNowPlaying = navigationActions::closeNowPlaying,
        onCloseAlbum = navigationActions::closeAlbum,
        onCloseArtist = navigationActions::closeArtist,
        onGoHome = navigationActions::goHome,
        onRequestExit = ::requestAppExit,
    )

    TuneFlowShellLayout(
        currentSection = shellState.currentSection,
        showNowPlaying = shellState.showNowPlaying,
        username = session?.username.orEmpty(),
        playbackQueue = playbackState.queue,
        homeViewModel = homeViewModel,
        albumsViewModel = albumsViewModel,
        albumDetailViewModel = albumDetailViewModel,
        artistDetailViewModel = artistDetailViewModel,
        playlistsViewModel = playlistsViewModel,
        searchViewModel = searchViewModel,
        playbackViewModel = playbackViewModel,
        selectedAlbumId = shellState.selectedAlbumId,
        selectedArtistId = shellState.selectedArtistId,
        preselectedPlaylistId = shellState.preselectedPlaylistId,
        streamModeLabel = if (preferDirectWithFallback) "FLAC" else "MP3",
        autoFocusNowPlayingTransport = shellState.autoFocusNowPlayingTransport,
        onSectionSelected = navigationActions::openSection,
        onNowPlaying = navigationActions::openNowPlaying,
        onExitApp = onExitApp,
        onCycleStreamMode = ::cycleStreamMode,
        onNowPlayingAutoFocusConsumed = { updateShellState { it.consumeNowPlayingTransportFocus() } },
        onOpenAlbum = navigationActions::openAlbum,
        onOpenArtist = navigationActions::openArtist,
        onOpenSection = navigationActions::openSection,
        onOpenPlaylist = navigationActions::openPlaylist,
        onPreselectedPlaylistConsumed = { updateShellState { it.consumePreselectedPlaylist() } },
        onOpenNowPlaying = navigationActions::openNowPlaying,
        onPlayTracks = ::playTracks,
        onShuffleTracks = ::shuffleTracks,
        showExitPrompt = shellState.showExitPrompt,
    )
}
