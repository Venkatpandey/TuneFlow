package com.tuneflow.tv

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuneflow.core.network.PlaybackPreferencesStore
import com.tuneflow.core.network.ScreenScaleOption
import com.tuneflow.core.network.SearchHistoryStore
import com.tuneflow.core.network.SessionStore
import com.tuneflow.core.network.TrackStreamOptions
import com.tuneflow.core.player.FLAC_AUDIO_MIME_TYPE
import com.tuneflow.core.player.MPEG_AUDIO_MIME_TYPE
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.core.player.PlayerGraph
import com.tuneflow.core.player.QueueItem
import com.tuneflow.core.player.TuneFlowPlaybackService
import com.tuneflow.feature.auth.AuthRepository
import com.tuneflow.feature.auth.LoginScreen
import com.tuneflow.feature.browse.BrowseRepository
import com.tuneflow.feature.playback.LyricsRepository
import com.tuneflow.feature.video.PreferredVideoServiceConfigStore
import com.tuneflow.feature.video.PreferredVideoStore
import com.tuneflow.feature.video.RemotePreferredVideoStore
import com.tuneflow.feature.video.VIDEO_HISTORY_LIMIT
import com.tuneflow.feature.video.VideoViewModel
import com.tuneflow.feature.video.hasVisiblePlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var playerManager: com.tuneflow.core.player.TvPlayerManager
    private lateinit var playbackServiceIntent: Intent
    private var isAppExitInProgress = false
    private val userActivityEvents = MutableSharedFlow<UserInputCategory>(extraBufferCapacity = 32)
    private val wakeConsumedKeyCodes = mutableSetOf<Int>()
    private val videoConsumedKeyCodes = mutableSetOf<Int>()
    private var consumeWakeTouchGesture = false
    private var videoMediaKeyHandler: ((Int) -> Boolean)? = null

    @Volatile
    private var screensaverActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(applicationContext)
        val searchHistoryStore = SearchHistoryStore(applicationContext)
        val playbackPreferencesStore = PlaybackPreferencesStore(applicationContext)
        val preferredVideoServiceConfigStore =
            PreferredVideoServiceConfigStore(applicationContext, BuildConfig.PREFERRED_VIDEO_SERVICE_URL)
        val preferredVideoStore = RemotePreferredVideoStore(preferredVideoServiceConfigStore.serviceUrl)
        val authRepository = AuthRepository(sessionStore)
        val browseRepository = BrowseRepository(sessionStore)
        val lyricsRepository = LyricsRepository(sessionStore)
        playerManager = PlayerGraph.get(applicationContext)
        playbackServiceIntent = Intent(this, TuneFlowPlaybackService::class.java)
        startService(playbackServiceIntent)

        val videoOverlayHost =
            FrameLayout(this).apply {
                visibility = View.GONE
                clipChildren = true
                clipToPadding = true
            }

        setContent {
            TuneFlowTheme {
                val authViewModel: com.tuneflow.feature.auth.AuthViewModel =
                    viewModel(
                        factory = authViewModelFactory(authRepository, sessionStore),
                    )
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val screenScaleOption = ScreenScaleOption.Compact
                var preferredVideoServiceUrl by remember {
                    mutableStateOf(preferredVideoServiceConfigStore.serviceUrl)
                }

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
                        lyricsRepository = lyricsRepository,
                        preferredVideoStore = preferredVideoStore,
                        preferredVideoServiceUrl = preferredVideoServiceUrl,
                        onPreferredVideoServiceUrlChanged = { serviceUrl ->
                            preferredVideoServiceConfigStore.saveServiceUrl(serviceUrl)?.let { savedUrl ->
                                preferredVideoServiceUrl = savedUrl
                                preferredVideoStore.updateServiceUrl(savedUrl)
                            }
                        },
                        videoOverlayHost = videoOverlayHost,
                        userActivityEvents = userActivityEvents,
                        onScreensaverActiveChanged = { screensaverActive = it },
                        onVideoMediaKeyHandlerChanged = { videoMediaKeyHandler = it },
                        onExitApp = ::closeAppToSystem,
                    )
                }
            }
        }
        addContentView(
            videoOverlayHost,
            FrameLayout.LayoutParams(1, 1),
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        closeAppToSystem()
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return when {
            event.keyCode in videoConsumedKeyCodes -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    videoConsumedKeyCodes.remove(event.keyCode)
                }
                true
            }
            event.keyCode in wakeConsumedKeyCodes -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    wakeConsumedKeyCodes.remove(event.keyCode)
                }
                true
            }
            event.action != KeyEvent.ACTION_DOWN -> super.dispatchKeyEvent(event)
            else -> {
                userActivityEvents.tryEmit(classifyUserInput(event.keyCode))
                when (resolveScreensaverKeyAction(screensaverActive, event.keyCode)) {
                    ScreensaverKeyAction.RecordAndDispatch -> dispatchVideoMediaKeyOrSystem(event)
                    ScreensaverKeyAction.WakeAndConsume -> {
                        screensaverActive = false
                        wakeConsumedKeyCodes += event.keyCode
                        true
                    }
                    ScreensaverKeyAction.WakeAndDispatchMedia -> {
                        screensaverActive = false
                        if (videoMediaKeyHandler?.invoke(event.keyCode) == true) {
                            videoConsumedKeyCodes += event.keyCode
                            true
                        } else if (handleScreensaverMediaKey(event.keyCode)) {
                            wakeConsumedKeyCodes += event.keyCode
                            true
                        } else {
                            super.dispatchKeyEvent(event)
                        }
                    }
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean =
        when {
            consumeWakeTouchGesture -> {
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    consumeWakeTouchGesture = false
                }
                true
            }
            event.actionMasked != MotionEvent.ACTION_DOWN -> super.dispatchTouchEvent(event)
            else -> {
                userActivityEvents.tryEmit(UserInputCategory.Touch)
                if (screensaverActive) {
                    screensaverActive = false
                    consumeWakeTouchGesture = true
                    true
                } else {
                    super.dispatchTouchEvent(event)
                }
            }
        }

    private fun handleScreensaverMediaKey(keyCode: Int): Boolean =
        when (resolveMediaPlaybackAction(keyCode)) {
            MediaPlaybackAction.Toggle -> {
                if (playerManager.isPlaying.value) playerManager.pause() else playerManager.play()
                true
            }
            MediaPlaybackAction.Play -> {
                playerManager.play()
                true
            }
            MediaPlaybackAction.Pause -> {
                playerManager.pause()
                true
            }
            MediaPlaybackAction.Next -> {
                playerManager.next()
                true
            }
            MediaPlaybackAction.Previous -> {
                playerManager.previous()
                true
            }
            MediaPlaybackAction.Stop -> {
                playerManager.stopAndClear()
                true
            }
            MediaPlaybackAction.DispatchToSystem -> false
        }

    @SuppressLint("RestrictedApi")
    private fun dispatchVideoMediaKeyOrSystem(event: KeyEvent): Boolean =
        if (videoMediaKeyHandler?.invoke(event.keyCode) == true) {
            videoConsumedKeyCodes += event.keyCode
            true
        } else {
            super.dispatchKeyEvent(event)
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

internal fun com.tuneflow.core.network.TrackSummary.toQueueItem(
    streamOptions: TrackStreamOptions,
    preferDirectWithFallback: Boolean,
): QueueItem {
    val directMimeType = directStreamMimeType()
    val directFormatLabel = directStreamFormatLabel(directMimeType)
    return QueueItem(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artUrl = artUrl,
        streamUrl = if (preferDirectWithFallback) streamOptions.directUrl else streamOptions.fallbackMp3Url,
        fallbackStreamUrl = if (preferDirectWithFallback) streamOptions.fallbackMp3Url else null,
        streamFormatLabel = if (preferDirectWithFallback) directFormatLabel else "MP3",
        streamBitrateLabel = if (preferDirectWithFallback) "Original" else "Max",
        durationMs = durationSec * 1000L,
        streamMimeType = if (preferDirectWithFallback) directMimeType else MPEG_AUDIO_MIME_TYPE,
        directStreamMimeType = directMimeType,
        directStreamFormatLabel = directFormatLabel,
    )
}

private fun com.tuneflow.core.network.TrackSummary.directStreamMimeType(): String? {
    val normalizedContentType = contentType?.trim()?.lowercase(Locale.ROOT)
    return when (normalizedContentType) {
        "audio/flac", "audio/x-flac" -> FLAC_AUDIO_MIME_TYPE
        "audio/mp3", "audio/mpeg" -> MPEG_AUDIO_MIME_TYPE
        else -> normalizedContentType?.takeIf { it.startsWith("audio/") }
    } ?: when (suffix?.trim()?.trimStart('.')?.lowercase(Locale.ROOT)) {
        "flac" -> FLAC_AUDIO_MIME_TYPE
        "mp3" -> MPEG_AUDIO_MIME_TYPE
        else -> null
    }
}

private fun com.tuneflow.core.network.TrackSummary.directStreamFormatLabel(mimeType: String?): String =
    suffix
        ?.trim()
        ?.trimStart('.')
        ?.takeIf { it.isNotBlank() }
        ?.uppercase(Locale.ROOT)
        ?: when (mimeType) {
            FLAC_AUDIO_MIME_TYPE -> "FLAC"
            MPEG_AUDIO_MIME_TYPE -> "MP3"
            else -> "FLAC"
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
                streamFormatLabel = if (nextPreferDirectWithFallback) item.directStreamFormatLabel else "MP3",
                streamBitrateLabel = if (nextPreferDirectWithFallback) "Original" else "Max",
                streamMimeType = if (nextPreferDirectWithFallback) item.directStreamMimeType else MPEG_AUDIO_MIME_TYPE,
            )
        }

    playerManager.playQueue(
        items = updatedItems,
        startIndex = queue.currentIndex,
        sourcePlaylistName = queue.sourcePlaylistName,
    )
    playerManager.seekTo(positionMs)
    if (!wasPlaying) {
        playerManager.pause()
    }
}

