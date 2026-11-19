/**
 * MiMetrolist: revisar las listas importadas contra la original y mantenerlas al día.
 */

package com.metrolist.shared.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_link_import_hint
import com.metrolist.shared.generated.resources.mio_missing_close
import com.metrolist.shared.generated.resources.mio_sync_auto
import com.metrolist.shared.generated.resources.mio_sync_auto_desc
import com.metrolist.shared.generated.resources.mio_sync_check
import com.metrolist.shared.generated.resources.mio_sync_check_all
import com.metrolist.shared.generated.resources.mio_sync_checking
import com.metrolist.shared.generated.resources.mio_sync_empty
import com.metrolist.shared.generated.resources.mio_sync_failed
import com.metrolist.shared.generated.resources.mio_sync_link_save
import com.metrolist.shared.generated.resources.mio_sync_linked
import com.metrolist.shared.generated.resources.mio_sync_needs_link
import com.metrolist.shared.generated.resources.mio_sync_paste_link
import com.metrolist.shared.generated.resources.mio_sync_result
import com.metrolist.shared.generated.resources.mio_sync_see_missing
import com.metrolist.shared.generated.resources.mio_sync_subtitle
import com.metrolist.shared.generated.resources.mio_sync_title
import com.metrolist.shared.generated.resources.mio_sync_up_to_date
import com.metrolist.shared.mio.MioImportedPlaylist
import com.metrolist.shared.mio.MioPlaylistSync
import com.metrolist.shared.mio.MioSyncPhase
import com.metrolist.shared.mio.MioSyncStatus
import com.metrolist.shared.mio.MioUnresolved
import com.metrolist.shared.playlistimport.ImportSourceType
import com.metrolist.shared.playlistimport.UnifiedPlaylistImporter
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

private val SyncGreen = Color(0xFF2FD66B)

/** [inWindow] draws the review over the desktop window; [fullScreen] lets a platform supply an edge-to-edge container. */
@Composable
fun MioSyncHost(
    inWindow: Boolean = false,
    fullScreen: (@Composable (onDismiss: () -> Unit, content: @Composable () -> Unit) -> Unit)? = null,
) {
    if (!MioPlaylistSync.reviewOpen) return
    if (inWindow) {
        MioSyncContent()
    } else if (fullScreen != null) {
        fullScreen({ MioPlaylistSync.reviewOpen = false }) { MioSyncContent() }
    } else {
        Dialog(
            onDismissRequest = { MioPlaylistSync.reviewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) { MioSyncContent() }
    }
}

@Composable
private fun MioSyncContent() {
    var playlists by remember { mutableStateOf<List<MioImportedPlaylist>?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    // Reload when a check finishes, since it may have linked playlists by name.
    val finished = MioPlaylistSync.status.values.count { it.phase != MioSyncPhase.CHECKING }
    LaunchedEffect(refresh, finished, MioPlaylistSync.checkingAll) {
        playlists = MioPlaylistSync.importedPlaylists()
    }
    Surface(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.mio_sync_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.mio_sync_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { MioPlaylistSync.reviewOpen = false }) {
                        Icon(MetrolistIcons.Default.Close, stringResource(Res.string.mio_missing_close))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.mio_sync_auto), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(Res.string.mio_sync_auto_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = Settings.shared.mioAutoSyncPlaylists, onCheckedChange = { Settings.shared.mioAutoSyncPlaylists = it })
                }
                val list = playlists
                when {
                    list == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    list.isEmpty() ->
                        Text(
                            stringResource(Res.string.mio_sync_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                        )
                    else ->
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(list, key = { it.playlist.id }) { item ->
                                SyncRow(item, MioPlaylistSync.status[item.playlist.id] ?: MioSyncStatus()) { refresh++ }
                            }
                        }
                }
                if (!list.isNullOrEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { MioPlaylistSync.checkAll() },
                            enabled = !MioPlaylistSync.checkingAll,
                            colors = ButtonDefaults.buttonColors(containerColor = SyncGreen, contentColor = Color.Black),
                        ) {
                            if (MioPlaylistSync.checkingAll) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(stringResource(Res.string.mio_sync_check_all), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncRow(
    item: MioImportedPlaylist,
    status: MioSyncStatus,
    onLinked: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.playlist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        item.provider.displayName + " · " +
                            stringResource(if (item.link != null) Res.string.mio_sync_linked else Res.string.mio_sync_needs_link),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.link != null) SyncGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val line =
                        when (status.phase) {
                            MioSyncPhase.CHECKING -> stringResource(Res.string.mio_sync_checking)
                            MioSyncPhase.DONE ->
                                if (status.added == 0 && status.missing == 0) {
                                    stringResource(Res.string.mio_sync_up_to_date)
                                } else {
                                    stringResource(Res.string.mio_sync_result, status.added, status.missing)
                                }
                            MioSyncPhase.FAILED -> stringResource(Res.string.mio_sync_failed)
                            MioSyncPhase.NEEDS_LINK -> stringResource(Res.string.mio_sync_paste_link)
                            MioSyncPhase.IDLE -> null
                        }
                    if (line != null) {
                        Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (status.phase == MioSyncPhase.CHECKING) {
                    CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(20.dp), strokeWidth = 2.dp)
                } else if (item.link == null) {
                    TextButton(onClick = { editing = !editing }) { Text(stringResource(Res.string.mio_sync_paste_link)) }
                } else {
                    TextButton(onClick = { MioPlaylistSync.checkOne(item.playlist.id) }) { Text(stringResource(Res.string.mio_sync_check)) }
                }
            }
            if (status.phase == MioSyncPhase.DONE && status.missing > 0) {
                TextButton(onClick = {
                    MioPlaylistSync.reviewOpen = false
                    MioUnresolved.open = true
                }) { Text(stringResource(Res.string.mio_sync_see_missing, status.missing)) }
            }
            if (editing) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.mio_link_import_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                val provider = UnifiedPlaylistImporter.detectSource(link)
                TextButton(
                    onClick = {
                        MioPlaylistSync.linkManually(item.playlist, provider, link)
                        editing = false
                        onLinked()
                        MioPlaylistSync.checkOne(item.playlist.id)
                    },
                    enabled = link.isNotBlank() && provider !in setOf(ImportSourceType.UNKNOWN, ImportSourceType.YOUTUBE, ImportSourceType.FILE_OR_TEXT),
                ) { Text(stringResource(Res.string.mio_sync_link_save)) }
            }
        }
    }
}
