@file:Suppress("TooManyFunctions")

package com.tuneflow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.tuneflow.core.design.InitialFocusEffect
import com.tuneflow.core.design.TuneFlowActionSurface
import com.tuneflow.core.design.TuneFlowArtwork
import com.tuneflow.core.design.TuneFlowFocusableCard
import com.tuneflow.core.design.TuneFlowShapes
import com.tuneflow.core.network.AlbumSummary
import com.tuneflow.core.network.ArtistSummary
import com.tuneflow.core.network.FavoritesBundle
import com.tuneflow.core.network.PlaylistSummary
import com.tuneflow.core.network.TrackFavoriteState
import com.tuneflow.core.network.TrackFavoriteStore
import com.tuneflow.core.network.TrackSummary
import com.tuneflow.core.player.PlaybackQueue
import com.tuneflow.feature.browse.BrowseFocusTarget
import com.tuneflow.feature.browse.BrowseFocusTargetKind
import com.tuneflow.feature.browse.HomeCategoryKind
import com.tuneflow.feature.video.VideoHistoryEntry
import kotlinx.coroutines.flow.distinctUntilChanged
import android.view.KeyEvent as AndroidKeyEvent

@Composable
@Suppress("CyclomaticComplexMethod")
fun HomeScreen(
    viewModel: HomeViewModel,
    favoriteStore: TrackFavoriteStore,
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
    onOpenVideoHistory: () -> Unit,
    onPlayVideo: (VideoHistoryEntry) -> Unit,
    onPlayTracks: (List<TrackSummary>, Int) -> Unit,
    preferredVideoServiceUrl: String,
    onPreferredVideoServiceUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteStates by favoriteStore.states.collectAsStateWithLifecycle()
    val homeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val videoHistoryRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val favoritesRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val artistsRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val albumsRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val playlistsRowState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val initialFocusRequester = remember { FocusRequester() }
    val restoredItemFocusRequester = remember { FocusRequester() }
    var showPreferredVideoServiceDialog by rememberSaveable { mutableStateOf(false) }
    val visibleFavorites =
        state.favorites.copy(
            tracks =
                state.favorites.tracks.filter { track ->
                    val favoriteState = favoriteStates[track.id] ?: TrackFavoriteState(isFavorite = false)
                    favoriteState.isFavorite || favoriteState.isPending
                },
        )

    InitialFocusEffect(
        focusRequester = initialFocusRequester,
        targetAvailable = true,
        restorationPending = focusRestoreTarget != null,
    )

    LaunchedEffect(homeListState, viewModel, state.rails) {
        snapshotFlow { homeListState.layoutInfo.visibleItemsInfo.map { it.key } }
            .distinctUntilChanged()
            .collect { keys ->
                HomeCategoryKind.entries.forEach { category ->
                    if ("${category.name.lowercase()}-row" in keys) viewModel.loadRail(category)
                }
            }
    }

    LaunchedEffect(focusRestoreTarget, state) {
        val target = focusRestoreTarget ?: return@LaunchedEffect
        val location = state.copy(favorites = visibleFavorites).focusLocation(target)
        if (location == null) {
            val categories =
                when (target.kind) {
                    BrowseFocusTargetKind.Album -> listOf(HomeCategoryKind.Favorites, HomeCategoryKind.Albums)
                    BrowseFocusTargetKind.Artist -> listOf(HomeCategoryKind.Artists)
                    BrowseFocusTargetKind.Playlist -> listOf(HomeCategoryKind.Playlists)
                    BrowseFocusTargetKind.HomeCategory -> emptyList()
                }
            val pending = categories.filter { !state.rail(it).isLoaded && state.rail(it).error == null }
            pending.forEach(viewModel::loadRail)
            if (pending.isNotEmpty()) return@LaunchedEffect
            runCatching { initialFocusRequester.requestFocus() }
            onFocusRestoreConsumed()
            return@LaunchedEffect
        }
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
        item(key = "hero") {
            HomeHero(
                playbackQueue = playbackQueue,
                onPrimaryAction = if (playbackQueue.items.isNotEmpty()) onOpenNowPlaying else onOpenSearch,
                onSecondaryAction = onOpenAlbums,
                primaryActionModifier = Modifier.focusRequester(initialFocusRequester),
            )
        }

        if (state.videoHistory.isNotEmpty()) {
            item(key = "video-history-heading") { SectionHeading("Recently played videos") }
            item(key = "video-history-row") {
                HomeContentRow(
                    items = state.videoHistory,
                    listState = videoHistoryRowState,
                    key = { _, video -> video.videoHistoryItemKey() },
                    onShowAll = onOpenVideoHistory,
                ) { video ->
                    HomeVideoCard(video = video, onClick = { onPlayVideo(video) })
                }
            }
        }

        item(key = "favorites-heading") { SectionHeading("Favorites") }
        item(key = "favorites-row") {
            FavoriteRail(
                favorites = visibleFavorites,
                listState = favoritesRowState,
                focusRestoreTarget = focusRestoreTarget,
                restoredItemFocusRequester = restoredItemFocusRequester,
                onOpenAlbum = onOpenAlbum,
                onPlayTrack = { track -> onPlayTracks(listOf(track), 0) },
                onShowAll = { onOpenHomeCategory(HomeCategoryKind.Favorites) },
                hasMore = state.rail(HomeCategoryKind.Favorites).hasMore,
                isLoading = state.rail(HomeCategoryKind.Favorites).isLoading,
                onLoadMore = { viewModel.loadMoreRail(HomeCategoryKind.Favorites) },
                showAllModifier =
                    showAllFocusModifier(
                        target = focusRestoreTarget,
                        category = HomeCategoryKind.Favorites,
                        focusRequester = restoredItemFocusRequester,
                    ),
            )
        }

        item(key = "favorites-status") {
            HomeRailFeedback(
                state.rail(HomeCategoryKind.Favorites),
                visibleFavorites.albums.isEmpty() && visibleFavorites.tracks.isEmpty(),
            ) {
                viewModel.loadMoreRail(HomeCategoryKind.Favorites)
            }
        }
        item(key = "artists-heading") { SectionHeading("Artists") }
        item(key = "artists-row") {
            HomeContentRow(
                items = state.artists,
                listState = artistsRowState,
                key = { _, artist -> artist.id },
                onShowAll = { onOpenHomeCategory(HomeCategoryKind.Artists) },
                hasMore = state.rail(HomeCategoryKind.Artists).hasMore,
                isLoading = state.rail(HomeCategoryKind.Artists).isLoading,
                onLoadMore = { viewModel.loadMoreRail(HomeCategoryKind.Artists) },
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

        item(key = "artists-status") {
            HomeRailFeedback(
                state.rail(HomeCategoryKind.Artists),
                state.artists.isEmpty(),
            ) { viewModel.loadMoreRail(HomeCategoryKind.Artists) }
        }
        item(key = "albums-heading") { SectionHeading("Albums") }
        item(key = "albums-row") {
            HomeContentRow(
                items = state.recentAlbums,
                listState = albumsRowState,
                key = { _, album -> album.id },
                onShowAll = { onOpenHomeCategory(HomeCategoryKind.Albums) },
                hasMore = state.rail(HomeCategoryKind.Albums).hasMore,
                isLoading = state.rail(HomeCategoryKind.Albums).isLoading,
                onLoadMore = { viewModel.loadMoreRail(HomeCategoryKind.Albums) },
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

        item(key = "albums-status") {
            HomeRailFeedback(
                state.rail(HomeCategoryKind.Albums),
                state.recentAlbums.isEmpty(),
            ) { viewModel.loadMoreRail(HomeCategoryKind.Albums) }
        }
        item(key = "playlists-heading") { SectionHeading("Playlists") }
        item(key = "playlists-row") {
            HomeContentRow(
                items = state.playlists,
                listState = playlistsRowState,
                key = { _, playlist -> playlist.id },
                onShowAll = { onOpenHomeCategory(HomeCategoryKind.Playlists) },
                hasMore = state.rail(HomeCategoryKind.Playlists).hasMore,
                isLoading = state.rail(HomeCategoryKind.Playlists).isLoading,
                onLoadMore = { viewModel.loadMoreRail(HomeCategoryKind.Playlists) },
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

        item(key = "playlists-status") {
            HomeRailFeedback(
                state.rail(HomeCategoryKind.Playlists),
                state.playlists.isEmpty(),
            ) { viewModel.loadMoreRail(HomeCategoryKind.Playlists) }
        }
        item(key = "quick-actions-heading") { SectionHeading("Quick Actions") }
        item(key = "quick-actions-row") {
            LazyRow(
                modifier = Modifier.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
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
                item {
                    ActionCard(
                        title = if (preferredVideoServiceUrl.isBlank()) "Video Service: Off" else "Video Service",
                        onClick = { showPreferredVideoServiceDialog = true },
                        modifier = Modifier.homeBottomBoundaryItem(),
                    )
                }
            }
        }
    }

    if (showPreferredVideoServiceDialog) {
        PreferredVideoServiceDialog(
            currentUrl = preferredVideoServiceUrl,
            onSave = onPreferredVideoServiceUrlChanged,
            onDismiss = { showPreferredVideoServiceDialog = false },
        )
    }
}

internal fun HomeUiState.focusLocation(target: BrowseFocusTarget): HomeFocusLocation? {
    val sections = focusSections()
    val sectionIndex = sections.indexOfFirst { it.matches(target) }
    if (sectionIndex < 0) return null
    val contentStartIndex = 1 + if (videoHistory.isNotEmpty()) 2 else 0
    val section = sections[sectionIndex]
    return HomeFocusLocation(
        category = section.category,
        sectionRowIndex = contentStartIndex + sectionIndex * 3 + 1,
        rowItemIndex = section.focusItemIndex(target),
    )
}

internal data class HomeFocusLocation(
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
    listOf(
        HomeFocusSection(
            category = HomeCategoryKind.Favorites,
            itemTargets =
                favorites.albums
                    .map { BrowseFocusTarget(BrowseFocusTargetKind.Album, it.id) },
            contentItemCount =
                favorites.albums.size + favorites.tracks.size + if (rail(HomeCategoryKind.Favorites).hasMore) 1 else 0,
        ),
        HomeFocusSection(
            category = HomeCategoryKind.Artists,
            itemTargets =
                artists
                    .map { BrowseFocusTarget(BrowseFocusTargetKind.Artist, it.id) },
            contentItemCount = artists.size + if (rail(HomeCategoryKind.Artists).hasMore) 1 else 0,
        ),
        HomeFocusSection(
            category = HomeCategoryKind.Albums,
            itemTargets =
                recentAlbums
                    .map { BrowseFocusTarget(BrowseFocusTargetKind.Album, it.id) },
            contentItemCount = recentAlbums.size + if (rail(HomeCategoryKind.Albums).hasMore) 1 else 0,
        ),
        HomeFocusSection(
            category = HomeCategoryKind.Playlists,
            itemTargets =
                playlists
                    .map { BrowseFocusTarget(BrowseFocusTargetKind.Playlist, it.id) },
            contentItemCount = playlists.size + if (rail(HomeCategoryKind.Playlists).hasMore) 1 else 0,
        ),
    )

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

@Composable
private fun <T> HomeContentRow(
    items: List<T>,
    listState: LazyListState,
    key: (Int, T) -> Any,
    onShowAll: () -> Unit,
    hasMore: Boolean = false,
    isLoading: Boolean = false,
    onLoadMore: () -> Unit = {},
    showAllModifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    val showAllRequester = remember { FocusRequester() }
    var pagingInitiated by remember { mutableStateOf(false) }
    LaunchedEffect(items.size, hasMore, isLoading) {
        if (pagingInitiated && !isLoading) {
            if (!hasMore) {
                withFrameNanos { }
                runCatching { showAllRequester.requestFocus() }
            }
            pagingInitiated = false
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier.heightIn(min = 300.dp).focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(items, key = key) { _, item ->
            itemContent(item)
        }
        if (hasMore) {
            item(key = "load-more") {
                ShowAllCard(onClick = {
                    pagingInitiated = true
                    onLoadMore()
                }, label = "Load more")
            }
        }
        item(key = "show-all") {
            ShowAllCard(onClick = onShowAll, modifier = showAllModifier.focusRequester(showAllRequester))
        }
    }
}

@Composable
private fun HomeRailFeedback(
    rail: HomeRailUiState,
    isEmpty: Boolean,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.heightIn(min = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rail.error != null) {
            Text(rail.error, color = MaterialTheme.colorScheme.error)
            HeroActionButton(label = "Retry", onClick = onRetry)
        } else if (rail.isLoading) {
            Text(if (rail.isLoaded) "Loading more…" else "Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (rail.isLoaded && isEmpty) {
            Text("No items in this rail.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeHero(
    playbackQueue: PlaybackQueue,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit,
    primaryActionModifier: Modifier = Modifier,
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
                        modifier = primaryActionModifier,
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
    modifier: Modifier = Modifier,
) {
    TuneFlowActionSurface(
        modifier = modifier.width(184.dp),
        accent = accent,
        onClick = onClick,
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
private fun FavoriteRail(
    favorites: FavoritesBundle,
    listState: LazyListState,
    focusRestoreTarget: BrowseFocusTarget?,
    restoredItemFocusRequester: FocusRequester,
    onOpenAlbum: (String) -> Unit,
    onPlayTrack: (TrackSummary) -> Unit,
    onShowAll: () -> Unit,
    hasMore: Boolean,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    showAllModifier: Modifier = Modifier,
) {
    val favoriteAlbums = favorites.albums
    val favoriteTracks = favorites.tracks
    val showAllRequester = remember { FocusRequester() }
    var pagingInitiated by remember { mutableStateOf(false) }
    LaunchedEffect(favoriteAlbums.size + favoriteTracks.size, hasMore, isLoading) {
        if (pagingInitiated && !isLoading) {
            if (!hasMore) {
                withFrameNanos { }
                runCatching { showAllRequester.requestFocus() }
            }
            pagingInitiated = false
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.heightIn(min = 300.dp).focusGroup(),
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
        if (hasMore) {
            item(key = "load-more") {
                ShowAllCard(onClick = {
                    pagingInitiated = true
                    onLoadMore()
                }, label = "Load more")
            }
        }
        item(key = "show-all") {
            ShowAllCard(onClick = onShowAll, modifier = showAllModifier.focusRequester(showAllRequester))
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
private fun HomeVideoCard(
    video: VideoHistoryEntry,
    onClick: () -> Unit,
) {
    FocusCard(
        modifier = Modifier.width(280.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                        .clip(TuneFlowShapes.artwork)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
            ) {
                TuneFlowArtwork(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    width = 256.dp,
                    height = 144.dp,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholderText = video.title,
                )
            }
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = video.publisher,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
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
    label: String = "Show all",
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
                text = label,
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
    TuneFlowFocusableCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(content = content)
    }
}

private fun Modifier.homeBottomBoundaryItem(): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

        event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
    }