@Composable
private fun rememberPlaybackScreensaverState(
    playbackState: com.tuneflow.feature.playback.NowPlayingUiState,
    videoVisible: Boolean,
    userActivityEvents: Flow<UserInputCategory>,
    onActiveChanged: (Boolean) -> Unit,
): PlaybackScreensaverState {
    val controller = remember { PlaybackScreensaverController() }
    val state by controller.state.collectAsStateWithLifecycle()
    val playbackEligible =
        !videoVisible &&
            playbackState.queue.currentItem != null &&
            (
                playbackState.isPlaying ||
                    (state.active && playbackState.playbackStatus.expectedToPlay)
            )

    SideEffect {
        onActiveChanged(state.active)
    }

    DisposableEffect(Unit) {
        onDispose { onActiveChanged(false) }
    }

    LaunchedEffect(userActivityEvents) {
        userActivityEvents.collect { category -> controller.onUserActivity(category) }
    }

    LaunchedEffect(playbackEligible) {
        controller.onPlaybackEligibilityChanged(playbackEligible)
    }

    LaunchedEffect(state.playbackEligible, state.active, state.lastUserActivityMs) {
        if (state.playbackEligible && !state.active) {
            delay(controller.remainingDelayMs())
            controller.onDeadlineReached()
        }
    }

    return state
}

