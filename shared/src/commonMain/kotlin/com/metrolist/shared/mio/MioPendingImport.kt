/**
 * MiMetrolist: enlaces de playlists de otras apps que esperan a ser importados.
 * Llegan por "Compartir" en el teléfono o al pegarlos en el buscador.
 */

package com.metrolist.shared.mio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.playlistimport.ImportSourceType
import com.metrolist.shared.playlistimport.UnifiedPlaylistImporter

object MioPendingImport {
    /** Link the import screen should start with, consumed once it opens. */
    var url by mutableStateOf<String?>(null)

    /** Opens the platform's playlist import screen. */
    var opener: (() -> Unit)? = null

    private val importSources =
        setOf(
            ImportSourceType.SPOTIFY,
            ImportSourceType.APPLE_MUSIC,
            ImportSourceType.DEEZER,
            ImportSourceType.TIDAL,
            ImportSourceType.AMAZON,
        )

    private val whitespace = Regex("\\s+")

    /** The first playlist link from another service found in [text], if any. */
    fun linkIn(text: String): String? =
        text
            .trim()
            .split(whitespace)
            .firstOrNull { token ->
                token.startsWith("http://", ignoreCase = true) || token.startsWith("https://", ignoreCase = true)
            }?.takeIf { UnifiedPlaylistImporter.detectSource(it) in importSources }

    fun sourceName(link: String): String = UnifiedPlaylistImporter.detectSource(link).displayName

    fun request(link: String) {
        url = link
        opener?.invoke()
    }

    /** Hands the pending link to the import screen exactly once. */
    fun consume(): String? = url?.also { url = null }
}
