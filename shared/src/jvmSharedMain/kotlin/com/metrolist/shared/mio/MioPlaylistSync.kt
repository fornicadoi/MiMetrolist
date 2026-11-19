/**
 * MiMetrolist: mantiene al día las listas importadas de Spotify y otras apps.
 * Revisa la lista original, agrega las canciones nuevas y deja las que no se
 * encontraron en "Canciones no encontradas" para la búsqueda a fondo.
 */

package com.metrolist.shared.mio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.entities.PlaylistEntity
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_sync_added
import com.metrolist.shared.playlistimport.ImportSourceType
import com.metrolist.shared.playlistimport.PlaylistImportManager
import com.metrolist.shared.playlistimport.UnifiedPlaylistImporter
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.spotifyimport.SpotifyImportManager
import com.metrolist.shared.state.GlobalYouTubeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import meow.nyx.spotlib.PlaylistMeta
import meow.nyx.spotlib.PlaylistSource
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** One imported playlist as shown in the review screen. */
data class MioImportedPlaylist(
    val playlist: PlaylistEntity,
    val provider: ImportSourceType,
    val link: MioSyncLink?,
)

enum class MioSyncPhase { IDLE, CHECKING, DONE, FAILED, NEEDS_LINK }

data class MioSyncStatus(
    val phase: MioSyncPhase = MioSyncPhase.IDLE,
    val added: Int = 0,
    val missing: Int = 0,
)

object MioPlaylistSync {
    /** Set by each app to its Spotify manager, so private playlists and liked songs can be read. */
    var spotify: SpotifyImportManager? = null

    val status = mutableStateMapOf<String, MioSyncStatus>()
    var reviewOpen by mutableStateOf(false)
    var checkingAll by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var loop: Job? = null

    private fun database(): MusicDatabase = KoinPlatform.getKoin().get()

    private fun links(): List<MioSyncLink> = MioSyncLinks.all()

    private fun putLink(link: MioSyncLink) = MioSyncLinks.put(link)

    fun linkManually(
        playlist: PlaylistEntity,
        provider: ImportSourceType,
        url: String,
    ) {
        putLink(MioSyncLink(playlist.id, provider.name, url.trim()))
        status[playlist.id] = MioSyncStatus()
    }

    /** Every playlist that came from an import, with its link when there is one. */
    suspend fun importedPlaylists(): List<MioImportedPlaylist> {
        val database = database()
        val known = links().associateBy { it.playlistId }
        return database
            .importTrackMatchDao()
            .mioImportedPlaylistKeys()
            .mapNotNull { key ->
                val (playlistId, providerName) = key.split('\t').takeIf { it.size == 2 } ?: return@mapNotNull null
                val provider = runCatching { ImportSourceType.valueOf(providerName) }.getOrNull() ?: return@mapNotNull null
                val playlist = database.dao().findPlaylistEntity(playlistId) ?: return@mapNotNull null
                MioImportedPlaylist(playlist, provider, known[playlistId])
            }.distinctBy { it.playlist.id }
            .sortedBy { it.playlist.name.lowercase() }
    }

    /** Links Spotify playlists imported before links were kept, by matching their names. */
    private suspend fun linkSpotifyByName(playlists: List<MioImportedPlaylist>) {
        val manager = spotify?.takeIf { it.isAuthenticated } ?: return
        val unlinked = playlists.filter { it.link == null && it.provider == ImportSourceType.SPOTIFY }
        if (unlinked.isEmpty()) return
        val library =
            try {
                manager.loadPlaylists()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                return
            }
        val byName = library.filterNot(MioLibraryItemSaver::isSavedItem).groupBy { MioMatchText.normalize(it.name) }
        unlinked.forEach { item ->
            val match = byName[MioMatchText.normalize(item.playlist.name)]?.singleOrNull() ?: return@forEach
            putLink(MioSyncLink(item.playlist.id, ImportSourceType.SPOTIFY.name, match.uri))
        }
    }

    fun checkOne(playlistId: String) {
        scope.launch {
            mutex.withLock {
                val item = importedPlaylists().firstOrNull { it.playlist.id == playlistId } ?: return@withLock
                linkSpotifyByName(listOf(item))
                val linked = importedPlaylists().firstOrNull { it.playlist.id == playlistId } ?: return@withLock
                sync(linked, announce = true)
            }
        }
    }

