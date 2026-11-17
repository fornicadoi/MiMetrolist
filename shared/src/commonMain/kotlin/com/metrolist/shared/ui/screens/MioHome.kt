/**
 * MiMetrolist: saludo y accesos rápidos del inicio.
 */

package com.metrolist.shared.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_greeting_afternoon
import com.metrolist.shared.generated.resources.mio_greeting_evening
import com.metrolist.shared.generated.resources.mio_greeting_morning
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.model.displayThumbnailUrl
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.SongMetadataState
import com.metrolist.shared.ui.components.PlaylistCoverImage
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.components.localizedBuiltinPlaylist
import com.metrolist.shared.ui.theme.mioCorner
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

internal fun mioGreetingEnabled(): Boolean = Settings.shared.mioStyle && Settings.shared.mioHomeGreeting

internal fun mioShortcutsEnabled(): Boolean = Settings.shared.mioStyle && Settings.shared.mioHomeShortcuts

@Composable
internal fun MioGreetingHeader(modifier: Modifier = Modifier) {
    val hour = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour }
    val greeting =
        when (hour) {
            in 5..11 -> Res.string.mio_greeting_morning
            in 12..18 -> Res.string.mio_greeting_afternoon
            else -> Res.string.mio_greeting_evening
        }
    Text(
        text = stringResource(greeting),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** Cuadrícula de dos columnas con tarjetas horizontales (carátula + título). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MioShortcutGrid(
    items: List<YTItem>,
    onItemClick: (YTItem) -> Unit,
    onItemLongClick: ((YTItem) -> Unit)?,
    currentSongId: State<String?>,
    isItemEnabled: (YTItem) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val shown = items.take(8)
    if (shown.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        shown.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    MioShortcutTile(
                        item = item,
                        isCurrent = currentSongId.value != null && currentSongId.value == item.id,
                        enabled = isItemEnabled(item),
                        onClick = { onItemClick(item) },
                        onLongClick = onItemLongClick?.let { { it(item) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MioShortcutTile(
    item: YTItem,
    isCurrent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(mioCorner(10.dp))
    val displayItem = if (item is PlaylistItem) localizedBuiltinPlaylist(item) else item
    val title = (item as? SongItem)?.let(SongMetadataState::apply)?.title ?: displayItem.title
    Row(
        modifier =
            modifier
                .height(56.dp)
                .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp)) {
            if (displayItem is PlaylistItem) {
                PlaylistCoverImage(displayItem, Modifier.size(56.dp))
            } else {
                AsyncImage(
                    url = displayItem.displayThumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        if (isCurrent) {
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .width(4.dp)
                    .fillMaxHeight(0.4f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
