package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import coil.compose.AsyncImage
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.player.PlaybackQueue

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playbackQueue: PlaybackQueue,
    onOpenAlbum: (String) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenPlaylists: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            HomeHero(
                playbackQueue = playbackQueue,
                onPrimaryAction = if (playbackQueue.items.isNotEmpty()) onOpenNowPlaying else onOpenSearch,
                onSecondaryAction = onOpenAlbums,
            )
        }

        if (state.isLoading) {
            item {
                HomeLoadingSection()
            }
        }

        if (state.error != null && state.recentAlbums.isEmpty() && state.playlists.isEmpty()) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                            .padding(28.dp),
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        if (state.recentAlbums.isNotEmpty()) {
            item { SectionHeading("Recent Albums", "New arrivals from your Navidrome library") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(state.recentAlbums, key = { it.id }) { album ->
                        HomeAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                    }
                }
            }
        }

        if (state.playlists.isNotEmpty()) {
            item { SectionHeading("Playlists", "Jump back into curated mixes and saved flows") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(state.playlists, key = { it.id }) { playlist ->
                        HomePlaylistCard(
                            playlist = playlist,
                            onClick = { onOpenPlaylists(playlist.id) },
                        )
                    }
                }
            }
        }

        item { SectionHeading("Quick Actions", "Fast paths built for remote-first browsing") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    ActionCard(
                        title = "Search",
                        subtitle = "Find artists, albums, and tracks with fewer clicks.",
                        onClick = onOpenSearch,
                    )
                }
                item {
                    ActionCard(
                        title = "Browse Albums",
                        subtitle = "Explore the full album catalog with larger art-led cards.",
                        onClick = onOpenAlbums,
                    )
                }
                item {
                    ActionCard(
                        title = "All Playlists",
                        subtitle = "Open playlist browser and keep playback flow moving.",
                        onClick = { onOpenPlaylists(null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHero(
    playbackQueue: PlaybackQueue,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    val currentItem = playbackQueue.currentItem
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(currentItem?.id) {
        primaryFocusRequester.requestFocus()
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
    ) {
        if (currentItem?.artUrl != null) {
            AsyncImage(
                model = currentItem.artUrl,
                contentDescription = currentItem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.28f,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.38f)),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 34.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (currentItem != null) "Continue Listening" else "Welcome to TuneFlow",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = currentItem?.title ?: "A TV-first Navidrome experience tuned for your remote.",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text =
                        currentItem?.let { "${it.artist} • ${it.album}" }
                            ?: "Calm dark surfaces, large artwork, and quick access to your library.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier.focusRequester(primaryFocusRequester),
                    ) {
                        Text(if (currentItem != null) "Resume Playback" else "Start Searching")
                    }
                    Button(onClick = onSecondaryAction) {
                        Text("Browse Albums")
                    }
                }
            }

            Spacer(Modifier.width(24.dp))

            Box(
                modifier =
                    Modifier
                        .size(248.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
            ) {
                if (currentItem?.artUrl != null) {
                    AsyncImage(
                        model = currentItem.artUrl,
                        contentDescription = currentItem.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = "TuneFlow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLoadingSection() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .padding(28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = "Curating your library...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SectionHeading(
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
private fun HomeAlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(240.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
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
private fun HomePlaylistCard(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(300.dp),
        onClick = onClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(196.dp)
                    .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "TF",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.songCount} tracks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(320.dp),
        onClick = onClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
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
}

@Composable
private fun FocusCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .scale(if (focused) 1.04f else 1f)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (focused) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                    },
                )
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = RoundedCornerShape(26.dp),
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(18.dp),
    ) {
        Column(content = content)
    }
}
