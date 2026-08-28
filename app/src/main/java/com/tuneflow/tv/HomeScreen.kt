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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tuneflow.core.design.ArtworkPlaceholder
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.TrackSummary
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.feature.browse.BrowseFocusTarget
import com.tuneflow.feature.browse.BrowseFocusTargetKind
import com.tuneflow.feature.browse.HomeCategoryKind
import android.view.KeyEvent as AndroidKeyEvent

@Composable
@Suppress("CyclomaticComplexMethod")
fun HomeScreen(
    viewModel: HomeViewModel,
    playbackQueue: PlaybackQueue,
    focusRestoreTarget: BrowseFocusTarget? = null,
    onFocusRestoreConsumed: () -> Unit = {},
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenHomeCategory: (HomeCategoryKind) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenPlaylists: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onPlayTracks: (List<TrackSummary>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val homeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val favoritesRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val artistsRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val albumsRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val playlistsRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val restoredItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusRestoreTarget, state) {
        val target = focusRestoreTarget ?: return@LaunchedEffect
        val location = state.focusLocation(target) ?: return@LaunchedEffect
        homeListState.scrollToItem(location.sectionRowIndex)
        val rowState =
            when (location.category) {
                HomeCategoryKind.Favorites -> favoritesRowState
                HomeCategoryKind.Artists -> artistsRowState
                HomeCategoryKind.Albums -> albumsRowState
                HomeCategoryKind.Playlists -> playlistsRowState
            }
        rowState.scrollToItem(location.rowItemIndex)
        withFrameNanos { }
        runCatching { restoredItemFocusRequester.requestFocus() }
        onFocusRestoreConsumed()
    }

    LazyColumn(
        state = homeListState,
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

        if (
            state.error != null &&
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
                    listState = favoritesRowState,
                    focusRestoreTarget = focusRestoreTarget,
                    restoredItemFocusRequester = restoredItemFocusRequester,
                    onOpenAlbum = onOpenAlbum,
                    onPlayTrack = { track -> onPlayTracks(listOf(track), 0) },
                    onShowAll = { onOpenHomeCategory(HomeCategoryKind.Favorites) },
                    showAllModifier =
                        showAllFocusModifier(
                            target = focusRestoreTarget,
                            category = HomeCategoryKind.Favorites,
                            focusRequester = restoredItemFocusRequester,
                        ),
                )
            }
        }

        if (state.artists.isNotEmpty()) {
            item { SectionHeading("Artists") }
            item {
                HomeContentRow(
                    items = state.artists,
                    listState = artistsRowState,
                    key = { _, artist -> artist.id },
                    onShowAll = { onOpenHomeCategory(HomeCategoryKind.Artists) },
                    showAllModifier =
                        showAllFocusModifier(
                            target = focusRestoreTarget,
                            category = HomeCategoryKind.Artists,
                            focusRequester = restoredItemFocusRequester,
                        ),
                ) { artist ->
                    HomeArtistCard(
                        artist = artist,
                        onClick = { onOpenArtist(artist.id) },
                        modifier =
                            itemFocusModifier(
                                target = focusRestoreTarget,
                                kind = BrowseFocusTargetKind.Artist,
                                id = artist.id,
                                focusRequester = restoredItemFocusRequester,
                            ),
                    )
                }
            }
        }

        if (state.recentAlbums.isNotEmpty()) {
            item { SectionHeading("Albums") }
            item {
                HomeContentRow(
                    items = state.recentAlbums,
                    listState = albumsRowState,
                    key = { _, album -> album.id },
                    onShowAll = { onOpenHomeCategory(HomeCategoryKind.Albums) },
                    showAllModifier =
                        showAllFocusModifier(
                            target = focusRestoreTarget,
                            category = HomeCategoryKind.Albums,
                            focusRequester = restoredItemFocusRequester,
                        ),
                ) { album ->
                    HomeAlbumCard(
                        album = album,
                        onClick = { onOpenAlbum(album.id) },
                        modifier =
                            itemFocusModifier(
                                target = focusRestoreTarget,
                                kind = BrowseFocusTargetKind.Album,
                                id = album.id,
                                focusRequester = restoredItemFocusRequester,
                            ),
                    )
                }
            }
        }

        if (state.playlists.isNotEmpty()) {
            item { SectionHeading("Playlists") }
            item {
                HomeContentRow(
                    items = state.playlists,
                    listState = playlistsRowState,
                    key = { _, playlist -> playlist.id },
                    onShowAll = { onOpenHomeCategory(HomeCategoryKind.Playlists) },
                    showAllModifier =
                        showAllFocusModifier(
                            target = focusRestoreTarget,
                            category = HomeCategoryKind.Playlists,
                            focusRequester = restoredItemFocusRequester,
                        ),
                ) { playlist ->
                    HomePlaylistCard(
                        playlist = playlist,
                        onClick = { onOpenPlaylists(playlist.id) },
                        modifier =
                            itemFocusModifier(
                                target = focusRestoreTarget,
                                kind = BrowseFocusTargetKind.Playlist,
                                id = playlist.id,
                                focusRequester = restoredItemFocusRequester,
                            ),
                    )
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
                        modifier = Modifier.homeBottomBoundaryItem(),
                    )
                }
                item {
                    ActionCard(
                        title = "Browse Albums",
                        onClick = onOpenAlbums,
                        modifier = Modifier.homeBottomBoundaryItem(),
                    )
                }
                item {
                    ActionCard(
                        title = "All Playlists",
                        onClick = { onOpenPlaylists(null) },
                        modifier = Modifier.homeBottomBoundaryItem(),
                    )
                }
            }
        }
    }
}

