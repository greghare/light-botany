package com.thelightphone.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightColors
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIconConfiguration
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightSurfaceScheme
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBarButton
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

enum class BibleTab { Read, Search, Highlights, Settings }

@Composable
fun BibleBottomBar(
    active: BibleTab,
    onRead: () -> Unit,
    onSearch: () -> Unit,
    onHighlights: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.9f.gridUnitsAsDp(), bottom = 0.8f.gridUnitsAsDp()),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BibleNavIcon(LightIcons.BOOK_OPEN, "Read", active == BibleTab.Read, onRead)
        BibleNavIcon(LightIcons.SEARCH, "Search", active == BibleTab.Search, onSearch)
        BibleNavIcon(LightIcons.STAR, "Highlights", active == BibleTab.Highlights, onHighlights)
        BibleNavIcon(LightIcons.SETTINGS, "Settings", active == BibleTab.Settings, onSettings)
    }
}

@Composable
private fun BibleNavIcon(
    icon: LightIconConfiguration,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LightThemeTokens.colors
    val scheme = LightThemeTokens.surfaceScheme
    val typography = LightThemeTokens.typography
    val tintedColors = if (active) colors else mutedColors(colors, scheme)
    LightTheme(colors = tintedColors, typography = typography, surfaceScheme = scheme) {
        LightIcon(
            icon = icon,
            size = 2f,
            contentDescription = description,
            modifier = Modifier.lightClickable(onClick = onClick),
        )
    }
}

private fun mutedColors(colors: LightColors, scheme: LightSurfaceScheme): LightColors {
    val alpha = if (scheme == LightSurfaceScheme.Dark) 0.4f else 0.38f
    return colors.copy(content = colors.content.copy(alpha = alpha))
}

/** A pause button for a [LightTopBar]'s rightButton slot, shown on any screen while chapter
 *  audio is playing so it can be paused without navigating back to the Reading Screen. Returns
 *  null (no button) when nothing is playing. */
@Composable
fun globalAudioTopBarButton(repository: BibleRepository): LightTopBarButton? {
    val audioState by repository.audioState.collectAsState()
    if (!audioState.isPlaying) return null
    return LightBarButton.LightIcon(
        icon = LightIcons.PAUSE,
        contentDescription = "Pause chapter audio",
        onClick = { repository.toggleAudioPlayPause() },
        sizeUnits = 1.4f,
    )
}

@Composable
fun VerseActionFlyout(
    selectedCount: Int,
    allHighlighted: Boolean,
    onCancel: () -> Unit,
    onToggleHighlight: () -> Unit,
) {
    val colors = LightThemeTokens.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.content.copy(alpha = 0.14f)),
        )
        LightText(
            text = if (selectedCount == 1) "1 verse selected" else "$selectedCount verses selected",
            variant = LightTextVariant.Detail,
            lighten = true,
            align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.5f.gridUnitsAsDp()),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.3f.gridUnitsAsDp(), bottom = 0.6f.gridUnitsAsDp()),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            FlyoutButton(icon = LightIcons.CLOSE, label = "Cancel", onClick = onCancel)
            FlyoutButton(
                icon = if (allHighlighted) LightIcons.STAR else LightIcons.STAR_OUTLINE,
                label = if (allHighlighted) "Remove" else "Highlight",
                onClick = onToggleHighlight,
            )
        }
    }
}

@Composable
private fun FlyoutButton(icon: LightIconConfiguration, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .lightClickable(onClick = onClick)
            .padding(0.3f.gridUnitsAsDp()),
    ) {
        LightIcon(icon = icon, size = 1.4f)
        LightText(
            text = label,
            variant = LightTextVariant.Fine,
            modifier = Modifier.padding(top = 0.2f.gridUnitsAsDp()),
        )
    }
}

@Composable
fun AudioPlayerFlyout(
    state: AudioPlayerState,
    onTogglePlayPause: () -> Unit,
    onSkipBack15: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LightThemeTokens.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.content.copy(alpha = 0.14f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 1f.gridUnitsAsDp())
                .padding(top = 0.5f.gridUnitsAsDp()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val label = if (state.book != null && state.chapter != null) {
                "${state.book.name} ${state.chapter}"
            } else {
                "Chapter Audio"
            }
            LightText(text = label, variant = LightTextVariant.Detail, lighten = true)
            LightIcon(
                icon = LightIcons.CLOSE,
                size = 1.1f,
                contentDescription = "Close audio player",
                modifier = Modifier.lightClickable(onClick = onClose),
            )
        }

        if (state.error != null) {
            LightText(
                text = state.error,
                variant = LightTextVariant.Detail,
                align = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.6f.gridUnitsAsDp(), horizontal = 1f.gridUnitsAsDp()),
            )
        } else {
            AudioProgressBar(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                onSeek = onSeek,
                modifier = Modifier
                    .padding(horizontal = 1f.gridUnitsAsDp())
                    .padding(top = 0.6f.gridUnitsAsDp()),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 1f.gridUnitsAsDp())
                    .padding(top = 0.2f.gridUnitsAsDp()),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                LightText(text = formatMillis(state.positionMs), variant = LightTextVariant.Fine, lighten = true)
                LightText(text = formatMillis(state.durationMs), variant = LightTextVariant.Fine, lighten = true)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.3f.gridUnitsAsDp(), bottom = 0.6f.gridUnitsAsDp()),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlyoutButton(icon = LightIcons.SKIP_BACKWARD_FIFTEEN, label = "15s", onClick = onSkipBack15)
            if (state.isLoading) {
                LightText(text = "Loading…", variant = LightTextVariant.Detail, lighten = true)
            } else {
                FlyoutButton(
                    icon = if (state.isPlaying) LightIcons.PAUSE else LightIcons.PLAY,
                    label = if (state.isPlaying) "Pause" else "Play",
                    onClick = onTogglePlayPause,
                )
            }
        }
    }
}

@Composable
private fun AudioProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LightThemeTokens.colors
    var trackWidthPx by remember { mutableStateOf(0) }
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    fun seekToOffsetX(offsetX: Float) {
        if (durationMs <= 0 || trackWidthPx <= 0) return
        val ratio = (offsetX / trackWidthPx).coerceIn(0f, 1f)
        onSeek((ratio * durationMs).toLong())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5f.gridUnitsAsDp())
            .onSizeChanged { trackWidthPx = it.width }
            .background(colors.content.copy(alpha = 0.14f))
            .pointerInput(durationMs) {
                detectTapGestures(onTap = { offset -> seekToOffsetX(offset.x) })
            }
            .pointerInput(durationMs) {
                detectDragGestures(onDrag = { change, _ ->
                    change.consume()
                    seekToOffsetX(change.position.x)
                })
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(colors.content),
        )
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}


