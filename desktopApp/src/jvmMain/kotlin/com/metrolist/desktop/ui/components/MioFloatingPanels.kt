/**
 * MiMetrolist: cola y letras en ventanas flotantes que se pueden mover,
 * cambiar de tamaño, dejar encima de otras aplicaciones o convertir en burbuja.
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.metrolist.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.metrolist.desktop.state.app.UiState
import com.metrolist.desktop.state.playback.LyricsState
import com.metrolist.desktop.state.playback.PlayerState
import com.metrolist.desktop.ui.DesktopDensityProvider
import com.metrolist.desktop.window.WindowVisibility
import com.metrolist.desktop.window.WindowsMiniPlayerWindowFrame
import com.metrolist.shared.api.lyrics.LyricsUtils
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.close
import com.metrolist.shared.generated.resources.lyrics
import com.metrolist.shared.generated.resources.mio_float_bubble
import com.metrolist.shared.generated.resources.mio_float_detach
import com.metrolist.shared.generated.resources.mio_float_dock
import com.metrolist.shared.generated.resources.mio_float_expand
import com.metrolist.shared.generated.resources.mio_float_pin
import com.metrolist.shared.generated.resources.mio_float_resize
import com.metrolist.shared.generated.resources.queue
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.components.lyrics.rememberCaptionLyricsPositionState
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.utils.OsUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import java.awt.Cursor
import java.awt.MouseInfo

enum class MioPanelKind { QUEUE, LYRICS }

private val QueueBubbleSize = DpSize(76.dp, 76.dp)
private val LyricsBubbleSize = DpSize(420.dp, 76.dp)
private const val MIN_FLOAT_WIDTH = 260
private const val MIN_FLOAT_HEIGHT = 220

internal object MioFloatingPanels {
    fun isFloating(kind: MioPanelKind): Boolean =
        when (kind) {
            MioPanelKind.QUEUE -> Settings.shared.mioQueueFloating
            MioPanelKind.LYRICS -> Settings.shared.mioLyricsFloating
        }

    fun setFloating(
        kind: MioPanelKind,
        floating: Boolean,
    ) {
        when (kind) {
            MioPanelKind.QUEUE -> Settings.shared.mioQueueFloating = floating
            MioPanelKind.LYRICS -> Settings.shared.mioLyricsFloating = floating
        }
        if (!floating) setBubble(kind, false)
    }

    fun isBubble(kind: MioPanelKind): Boolean =
        when (kind) {
            MioPanelKind.QUEUE -> Settings.shared.mioQueueBubble
            MioPanelKind.LYRICS -> Settings.shared.mioLyricsBubble
        }

    fun setBubble(
        kind: MioPanelKind,
        bubble: Boolean,
    ) {
        when (kind) {
            MioPanelKind.QUEUE -> Settings.shared.mioQueueBubble = bubble
            MioPanelKind.LYRICS -> Settings.shared.mioLyricsBubble = bubble
        }
    }

    fun close(kind: MioPanelKind) {
        when (kind) {
            MioPanelKind.QUEUE -> UiState.showPersistentPlayerDrawer = false
            MioPanelKind.LYRICS -> UiState.showLyricsDrawer = false
        }
    }

    internal fun savedBounds(kind: MioPanelKind): String =
        when (kind) {
            MioPanelKind.QUEUE -> Settings.shared.mioQueueWindow
            MioPanelKind.LYRICS -> Settings.shared.mioLyricsWindow
        }

    internal fun saveBounds(
        kind: MioPanelKind,
        value: String,
    ) {
        when (kind) {
            MioPanelKind.QUEUE -> Settings.shared.mioQueueWindow = value
            MioPanelKind.LYRICS -> Settings.shared.mioLyricsWindow = value
        }
    }
}

/** Saved as "x;y;width;height" in window dp; width and height belong to the expanded window. */
internal data class MioFloatBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    fun encode(): String = "$x;$y;$width;$height"

    companion object {
        fun parse(value: String): MioFloatBounds? {
            val parts = value.split(';').mapNotNull { it.toFloatOrNull() }
            if (parts.size != 4) return null
            return MioFloatBounds(parts[0], parts[1], parts[2], parts[3])
        }
    }
}

/** Button shown in the docked panel headers to move the panel into its own window. */
@Composable
internal fun RowScope.MioDetachButton(
    kind: MioPanelKind,
    tint: Color,
) {
    IconButton(
        onClick = { MioFloatingPanels.setFloating(kind, true) },
        modifier = Modifier.size(36.dp),
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(
            MetrolistIcons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(Res.string.mio_float_detach),
            modifier = Modifier.size(18.dp),
            tint = tint,
        )
    }
}

