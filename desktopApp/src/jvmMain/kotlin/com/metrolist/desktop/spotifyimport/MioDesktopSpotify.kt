/**
 * MiMetrolist: administrador de Spotify para mantener al día las listas importadas en la PC.
 */

package com.metrolist.desktop.spotifyimport

import com.metrolist.shared.network.sharedHttpEngine
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.spotifyimport.SpotifyImportManager
import meow.nyx.spotlib.LogLevel
import meow.nyx.spotlib.SpotifyClient
import meow.nyx.spotlib.SpotifyConfig
import meow.nyx.spotlib.SpotifyLogger
import java.util.logging.Logger

internal fun createMioSpotifyImportManager(): SpotifyImportManager {
    val logger = Logger.getLogger("SpotifySync")
    return SpotifyImportManager(
        clientFactory = {
            SpotifyClient.create(
                SpotifyConfig(
                    engine = sharedHttpEngine,
                    logLevel = LogLevel.SUMMARY,
                    logger = SpotifyLogger { _, message -> logger.fine(message) },
                ),
            )
        },
        loadStoredSession = { Settings.shared.spotifySession },
        saveStoredSession = { Settings.shared.spotifySession = it },
    )
}
