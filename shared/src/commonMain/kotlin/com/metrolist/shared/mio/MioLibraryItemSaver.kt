/**
 * MiMetrolist: guarda en tu biblioteca de YouTube Music los álbumes y podcasts
 * que tenías guardados en otra app, buscándolos por nombre y artista.
 */

package com.metrolist.shared.mio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_saved_items_missing
import com.metrolist.shared.generated.resources.mio_saved_items_result
import com.metrolist.shared.generated.resources.mio_saved_items_working
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.state.PodcastLibrary
import com.metrolist.shared.state.toggleAlbumLiked
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import meow.nyx.spotlib.PlaylistMeta
import meow.nyx.spotlib.PlaylistSource
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform

/** A short message for the user, shown in a dialog until dismissed. */
object MioNotice {
    var message by mutableStateOf<String?>(null)
}

object MioLibraryItemSaver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun isSavedItem(item: PlaylistMeta): Boolean = item.source == PlaylistSource.ALBUM || item.source == PlaylistSource.PODCAST

    fun saveAll(items: List<PlaylistMeta>) {
        if (items.isEmpty()) return
        scope.launch {
            MioNotice.message = getString(Res.string.mio_saved_items_working, items.size)
            val missing = mutableListOf<String>()
            var albums = 0
            var podcasts = 0
            items.forEach { item ->
                val saved =
                    try {
                        if (item.source == PlaylistSource.ALBUM) saveAlbum(item) else savePodcast(item)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        false
                    }
                when {
                    !saved -> missing += listOfNotNull(item.name, item.owner).joinToString(" — ")
                    item.source == PlaylistSource.ALBUM -> albums++
                    else -> podcasts++
                }
            }
            val summary = getString(Res.string.mio_saved_items_result, albums, podcasts)
            MioNotice.message =
                if (missing.isEmpty()) {
                    summary
                } else {
                    summary + "\n\n" + getString(Res.string.mio_saved_items_missing) + "\n" + missing.joinToString("\n") { "• $it" }
                }
        }
    }

    private suspend fun saveAlbum(item: PlaylistMeta): Boolean {
        val repository = GlobalYouTubeRepository.instance
        val queries = listOfNotNull(listOfNotNull(item.name, item.owner).joinToString(" "), item.name).distinct()
        val album =
            queries.firstNotNullOfOrNull { query ->
                repository
                    .searchFiltered(query, SearchFilter.ALBUM, allowOfflineFallback = false)
                    .items
                    .filterIsInstance<AlbumItem>()
                    .firstOrNull { candidate -> titlesMatch(item.name, candidate.title) && artistsMatch(item.owner, candidate.artists.map { it.name }) }
            } ?: return false
        val database = KoinPlatform.getKoin().get<MusicDatabase>()
        if (database.dao().album(album.id).first()?.album?.bookmarkedAt != null) return true
        database.toggleAlbumLiked(album, emptyList(), repository)
        return true
    }

    private suspend fun savePodcast(item: PlaylistMeta): Boolean {
        val repository = GlobalYouTubeRepository.instance
        val podcast =
            repository
                .searchFiltered(item.name, SearchFilter.PODCAST, allowOfflineFallback = false)
                .items
                .filterIsInstance<PodcastItem>()
                .firstOrNull { candidate -> titlesMatch(item.name, candidate.title) }
                ?: return false
        val database = KoinPlatform.getKoin().get<MusicDatabase>()
        return PodcastLibrary.setPodcastSaved(database, podcast, true).isSuccess
    }

    private fun titlesMatch(
        source: String,
        candidate: String,
    ): Boolean {
        val a = MioMatchText.normalize(MioMatchText.stripExtras(source))
        val b = MioMatchText.normalize(MioMatchText.stripExtras(candidate))
        return a.isNotEmpty() && b.isNotEmpty() && (a == b || a.contains(b) || b.contains(a))
    }

    private fun artistsMatch(
        owner: String?,
        candidates: List<String>,
    ): Boolean {
        if (owner.isNullOrBlank() || candidates.isEmpty()) return true
        val wanted = MioMatchText.normalize(owner)
        return candidates.any { name ->
            val artist = MioMatchText.normalize(name)
            artist.isNotEmpty() && (wanted.contains(artist) || artist.contains(wanted))
        }
    }
}

/** Text clean-up shared by the album, podcast and song searches. */
internal object MioMatchText {
    private val brackets = Regex("""\s*[(\[][^)\]]*[)\]]""")
    private val dashSuffix = Regex("""\s+-\s+.*$""")
    private val featuring = Regex("""\s+(feat\.?|ft\.?|featuring|con)\s+.*$""", RegexOption.IGNORE_CASE)
    private val nonWord = Regex("""[^\p{L}\p{N}]+""")

    /** Drops "(Remastered 2011)", "- Live", "feat. X" and similar extras. */
    fun stripExtras(text: String): String =
        text
            .replace(brackets, "")
            .replace(dashSuffix, "")
            .replace(featuring, "")
            .trim()
            .ifEmpty { text }

    fun normalize(text: String): String =
        text
            .lowercase()
            .map { foldAccent(it) }
            .joinToString("")
            .replace(nonWord, " ")
            .trim()

    private fun foldAccent(c: Char): Char =
        when (c) {
            'á', 'à', 'ä', 'â', 'ã' -> 'a'
            'é', 'è', 'ë', 'ê' -> 'e'
            'í', 'ì', 'ï', 'î' -> 'i'
            'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
            'ú', 'ù', 'ü', 'û' -> 'u'
            'ñ' -> 'n'
            'ç' -> 'c'
            else -> c
        }
}