private fun HomeUiState.focusLocation(target: BrowseFocusTarget): HomeFocusLocation? {
    val sections = focusSections()
    val sectionIndex = sections.indexOfFirst { it.matches(target) }
    if (sectionIndex < 0) return null
    val contentStartIndex = 2 + isLoading.toItemCount() + showsFatalError().toItemCount()
    val section = sections[sectionIndex]
    return HomeFocusLocation(
        category = section.category,
        sectionRowIndex = contentStartIndex + sectionIndex * 2 + 1,
        rowItemIndex = section.focusItemIndex(target),
    )
}

private data class HomeFocusLocation(
    val category: HomeCategoryKind,
    val sectionRowIndex: Int,
    val rowItemIndex: Int,
)

private data class HomeFocusSection(
    val category: HomeCategoryKind,
    val itemTargets: List<BrowseFocusTarget>,
    val contentItemCount: Int,
) {
    fun matches(target: BrowseFocusTarget): Boolean = target.matchesHomeCategory(category) || target in itemTargets

    fun focusItemIndex(target: BrowseFocusTarget): Int =
        if (target.matchesHomeCategory(category)) contentItemCount else itemTargets.indexOf(target).coerceAtLeast(0)
}

private fun HomeUiState.focusSections(): List<HomeFocusSection> =
    buildList {
        if (favorites.albums.isNotEmpty() || favorites.tracks.isNotEmpty()) {
            add(
                HomeFocusSection(
                    category = HomeCategoryKind.Favorites,
                    itemTargets =
                        favorites.albums
                            .take(HOME_ROW_VISIBLE_ITEM_LIMIT)
                            .map { BrowseFocusTarget(BrowseFocusTargetKind.Album, it.id) },
                    contentItemCount =
                        (favorites.albums.size + favorites.tracks.size)
                            .coerceAtMost(HOME_ROW_VISIBLE_ITEM_LIMIT),
                ),
            )
        }
        if (artists.isNotEmpty()) {
            add(
                HomeFocusSection(
                    category = HomeCategoryKind.Artists,
                    itemTargets =
                        artists
                            .take(HOME_ROW_VISIBLE_ITEM_LIMIT)
                            .map { BrowseFocusTarget(BrowseFocusTargetKind.Artist, it.id) },
                    contentItemCount = artists.size.coerceAtMost(HOME_ROW_VISIBLE_ITEM_LIMIT),
                ),
            )
        }
        if (recentAlbums.isNotEmpty()) {
            add(
                HomeFocusSection(
                    category = HomeCategoryKind.Albums,
                    itemTargets =
                        recentAlbums
                            .take(HOME_ROW_VISIBLE_ITEM_LIMIT)
                            .map { BrowseFocusTarget(BrowseFocusTargetKind.Album, it.id) },
                    contentItemCount = recentAlbums.size.coerceAtMost(HOME_ROW_VISIBLE_ITEM_LIMIT),
                ),
            )
        }
        if (playlists.isNotEmpty()) {
            add(
                HomeFocusSection(
                    category = HomeCategoryKind.Playlists,
                    itemTargets =
                        playlists
                            .take(HOME_ROW_VISIBLE_ITEM_LIMIT)
                            .map { BrowseFocusTarget(BrowseFocusTargetKind.Playlist, it.id) },
                    contentItemCount = playlists.size.coerceAtMost(HOME_ROW_VISIBLE_ITEM_LIMIT),
                ),
            )
        }
    }

private fun HomeUiState.showsFatalError(): Boolean =
    error != null &&
        recentAlbums.isEmpty() &&
        playlists.isEmpty() &&
        favorites.albums.isEmpty() &&
        favorites.tracks.isEmpty() &&
        artists.isEmpty()