    fun checkAll(announce: Boolean = true) {
        if (checkingAll) return
        scope.launch {
            mutex.withLock {
                checkingAll = true
                try {
                    var added = 0
                    linkSpotifyByName(importedPlaylists())
                    importedPlaylists().forEach { added += sync(it, announce) }
                    if (!announce && added > 0) MioNotice.message = getString(Res.string.mio_sync_added, added)
                } finally {
                    checkingAll = false
                }
            }
        }
    }

    /** Checks linked playlists when the app starts and every 30 minutes while it stays open. */
    fun startAutoSync() {
        if (loop != null) return
        loop =
            scope.launch {
                delay(45.seconds)
                while (true) {
                    if (Settings.shared.mioAutoSyncPlaylists) checkAll(announce = false)
                    delay(30.minutes)
                }
            }
    }

    private suspend fun sync(
        item: MioImportedPlaylist,
        announce: Boolean,
    ): Int {
        val playlistId = item.playlist.id
        val link = item.link
        if (link == null) {
            status[playlistId] = MioSyncStatus(MioSyncPhase.NEEDS_LINK)
            return 0
        }
        status[playlistId] = MioSyncStatus(MioSyncPhase.CHECKING)
        return try {
            val allTracks = sourceTracks(link, item.playlist.name)
            // Automatic checks skip songs that were already not found, so they do not search again every time.
            val tracks = if (announce) allTracks else allTracks.filterNot { trackKey(it) in link.skippedKeys }
            val database = database()
            val manager = PlaylistImportManager(database, GlobalYouTubeRepository.instance)
            val destination = PlaylistImportManager.Destination.EXISTING(item.playlist)
            val matches = manager.resolveImportTracks(tracks, destination, item.provider)
            val before = database.dao().playlistSongMaps(playlistId, 0).size
            if (matches.isNotEmpty()) {
                manager.importResolvedTracks(item.playlist.name, matches, destination, item.provider).let { result ->
                    if (result.isFailure && result.exceptionOrNull() !is PlaylistImportManager.IncompleteImportException) {
                        throw result.exceptionOrNull()!!
                    }
                }
            }
            val added = (database.dao().playlistSongMaps(playlistId, 0).size - before).coerceAtLeast(0)
            val missing = UnifiedPlaylistImporter.unresolvedTracks(tracks, matches)
            if (announce || missing.isNotEmpty()) {
                val current = database.dao().findPlaylistEntity(playlistId) ?: item.playlist
                MioUnresolved.record(current, missing, announce)
            }
            putLink(
                link.copy(
                    skippedKeys =
                        if (announce) {
                            missing.map(::trackKey)
                        } else {
                            (link.skippedKeys + missing.map(::trackKey)).distinct()
                        },
                ),
            )
            status[playlistId] = MioSyncStatus(MioSyncPhase.DONE, added, missing.size)
            added
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            status[playlistId] = MioSyncStatus(MioSyncPhase.FAILED)
            0
        }
    }

    private suspend fun sourceTracks(
        link: MioSyncLink,
        name: String,
    ): List<PlaylistImportManager.CsvTrack> {
        val manager = spotify
        if (link.source.startsWith("spotify:") && manager != null && manager.isAuthenticated) {
            val source = if (link.source == "spotify:collection:tracks") PlaylistSource.LIKED_SONGS else PlaylistSource.PLAYLIST
            return manager
                .loadPlaylist(PlaylistMeta(uri = link.source, name = name, source = source))
                .tracks
                .map { PlaylistImportManager.CsvTrack(it.title, it.artist, null, album = it.album, durationSeconds = it.durationSeconds) }
        }
        val url =
            if (link.source.startsWith("spotify:playlist:")) {
                "https://open.spotify.com/playlist/" + link.source.removePrefix("spotify:playlist:")
            } else {
                link.source
            }
        return UnifiedPlaylistImporter.parseFromInput(url).getOrThrow().tracks
    }

    private fun trackKey(track: PlaylistImportManager.CsvTrack): String = MioMatchText.normalize("${track.title} ${track.artist}")
}
