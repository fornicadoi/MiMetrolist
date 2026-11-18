/**
 * MiMetrolist: modo carro. Pantalla completa con botones gigantes para usar la app manejando.
 */

package com.metrolist.android.mio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.android.ui.theme.LocalMioSongColor
import com.metrolist.shared.model.previewThumbnailUrl
import com.metrolist.shared.settings.displayName
import com.metrolist.shared.state.SongLikeState
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.android.playback.LocalPlayerConnection

object MioCarModeState {
    var visible by mutableStateOf(false)
}

@Composable
fun MioCarMode(onDismiss: () -> Unit) {
    val connection = LocalPlayerConnection.current ?: return onDismiss()
    val song by connection.currentSong.collectAsState(initial = null)
    val isPlaying by connection.isPlaying.collectAsState(initial = false)
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val background = LocalMioSongColor.current?.let { lerp(it, Color.Black, 0.7f) } ?: Color(0xFF0B0B0B)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(background).safeDrawingPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(MetrolistIcons.Outlined.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(10.dp))
                Text("Modo carro", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(56.dp)) {
                    Icon(MetrolistIcons.Default.Close, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            val current = song
            if (current != null) {
                AsyncImage(
                    url = current.previewThumbnailUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f, fill = false).aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    current.title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    current.artists.joinToString { it.displayName },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BigButton(onClick = { connection.skipToPrevious() }) {
                    Icon(MetrolistIcons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
                Box(
                    Modifier.size(120.dp).clip(CircleShape).background(Color.White).clickable { connection.togglePlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isPlaying) MetrolistIcons.Filled.Pause else MetrolistIcons.Filled.PlayArrow,
                        null,
                        tint = Color.Black,
                        modifier = Modifier.size(72.dp),
                    )
                }
                BigButton(onClick = { connection.skipToNext() }) {
                    Icon(MetrolistIcons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            if (current != null) {
                val liked = SongLikeState.isLiked(current.id)
                BigButton(onClick = { SongLikeState.toggleLike(current) }) {
                    Icon(
                        if (liked) MetrolistIcons.Filled.Favorite else MetrolistIcons.Outlined.FavoriteBorder,
                        null,
                        tint = if (liked) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BigButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
