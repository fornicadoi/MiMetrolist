/**
 * MiMetrolist: canciones que no se encontraron al importar una lista.
 * Permite buscarlas más a fondo (también entre los videos de YouTube),
 * elegir a mano entre varios resultados y agregarlas a la lista importada.
 */

package com.metrolist.shared.mio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.entities.PlaylistEntity
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_missing_added
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.playlistimport.PlaylistImportManager
import com.metrolist.shared.state.GlobalYouTubeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform

enum class MioMissingState { PENDING, SEARCHING, FOUND, NOT_FOUND, ADDING, ADDED }

data class MioMissingSong(
    val id: Long,
    val playlist: PlaylistEntity,
    val track: PlaylistImportManager.CsvTrack,
    val state: MioMissingState = MioMissingState.PENDING,
    val match: SongItem? = null,
    val selected: Boolean = true,
    val candidates: List<SongItem> = emptyList(),
    val showCandidates: Boolean = false,
)

object MioUnresolved {
    val songs = mutableStateListOf<MioMissingSong>()

    /** Set when new songs arrive, so the review opens once the import finishes. */
    var unseen by mutableStateOf(false)
    var open by mutableStateOf(false)
    var searchingAll by mutableStateOf(false)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var nextId = 0L
    private var searchJob: Job? = null

    fun record(
        playlist: PlaylistEntity,
        tracks: List<PlaylistImportManager.CsvTrack>,
    ) {
        if (tracks.isEmpty()) return
        scope.launch {
            // A new import of the same playlist replaces its earlier leftovers.
            songs.removeAll { it.playlist.id == playlist.id && it.state != MioMissingState.ADDED }
            tracks.forEach { songs += MioMissingSong(nextId++, playlist, it) }
            unseen = true
        }
    }

    fun dismiss() {
        searchJob?.cancel()
        searchingAll = false
        songs.clear()
        open = false
        unseen = false
    }

    private fun update(
        id: Long,
        change: (MioMissingSong) -> MioMissingSong,
    ) {
        val index = songs.indexOfFirst { it.id == id }
        if (index >= 0) songs[index] = change(songs[index])
    }

    fun toggleSelected(id: Long) = update(id) { it.copy(selected = !it.selected) }

    fun choose(
        id: Long,
        song: SongItem,
    ) = update(id) { it.copy(state = MioMissingState.FOUND, match = song, selected = true, showCandidates = false) }

    fun toggleCandidates(id: Long) {
        val song = songs.firstOrNull { it.id == id } ?: return
        if (song.showCandidates) {
            update(id) { it.copy(showCandidates = false) }
        } else if (song.candidates.isNotEmpty()) {
            update(id) { it.copy(showCandidates = true) }
        } else {
            scope.launch {
                update(id) { it.copy(state = MioMissingState.SEARCHING) }
                val result = deepSearch(song.track)
                update(id) {
                    it.copy(
                        state = if (it.match != null) MioMissingState.FOUND else MioMissingState.NOT_FOUND,
                        candidates = result.second,
                        showCandidates = true,
                    )
                }
            }
        }
    }

    fun searchOne(id: Long) {
        val song = songs.firstOrNull { it.id == id } ?: return
        scope.launch { runSearch(song) }
    }

    fun searchAll() {
        if (searchingAll) return
        searchJob =
            scope.launch {
                searchingAll = true
                try {
                    songs
                        .filter { it.state == MioMissingState.PENDING || it.state == MioMissingState.NOT_FOUND }
                        .forEach { runSearch(it) }
                } finally {
                    searchingAll = false
                }
            }
    }

    private suspend fun runSearch(song: MioMissingSong) {
        update(song.id) { it.copy(state = MioMissingState.SEARCHING) }
        val (match, candidates) = deepSearch(song.track)
        update(song.id) {
            it.copy(
                state = if (match != null) MioMissingState.FOUND else MioMissingState.NOT_FOUND,
                match = match,
                candidates = candidates,
                selected = match != null,
            )
        }
    }