@Composable
private fun ObserveVideoLifecycle(videoViewModel: VideoViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, videoViewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) videoViewModel.onAppBackgrounded()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun TuneFlowShell(
    browseRepository: BrowseRepository,
    playerManager: com.tuneflow.core.player.TvPlayerManager,
    sessionStore: SessionStore,
    playbackPreferencesStore: PlaybackPreferencesStore,
    searchHistoryStore: SearchHistoryStore,
    lyricsRepository: LyricsRepository,
    preferredVideoStore: PreferredVideoStore,
    preferredVideoServiceUrl: String,
    onPreferredVideoServiceUrlChanged: (String) -> Unit,
    videoOverlayHost: FrameLayout,
    userActivityEvents: Flow<UserInputCategory>,
    onScreensaverActiveChanged: (Boolean) -> Unit,
    onVideoMediaKeyHandlerChanged: (((Int) -> Boolean)?) -> Unit,
    onExitApp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory(browseRepository, preferredVideoStore))
    val albumsViewModel: com.tuneflow.feature.browse.AlbumsViewModel = viewModel(factory = albumsViewModelFactory(browseRepository))
    val homeCategoryViewModel: com.tuneflow.feature.browse.HomeCategoryViewModel =
        viewModel(factory = homeCategoryViewModelFactory(browseRepository))
    val albumDetailViewModel: com.tuneflow.feature.browse.AlbumDetailViewModel =
        viewModel(factory = albumDetailViewModelFactory(browseRepository))
    val artistDetailViewModel: com.tuneflow.feature.browse.ArtistDetailViewModel =
        viewModel(factory = artistDetailViewModelFactory(browseRepository))
    val playlistsViewModel: com.tuneflow.feature.browse.PlaylistsViewModel =
        viewModel(factory = playlistsViewModelFactory(browseRepository))
    val searchViewModel: com.tuneflow.feature.browse.SearchViewModel =
        viewModel(factory = searchViewModelFactory(browseRepository, searchHistoryStore))
    val playbackViewModel: com.tuneflow.feature.playback.PlaybackViewModel =
        viewModel(factory = playbackViewModelFactory(playerManager, lyricsRepository))
    val videoViewModel: VideoViewModel =
        viewModel(
            factory =
                videoViewModelFactory(
                    androidx.compose.ui.platform.LocalContext.current,
                    playerManager,
                    preferredVideoStore,
                ),
        )
    val playbackState by playbackViewModel.uiState.collectAsStateWithLifecycle()
    val lyricsState by playbackViewModel.lyricsState.collectAsStateWithLifecycle()
    val videoState by videoViewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(videoViewModel, playbackViewModel, onVideoMediaKeyHandlerChanged) {
        onVideoMediaKeyHandlerChanged { keyCode ->
            handleVideoModeMediaKey(keyCode, videoViewModel, playbackViewModel)
        }
        onDispose { onVideoMediaKeyHandlerChanged(null) }
    }
    val screensaverState =
        rememberPlaybackScreensaverState(
            playbackState = playbackState,
            videoVisible = videoState.hasVisiblePlayer,
            userActivityEvents = userActivityEvents,
            onActiveChanged = onScreensaverActiveChanged,
        )
    ObserveVideoLifecycle(videoViewModel)
    val session by sessionStore.sessionFlow.collectAsStateWithLifecycle(initialValue = null)
    val preferDirectWithFallback by playbackPreferencesStore.preferDirectWithFallbackFlow.collectAsStateWithLifecycle(initialValue = false)
    var playbackPositionMs by remember { mutableLongStateOf(0L) }
    var navClockText by remember { mutableStateOf(currentTime24h()) }

    var shellState by rememberSaveable(stateSaver = TuneFlowShellState.Saver) {
        mutableStateOf(TuneFlowShellState())
    }

    LaunchedEffect(shellState.showNowPlaying, videoViewModel) {
        videoViewModel.setNowPlayingVisible(shellState.showNowPlaying)
    }
    DisposableEffect(videoViewModel) {
        onDispose { videoViewModel.setNowPlayingVisible(false) }
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

    LaunchedEffect(videoViewModel, navigationActions) {
        videoViewModel.returnToNowPlayingEvents.collect {
            navigationActions.openNowPlayingWithTransportFocus()
        }
    }

    fun playTracks(
        tracks: List<com.tuneflow.core.network.TrackSummary>,
        index: Int,
        sourcePlaylistName: String? = null,
    ) {
        scope.launch {
            val queue = buildQueueItems(tracks, browseRepository, preferDirectWithFallback)
            playerManager.playQueue(queue, index, sourcePlaylistName)
        }
    }

    fun shuffleTracks(
        tracks: List<com.tuneflow.core.network.TrackSummary>,
        sourcePlaylistName: String? = null,
    ) = playTracks(tracks.shuffled(), index = 0, sourcePlaylistName)

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

    LaunchedEffect(shellState.showExitPrompt, shellState.lastExitPromptAt) {
        if (!shellState.showExitPrompt) return@LaunchedEffect
        delay(EXIT_CONFIRM_TIMEOUT_MS)
        if (System.currentTimeMillis() - shellState.lastExitPromptAt >= EXIT_CONFIRM_TIMEOUT_MS) {
            updateShellState { it.hideExitPrompt() }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            navClockText = currentTime24h()
            val nowMs = System.currentTimeMillis()
            val delayMs = 60_000L - (nowMs % 60_000L)
            delay(delayMs.coerceAtLeast(250L))
        }
    }

    LaunchedEffect(playbackState.queue.currentItem?.id, playbackState.isPlaying) {
        playbackPositionMs = playerManager.currentPositionMs()
        while (playbackState.queue.currentItem != null) {
            playbackPositionMs = playerManager.currentPositionMs()
            delay(500L)
        }
    }

    ShellBackHandler(
        state = shellState,
        onPopDestination = navigationActions::popDestination,
        onGoHome = navigationActions::goHome,
        onRequestExit = ::requestAppExit,
    )

    TuneFlowShellLayout(
        currentSection = shellState.currentSection,
        currentDestination = shellState.currentDestination,
        showNowPlaying = shellState.showNowPlaying,
        username = session?.username.orEmpty(),
        currentTimeText = navClockText,
        playbackQueue = playbackState.queue,
        playbackPositionMs = playbackPositionMs,
        screensaverActive = screensaverState.active,
        lyricsState = lyricsState,
        homeViewModel = homeViewModel,
        albumsViewModel = albumsViewModel,
        homeCategoryViewModel = homeCategoryViewModel,
        albumDetailViewModel = albumDetailViewModel,
        artistDetailViewModel = artistDetailViewModel,
        playlistsViewModel = playlistsViewModel,
        searchViewModel = searchViewModel,
        playbackViewModel = playbackViewModel,
        videoViewModel = videoViewModel,
        videoState = videoState,
        videoOverlayHost = videoOverlayHost,
        preselectedPlaylistId = shellState.preselectedPlaylistId,
        focusRestoreTarget = shellState.pendingFocusRestore,
        streamModeLabel = if (preferDirectWithFallback) "FLAC" else "MP3",
        autoFocusNowPlayingTransport = shellState.autoFocusNowPlayingTransport,
        onSectionSelected = navigationActions::openSection,
        onNowPlaying = navigationActions::openNowPlaying,
        onCycleStreamMode = ::cycleStreamMode,
        onNowPlayingAutoFocusConsumed = { updateShellState { it.consumeNowPlayingTransportFocus() } },
        onFocusRestoreConsumed = { updateShellState { it.consumeFocusRestore() } },
        onOpenAlbum = navigationActions::openAlbum,
        onOpenArtist = navigationActions::openArtist,
        onOpenSection = navigationActions::openSection,
        onOpenHomeCategory = navigationActions::openHomeCategory,
        onOpenPlaylist = navigationActions::openPlaylist,
        onPreselectedPlaylistConsumed = { updateShellState { it.consumePreselectedPlaylist() } },
        onOpenNowPlaying = navigationActions::openNowPlaying,
        onOpenVideoHistory = navigationActions::openVideoHistory,
        onPlayVideo = { entry ->
            videoViewModel.playHistory(entry)
            navigationActions.openNowPlaying()
        },
        onPlayTracks = { tracks, index -> playTracks(tracks, index) },
        onShuffleTracks = { tracks -> shuffleTracks(tracks) },
        onPlayPlaylistTracks = { playlistName, tracks, index ->
            playTracks(tracks, index, playlistName)
        },
        onShufflePlaylistTracks = { playlistName, tracks ->
            shuffleTracks(tracks, playlistName)
        },
        preferredVideoServiceUrl = preferredVideoServiceUrl,
        onPreferredVideoServiceUrlChanged = { serviceUrl ->
            onPreferredVideoServiceUrlChanged(serviceUrl)
            scope.launch { preferredVideoStore.refreshHistory(VIDEO_HISTORY_LIMIT) }
        },
        showExitPrompt = shellState.showExitPrompt,
    )
}

private val shellClockFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun currentTime24h(): String = shellClockFormatter.format(Date())
