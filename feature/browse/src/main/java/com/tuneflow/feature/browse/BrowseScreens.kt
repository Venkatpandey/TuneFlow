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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

    when {
        state.isLoading -> {
            LoadingState(modifier = modifier, label = "Loading albums...")
        }

        state.error != null && state.items.isEmpty() -> {
            ErrorState(modifier = modifier, message = state.error.orEmpty())
        }

        else -> {
            Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                ScreenInitialFocusAnchor()
                SectionTitle(title = "Albums")
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 48.dp),
                ) {
                    items(state.items, key = { it.id }) { album ->
                        PremiumAlbumCard(album = album, onClick = { onAlbumSelected(album.id) })
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

    LaunchedEffect(albumId) {
        viewModel.load(albumId)
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
                            .width(320.dp)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                            .padding(18.dp),
                ) {
                    if (album.artUrl != null) {
                        AsyncImage(
                            model = album.artUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(276.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .align(Alignment.TopCenter),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
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
                    ) {
                        Text("Play Album")
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 48.dp),
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

    LaunchedEffect(artistId) {
        viewModel.load(artistId)
    }

    when {
        state.isLoading -> LoadingState(modifier = modifier, label = "Loading artist...")
        state.error != null -> ErrorState(modifier = modifier, message = state.error.orEmpty())
        state.artist == null -> ErrorState(modifier = modifier, message = "No artist data")
        else -> {
            val artist = state.artist!!
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                ScreenInitialFocusAnchor()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(28.dp))
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
                        modifier = Modifier.padding(24.dp),
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(artist.albums, key = { it.id }) { album ->
                        PremiumAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
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

    LaunchedEffect(preselectedPlaylistId) {
        if (preselectedPlaylistId != null) {
            viewModel.loadPlaylistDetail(preselectedPlaylistId)
            onPreselectedPlaylistConsumed()
        }
    }

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .width(348.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScreenInitialFocusAnchor()
            SectionTitle(title = "Playlists")

            if (state.isLoading && state.playlists.isEmpty()) {
                LoadingState(label = "Loading playlists...")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 48.dp),
                ) {
                    items(state.playlists, key = { it.id }) { playlist ->
                        PremiumPlaylistRow(playlist = playlist, onClick = { viewModel.loadPlaylistDetail(playlist.id) })
                    }
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        contentPadding = PaddingValues(bottom = 48.dp),
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

    LaunchedEffect(state.query) {
        query = state.query
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(state.result.albums, key = { it.id }) { album ->
                            PremiumAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                        }
                    }
                }
            }

            if (state.result.tracks.isNotEmpty()) {
                item { SectionTitle(title = "Tracks") }
                items(state.result.tracks, key = { it.id }) { track ->
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
) {
    FocusScaleCard(
        modifier = Modifier.fillMaxWidth(),
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
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp)),
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
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val editFocusRequester = remember { FocusRequester() }
    val displayFocusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var restoreDisplayFocus by remember { mutableStateOf(false) }

    fun stopEditing() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        restoreDisplayFocus = true
        onEditingChange(false)
    }

    LaunchedEffect(editing, restoreDisplayFocus) {
        when {
            editing -> {
                editFocusRequester.requestFocus()
                keyboardController?.show()
            }
            restoreDisplayFocus -> {
                displayFocusRequester.requestFocus()
                restoreDisplayFocus = false
            }
        }
    }

    if (editing) {
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
                    .focusRequester(editFocusRequester)
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Back) {
                            stopEditing()
                            true
                        } else {
                            false
                        }
                    },
            colors = searchFieldColors(),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(displayFocusRequester)
                    .scale(if (focused) 1.01f else 1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
                    .border(
                        width = if (focused) 2.dp else 1.dp,
                        color =
                            if (focused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        shape = RoundedCornerShape(18.dp),
                    )
                    .onFocusChanged { focused = it.hasFocus }
                    .focusable()
                    .clickable { onEditingChange(true) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
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
) {
    FocusScaleCard(
        modifier = Modifier.width(220.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
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
) {
    FocusScaleCard(
        modifier = Modifier.fillMaxWidth(),
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
        modifier =
            Modifier
                .width(196.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
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
                .scale(if (focused) 1.02f else 1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f))
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = RoundedCornerShape(20.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface,
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