    /** Adds every found and selected song to the playlist it belongs to. */
    fun addFound() {
        val ready = songs.filter { it.state == MioMissingState.FOUND && it.selected && it.match != null }
        if (ready.isEmpty()) return
        scope.launch {
            ready.forEach { song -> update(song.id) { it.copy(state = MioMissingState.ADDING) } }
            val database = KoinPlatform.getKoin().get<MusicDatabase>()
            val manager = PlaylistImportManager(database, GlobalYouTubeRepository.instance)
            var added = 0
            ready.groupBy { it.playlist.id }.forEach { (_, group) ->
                val playlist = group.first().playlist
                val destination =
                    if (playlist.id == FAVORITES_ID) {
                        PlaylistImportManager.Destination.Favorites
                    } else {
                        PlaylistImportManager.Destination.EXISTING(playlist)
                    }
                val result =
                    try {
                        manager.importVerbose(playlist.name, group.mapNotNull { it.match }, destination)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Result.failure(error)
                    }
                val ok = result.isSuccess || result.exceptionOrNull() is PlaylistImportManager.IncompleteImportException
                group.forEach { song -> update(song.id) { it.copy(state = if (ok) MioMissingState.ADDED else MioMissingState.FOUND) } }
                if (ok) added += group.size
            }
            if (added > 0) MioNotice.message = getString(Res.string.mio_missing_added, added)
        }
    }

    // ---- Deeper search ----

    private val artistSplit = Regex("""\s*(?:,|&|\+|/|;|\b(?:feat|ft|featuring|with|x|y|con)\b\.?)\s*""", RegexOption.IGNORE_CASE)

    /** Tries simpler titles and YouTube videos as well as songs; returns the best match and a short list to pick from. */
    internal suspend fun deepSearch(track: PlaylistImportManager.CsvTrack): Pair<SongItem?, List<SongItem>> {
        val repository = GlobalYouTubeRepository.instance
        val cleanTitle = MioMatchText.stripExtras(track.title)
        val mainArtist = track.artist.split(artistSplit).firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        val queries =
            listOf(
                "$cleanTitle $mainArtist".trim() to SearchFilter.SONG,
                "$cleanTitle $mainArtist".trim() to SearchFilter.VIDEO,
                cleanTitle to SearchFilter.SONG,
                "${track.title} ${track.artist}".trim() to SearchFilter.VIDEO,
            ).distinct()
        val candidates = LinkedHashMap<String, SongItem>()
        for ((query, filter) in queries) {
            val items =
                try {
                    repository.searchFiltered(query, filter, allowOfflineFallback = false).items.filterIsInstance<SongItem>()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    emptyList()
                }
            items.filterNot { it.isUnavailable }.take(5).forEach { candidates.putIfAbsent(it.id, it) }
            val pool = candidates.values.toList()
            val match =
                PlaylistImportManager.bestTrackMatch(track, pool)
                    ?: PlaylistImportManager.bestTrackMatch(track.copy(title = cleanTitle), pool)
                    ?: relaxedMatch(cleanTitle, mainArtist, pool)
            if (match != null) return match to pool.take(10)
        }
        return null to candidates.values.take(10)
    }

    private fun relaxedMatch(
        title: String,
        artist: String,
        pool: List<SongItem>,
    ): SongItem? {
        val wantedTitle = MioMatchText.normalize(title)
        val wantedArtist = MioMatchText.normalize(artist)
        if (wantedTitle.isEmpty()) return null
        return pool.firstOrNull { song ->
            val candidateTitle = MioMatchText.normalize(MioMatchText.stripExtras(song.title))
            val fullTitle = MioMatchText.normalize(song.title)
            val titleOk =
                candidateTitle == wantedTitle || candidateTitle.contains(wantedTitle) || fullTitle.contains(wantedTitle)
            val artists = MioMatchText.normalize(song.artists.joinToString(" ") { it.name })
            // Video titles often carry the artist as "Artist - Title".
            val artistOk = wantedArtist.isEmpty() || artists.contains(wantedArtist) || fullTitle.contains(wantedArtist)
            titleOk && artistOk
        }
    }

    private const val FAVORITES_ID = "favorites"
}