private fun Boolean.toItemCount(): Int = if (this) 1 else 0

private fun BrowseFocusTarget.matchesHomeCategory(category: HomeCategoryKind): Boolean =
    kind == BrowseFocusTargetKind.HomeCategory && id == category.name

private fun showAllFocusModifier(
    target: BrowseFocusTarget?,
    category: HomeCategoryKind,
    focusRequester: FocusRequester,
): Modifier =
    if (target?.matchesHomeCategory(category) == true) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

private fun itemFocusModifier(
    target: BrowseFocusTarget?,
    kind: BrowseFocusTargetKind,
    id: String,
    focusRequester: FocusRequester,
): Modifier =
    if (target?.kind == kind && target.id == id) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

private const val HOME_ROW_VISIBLE_ITEM_LIMIT = 5

@Composable
private fun <T> HomeContentRow(
    items: List<T>,
    listState: LazyListState,
    key: (Int, T) -> Any,
    onShowAll: () -> Unit,
    showAllModifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(items.take(HOME_ROW_VISIBLE_ITEM_LIMIT), key = key) { _, item ->
            itemContent(item)
        }
        item {
            ShowAllCard(onClick = onShowAll, modifier = showAllModifier)
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
                .clip(TuneFlowShapes.container)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
    ) {
        if (currentItem?.artUrl != null) {
            TuneFlowArtwork(
                model = currentItem.artUrl,
                contentDescription = currentItem.title,
                width = 1280.dp,
                height = 246.dp,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                placeholderText = currentItem.title,
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
                        label = if (currentItem != null) "Resume" else "Start Searching",
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
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
            ) {
                if (currentItem != null) {
                    TuneFlowArtwork(
                        model = currentItem.artUrl,
                        contentDescription = currentItem.title,
                        width = 168.dp,
                        height = 168.dp,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholderText = currentItem.title,
                        fallbackPainterResId = R.drawable.ic_tuneflow_brand,
                    )
                } else {
                    ArtworkPlaceholder(
                        fallbackPainterResId = R.drawable.ic_tuneflow_brand,
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
    listState: LazyListState,
    focusRestoreTarget: BrowseFocusTarget?,
    restoredItemFocusRequester: FocusRequester,
    onOpenAlbum: (String) -> Unit,
    onPlayTrack: (TrackSummary) -> Unit,
    onShowAll: () -> Unit,
    showAllModifier: Modifier = Modifier,
) {
    val favoriteAlbums = favorites.albums.take(HOME_ROW_VISIBLE_ITEM_LIMIT)
    val favoriteTracks =
        favorites.tracks.take((HOME_ROW_VISIBLE_ITEM_LIMIT - favoriteAlbums.size).coerceAtLeast(0))

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(favoriteAlbums, key = { "album-${it.id}" }) { album ->
            HomeAlbumCard(
                album = album,
                onClick = { onOpenAlbum(album.id) },
                modifier =
                    itemFocusModifier(
                        target = focusRestoreTarget,
                        kind = BrowseFocusTargetKind.Album,
                        id = album.id,
                        focusRequester = restoredItemFocusRequester,
                    ),
            )
        }
        items(favoriteTracks, key = { "track-${it.id}" }) { track ->
            FavoriteTrackCard(track = track, onClick = { onPlayTrack(track) })
        }
        item {
            ShowAllCard(onClick = onShowAll, modifier = showAllModifier)
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
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
            ) {
                TuneFlowArtwork(
                    model = track.artUrl,
                    contentDescription = track.title,
                    width = 196.dp,
                    height = 196.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = track.title,
                )
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
    modifier: Modifier = Modifier,
) {
    FocusCard(
        modifier = modifier.width(208.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
            ) {
                TuneFlowArtwork(
                    model = artist.artUrl,
                    contentDescription = artist.name,
                    width = 208.dp,
                    height = 172.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = artist.name,
                )
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
private fun HomeLoadingSection() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(5) {
            AlbumCardSkeleton()
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TuneFlowShapes.panel)
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
    modifier: Modifier = Modifier,
) {
    FocusCard(
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
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
private fun HomePlaylistCard(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(
        modifier = modifier.width(236.dp),
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
                .clip(TuneFlowShapes.artwork)
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
                        TuneFlowArtwork(
                            model = artUrl,
                            contentDescription = playlist.name,
                            width = 58.dp,
                            height = 58.dp,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholderText = playlist.name,
                        )
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
    modifier: Modifier = Modifier,
) {
    FocusCard(
        modifier = modifier.width(208.dp),
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
private fun ShowAllCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(
        modifier = modifier.width(208.dp),
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
                text = "Show all",
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

private fun Modifier.homeBottomBoundaryItem(): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
    }
