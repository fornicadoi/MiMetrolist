/**
 * MiMetrolist: descargar todas las listas de la biblioteca de una vez.
 */

package com.metrolist.shared.mio

import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_library_download_all_started
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.playlist.PlaylistChanges
import com.metrolist.shared.storage.DownloadedPlaylists
import com.metrolist.shared.storage.trackedDownloadPlaylistId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

object MioLibraryDownloads {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Playlists in [items] that can be kept downloaded, plus Liked songs when it is shown in the library. */
    fun playlistIds(
        items: List<YTItem>,
        includeLiked: Boolean,
    ): List<String> =
        (items.filterIsInstance<PlaylistItem>().mapNotNull { trackedDownloadPlaylistId(it.id) } + listOfNotNull("LM".takeIf { includeLiked }))
            .distinct()

    fun notDownloaded(ids: List<String>): List<String> = ids.filterNot(DownloadedPlaylists::contains)

    /** Keeps every playlist downloaded, the same as turning on download for each one; new songs follow. */
    fun downloadAll(ids: List<String>) {
        scope.launch {
            ids.forEach { DownloadedPlaylists.track(it) }
            // Fetches each playlist's songs now instead of waiting until it is opened.
            ids.forEach { PlaylistChanges.notifyChanged(it) }
            MioNotice.message = getString(Res.string.mio_library_download_all_started)
        }
    }
}
