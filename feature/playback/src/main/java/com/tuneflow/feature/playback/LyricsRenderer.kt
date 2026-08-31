package com.tuneflow.feature.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuneflow.core.design.TuneFlowShapes
import kotlinx.coroutines.launch
import android.view.KeyEvent as AndroidKeyEvent

@Composable
fun LyricsRenderer(
    lyrics: Lyrics,
    positionMs: Long,
    modifier: Modifier = Modifier,
    autoFollow: Boolean = true,
    interactive: Boolean = true,
    onManualScroll: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val activeIndex = resolveActiveLyricLine(lyrics, positionMs)

    LaunchedEffect(activeIndex, autoFollow, lyrics) {
        if (autoFollow && activeIndex != null) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier.onPreviewKeyEvent { event ->
                if (!interactive || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_UP,
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                    -> {
                        onManualScroll()
                        false
                    }
                    AndroidKeyEvent.KEYCODE_PAGE_UP,
                    AndroidKeyEvent.KEYCODE_PAGE_DOWN,
                    -> {
                        onManualScroll()
                        val visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                        val direction = if (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_PAGE_UP) -1 else 1
                        val target =
                            (listState.firstVisibleItemIndex + direction * visibleCount)
                                .coerceIn(0, lyrics.lines.lastIndex)
                        scope.launch { listState.animateScrollToItem(target) }
                        true
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            LyricLineRow(
                text = line.text,
                active = index == activeIndex,
                interactive = interactive,
            )
        }
    }
}

@Composable
private fun LyricLineRow(
    text: String,
    active: Boolean,
    interactive: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    Text(
        text = text,
        modifier =
            Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.hasFocus }
                .then(if (interactive) Modifier.focusable() else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        style =
            if (active || focused) {
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
        color =
            if (active || focused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

@Composable
internal fun LyricsPanel(
    lyrics: Lyrics,
    positionMs: Long,
    onExit: () -> Unit,
) {
    var autoFollow by remember(lyrics) { mutableStateOf(true) }

    Column(
        modifier =
            Modifier
                .width(312.dp)
                .fillMaxHeight()
                .clip(TuneFlowShapes.panel)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    shape = TuneFlowShapes.panel,
                )
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                            onExit()
                            true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP,
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                        AndroidKeyEvent.KEYCODE_PAGE_UP,
                        AndroidKeyEvent.KEYCODE_PAGE_DOWN,
                        -> {
                            autoFollow = false
                            false
                        }
                        else -> false
                    }
                }
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PlaybackTextButton(
                label = "Follow",
                accent = autoFollow,
                onClick = { autoFollow = true },
                modifier = Modifier.width(104.dp).height(44.dp),
                iconResId = R.drawable.ic_follow_lyrics,
                compact = true,
                requestFocus = true,
            )
        }
        LyricsRenderer(
            lyrics = lyrics,
            positionMs = positionMs,
            modifier = Modifier.fillMaxWidth().weight(1f),
            autoFollow = autoFollow,
            interactive = true,
            onManualScroll = { autoFollow = false },
        )
    }
}
