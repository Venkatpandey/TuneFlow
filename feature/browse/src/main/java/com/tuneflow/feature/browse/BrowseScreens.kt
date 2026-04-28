@file:Suppress("TooManyFunctions")

package com.tuneflow.feature.browse

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.TrackSummary

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val firstAlbumFocusRequester = remember { FocusRequester() }
    var initialAlbumFocusRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.items.size) {
        if (!initialAlbumFocusRequested && state.items.isNotEmpty()) {
            firstAlbumFocusRequester.requestFocus()
            initialAlbumFocusRequested = true
        }
    }

    when {
        state.isLoading -> {
            LoadingState(modifier = modifier, label = "Loading albums...")
        }

        state.error != null && state.items.isEmpty() -> {
            ErrorState(modifier = modifier, message = state.error.orEmpty())
        }

        else -> {
            Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ScreenInitialFocusAnchor()
                SectionTitle(title = "Albums")
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 196.dp),
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
                                if (index == 0) {
                                    Modifier.focusRequester(firstAlbumFocusRequester)
                                } else {
                                    Modifier
                                },
                        )
                    }

                    if (state.hasMore) {
                        item {
                            LaunchedEffect(state.items.size) {
                                viewModel.loadMore()
                            }
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
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
        state.isLoading -> LoadingState(modifier = modifier, label = "Loading album...")
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
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                            .padding(16.dp),
                ) {
                    if (album.artUrl != null) {
                        AsyncImage(
                            model = album.artUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(248.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .align(Alignment.TopCenter),
                        )
                    }
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
                    BrowseActionButton(
                        onClick = { onPlayAlbum(album.tracks, 0) },
                        modifier = Modifier.focusRequester(playAlbumFocusRequester),
                    ) {
                        Text("Play Album")
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        items(album.tracks, key = { it.id }) { track ->
                            PremiumListRow(
                                title = track.title,
                                subtitle = track.artist,
                                trailing = formatTrackDuration(track.durationSec),
                                onClick = { onPlayAlbum(album.tracks, album.tracks.indexOf(track)) },
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
    onOpenAlbum: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstArtistAlbumFocusRequester = remember { FocusRequester() }
    var initialArtistFocusRequested by rememberSaveable(artistId) { mutableStateOf(false) }

    LaunchedEffect(artistId) {
        viewModel.load(artistId)
    }

    LaunchedEffect(state.artist?.albums?.size) {
        if (!initialArtistFocusRequested && state.artist?.albums?.isNotEmpty() == true) {
            firstArtistAlbumFocusRequester.requestFocus()
            initialArtistFocusRequested = true
        }
    }

    when {
        state.isLoading -> LoadingState(modifier = modifier, label = "Loading artist...")
        state.error != null -> ErrorState(modifier = modifier, message = state.error.orEmpty())
        state.artist == null -> ErrorState(modifier = modifier, message = "No artist data")
        else -> {
            val artist = state.artist!!
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
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)),
                ) {
                    if (artist.artUrl != null) {
                        AsyncImage(
                            model = artist.artUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.34f,
                        )
                    }
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(artist.albums, key = { _, album -> album.id }) { index, album ->
                        PremiumAlbumCard(
                            album = album,
                            onClick = { onOpenAlbum(album.id) },
                            modifier =
                                if (index == 0) {
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
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    preselectedPlaylistId: String? = null,
    onPreselectedPlaylistConsumed: () -> Unit = {},
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstPlaylistFocusRequester = remember { FocusRequester() }
    var initialPlaylistFocusRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(preselectedPlaylistId) {
        if (preselectedPlaylistId != null) {
            viewModel.loadPlaylistDetail(preselectedPlaylistId)
            onPreselectedPlaylistConsumed()
        }
    }

    LaunchedEffect(state.playlists.size) {
        if (!initialPlaylistFocusRequested && state.playlists.isNotEmpty()) {
            firstPlaylistFocusRequester.requestFocus()
            initialPlaylistFocusRequested = true
        }
    }

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .width(312.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenInitialFocusAnchor()
            SectionTitle(title = "Playlists")

            if (state.isLoading && state.playlists.isEmpty()) {
                LoadingState(label = "Loading playlists...")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    itemsIndexed(state.playlists, key = { _, playlist -> playlist.id }) { index, playlist ->
                        PremiumPlaylistRow(
                            playlist = playlist,
                            onClick = { viewModel.loadPlaylistDetail(playlist.id) },
                            modifier =
                                if (index == 0) {
                                    Modifier.focusRequester(firstPlaylistFocusRequester)
                                } else {
                                    Modifier
                                },
                        )
                    }
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                state.selected == null && state.error != null -> {
                    ErrorState(message = state.error.orEmpty())
                }
                state.selected == null -> {
                    Text(
                        text = "Select a playlist to inspect tracks and start playback.",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    val selected = state.selected!!
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    BrowseActionButton(onClick = { onPlayTracks(selected.tracks, 0) }) {
                        Text("Play Playlist")
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        items(selected.tracks, key = { it.id }) { track ->
                            PremiumListRow(
                                title = track.title,
                                subtitle = track.artist,
                                trailing = formatTrackDuration(track.durationSec),
                                onClick = {
                                    onPlayTracks(
                                        selected.tracks,
                                        selected.tracks.indexOf(track),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf(state.query) }
    var editingQuery by remember { mutableStateOf(false) }
    var requestSearchFocus by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(state.query) {
        query = state.query
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenInitialFocusAnchor()
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
            LoadingState(label = "Searching...")
        }
        if (state.error != null) {
            Text(
                text = state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        LazyColumn(
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.result.artists, key = { it.id }) { artist ->
                            PremiumChip(
                                label = artist.name,
                                onClick = { onOpenArtist(artist.id) },
                            )
                        }
                    }
                }
            }

            if (state.result.albums.isNotEmpty()) {
                item { SectionTitle(title = "Albums") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(state.result.albums, key = { _, album -> album.id }) { _, album ->
                            PremiumAlbumCard(
                                album = album,
                                onClick = { onOpenAlbum(album.id) },
                            )
                        }
                    }
                }
            }

            if (state.result.tracks.isNotEmpty()) {
                item { SectionTitle(title = "Tracks") }
                itemsIndexed(state.result.tracks, key = { _, track -> track.id }) { _, track ->
                    PremiumListRow(
                        title = track.title,
                        subtitle = "${track.artist} • ${track.album}",
                        trailing = formatTrackDuration(track.durationSec),
                        onClick = { onPlayTracks(state.result.tracks, state.result.tracks.indexOf(track)) },
                    )
                }
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
                        .clip(RoundedCornerShape(12.dp)),
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
                    text = "${playlist.songCount} tracks",
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
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    shape = RoundedCornerShape(16.dp),
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
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)),
            ) {
                if (album.artUrl != null) {
                    AsyncImage(
                        model = album.artUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = album.title.take(1),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusScaleCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
private fun PremiumChip(
    label: String,
    onClick: () -> Unit,
) {
    FocusScaleCard(
        modifier = Modifier.width(172.dp),
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
                        if (artUrl != null) {
                            AsyncImage(
                                model = artUrl,
                                contentDescription = label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = "TF",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
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
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (focused) 0.94f else 0.84f))
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = RoundedCornerShape(18.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 11.dp),
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
private fun LoadingState(
    modifier: Modifier = Modifier,
    label: String,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
