@file:Suppress("TooManyFunctions")

package com.tuneflow.feature.browse

import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
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
                SectionTitle(
                    title = "Albums",
                    subtitle = "Larger artwork and calmer spacing for distance-friendly browsing.",
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 240.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 56.dp),
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
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(24.dp))
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
    val playButtonFocusRequester = remember { FocusRequester() }

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
                horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(360.dp)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                            .padding(22.dp),
                ) {
                    if (album.artUrl != null) {
                        AsyncImage(
                            model = album.artUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(316.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .align(Alignment.TopCenter),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onPlayAlbum(album.tracks, 0) },
                        modifier = Modifier.focusRequester(playButtonFocusRequester),
                    ) {
                        Text("Play Album")
                    }
                    LaunchedEffect(album.id) {
                        playButtonFocusRequester.requestFocus()
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
                    subtitle = "Jump from artist overview into album playback quickly.",
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
                    .width(380.dp)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle(
                title = "Playlists",
                subtitle = "Curated listening paths with clearer focus and lower density.",
            )

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
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                    .padding(22.dp),
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
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(onClick = { onPlayTracks(selected.tracks, 0) }) {
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

    LaunchedEffect(state.query) {
        query = state.query
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SectionTitle(
            title = "Search",
            subtitle = "Grouped results and calmer layouts to reduce typing fatigue on TV.",
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onQueryChanged(it)
            },
            label = { Text("Search your library") },
            placeholder = { Text("Artist, album, or track") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                ),
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
                item { SectionTitle(title = "Recent Queries", subtitle = "Jump back into common searches with fewer clicks") }
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
                item { SectionTitle(title = "Suggestions", subtitle = "Fast fills from live Navidrome results") }
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
                item { SectionTitle(title = "Artists", subtitle = "Open the artist surface directly from search") }
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
                item { SectionTitle(title = "Albums", subtitle = "Artwork-forward album matches") }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        items(state.result.albums, key = { it.id }) { album ->
                            PremiumAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                        }
                    }
                }
            }

            if (state.result.tracks.isNotEmpty()) {
                item { SectionTitle(title = "Tracks", subtitle = "Start playback directly from search results") }
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
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp)),
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
private fun PremiumAlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit,
) {
    FocusScaleCard(
        modifier = Modifier.width(244.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(244.dp)
                        .clip(RoundedCornerShape(22.dp))
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
                .width(220.dp),
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
private fun SectionTitle(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
