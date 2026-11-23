/**
 * MiMetrolist: mover una canción dentro de su lista desde el menú de tres puntos.
 */

package com.metrolist.android.mio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.android.ui.component.AdaptiveModalBottomSheet
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_move_song
import com.metrolist.shared.generated.resources.mio_move_song_done
import com.metrolist.shared.generated.resources.mio_move_song_down
import com.metrolist.shared.generated.resources.mio_move_song_position
import com.metrolist.shared.generated.resources.mio_move_song_to_bottom
import com.metrolist.shared.generated.resources.mio_move_song_to_top
import com.metrolist.shared.generated.resources.mio_move_song_up
import org.jetbrains.compose.resources.stringResource

/** [position] is zero-based; the sheet stays open so the song can be moved several times. */
@Composable
fun MioMoveSongSheet(
    title: String,
    position: Int,
    total: Int,
    onMove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AdaptiveModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.mio_move_song), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(Res.string.mio_move_song_position, position + 1, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onMove(0) }, enabled = position > 0, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.mio_move_song_to_top), maxLines = 1)
                }
                FilledTonalButton(onClick = { onMove(position - 1) }, enabled = position > 0, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.mio_move_song_up), maxLines = 1)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onMove(position + 1) }, enabled = position < total - 1, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.mio_move_song_down), maxLines = 1)
                }
                FilledTonalButton(onClick = { onMove(total - 1) }, enabled = position < total - 1, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.mio_move_song_to_bottom), maxLines = 1)
                }
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.mio_move_song_done))
            }
        }
    }
}
