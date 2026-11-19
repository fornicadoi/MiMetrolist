/**
 * MiMetrolist: de dónde vino cada lista importada, para poder revisarla y mantenerla al día.
 */

package com.metrolist.shared.mio

import com.metrolist.shared.db.entities.PlaylistEntity
import com.metrolist.shared.playlistimport.ImportSourceType
import com.metrolist.shared.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Where an imported playlist came from. [source] is a Spotify URI or a public link. */
@Serializable
data class MioSyncLink(
    val playlistId: String,
    val provider: String,
    val source: String,
    /** Source tracks that could not be found last time, so automatic checks do not search them again. */
    val skippedKeys: List<String> = emptyList(),
)

object MioSyncLinks {
    private val json = Json { ignoreUnknownKeys = true }

    // Links remembered while a public link is being imported, matched by title once the import finishes.
    private val pending = mutableMapOf<String, Pair<ImportSourceType, String>>()

    fun all(): List<MioSyncLink> =
        Settings.shared.mioSyncLinks
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<List<MioSyncLink>>(it) }.getOrNull() }
            .orEmpty()

    fun put(link: MioSyncLink) {
        val links = all().filterNot { it.playlistId == link.playlistId } + link
        Settings.shared.mioSyncLinks = json.encodeToString(links)
    }

    /** Called when a public link is parsed for import. */
    fun rememberSource(
        title: String,
        provider: ImportSourceType,
        url: String,
    ) {
        if (provider == ImportSourceType.YOUTUBE || provider == ImportSourceType.UNKNOWN || provider == ImportSourceType.FILE_OR_TEXT) return
        pending[title] = provider to url
    }

    /** Called when an import finishes; links the playlist to the link it came from, if known. */
    fun onImported(
        playlist: PlaylistEntity,
        title: String,
    ) {
        val source = pending.remove(title) ?: return
        if (playlist.id == "favorites") return
        put(MioSyncLink(playlist.id, source.first.name, source.second))
    }
}