@Composable
fun MioFloatingPanelWindows() {
    if (PlayerState.currentTrack == null) return
    if (UiState.showPersistentPlayerDrawer && Settings.shared.mioQueueFloating) {
        MioFloatingPanelWindow(MioPanelKind.QUEUE)
    }
    if (UiState.showLyricsDrawer && Settings.shared.mioLyricsFloating) {
        MioFloatingPanelWindow(MioPanelKind.LYRICS)
    }
}

@Composable
private fun MioFloatingPanelWindow(kind: MioPanelKind) {
    val saved = remember(kind) { MioFloatBounds.parse(MioFloatingPanels.savedBounds(kind)) }
    val expandedSize = remember(kind) { DpSize((saved?.width ?: 380f).dp, (saved?.height ?: 560f).dp) }
    val bubble = MioFloatingPanels.isBubble(kind)
    val bubbleSize = if (kind == MioPanelKind.QUEUE) QueueBubbleSize else LyricsBubbleSize
    val windowState =
        remember(kind) {
            WindowState(
                position = saved?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.Aligned(Alignment.CenterEnd),
                size = if (bubble) bubbleSize else expandedSize,
            )
        }

    val lastExpanded = remember(kind) { arrayOf(expandedSize) }
    LaunchedEffect(bubble) {
        windowState.size = if (bubble) bubbleSize else lastExpanded[0]
    }
    LaunchedEffect(windowState) {
        snapshotFlow { windowState.position to windowState.size }.collectLatest { (position, size) ->
            delay(400)
            if (position !is WindowPosition.Absolute) return@collectLatest
            if (!MioFloatingPanels.isBubble(kind) && size.width.value >= MIN_FLOAT_WIDTH) lastExpanded[0] = size
            val expanded = lastExpanded[0]
            MioFloatingPanels.saveBounds(
                kind,
                MioFloatBounds(position.x.value, position.y.value, expanded.width.value, expanded.height.value).encode(),
            )
        }
    }

    val title = stringResource(if (kind == MioPanelKind.QUEUE) Res.string.queue else Res.string.lyrics)
    Window(
        onCloseRequest = { MioFloatingPanels.close(kind) },
        title = title,
        resizable = false,
        undecorated = !OsUtil.isWindows,
        transparent = false,
        state = windowState,
        alwaysOnTop = Settings.shared.mioFloatOnTop,
    ) {
        DesktopDensityProvider {
            WindowVisibility(window) {
                if (OsUtil.isWindows) {
                    WindowsMiniPlayerWindowFrame(
                        onCloseRequest = { MioFloatingPanels.close(kind) },
                        state = windowState,
                        darkTheme = true,
                    ) {
                        MioFloatingBody(kind, title, bubble)
                    }
                } else {
                    MioFloatingBody(kind, title, bubble)
                }
            }
        }
    }
}

@Composable
private fun FrameWindowScope.MioFloatingBody(
    kind: MioPanelKind,
    title: String,
    bubble: Boolean,
) {
    if (bubble) {
        MioBubble(kind)
        return
    }
    val colorScheme = MaterialTheme.colorScheme
    val background = if (Settings.shared.mioStyle) Color(0xFF121212) else colorScheme.surface
    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color.Black)
                        .mioWindowGesture(window, resize = false)
                        .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    MetrolistIcons.Default.DragIndicator,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.5f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                MioHeaderButton(
                    MetrolistIcons.Filled.PushPin,
                    stringResource(Res.string.mio_float_pin),
                    if (Settings.shared.mioFloatOnTop) colorScheme.primary else Color.White.copy(alpha = 0.45f),
                ) { Settings.shared.mioFloatOnTop = !Settings.shared.mioFloatOnTop }
                MioHeaderButton(
                    MetrolistIcons.Filled.PictureInPicture,
                    stringResource(Res.string.mio_float_bubble),
                    Color.White.copy(alpha = 0.85f),
                ) { MioFloatingPanels.setBubble(kind, true) }
                MioHeaderButton(
                    MetrolistIcons.Outlined.FullscreenExit,
                    stringResource(Res.string.mio_float_dock),
                    Color.White.copy(alpha = 0.85f),
                ) { MioFloatingPanels.setFloating(kind, false) }
                MioHeaderButton(
                    MetrolistIcons.Default.Close,
                    stringResource(Res.string.close),
                    Color.White.copy(alpha = 0.85f),
                ) { MioFloatingPanels.close(kind) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (kind) {
                    MioPanelKind.QUEUE -> {
                        DesktopQueueContent(
                            onDismiss = { MioFloatingPanels.close(kind) },
                            colorScheme = colorScheme,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    MioPanelKind.LYRICS -> {
                        DesktopLyricsContent(
                            onClose = { MioFloatingPanels.close(kind) },
                            colorScheme = colorScheme,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                val resizeCursor = remember { PointerIcon(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)) }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .pointerHoverIcon(resizeCursor)
                            .mioWindowGesture(window, resize = true),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        MetrolistIcons.Default.DragIndicator,
                        contentDescription = stringResource(Res.string.mio_float_resize),
                        modifier = Modifier.size(14.dp),
                        tint = Color.White.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MioHeaderButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp), shapes = IconButtonDefaults.shapes()) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(16.dp), tint = tint)
    }
}

