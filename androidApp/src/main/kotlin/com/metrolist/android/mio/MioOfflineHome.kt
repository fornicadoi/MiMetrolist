/**
 * MiMetrolist: inicio sin conexión. No carga nada de internet; solo muestra lo descargado.
 */

package com.metrolist.android.mio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.android.LocalMiniPlayerBottomInset
import com.metrolist.android.playback.LocalPlayerConnection
import com.metrolist.android.ui.component.tabContentTopPadding
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.entities.toSongItem
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_offline_downloaded_songs
import com.metrolist.shared.generated.resources.mio_offline_empty
import com.metrolist.shared.generated.resources.mio_offline_liked
import com.metrolist.shared.generated.resources.mio_offline_play_all
import com.metrolist.shared.generated.resources.mio_offline_playlists
import com.metrolist.shared.generated.resources.mio_offline_shuffle
import com.metrolist.shared.generated.resources.mio_offline_song_count
import com.metrolist.shared.generated.resources.mio_offline_text
import com.metrolist.shared.generated.resources.mio_offline_title
import com.metrolist.shared.storage.DownloadedPlaylists
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.youtube.playback.PlaybackNetworkStateStore
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

private data class MioOfflinePlaylist(
    val id: String,
    val name: String,
    val cover: String?,
)

/** True once the connection has been gone for a moment, so a short drop does not swap the screen. */
@Composable
fun rememberMioOffline(): Boolean {
    val network by PlaybackNetworkStateStore.state.collectAsState()
    var offline by remember { mutableStateOf(!network.connected) }
    LaunchedEffect(network.connected) {
        if (network.connected) {
            offline = false
        } else {
            delay(3.seconds)
            offline = true
        }
    }
    return offline
}

@Composable
fun MioOfflineHome(
    onOpenPlaylist: (String) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val database: MusicDatabase = koinInject()
    val playerConnection = LocalPlayerConnection.current
    val downloaded by remember { database.dao().downloadedSongsByCreateDateAsc() }.collectAsState(emptyList())
    val songs = remember(downloaded) { downloaded.asReversed().map { it.toSongItem() } }
    var playlists by remember { mutableStateOf<List<MioOfflinePlaylist>>(emptyList()) }
    val tracked = remember { DownloadedPlaylists.ids() }
    LaunchedEffect(tracked) {
        playlists =
            tracked.filter { it != "LM" }.mapNotNull { id ->
                database.dao().findPlaylistEntity(id)?.let { MioOfflinePlaylist(id, it.name, it.thumbnailUrl) }
            }.sortedBy { it.name.lowercase() }
    }
    val downloadedTitle = stringResource(Res.string.mio_offline_downloaded_songs)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(start = 16.dp, end = 16.dp, top = tabContentTopPadding() + 8.dp, bottom = LocalMiniPlayerBottomInset.current + 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), MaterialTheme.colorScheme.surfaceContainerHigh),
                            ),
                        ).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(MetrolistIcons.Outlined.OfflinePin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(Res.string.mio_offline_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(Res.string.mio_offline_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (songs.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { playerConnection?.playQueue(songs, 0, downloadedTitle, finite = true) }) {
                                Icon(MetrolistIcons.Filled.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.mio_offline_play_all))
                            }
                            FilledTonalButton(onClick = { playerConnection?.shuffleQueue(songs, downloadedTitle, finite = true) }) {
                                Icon(MetrolistIcons.Filled.Shuffle, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(Res.string.mio_offline_shuffle))
                            }
                        }
                    }
                }
            }
        }
        if (songs.isEmpty() && playlists.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(Res.string.mio_offline_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
            return@LazyColumn
        }
        item(key = "downloads") {
            OfflineRow(
                title = downloadedTitle,
                subtitle = pluralStringResource(Res.plurals.mio_offline_song_count, songs.size, songs.size),
                cover = songs.firstOrNull()?.thumbnail,
                icon = MetrolistIcons.Outlined.Download,
                onClick = onOpenDownloads,
            )
        }
        if ("LM" in tracked) {
            item(key = "liked") {
                OfflineRow(stringResource(Res.string.mio_offline_liked), null, null, MetrolistIcons.Filled.Favorite, onOpenLiked)
            }
        }
        if (playlists.isNotEmpty()) {
            item(key = "playlists-title") {
                Text(
                    stringResource(Res.string.mio_offline_playlists),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                OfflineRow(playlist.name, null, playlist.cover, MetrolistIcons.AutoMirrored.Outlined.QueueMusic) { onOpenPlaylist(playlist.id) }
            }
        }
    }
}

@Composable
private fun OfflineRow(
    title: String,
    subtitle: String?,
    cover: String?,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (cover != null) AsyncImage(url = cover, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
