/**
 * MiMetrolist: panel de biblioteca a la izquierda en la versión de escritorio.
 */

package com.metrolist.desktop.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.state.content.LibraryState
import com.metrolist.desktop.state.content.PlaylistAlbumState
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_missing_open
import com.metrolist.shared.generated.resources.mio_missing_open_desc
import com.metrolist.shared.mio.MioUnresolved
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform

@Composable
internal fun MioLibraryPanel(modifier: Modifier = Modifier) {
    val database = remember { KoinPlatform.getKoin().get<MusicDatabase>() }
    val playlists by remember(database) { database.dao().playlistsByNameAsc() }.collectAsState(initial = emptyList())
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(MioPanelColor)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(MetrolistIcons.Outlined.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Tu biblioteca", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            val missing = MioUnresolved.songs.size
            if (missing > 0) {
                item(key = "missing") {
                    PanelRow(
                        title = stringResource(Res.string.mio_missing_open, missing),
                        subtitle = stringResource(Res.string.mio_missing_open_desc),
                        gradient = listOf(Color(0xFFDC2626), Color(0xFF7F1D1D)),
                        icon = MetrolistIcons.Default.QuestionMark,
                    ) { MioUnresolved.open = true }
                }
            }
            item(key = "liked") {
                PanelRow(
                    title = "Tus me gusta",
                    subtitle = "Lista automática",
                    gradient = listOf(Color(0xFF4F46E5), Color(0xFF9CA3AF)),
                    icon = MetrolistIcons.Filled.Favorite,
                ) { LibraryState.navigateToAutoPlaylist("liked") }
            }
            item(key = "downloaded") {
                PanelRow(
                    title = "Descargado",
                    subtitle = "Lista automática",
                    gradient = listOf(Color(0xFF059669), Color(0xFF064E3B)),
                    icon = MetrolistIcons.Outlined.Download,
                ) { LibraryState.navigateToAutoPlaylist("downloaded") }
            }
            items(playlists, key = { it.id }) { playlist ->
                PanelRow(
                    title = playlist.title,
                    subtitle = "Lista • ${playlist.songCount} canciones",
                    thumbnail = playlist.playlist.thumbnailUrl,
                ) { PlaylistAlbumState.fetchPlaylistData(playlist.id) }
            }
        }
    }
}

@Composable
private fun PanelRow(
    title: String,
    subtitle: String,
    thumbnail: String? = null,
    gradient: List<Color>? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (gradient != null) Brush.linearGradient(gradient) else Brush.linearGradient(listOf(Color(0xFF282828), Color(0xFF282828)))),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                AsyncImage(url = thumbnail, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Icon(MetrolistIcons.Outlined.MusicNote, null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color(0xFFB3B3B3), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