@Composable
private fun FrameWindowScope.MioBubble(kind: MioPanelKind) {
    val expand = remember(kind) { { MioFloatingPanels.setBubble(kind, false) } }
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(if (OsUtil.isWindows) 0.dp else 38.dp))
                .mioWindowGesture(window, resize = false, onClick = expand),
    ) {
        when (kind) {
            MioPanelKind.QUEUE -> {
                AsyncImage(
                    url = PlayerState.currentArtworkUrl ?: "",
                    artworkPreview = PlayerState.artworkPreview,
                    modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(accent, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        MetrolistIcons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = stringResource(Res.string.mio_float_expand),
                        modifier = Modifier.size(14.dp),
                        tint = Color.Black,
                    )
                }
            }

            MioPanelKind.LYRICS -> {
                Row(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        url = PlayerState.currentArtworkUrl ?: "",
                        artworkPreview = PlayerState.artworkPreview,
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(30.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                    MioBubbleLyricLine(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun MioBubbleLyricLine(modifier: Modifier) {
    val track = PlayerState.currentTrack
    LaunchedEffect(track?.id, Settings.shared.lyricsProviderOrder, Settings.shared.lyricsEnabledProviders) {
        track?.let { LyricsState.fetchLyrics(it) }
    }
    val lyrics = LyricsState.currentLyricsEntries
    val castState by PlayerState.castManager.state.collectAsState()
    val position =
        rememberCaptionLyricsPositionState(
            lyrics = lyrics,
            currentPositionFlow = PlayerState.playbackPosition,
            positionReader = PlayerState::currentMediaPositionMs,
            playbackSpeed =
                PlayerState.player.playbackSpeed
                    .collectAsState()
                    .value,
            isPlaying = PlayerState.isPlaying,
            songId = track?.id,
            timingOffsetMs = LyricsState.currentLyricsOffsetMs,
            isCasting = castState.isConnected,
            isBuffering = castState.isBuffering,
        )
    val timed = remember(lyrics) { LyricsUtils.hasPlaybackTiming(lyrics) }
    val line by remember(lyrics, position, timed) {
        derivedStateOf(structuralEqualityPolicy()) {
            if (!timed) null else lyrics.getOrNull(LyricsUtils.findCurrentLineIndex(lyrics, position.value))?.text
        }
    }
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Text(
            text = line?.takeIf { it.isNotBlank() } ?: track?.title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Moves (or resizes) the AWT window with the pointer. Uses screen coordinates because the
 * window itself moves under the pointer while dragging. A press without movement is a click.
 */
private fun Modifier.mioWindowGesture(
    window: java.awt.Window,
    resize: Boolean,
    onClick: (() -> Unit)? = null,
): Modifier =
    pointerInput(window, resize, onClick) {
        awaitPointerEventScope {
            while (true) {
                awaitFirstDown(requireUnconsumed = true)
                if (!currentEvent.buttons.isPrimaryPressed) continue
                val start = MouseInfo.getPointerInfo()?.location ?: continue
                val origin = window.location
                val originSize = window.size
                var moved = false
                while (true) {
                    val event = awaitPointerEvent()
                    if (!event.buttons.isPrimaryPressed) break
                    val pointer = MouseInfo.getPointerInfo()?.location ?: continue
                    val dx = pointer.x - start.x
                    val dy = pointer.y - start.y
                    if (!moved && dx * dx + dy * dy < 16) continue
                    moved = true
                    if (resize) {
                        window.setSize(
                            maxOf(MIN_FLOAT_WIDTH, originSize.width + dx),
                            maxOf(MIN_FLOAT_HEIGHT, originSize.height + dy),
                        )
                    } else {
                        window.setLocation(origin.x + dx, origin.y + dy)
                    }
                    event.changes.forEach { it.consume() }
                }
                if (!moved) onClick?.invoke()
            }
        }
    }
