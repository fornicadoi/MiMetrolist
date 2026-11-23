/**
 * MiMetrolist: las canciones que te gustan en Spotify pueden ir a tus Favoritos en vez de a una lista aparte.
 */

package com.metrolist.shared.mio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_liked_to_favorites
import com.metrolist.shared.generated.resources.mio_liked_to_favorites_desc
import com.metrolist.shared.settings.Settings
import org.jetbrains.compose.resources.stringResource

object MioLikedFavorites {
    /** Spotify's URI for the signed-in user's Liked Songs. */
    const val SPOTIFY_LIKED_SONGS_URI = "spotify:collection:tracks"

    fun goesToFavorites(sourceUri: String): Boolean = Settings.shared.mioLikedToFavorites && sourceUri == SPOTIFY_LIKED_SONGS_URI
}

/** Shown under Spotify's Liked Songs in the playlist picker. */
@Composable
fun MioLikedToFavoritesSwitch(modifier: Modifier = Modifier) {
    val checked = Settings.shared.mioLikedToFavorites
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch) { Settings.shared.mioLikedToFavorites = it }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(Res.string.mio_liked_to_favorites), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(Res.string.mio_liked_to_favorites_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
