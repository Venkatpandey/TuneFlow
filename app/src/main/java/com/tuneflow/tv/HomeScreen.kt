@file:Suppress("TooManyFunctions")

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.TrackSummary
import com.tuneflow.core.player.PlaybackQueue

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playbackQueue: PlaybackQueue,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenPlaylists: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onPlayTracks: (List<TrackSummary>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScreenInitialFocusAnchor()
        }
        item {
            HomeHero(
                playbackQueue = playbackQueue,
                onPrimaryAction = if (playbackQueue.items.isNotEmpty()) onOpenNowPlaying else onOpenSearch,
                onSecondaryAction = onOpenAlbums,
            )
        }

        if (state.isLoading) {
            item { HomeLoadingSection() }
        }

        if (state.error != null &&
            state.recentAlbums.isEmpty() &&
            state.playlists.isEmpty() &&
            state.favorites.albums.isEmpty() &&
            state.favorites.tracks.isEmpty() &&
            state.artists.isEmpty()
        ) {
            item {
                ErrorBanner(message = state.error.orEmpty())
            }
        }

        if (state.favorites.albums.isNotEmpty() || state.favorites.tracks.isNotEmpty()) {
            item { SectionHeading("Favorites") }
            item {
                FavoriteRail(
                    favorites = state.favorites,
                    onOpenAlbum = onOpenAlbum,
                    onPlayTrack = { track -> onPlayTracks(listOf(track), 0) },
                )
            }
        }

        if (state.artists.isNotEmpty()) {
            item { SectionHeading("Artists") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(state.artists, key = { it.id }) { artist ->
                        HomeArtistCard(artist = artist, onClick = { onOpenArtist(artist.id) })
                    }
                }
            }
        }

        if (state.recentAlbums.isNotEmpty()) {
            item { SectionHeading("Albums") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(state.recentAlbums, key = { it.id }) { album ->
                        HomeAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                    }
                }
            }
        }

        if (state.playlists.isNotEmpty()) {
            item { SectionHeading("Playlists") }
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

        item { SectionHeading("Quick Actions") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    ActionCard(
                        title = "Search",
                        onClick = onOpenSearch,
                    )
                }
                item {
                    ActionCard(
                        title = "Browse Albums",
                        onClick = onOpenAlbums,
                    )
                }
                item {
                    ActionCard(
                        title = "All Playlists",
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

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(246.dp)
                .clip(TuneFlowShapes.hero)
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
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.background.copy(alpha = 0.24f),
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0.58f),
                        ),
                    ),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            ?: "Calm dark surfaces, large artwork, and fast access to favorites, artists, and search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HeroActionButton(
                        label = if (currentItem != null) "Resume Playback" else "Start Searching",
                        accent = true,
                        onClick = onPrimaryAction,
                    )
                    HeroActionButton(
                        label = "Browse Albums",
                        onClick = onSecondaryAction,
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            Box(
                modifier =
                    Modifier
                        .size(168.dp)
                        .clip(TuneFlowShapes.albumArt)
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
private fun HeroActionButton(
    label: String,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = TuneFlowShapes.button

    Box(
        modifier =
            Modifier
                .scale(if (focused) 1.01f else 1f)
                .clip(shape)
                .background(
                    if (accent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f)
                    },
                )
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color =
                        if (focused) {
                            MaterialTheme.colorScheme.onSurface
                        } else if (accent) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                        },
                    shape = shape,
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .width(184.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color =
                if (accent) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

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
private fun FavoriteRail(
    favorites: FavoritesBundle,
    onOpenAlbum: (String) -> Unit,
    onPlayTrack: (TrackSummary) -> Unit,
) {
    val favoriteAlbums = favorites.albums.take(8)
    val favoriteTracks = favorites.tracks.take((8 - favoriteAlbums.size).coerceAtLeast(0))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        items(favoriteAlbums, key = { "album-${it.id}" }) { album ->
            HomeAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
        }
        items(favoriteTracks, key = { "track-${it.id}" }) { track ->
            FavoriteTrackCard(track = track, onClick = { onPlayTrack(track) })
        }
    }
}

@Composable
private fun FavoriteTrackCard(
    track: TrackSummary,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(196.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .clip(TuneFlowShapes.card)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
            ) {
                if (track.artUrl != null) {
                    AsyncImage(
                        model = track.artUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = track.title.take(1),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist} • ${track.album}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeArtistCard(
    artist: ArtistSummary,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(208.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(TuneFlowShapes.card)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
            ) {
                if (artist.artUrl != null) {
                    AsyncImage(
                        model = artist.artUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = artist.name.take(1),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${artist.albumCount} albums",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeLoadingSection() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TuneFlowShapes.card)
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
private fun ErrorBanner(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TuneFlowShapes.card)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                .padding(28.dp),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SectionHeading(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HomeAlbumCard(
    album: AlbumSummary,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(196.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .clip(TuneFlowShapes.card)
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
        modifier = Modifier.width(236.dp),
        onClick = onClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlaylistArtCollage(playlist = playlist)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
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
private fun PlaylistArtCollage(playlist: PlaylistSummary) {
    val artUrls =
        when {
            playlist.artUrls.isEmpty() -> List(4) { null }
            playlist.artUrls.size >= 4 -> playlist.artUrls.take(4)
            else -> List(4) { index -> playlist.artUrls[index % playlist.artUrls.size] }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(TuneFlowShapes.card)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (rowIndex in 0 until 2) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (columnIndex in 0 until 2) {
                    val artUrl = artUrls[rowIndex * 2 + columnIndex]
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
                                contentDescription = playlist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = "TF",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(208.dp),
        onClick = onClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
                .scale(if (focused) 1.01f else 1f)
                .clip(TuneFlowShapes.card)
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
                    shape = TuneFlowShapes.card,
                )
                .onFocusChanged { focused = it.hasFocus }
                .focusable()
                .clickable(onClick = onClick)
                .padding(12.dp),
    ) {
        Column(content = content)
    }
}
