/**
 * MiMetrolist: busca lanzamientos nuevos de los artistas que sigues.
 */

package com.metrolist.shared.api

import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.GlobalYouTubeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

data class MioNewRelease(
    val artistName: String,
    val album: AlbumItem,
)

object MioReleaseChecker {
    private const val MAX_ARTISTS = 25
    private const val MAX_SEEN = 400
    private const val MIN_INTERVAL_MS = 12 * 60 * 60 * 1000L

    fun shouldCheck(now: Long = Clock.System.now().toEpochMilliseconds()): Boolean =
        Settings.shared.mioReleaseAlerts && now - Settings.shared.mioReleasesCheckedAt >= MIN_INTERVAL_MS

    /**
     * Devuelve los lanzamientos que no se habían visto antes. La primera vez solo memoriza
     * lo que ya existe, para no avisar de discos viejos.
     */
    suspend fun check(timeoutMs: Long = 45_000): List<MioNewRelease> {
        val settings = Settings.shared
        val firstRun = settings.mioReleasesSeen.isBlank()
        val seen = settings.mioReleasesSeen.split(',').filter(String::isNotBlank).toMutableList()
        val found = mutableListOf<MioNewRelease>()
        withTimeoutOrNull(timeoutMs) {
            val db = KoinPlatform.getKoin().get<MusicDatabase>()
            val artists =
                db
                    .dao()
                    .artistsBookmarkedByCreateDateAsc()
                    .first()
                    .map { it.artist }
                    .filter { !it.id.startsWith("LA") }
                    .takeLast(MAX_ARTISTS)
            for (artist in artists) {
                try {
                    val page = GlobalYouTubeRepository.instance.getArtist(artist.id)
                    val latest =
                        artistSectionsWithLatestRelease(page.sections)["latest_release"]
                            ?.filterIsInstance<AlbumItem>()
                            ?.firstOrNull() ?: continue
                    if (latest.id !in seen) {
                        seen += latest.id
                        if (!firstRun) found += MioNewRelease(artist.name, latest)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Un artista que falla no detiene la revisión de los demás.
                }
            }
        }
        settings.mioReleasesSeen = seen.takeLast(MAX_SEEN).joinToString(",")
        settings.mioReleasesCheckedAt = Clock.System.now().toEpochMilliseconds()
        return found
    }
}
