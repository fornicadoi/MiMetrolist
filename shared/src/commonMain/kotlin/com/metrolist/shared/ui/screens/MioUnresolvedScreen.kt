/**
 * MiMetrolist: pantalla de canciones que no se encontraron al importar.
 */

package com.metrolist.shared.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.metrolist.shared.generated.resources.mio_missing_add
import com.metrolist.shared.generated.resources.mio_missing_adding
import com.metrolist.shared.generated.resources.mio_missing_row_added
import com.metrolist.shared.generated.resources.mio_missing_choose
import com.metrolist.shared.generated.resources.mio_missing_close
import com.metrolist.shared.generated.resources.mio_missing_discard
import com.metrolist.shared.generated.resources.mio_missing_found
import com.metrolist.shared.generated.resources.mio_missing_none_found
import com.metrolist.shared.generated.resources.mio_missing_not_found
import com.metrolist.shared.generated.resources.mio_missing_search
import com.metrolist.shared.generated.resources.mio_missing_search_all
import com.metrolist.shared.generated.resources.mio_missing_subtitle
import com.metrolist.shared.generated.resources.mio_missing_title
import com.metrolist.shared.generated.resources.mio_missing_video
import com.metrolist.shared.mio.MioMissingSong
import com.metrolist.shared.mio.MioMissingState
import com.metrolist.shared.mio.MioUnresolved
import com.metrolist.shared.playlistimport.PlaylistImportStatusManager
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

private val FoundGreen = Color(0xFF2FD66B)

/**
 * Opens the review once an import with missing songs has finished. [inWindow] draws it over the desktop window;
 * [fullScreen] lets a platform supply its own edge-to-edge container.
 */
@Composable
fun MioUnresolvedHost(
    inWindow: Boolean = false,
    fullScreen: (@Composable (onDismiss: () -> Unit, content: @Composable () -> Unit) -> Unit)? = null,
) {
    val active by PlaylistImportStatusManager.activeImport.collectAsState()
    val running = active?.isRunning == true
    LaunchedEffect(MioUnresolved.unseen, running) {
        if (MioUnresolved.unseen && !running && MioUnresolved.songs.isNotEmpty()) {
            MioUnresolved.unseen = false
            MioUnresolved.open = true
        }
    }
    if (!MioUnresolved.open || MioUnresolved.songs.isEmpty()) return
    if (inWindow) {
        MioUnresolvedContent()
    } else if (fullScreen != null) {
        fullScreen({ MioUnresolved.open = false }) { MioUnresolvedContent() }
    } else {
        Dialog(
            onDismissRequest = { MioUnresolved.open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) { MioUnresolvedContent() }
    }
}

@Composable
private fun MioUnresolvedContent() {
    val songs = MioUnresolved.songs
    val ready = songs.count { it.state == MioMissingState.FOUND && it.selected && it.match != null }
    val pending = songs.any { it.state == MioMissingState.PENDING || it.state == MioMissingState.NOT_FOUND }
    val adding = MioUnresolved.addingTotal > 0
    Surface(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize().padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Text(
                            stringResource(Res.string.mio_missing_title, songs.count { it.state != MioMissingState.ADDED }),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.mio_missing_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { MioUnresolved.open = false }) {
                        Icon(MetrolistIcons.Default.Close, stringResource(Res.string.mio_missing_close))
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    songs.groupBy { it.playlist.id }.forEach { (_, group) ->
                        item(key = "header-${group.first().playlist.id}") {
                            Text(
                                group.first().playlist.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(group, key = { it.id }) { song -> MissingRow(song) }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (pending && !adding) {
                        OutlinedButton(
                            onClick = MioUnresolved::searchAll,
                            enabled = !MioUnresolved.searchingAll,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (MioUnresolved.searchingAll) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(stringResource(Res.string.mio_missing_search_all), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = MioUnresolved::dismiss, enabled = !adding) {
                            Text(stringResource(Res.string.mio_missing_discard), maxLines = 1)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { if (!adding) MioUnresolved.addFound() },
                            enabled = ready > 0 || adding,
                            colors = ButtonDefaults.buttonColors(containerColor = FoundGreen, contentColor = Color.Black),
                        ) {
                            if (adding) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(Res.string.mio_missing_adding, MioUnresolved.addingDone, MioUnresolved.addingTotal),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            } else {
                                Text(stringResource(Res.string.mio_missing_add, ready), fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingRow(song: MioMissingSong) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song.track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        song.track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val match = song.match
                    when (song.state) {
                        MioMissingState.FOUND, MioMissingState.ADDING ->
                            if (match != null) {
                                Text(
                                    stringResource(Res.string.mio_missing_found, match.title, match.artists.joinToString(", ") { it.name }),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FoundGreen,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                        MioMissingState.NOT_FOUND ->
                            Text(
                                stringResource(Res.string.mio_missing_not_found),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )

                        MioMissingState.ADDED ->
                            Text(
                                stringResource(Res.string.mio_missing_row_added),
                                style = MaterialTheme.typography.bodySmall,
                                color = FoundGreen,
                            )

                        else -> Unit
                    }
                }
                when (song.state) {
                    MioMissingState.SEARCHING, MioMissingState.ADDING -> {
                        CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(20.dp), strokeWidth = 2.dp)
                    }

                    MioMissingState.PENDING -> {
                        TextButton(onClick = { MioUnresolved.searchOne(song.id) }) { Text(stringResource(Res.string.mio_missing_search)) }
                    }

                    MioMissingState.FOUND -> {
                        TextButton(onClick = { MioUnresolved.toggleCandidates(song.id) }) { Text(stringResource(Res.string.mio_missing_choose)) }
                        Checkbox(checked = song.selected, onCheckedChange = { MioUnresolved.toggleSelected(song.id) })
                    }

                    MioMissingState.NOT_FOUND -> {
                        TextButton(onClick = { MioUnresolved.toggleCandidates(song.id) }) { Text(stringResource(Res.string.mio_missing_choose)) }
                    }

                    MioMissingState.ADDED -> {
                        Icon(MetrolistIcons.Default.Check, null, tint = FoundGreen, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
        if (song.showCandidates) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 6.dp)) {
                if (song.candidates.isEmpty()) {
                    Text(
                        stringResource(Res.string.mio_missing_none_found),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                song.candidates.forEach { candidate ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { MioUnresolved.choose(song.id, candidate) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(candidate.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                candidate.artists.joinToString(", ") { it.name } +
                                    if (candidate.musicVideoType != null && candidate.album == null) " · " + stringResource(Res.string.mio_missing_video) else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (song.match?.id == candidate.id) {
                            Icon(MetrolistIcons.Default.Check, null, tint = FoundGreen)
                        }
                    }
                }
            }
        }
    }
}
