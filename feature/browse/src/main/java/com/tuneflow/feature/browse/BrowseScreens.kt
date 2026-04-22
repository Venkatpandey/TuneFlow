package com.tuneflow.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.TrackSummary

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.error != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(state.items) { album ->
            AlbumCard(album = album, onClick = { onAlbumSelected(album.id) })
        }

        if (state.hasMore) {
            item {
                LaunchedEffect(state.items.size) {
                    viewModel.loadMore()
                }
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit,
) {
    FocusScaleCard(onClick = onClick) {
        Column {
            Box(
                modifier =
                    Modifier
                        .size(width = 220.dp, height = 220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = album.title.take(1),
                    modifier =
                        Modifier
                            .padding(12.dp)
                            .align(androidx.compose.ui.Alignment.TopStart),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(album.title, maxLines = 1)
            Text(album.artist, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        state.isLoading ->
            Box(
                modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) { CircularProgressIndicator() }
        state.error != null ->
            Box(modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        state.album == null ->
            Box(
                modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) { Text("No album data") }
        else -> {
            val album = state.album!!
            Column(modifier = modifier.fillMaxSize()) {
                Text(album.title, style = MaterialTheme.typography.headlineMedium)
                Text(album.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onPlayAlbum(album.tracks, 0) },
                    modifier = Modifier.focusRequester(playButtonFocusRequester),
                ) {
                    Text("Play Album")
                }
                LaunchedEffect(album.id) {
                    playButtonFocusRequester.requestFocus()
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(album.tracks) { track ->
                        FocusScaleCard(onClick = { onPlayAlbum(album.tracks, album.tracks.indexOf(track)) }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(track.title, modifier = Modifier.weight(1f))
                                Text(track.artist)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.playlists) { playlist ->
                FocusScaleCard(onClick = { viewModel.loadPlaylistDetail(playlist.id) }) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(playlist.name)
                        Text("${playlist.songCount} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            val selected = state.selected
            if (selected == null) {
                Text("Select a playlist")
            } else {
                Text(selected.name, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onPlayTracks(selected.tracks, 0) }) { Text("Play Playlist") }
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selected.tracks) { track ->
                        FocusScaleCard(onClick = { onPlayTracks(selected.tracks, selected.tracks.indexOf(track)) }) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                Text(track.title, modifier = Modifier.weight(1f))
                                Text(track.artist)
                            }
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
    onPlayTracks: (tracks: List<TrackSummary>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf(state.query) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.onQueryChanged(it)
            },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        }
        if (state.error != null) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
        }

        Text("Artists", style = MaterialTheme.typography.titleMedium)
        state.result.artists.take(6).forEach { artist ->
            Text(artist)
        }

        Spacer(Modifier.height(10.dp))
        Text("Albums", style = MaterialTheme.typography.titleMedium)
        state.result.albums.take(6).forEach { album ->
            Text("${album.title} - ${album.artist}")
        }

        Spacer(Modifier.height(10.dp))
        Text("Tracks", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.result.tracks) { track ->
                FocusScaleCard(onClick = { onPlayTracks(state.result.tracks, state.result.tracks.indexOf(track)) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(track.title, modifier = Modifier.weight(1f))
                        Text(track.artist)
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusScaleCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .scale(if (focused) 1.05f else 1f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = if (focused) 2.dp else 0.dp,
                    color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(4.dp),
    ) {
        content()
    }
}
