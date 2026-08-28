@file:Suppress("TooManyFunctions")

package com.tuneflow.feature.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.TrackSummary

@Composable
@Suppress("CyclomaticComplexMethod")
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    focusRestoreTarget: BrowseFocusTarget? = null,
    onFocusRestoreConsumed: () -> Unit = {},
    onAlbumSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val firstAlbumFocusRequester = remember { FocusRequester() }
    val restoredAlbumFocusRequester = remember { FocusRequester() }
    var initialAlbumFocusRequested by rememberSaveable { mutableStateOf(false) }
    val albumGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val restoredAlbumId = focusRestoreTarget?.takeIf { it.kind == BrowseFocusTargetKind.Album }?.id

    LaunchedEffect(state.items.size) {
        if (
            restoredAlbumId == null &&
            !initialAlbumFocusRequested &&
            state.items.isNotEmpty() &&
            albumGridState.firstVisibleItemIndex == 0
        ) {
            firstAlbumFocusRequester.requestFocus()
            initialAlbumFocusRequested = true
        }
    }

    LaunchedEffect(restoredAlbumId, state.items) {
        val targetIndex = state.items.indexOfFirst { it.id == restoredAlbumId }
        if (targetIndex >= 0) {
            albumGridState.scrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { restoredAlbumFocusRequester.requestFocus() }
            initialAlbumFocusRequested = true
            onFocusRestoreConsumed()
        }
    }

    when {
        state.isLoading -> {
            Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (restoredAlbumId == null) ScreenInitialFocusAnchor()
                SectionTitle(title = "Albums")
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 196.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(10) {
                        AlbumCardSkeleton()
                    }
                }
            }
        }

        state.error != null && state.items.isEmpty() -> {
            ErrorState(modifier = modifier, message = state.error.orEmpty())
        }

        else -> {
            Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (restoredAlbumId == null) ScreenInitialFocusAnchor()
                SectionTitle(title = "Albums")
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 196.dp),
                    state = albumGridState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    itemsIndexed(state.items, key = { _, album -> album.id }) { index, album ->
                        PremiumAlbumCard(
                            album = album,
                            onClick = { onAlbumSelected(album.id) },
                            modifier =
                                Modifier.then(
                                    if (album.id == restoredAlbumId) {
                                        Modifier.focusRequester(restoredAlbumFocusRequester)
                                    } else if (index == 0) {
                                        Modifier.focusRequester(firstAlbumFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }

                    if (state.hasMore) {
                        item {
                            LaunchedEffect(state.items.size) {
                                viewModel.loadMore()
                            }
                            AlbumGridLoadingMoreCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumDetailScreen(
    albumId: String,
    viewModel: AlbumDetailViewModel,
    onPlayAlbum: (tracks: List<TrackSummary>, index: Int) -> Unit,
    onShuffleAlbum: (tracks: List<TrackSummary>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playAlbumFocusRequester = remember { FocusRequester() }
    var initialAlbumFocusRequested by rememberSaveable(albumId) { mutableStateOf(false) }

    LaunchedEffect(albumId) {
        viewModel.load(albumId)
    }

    LaunchedEffect(state.album?.id) {
        if (!initialAlbumFocusRequested && state.album != null) {
            playAlbumFocusRequester.requestFocus()
            initialAlbumFocusRequested = true
        }
    }

    when {
        state.isLoading -> AlbumDetailSkeleton(modifier = modifier)
        state.error != null -> ErrorState(modifier = modifier, message = state.error.orEmpty())
        state.album == null -> ErrorState(modifier = modifier, message = "No album data")
        else -> {
            val album = state.album!!
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(292.dp)
                            .fillMaxSize()
                            .clip(TuneFlowShapes.container)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                            .padding(16.dp),
                ) {
                    TuneFlowArtwork(
                        model = album.artUrl,
                        contentDescription = album.title,
                        width = 260.dp,
                        height = 248.dp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(248.dp)
                                .clip(TuneFlowShapes.artwork)
                                .align(Alignment.TopCenter),
                        contentScale = ContentScale.Crop,
                        placeholderText = album.title,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ScreenInitialFocusAnchor()
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BrowseActionButton(
                            onClick = { onPlayAlbum(album.tracks, 0) },
                            modifier = Modifier.focusRequester(playAlbumFocusRequester),
                        ) {
                            BrowsePlayIcon()
                        }
                        BrowseActionButton(onClick = { onShuffleAlbum(album.tracks) }) {
                            BrowseShuffleIcon()
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        itemsIndexed(album.tracks, key = { _, track -> track.id }) { index, track ->
                            PremiumListRow(
                                title = track.title,
                                subtitle = track.artist,
                                trailing = formatTrackDuration(track.durationSec),
                                onClick = { onPlayAlbum(album.tracks, index) },
                                modifier =
                                    Modifier.boundaryLockedVerticalItem(
                                        index = index,
                                        lastIndex = album.tracks.lastIndex,
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    artistId: String,
    viewModel: ArtistDetailViewModel,
    focusRestoreTarget: BrowseFocusTarget? = null,
    onFocusRestoreConsumed: () -> Unit = {},
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstArtistAlbumFocusRequester = remember { FocusRequester() }
    val restoredArtistAlbumFocusRequester = remember { FocusRequester() }
    val artistAlbumListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var initialArtistFocusRequested by rememberSaveable(artistId) { mutableStateOf(false) }
    val restoredAlbumId = focusRestoreTarget?.takeIf { it.kind == BrowseFocusTargetKind.Album }?.id

    LaunchedEffect(artistId) {
        viewModel.load(artistId)
    }

    LaunchedEffect(state.artist?.albums?.size) {
        if (restoredAlbumId == null && !initialArtistFocusRequested && state.artist?.albums?.isNotEmpty() == true) {
            firstArtistAlbumFocusRequester.requestFocus()
            initialArtistFocusRequested = true
        }
    }

    LaunchedEffect(restoredAlbumId, state.artist?.albums) {
        val targetIndex = state.artist?.albums?.indexOfFirst { it.id == restoredAlbumId } ?: -1
        if (targetIndex >= 0) {
            artistAlbumListState.scrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { restoredArtistAlbumFocusRequester.requestFocus() }
            initialArtistFocusRequested = true
            onFocusRestoreConsumed()
        }
    }

    when {
        state.isLoading -> ArtistDetailSkeleton(modifier = modifier)
        state.error != null -> ErrorState(modifier = modifier, message = state.error.orEmpty())
        state.artist == null -> ErrorState(modifier = modifier, message = "No artist data")
        else -> {
            val artist = state.artist!!
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (restoredAlbumId == null) ScreenInitialFocusAnchor()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(208.dp)
                            .clip(TuneFlowShapes.container)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)),
                ) {
                    TuneFlowArtwork(
                        model = artist.artUrl,
                        contentDescription = artist.name,
                        width = 1280.dp,
                        height = 208.dp,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.34f,
                        placeholderText = artist.name,
                    )
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${artist.albumCount} albums",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SectionTitle(
                    title = "Albums",
                )
                LazyRow(
                    state = artistAlbumListState,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(artist.albums, key = { _, album -> album.id }) { index, album ->
                        PremiumAlbumCard(
                            album = album,
                            onClick = { onOpenAlbum(album.id) },
                            modifier =
                                if (album.id == restoredAlbumId) {
                                    Modifier.focusRequester(restoredArtistAlbumFocusRequester)
                                } else if (index == 0) {
                                    Modifier.focusRequester(firstArtistAlbumFocusRequester)
                                } else {
                                    Modifier
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    preselectedPlaylistId: String? = null,
    onPreselectedPlaylistConsumed: () -> Unit = {},
    currentTrackId: String? = null,
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
    onShuffleTracks: (tracks: List<TrackSummary>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstPlaylistFocusRequester = remember { FocusRequester() }
    val playPlaylistFocusRequester = remember { FocusRequester() }
    val playlistReturnFocusRequester = remember { FocusRequester() }
    val playlistListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var initialPlaylistFocusRequested by rememberSaveable { mutableStateOf(false) }
    var detailActionFocusRequested by rememberSaveable(state.selected?.id) { mutableStateOf(false) }
    var returnFocusPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var restorePlaylistFocus by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(preselectedPlaylistId) {
        if (preselectedPlaylistId != null) {
            returnFocusPlaylistId = preselectedPlaylistId
            viewModel.loadPlaylistDetail(preselectedPlaylistId)
            onPreselectedPlaylistConsumed()
        }
    }

    LaunchedEffect(state.playlists.size) {
        if (!initialPlaylistFocusRequested && state.playlists.isNotEmpty() && preselectedPlaylistId == null) {
            firstPlaylistFocusRequester.requestFocus()
            initialPlaylistFocusRequested = true
        }
    }

    LaunchedEffect(state.playlists, state.selected?.id, preselectedPlaylistId) {
        val targetPlaylistId = state.selected?.id ?: preselectedPlaylistId ?: return@LaunchedEffect
        val targetIndex = state.playlists.indexOfFirst { it.id == targetPlaylistId }
        if (targetIndex >= 0) {
            playlistListState.scrollToItem(targetIndex)
            initialPlaylistFocusRequested = true
        }
    }

    val hasPlaylistDetailLayer = state.selectedPlaylistId != null
    val showDetail = state.selected != null

    fun closePlaylistDetail() {
        restorePlaylistFocus = true
        viewModel.clearSelection()
    }

    BackHandler(enabled = hasPlaylistDetailLayer, onBack = ::closePlaylistDetail)

    LaunchedEffect(hasPlaylistDetailLayer, restorePlaylistFocus, state.playlists) {
        if (!hasPlaylistDetailLayer && restorePlaylistFocus) {
            val targetIndex = state.playlists.indexOfFirst { it.id == returnFocusPlaylistId }
            if (targetIndex >= 0) {
                playlistListState.scrollToItem(targetIndex)
                runCatching { playlistReturnFocusRequester.requestFocus() }
            }
            restorePlaylistFocus = false
        }
    }

    LaunchedEffect(state.selected?.id) {
        if (state.selected != null) {
            detailActionFocusRequested = false
        }
    }

    LaunchedEffect(showDetail, state.selected?.tracks?.size) {
        if (showDetail && state.selected?.tracks?.isNotEmpty() == true && !detailActionFocusRequested) {
            playPlaylistFocusRequester.requestFocus()
            detailActionFocusRequested = true
        }
    }

    val listWidth by animateDpAsState(
        targetValue = if (showDetail) 272.dp else 312.dp,
        label = "playlist-list-width",
    )

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .width(listWidth)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenInitialFocusAnchor()
            SectionTitle(title = "Playlists")

            if (state.isLoading && state.playlists.isEmpty()) {
                PlaylistListSkeleton()
            } else {
                LazyColumn(
                    state = playlistListState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    itemsIndexed(state.playlists, key = { _, playlist -> playlist.id }) { index, playlist ->
                        PremiumPlaylistRow(
                            playlist = playlist,
                            onClick = {
                                returnFocusPlaylistId = playlist.id
                                viewModel.loadPlaylistDetail(playlist.id)
                            },
                            modifier =
                                Modifier
                                    .boundaryLockedVerticalItem(
                                        index = index,
                                        lastIndex = state.playlists.lastIndex,
                                    )
                                    .then(
                                        if (playlist.id == returnFocusPlaylistId) {
                                            Modifier.focusRequester(playlistReturnFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(firstPlaylistFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    ),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showDetail,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 }),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(TuneFlowShapes.panel)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val selected = state.selected ?: return@AnimatedVisibility
                Text(
                    text = selected.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${selected.tracks.size} tracks • ${formatTotalDuration(selected.tracks.sumOf { it.durationSec })}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrowseActionButton(
                        onClick = { onPlayTracks(selected.tracks, 0) },
                        modifier = Modifier.focusRequester(playPlaylistFocusRequester),
                    ) {
                        BrowsePlayIcon()
                    }
                    BrowseActionButton(onClick = { onShuffleTracks(selected.tracks) }) {
                        BrowseShuffleIcon()
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    itemsIndexed(selected.tracks, key = { _, track -> track.id }) { index, track ->
                        PremiumListRow(
                            title = track.title,
                            subtitle = track.artist,
                            trailing = formatTrackDuration(track.durationSec),
                            leadingContent = {
                                if (track.id == currentTrackId) {
                                    CurrentlyPlayingIndicator()
                                }
                            },
                            onClick = {
                                onPlayTracks(
                                    selected.tracks,
                                    index,
                                )
                            },
                            modifier =
                                Modifier.boundaryLockedVerticalItem(
                                    index = index,
                                    lastIndex = selected.tracks.lastIndex,
                                ),
                        )
                    }
                }
            }
        }

        if (!showDetail) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(TuneFlowShapes.panel)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.error != null) {
                    ErrorState(message = state.error.orEmpty())
                } else {
                    Text(
                        text = "Select a playlist to inspect tracks and start playback.",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
fun SearchScreen(
    viewModel: SearchViewModel,
    focusRestoreTarget: BrowseFocusTarget? = null,
    onFocusRestoreConsumed: () -> Unit = {},
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf(state.query) }
    var editingQuery by remember { mutableStateOf(false) }
    var requestSearchFocus by rememberSaveable { mutableStateOf(focusRestoreTarget == null) }
    val restoredResultFocusRequester = remember { FocusRequester() }
    val searchResultsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val searchArtistRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val searchAlbumRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(state.query) {
        query = state.query
    }

    LaunchedEffect(focusRestoreTarget, state.result, state.suggestions, state.recentQueries, query) {
        val target = focusRestoreTarget ?: return@LaunchedEffect
        val sectionIndex = searchFocusSectionIndex(state, query, target) ?: return@LaunchedEffect
        searchResultsListState.scrollToItem(sectionIndex)
        when (target.kind) {
            BrowseFocusTargetKind.Artist -> {
                val targetIndex = state.result.artists.indexOfFirst { it.id == target.id }
                if (targetIndex >= 0) searchArtistRowState.scrollToItem(targetIndex)
            }
            BrowseFocusTargetKind.Album -> {
                val targetIndex = state.result.albums.indexOfFirst { it.id == target.id }
                if (targetIndex >= 0) searchAlbumRowState.scrollToItem(targetIndex)
            }
            BrowseFocusTargetKind.HomeCategory -> return@LaunchedEffect
            BrowseFocusTargetKind.Playlist -> return@LaunchedEffect
        }
        withFrameNanos { }
        runCatching { restoredResultFocusRequester.requestFocus() }
        onFocusRestoreConsumed()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (focusRestoreTarget == null) ScreenInitialFocusAnchor()
        SectionTitle(title = "Search")

        SearchField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onQueryChanged(it)
            },
            label = { Text("Search your library") },
            placeholder = { Text("Artist, album, or track") },
            editing = editingQuery,
            onEditingChange = { editingQuery = it },
            requestFocusOnDisplay = requestSearchFocus,
            onRequestFocusConsumed = { requestSearchFocus = false },
        )

        if (state.isLoading) {
            SearchResultsSkeleton()
        }
        if (state.error != null) {
            Text(
                text = state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        LazyColumn(
            state = searchResultsListState,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            if (query.isBlank() && state.recentQueries.isNotEmpty()) {
                item { SectionTitle(title = "Recent Queries") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.recentQueries, key = { it }) { recentQuery ->
                            PremiumChip(
                                label = recentQuery,
                                onClick = { viewModel.applySuggestedQuery(recentQuery) },
                            )
                        }
                    }
                }
            }

            if (query.isNotBlank() && state.suggestions.isNotEmpty()) {
                item { SectionTitle(title = "Suggestions") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.suggestions, key = { it }) { suggestion ->
                            PremiumChip(
                                label = suggestion,
                                onClick = { viewModel.applySuggestedQuery(suggestion) },
                            )
                        }
                    }
                }
            }

            if (state.result.artists.isNotEmpty()) {
                item { SectionTitle(title = "Artists") }
                item {
                    LazyRow(
                        state = searchArtistRowState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.result.artists, key = { it.id }) { artist ->
                            PremiumChip(
                                label = artist.name,
                                onClick = { onOpenArtist(artist.id) },
                                modifier =
                                    if (
                                        focusRestoreTarget?.kind == BrowseFocusTargetKind.Artist &&
                                        focusRestoreTarget.id == artist.id
                                    ) {
                                        Modifier.focusRequester(restoredResultFocusRequester)
                                    } else {
                                        Modifier
                                    },
                            )
                        }
                    }
                }
            }

            if (state.result.albums.isNotEmpty()) {
                item { SectionTitle(title = "Albums") }
                item {
                    LazyRow(
                        state = searchAlbumRowState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        itemsIndexed(state.result.albums, key = { _, album -> album.id }) { _, album ->
                            PremiumAlbumCard(
                                album = album,
                                onClick = { onOpenAlbum(album.id) },
                                modifier =
                                    if (
                                        focusRestoreTarget?.kind == BrowseFocusTargetKind.Album &&
                                        focusRestoreTarget.id == album.id
                                    ) {
                                        Modifier.focusRequester(restoredResultFocusRequester)
                                    } else {
                                        Modifier
                                    },
                            )
                        }
                    }
                }
            }

            if (state.result.tracks.isNotEmpty()) {
                item { SectionTitle(title = "Tracks") }
                itemsIndexed(state.result.tracks, key = { _, track -> track.id }) { index, track ->
                    PremiumListRow(
                        title = track.title,
                        subtitle = "${track.artist} • ${track.album}",
                        trailing = formatTrackDuration(track.durationSec),
                        onClick = { onPlayTracks(state.result.tracks, index) },
                        modifier =
                            Modifier.boundaryLockedVerticalItem(
                                index = index,
                                lastIndex = state.result.tracks.lastIndex,
                            ),
                    )
                }
            }
        }
    }
}

private fun searchFocusSectionIndex(
    state: SearchUiState,
    query: String,
    target: BrowseFocusTarget,
): Int? {
    var itemIndex = 0
    if (query.isBlank() && state.recentQueries.isNotEmpty()) itemIndex += 2
    if (query.isNotBlank() && state.suggestions.isNotEmpty()) itemIndex += 2
    val focusSections = mutableListOf<SearchFocusSection>()
    if (state.result.artists.isNotEmpty()) {
        focusSections +=
            SearchFocusSection(
                kind = BrowseFocusTargetKind.Artist,
                itemIds = state.result.artists.mapTo(mutableSetOf()) { it.id },
                rowIndex = itemIndex + 1,
            )
        itemIndex += 2
    }
    if (state.result.albums.isNotEmpty()) {
        focusSections +=
            SearchFocusSection(
                kind = BrowseFocusTargetKind.Album,
                itemIds = state.result.albums.mapTo(mutableSetOf()) { it.id },
                rowIndex = itemIndex + 1,
            )
    }
    return focusSections.firstOrNull { it.kind == target.kind && target.id in it.itemIds }?.rowIndex
}

private data class SearchFocusSection(
    val kind: BrowseFocusTargetKind,
    val itemIds: Set<String>,
    val rowIndex: Int,
)

@Composable
fun HomeCategoryScreen(
    category: HomeCategoryKind,
    viewModel: HomeCategoryViewModel,
    focusRestoreTarget: BrowseFocusTarget? = null,
    onFocusRestoreConsumed: () -> Unit = {},
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String?) -> Unit,
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var editingQuery by remember { mutableStateOf(false) }
    val categoryFocusTarget = focusRestoreTarget?.takeIf { it.matches(category) }
    var requestSearchFocus by rememberSaveable(category) { mutableStateOf(categoryFocusTarget == null) }

    LaunchedEffect(category) {
        viewModel.load(category)
    }

    LaunchedEffect(state.query) {
        query = state.query
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (categoryFocusTarget == null) ScreenInitialFocusAnchor()
        SectionTitle(title = state.title)

        SearchField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onQueryChanged(it)
            },
            label = { Text("Search ${state.title.lowercase()}") },
            placeholder = { Text(searchPlaceholderFor(category)) },
            editing = editingQuery,
            onEditingChange = { editingQuery = it },
            requestFocusOnDisplay = requestSearchFocus,
            onRequestFocusConsumed = { requestSearchFocus = false },
        )

        when {
            state.isLoading -> SearchResultsSkeleton()
            state.error != null -> ErrorState(message = state.error.orEmpty())
            else -> {
                HomeCategoryResults(
                    state = state,
                    focusRestoreTarget = categoryFocusTarget,
                    onFocusRestoreConsumed = onFocusRestoreConsumed,
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenPlaylist = onOpenPlaylist,
                    onPlayTracks = onPlayTracks,
                )
            }
        }
    }
}

@Composable
private fun HomeCategoryResults(
    state: HomeCategoryUiState,
    focusRestoreTarget: BrowseFocusTarget?,
    onFocusRestoreConsumed: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenPlaylist: (String?) -> Unit,
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
) {
    when (state.category) {
        HomeCategoryKind.Favorites ->
            FavoritesCategoryResults(
                state = state,
                focusRestoreTarget = focusRestoreTarget,
                onFocusRestoreConsumed = onFocusRestoreConsumed,
                onOpenAlbum = onOpenAlbum,
                onPlayTracks = onPlayTracks,
            )
        HomeCategoryKind.Artists ->
            ArtistCategoryResults(
                artists = state.filteredArtists,
                focusRestoreTarget = focusRestoreTarget,
                onFocusRestoreConsumed = onFocusRestoreConsumed,
                onOpenArtist = onOpenArtist,
            )
        HomeCategoryKind.Albums ->
            AlbumCategoryResults(
                albums = state.filteredAlbums,
                focusRestoreTarget = focusRestoreTarget,
                onFocusRestoreConsumed = onFocusRestoreConsumed,
                onOpenAlbum = onOpenAlbum,
            )
        HomeCategoryKind.Playlists ->
            PlaylistCategoryResults(
                playlists = state.filteredPlaylists,
                focusRestoreTarget = focusRestoreTarget,
                onFocusRestoreConsumed = onFocusRestoreConsumed,
                onOpenPlaylist = onOpenPlaylist,
            )
    }
}

@Composable
private fun FavoritesCategoryResults(
    state: HomeCategoryUiState,
    focusRestoreTarget: BrowseFocusTarget?,
    onFocusRestoreConsumed: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
) {
    val albums = state.filteredFavorites.albums
    val tracks = state.filteredFavorites.tracks
    val restoredAlbumFocusRequester = remember { FocusRequester() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(focusRestoreTarget, albums) {
        val targetIndex = albums.indexOfFirst { it.id == focusRestoreTarget?.id }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex + 1)
            withFrameNanos { }
            runCatching { restoredAlbumFocusRequester.requestFocus() }
            onFocusRestoreConsumed()
        }
    }

    if (albums.isEmpty() && tracks.isEmpty()) {
        EmptyCategoryResults(message = "No favorites match your search.")
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        if (albums.isNotEmpty()) {
            item { SectionTitle(title = "Albums") }
            itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                PremiumAlbumRow(
                    album = album,
                    onClick = { onOpenAlbum(album.id) },
                    modifier =
                        Modifier.boundaryLockedVerticalItem(
                            index = index,
                            lastIndex = if (tracks.isEmpty()) albums.lastIndex else Int.MAX_VALUE,
                        ).then(
                            if (album.id == focusRestoreTarget?.id) {
                                Modifier.focusRequester(restoredAlbumFocusRequester)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }

        if (tracks.isNotEmpty()) {
            item { SectionTitle(title = "Tracks") }
            itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                PremiumListRow(
                    title = track.title,
                    subtitle = "${track.artist} • ${track.album}",
                    trailing = formatTrackDuration(track.durationSec),
                    onClick = { onPlayTracks(tracks, index) },
                    modifier =
                        Modifier.boundaryLockedVerticalItem(
                            index = if (albums.isNotEmpty()) albums.size + index else index,
                            lastIndex = albums.size + tracks.lastIndex,
                        ),
                )
            }
        }
    }
}

@Composable
private fun ArtistCategoryResults(
    artists: List<ArtistSummary>,
    focusRestoreTarget: BrowseFocusTarget?,
    onFocusRestoreConsumed: () -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val restoredArtistFocusRequester = remember { FocusRequester() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(focusRestoreTarget, artists) {
        val targetIndex = artists.indexOfFirst { it.id == focusRestoreTarget?.id }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { restoredArtistFocusRequester.requestFocus() }
            onFocusRestoreConsumed()
        }
    }

    if (artists.isEmpty()) {
        EmptyCategoryResults(message = "No artists match your search.")
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        itemsIndexed(artists, key = { _, artist -> artist.id }) { index, artist ->
            PremiumArtistRow(
                artist = artist,
                onClick = { onOpenArtist(artist.id) },
                modifier =
                    Modifier.boundaryLockedVerticalItem(
                        index = index,
                        lastIndex = artists.lastIndex,
                    ).then(
                        if (artist.id == focusRestoreTarget?.id) {
                            Modifier.focusRequester(restoredArtistFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun AlbumCategoryResults(
    albums: List<AlbumSummary>,
    focusRestoreTarget: BrowseFocusTarget?,
    onFocusRestoreConsumed: () -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val restoredAlbumFocusRequester = remember { FocusRequester() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(focusRestoreTarget, albums) {
        val targetIndex = albums.indexOfFirst { it.id == focusRestoreTarget?.id }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { restoredAlbumFocusRequester.requestFocus() }
            onFocusRestoreConsumed()
        }
    }

    if (albums.isEmpty()) {
        EmptyCategoryResults(message = "No albums match your search.")
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
            PremiumAlbumRow(
                album = album,
                onClick = { onOpenAlbum(album.id) },
                modifier =
                    Modifier.boundaryLockedVerticalItem(
                        index = index,
                        lastIndex = albums.lastIndex,
                    ).then(
                        if (album.id == focusRestoreTarget?.id) {
                            Modifier.focusRequester(restoredAlbumFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PlaylistCategoryResults(
    playlists: List<PlaylistSummary>,
    focusRestoreTarget: BrowseFocusTarget?,
    onFocusRestoreConsumed: () -> Unit,
    onOpenPlaylist: (String?) -> Unit,
) {
    val restoredPlaylistFocusRequester = remember { FocusRequester() }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    LaunchedEffect(focusRestoreTarget, playlists) {
        val targetIndex = playlists.indexOfFirst { it.id == focusRestoreTarget?.id }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            withFrameNanos { }
            runCatching { restoredPlaylistFocusRequester.requestFocus() }
            onFocusRestoreConsumed()
        }
    }

    if (playlists.isEmpty()) {
        EmptyCategoryResults(message = "No playlists match your search.")
        return
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        itemsIndexed(playlists, key = { _, playlist -> playlist.id }) { index, playlist ->
            PremiumPlaylistRow(
                playlist = playlist,
                onClick = { onOpenPlaylist(playlist.id) },
                modifier =
                    Modifier.boundaryLockedVerticalItem(
                        index = index,
                        lastIndex = playlists.lastIndex,
                    ).then(
                        if (playlist.id == focusRestoreTarget?.id) {
                            Modifier.focusRequester(restoredPlaylistFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun EmptyCategoryResults(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TuneFlowShapes.panel)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                .padding(20.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun searchPlaceholderFor(category: HomeCategoryKind): String {
    return when (category) {
        HomeCategoryKind.Favorites -> "Album, artist, or track"
        HomeCategoryKind.Artists -> "Artist name"
        HomeCategoryKind.Albums -> "Album or artist"
        HomeCategoryKind.Playlists -> "Playlist name"
    }
}

private fun BrowseFocusTarget.matches(category: HomeCategoryKind): Boolean =
    when (category) {
        HomeCategoryKind.Favorites,
        HomeCategoryKind.Albums,
        -> kind == BrowseFocusTargetKind.Album
        HomeCategoryKind.Artists -> kind == BrowseFocusTargetKind.Artist
        HomeCategoryKind.Playlists -> kind == BrowseFocusTargetKind.Playlist
    }

@Composable
private fun PremiumArtistRow(
    artist: ArtistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusScaleCard(
        modifier = modifier.fillMaxWidth(),
        shape = TuneFlowShapes.row,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 92.dp, height = 68.dp)
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)),
            ) {
                TuneFlowArtwork(
                    model = artist.artUrl,
                    contentDescription = artist.name,
                    width = 92.dp,
                    height = 68.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = artist.name,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${artist.albumCount} albums",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PremiumAlbumRow(
    album: AlbumSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusScaleCard(
        modifier = modifier.fillMaxWidth(),
        shape = TuneFlowShapes.row,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(68.dp)
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)),
            ) {
                TuneFlowArtwork(
                    model = album.artUrl,
                    contentDescription = album.title,
                    width = 68.dp,
                    height = 68.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = album.title,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PremiumPlaylistRow(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusScaleCard(
        modifier = modifier.fillMaxWidth(),
        shape = TuneFlowShapes.row,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistArtworkGrid(
                artUrls = playlist.artUrls,
                label = playlist.name,
                modifier =
                    Modifier
                        .size(58.dp)
                        .clip(TuneFlowShapes.artwork),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.songCount} tracks • ${formatTotalDuration(playlist.durationSec)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    requestFocusOnDisplay: Boolean = false,
    onRequestFocusConsumed: () -> Unit = {},
    displayFocusRequesterOverride: FocusRequester? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val editFocusRequester = remember { FocusRequester() }
    val displayFocusRequester = displayFocusRequesterOverride ?: remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var restoreDisplayFocus by remember { mutableStateOf(false) }
    var pendingExitDirection by remember { mutableStateOf<FocusDirection?>(null) }

    fun stopEditing(direction: FocusDirection? = null) {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        pendingExitDirection = direction
        restoreDisplayFocus = true
        onEditingChange(false)
    }

    SearchFieldFocusEffect(
        editing = editing,
        restoreDisplayFocus = restoreDisplayFocus,
        editFocusRequester = editFocusRequester,
        displayFocusRequester = displayFocusRequester,
        keyboardController = keyboardController,
        focusManager = focusManager,
        pendingExitDirection = pendingExitDirection,
        onRestoreConsumed = {
            pendingExitDirection = null
            restoreDisplayFocus = false
        },
    )

    LaunchedEffect(requestFocusOnDisplay, editing) {
        if (requestFocusOnDisplay && !editing) {
            displayFocusRequester.requestFocus()
            onRequestFocusConsumed()
        }
    }

    if (editing) {
        EditingSearchField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            focusRequester = editFocusRequester,
            onKeyExit = ::stopEditing,
        )
    } else {
        SearchDisplayField(
            value = value,
            label = label,
            focused = focused,
            focusRequester = displayFocusRequester,
            onFocusedChange = { focused = it },
            onClick = { onEditingChange(true) },
        )
    }
}

@Composable
private fun SearchFieldFocusEffect(
    editing: Boolean,
    restoreDisplayFocus: Boolean,
    editFocusRequester: FocusRequester,
    displayFocusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusManager: androidx.compose.ui.focus.FocusManager,
    pendingExitDirection: FocusDirection?,
    onRestoreConsumed: () -> Unit,
) {
    LaunchedEffect(editing, restoreDisplayFocus, pendingExitDirection) {
        when {
            editing -> {
                editFocusRequester.requestFocus()
                keyboardController?.show()
            }
            restoreDisplayFocus -> {
                displayFocusRequester.requestFocus()
                pendingExitDirection?.let { focusManager.moveFocus(it) }
                onRestoreConsumed()
            }
        }
    }
}

@Composable
private fun EditingSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    placeholder: @Composable () -> Unit,
    focusRequester: FocusRequester,
    onKeyExit: (FocusDirection?) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions.Default,
        visualTransformation = VisualTransformation.None,
        shape = TuneFlowShapes.field,
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                    when {
                        it.key == Key.Back -> {
                            onKeyExit(null)
                            true
                        }
                        searchFieldFocusDirection(it.key) != null -> {
                            onKeyExit(searchFieldFocusDirection(it.key))
                            true
                        }
                        else -> false
                    }
                },
        colors = searchFieldColors(),
    )
}

@Composable
private fun SearchDisplayField(
    value: String,
    label: @Composable () -> Unit,
    focused: Boolean,
    focusRequester: FocusRequester,
    onFocusedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .scale(if (focused) 1.005f else 1f)
                .clip(TuneFlowShapes.field)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    shape = TuneFlowShapes.field,
                )
                .onFocusChanged { onFocusedChange(it.hasFocus) }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box { label() }
            Text(
                text = if (value.isNotBlank()) value else "Artist, album, or track",
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (value.isNotBlank()) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Modifier.boundaryLockedVerticalItem(
    index: Int,
    lastIndex: Int,
): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        when {
            event.key == Key.DirectionUp && index == 0 -> true
            event.key == Key.DirectionDown && index == lastIndex -> true
            else -> false
        }
    }

private fun searchFieldFocusDirection(key: Key): FocusDirection? {
    return when (key) {
        Key.DirectionUp -> FocusDirection.Up
        Key.DirectionDown -> FocusDirection.Down
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionRight -> FocusDirection.Right
        else -> null
    }
}

@Composable
private fun searchFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    )

@Composable
private fun ScreenInitialFocusAnchor() {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier =
            Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .focusable(),
    )
}

@Composable
private fun PremiumAlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusScaleCard(
        modifier = modifier.width(196.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)),
            ) {
                TuneFlowArtwork(
                    model = album.artUrl,
                    contentDescription = album.title,
                    width = 196.dp,
                    height = 196.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = album.title,
                )
            }
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PremiumListRow(
    title: String,
    subtitle: String,
    trailing: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusScaleCard(
        modifier = modifier.fillMaxWidth(),
        shape = TuneFlowShapes.row,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CurrentlyPlayingIndicator() {
    Image(
        painter = painterResource(id = R.drawable.currently_playing),
        contentDescription = "Currently playing",
        modifier = Modifier.size(20.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun PremiumChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusScaleCard(
        modifier = modifier.width(172.dp),
        shape = TuneFlowShapes.badge,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaylistArtworkGrid(
    artUrls: List<String>,
    label: String,
    modifier: Modifier = Modifier,
) {
    val collage =
        when {
            artUrls.isEmpty() -> List(4) { null }
            artUrls.size >= 4 -> artUrls.take(4)
            else -> List(4) { index -> artUrls[index % artUrls.size] }
        }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (rowIndex in 0 until 2) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                for (columnIndex in 0 until 2) {
                    val artUrl = collage[rowIndex * 2 + columnIndex]
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        TuneFlowArtwork(
                            model = artUrl,
                            contentDescription = label,
                            width = 58.dp,
                            height = 58.dp,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholderText = label,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BrowseActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .scale(if (focused) 1.01f else 1f)
                .clip(TuneFlowShapes.button)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (focused) 0.94f else 0.84f))
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = TuneFlowShapes.button,
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onPrimary,
        ) {
            content()
        }
    }
}

@Composable
private fun BrowsePlayIcon(modifier: Modifier = Modifier) {
    BrowseActionIcon(
        drawableRes = R.drawable.browse_play_action,
        contentDescription = "Play",
        modifier = modifier,
    )
}

@Composable
private fun BrowseShuffleIcon(modifier: Modifier = Modifier) {
    BrowseActionIcon(
        drawableRes = R.drawable.browse_shuffle_action,
        contentDescription = "Shuffle",
        modifier = modifier,
    )
}

@Composable
private fun BrowseActionIcon(
    drawableRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(id = drawableRes)
    val intrinsicSize = painter.intrinsicSize
    val aspectRatio =
        if (intrinsicSize.width > 0f && intrinsicSize.height > 0f) {
            intrinsicSize.width / intrinsicSize.height
        } else {
            1f
        }
    val iconSize = if (aspectRatio >= 1f) 28.dp else 30.dp

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(iconSize),
    )
}

@Composable
private fun AlbumGridLoadingMoreCard() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(TuneFlowShapes.card)
                .shimmerEffect(),
    )
}

@Composable
private fun AlbumCardSkeleton() {
    Column(
        modifier = Modifier.width(196.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .clip(TuneFlowShapes.artwork)
                    .shimmerEffect(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(TuneFlowShapes.field)
                    .shimmerEffect(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(TuneFlowShapes.field)
                    .shimmerEffect(),
        )
    }
}

@Composable
private fun DetailArtworkSkeleton(
    modifier: Modifier = Modifier,
    artworkWidth: androidx.compose.ui.unit.Dp,
    artworkHeight: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier =
            modifier
                .clip(TuneFlowShapes.container)
                .shimmerEffect(),
    ) {
        Box(
            modifier =
                Modifier
                    .width(artworkWidth)
                    .height(artworkHeight)
                    .clip(TuneFlowShapes.artwork)
                    .shimmerEffect()
                    .align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun TextLineSkeleton(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth(widthFraction)
                .height(height)
                .clip(TuneFlowShapes.field)
                .shimmerEffect(),
    )
}

@Composable
private fun ActionButtonSkeleton() {
    Box(
        modifier =
            Modifier
                .width(72.dp)
                .height(48.dp)
                .clip(TuneFlowShapes.button)
                .shimmerEffect(),
    )
}

@Composable
private fun ListRowSkeleton() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(TuneFlowShapes.row)
                .shimmerEffect(),
    )
}

@Composable
private fun ChipSkeleton() {
    Box(
        modifier =
            Modifier
                .width(172.dp)
                .height(56.dp)
                .clip(TuneFlowShapes.badge)
                .shimmerEffect(),
    )
}

@Composable
private fun AlbumDetailSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        DetailArtworkSkeleton(
            modifier =
                Modifier
                    .width(292.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f), TuneFlowShapes.container)
                    .padding(16.dp),
            artworkWidth = 260.dp,
            artworkHeight = 248.dp,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenInitialFocusAnchor()
            TextLineSkeleton(widthFraction = 0.52f, height = 34.dp)
            TextLineSkeleton(widthFraction = 0.34f, height = 28.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButtonSkeleton()
                ActionButtonSkeleton()
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(8) {
                    ListRowSkeleton()
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenInitialFocusAnchor()
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(208.dp)
                    .clip(TuneFlowShapes.container)
                    .shimmerEffect(),
        )
        TextLineSkeleton(widthFraction = 0.28f, height = 34.dp)
        TextLineSkeleton(widthFraction = 0.18f, height = 28.dp)
        SectionTitle(title = "Albums")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(6) {
                AlbumCardSkeleton()
            }
        }
    }
}

@Composable
private fun PlaylistListSkeleton() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        items(8) {
            ListRowSkeleton()
        }
    }
}

@Composable
private fun SearchResultsSkeleton() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        item { SectionTitle(title = "Suggestions") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(4) {
                    ChipSkeleton()
                }
            }
        }
        item { SectionTitle(title = "Albums") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(5) {
                    AlbumCardSkeleton()
                }
            }
        }
        item { SectionTitle(title = "Tracks") }
        items(6) {
            ListRowSkeleton()
        }
    }
}

@Composable
private fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmerTranslate",
    )

    return this.background(
        brush =
            Brush.linearGradient(
                colors =
                    listOf(
                        Color(0xFF141420),
                        Color(0xFF1E1E2E),
                        Color(0xFF141420),
                    ),
                start = Offset(translateAnim - 500f, 0f),
                end = Offset(translateAnim, 0f),
            ),
    )
}

@Composable
private fun ErrorState(
    modifier: Modifier = Modifier,
    message: String,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
