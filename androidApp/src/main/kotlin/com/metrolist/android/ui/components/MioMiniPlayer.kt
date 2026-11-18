/**
 * MiMetrolist: reproductor pequeño en forma de tarjeta teñida con el color de la carátula.
 */

package com.metrolist.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.android.ui.theme.LocalMioSongColor
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.previewThumbnailUrl
import com.metrolist.shared.settings.MioMiniPlayer
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.settings.displayName
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.icons.MetrolistIcons

internal val MioMiniPlayerHeight = 64.dp

internal fun mioCardMiniPlayerEnabled(useDesktopLayout: Boolean): Boolean =
    !useDesktopLayout && Settings.shared.mioStyle && Settings.shared.mioMiniPlayer == MioMiniPlayer.TARJETA

@Composable
internal fun MioMiniPlayerCard(
    song: SongItem,
    isPlaying: Boolean,
    isLoading: Boolean,
    progress: () -> Float,
    showProgress: Boolean,
    isLiked: Boolean,
    onLike: () -> Unit,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val songColor = LocalMioSongColor.current
    // Oscurece el color de la carátula para que el texto blanco siempre se lea.
    val target = songColor?.let { lerp(it, Color.Black, 0.55f) } ?: base
    val background by animateColorAsState(target, label = "mioMiniPlayerColor")
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MioMiniPlayerHeight)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(shape)
                .background(background)
                .clickable(onClick = onExpand),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                url = song.previewThumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
            )
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artists.joinToString { it.displayName },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onLike) {
                Icon(
                    imageVector = if (isLiked) MetrolistIcons.Filled.Favorite else MetrolistIcons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    IconButton(onClick = onTogglePlay) {
                        Icon(
                            imageVector = if (isPlaying) MetrolistIcons.Filled.Pause else MetrolistIcons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        }
        if (showProgress) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress().coerceIn(0f, 1f))
                        .background(Color.White),
                )
            }
        }
    }
}
