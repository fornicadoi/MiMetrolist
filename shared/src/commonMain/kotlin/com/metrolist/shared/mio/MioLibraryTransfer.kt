/**
 * MiMetrolist: pasa la biblioteca entre dispositivos emparejados con Device Connect.
 * El que recibe la mezcla con la suya (no borra nada): me gusta, listas propias,
 * historial, artistas y álbumes guardados.
 */

package com.metrolist.shared.mio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.backup.ForeignArtist
import com.metrolist.shared.backup.ForeignBookmark
import com.metrolist.shared.backup.ForeignLibrary
import com.metrolist.shared.backup.ForeignLibraryFamily
import com.metrolist.shared.backup.ForeignPlay
import com.metrolist.shared.backup.ForeignPlaylist
import com.metrolist.shared.backup.ForeignSong
import com.metrolist.shared.backup.importForeignLibrary
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.entities.Song
import com.metrolist.shared.deviceconnect.DcJson
import com.metrolist.shared.deviceconnect.DcSyncMessage
import com.metrolist.shared.deviceconnect.DcSyncMessageType
import com.metrolist.shared.deviceconnect.DeviceConnectManager
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_transfer_failed
import com.metrolist.shared.generated.resources.mio_transfer_received
import com.metrolist.shared.generated.resources.mio_transfer_sending
import com.metrolist.shared.generated.resources.mio_transfer_sent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform

@Serializable
data class MioLibraryPayload(
    val songs: List<MioLibSong> = emptyList(),
    val playlists: List<MioLibPlaylist> = emptyList(),
    val plays: List<MioLibPlay> = emptyList(),
    val artists: List<MioLibBookmark> = emptyList(),
    val albums: List<MioLibBookmark> = emptyList(),
)

@Serializable
data class MioLibArtist(
    val id: String? = null,
    val name: String,
)

@Serializable
data class MioLibSong(
    val id: String,
    val title: String,
    val artists: List<MioLibArtist> = emptyList(),
    val albumId: String? = null,
    val albumName: String? = null,
    val duration: Int? = null,
    val thumbnail: String? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val inLibraryAt: Long? = null,
    val playTimeMs: Long = 0,
    val isVideo: Boolean = false,
)

@Serializable
data class MioLibPlaylist(
    val name: String,
    val songIds: List<String>,
    val description: String? = null,
    val createdAt: Long? = null,
    val thumbnail: String? = null,
)

@Serializable
data class MioLibPlay(
    val songId: String,
    val at: Long,
    val ms: Long,
)

@Serializable
data class MioLibBookmark(
    val id: String,
    val name: String,
    val thumbnail: String? = null,
    val at: Long,
    val year: Int? = null,
)

object MioLibraryTransfer {
    /** Set by each app to its Device Connect manager. */
    var manager: DeviceConnectManager? = null

    /** Status of the last library this device sent. */
    var notice by mutableStateOf<String?>(null)

    /** Result of a library received from another device, shown until dismissed. */
    var received by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set

    // A single message must stay below Device Connect's 8 MiB playback limit.
    private const val MAX_BYTES = 7 * 1024 * 1024

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private fun database(): MusicDatabase = KoinPlatform.getKoin().get()

    fun send(
        deviceId: String,
        deviceName: String,
    ) {
        val manager = manager ?: return
        if (busy) return
        scope.launch {
            mutex.withLock {
                busy = true
                notice = getString(Res.string.mio_transfer_sending, deviceName)
                try {
                    val bytes = encode(database().exportLibrary())
                    val sent = manager.mioSendLibrary(deviceId, bytes)
                    notice = getString(if (sent) Res.string.mio_transfer_sent else Res.string.mio_transfer_failed, deviceName)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    notice = getString(Res.string.mio_transfer_failed, deviceName)
                } finally {
                    busy = false
                }
            }
        }
    }

    /** Called by Device Connect when a paired device sends its library. */
    fun onReceived(
        deviceName: String,
        payload: MioLibraryPayload,
    ) {
        scope.launch {
            mutex.withLock {
                busy = true
                try {
                    val summary = database().importForeignLibrary(payload.toForeignLibrary(), skipExistingPlays = true)
                    received =
                        getString(
                            Res.string.mio_transfer_received,
                            deviceName,
                            summary.songs,
                            summary.likes,
                            summary.playlists,
                            summary.plays,
                        )
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    received = getString(Res.string.mio_transfer_failed, deviceName)
                } finally {
                    busy = false
                }
            }
        }
    }

    /** Drops the oldest plays until the message fits. */
    private fun encode(payload: MioLibraryPayload): ByteArray {
        var current = payload
        while (true) {
            val bytes =
                DcJson
                    .encodeToString(DcSyncMessage(type = DcSyncMessageType.LIBRARY, library = current))
                    .encodeToByteArray()
            if (bytes.size <= MAX_BYTES) return bytes
            check(current.plays.isNotEmpty()) { "Library is too large to send" }
            current = current.copy(plays = current.plays.take(current.plays.size / 2))
        }
    }
}

private fun LocalDateTime.epochMs(): Long = toInstant(TimeZone.UTC).toEpochMilliseconds()

private fun Song.toMioSong(): MioLibSong =
    MioLibSong(
        id = song.id,
        title = song.title,
        artists = orderedArtists.map { MioLibArtist(it.id, it.name) },
        albumId = song.albumId ?: album?.id,
        albumName = song.albumName ?: album?.title,
        duration = song.duration.takeIf { it > 0 },
        thumbnail = thumbnailUrl,
        explicit = song.explicit,
        liked = song.liked,
        likedAt = song.likedDate?.epochMs(),
        inLibraryAt = song.inLibrary?.epochMs(),
        playTimeMs = song.totalPlayTime,
        isVideo = song.isVideo,
    )

private suspend fun MusicDatabase.exportLibrary(): MioLibraryPayload {
    val dao = dao()
    val songs = linkedMapOf<String, MioLibSong>()
    fun keep(song: Song) {
        if (!song.song.isLocal && !song.song.isEpisode && !song.song.isUploaded) songs.getOrPut(song.id) { song.toMioSong() }
    }
    dao.likedSongsByCreateDateAsc().first().forEach(::keep)
    val playlists =
        dao
            .allPlaylists()
            .first()
            .filter { it.browseId == null && it.isEditable && it.name.isNotBlank() }
            .map { playlist ->
                val entries = dao.playlistSongs(playlist.id).first()
                entries.forEach { keep(it.song) }
                MioLibPlaylist(
                    name = playlist.name,
                    songIds = entries.map { it.song.id },
                    description = playlist.description,
                    createdAt = playlist.createdAt?.epochMs(),
                    thumbnail = playlist.thumbnailUrl,
                )
            }
    val plays =
        dao.events().first().mapNotNull { entry ->
            keep(entry.song)
            if (entry.song.id !in songs) return@mapNotNull null
            MioLibPlay(entry.event.songId, entry.event.timestamp.epochMs(), entry.event.playTime.coerceAtLeast(0))
        }
    val artists =
        dao.artistsBookmarkedByCreateDateAsc().first().mapNotNull { artist ->
            val at = artist.artist.bookmarkedAt ?: return@mapNotNull null
            MioLibBookmark(artist.id, artist.artist.name, artist.artist.thumbnailUrl, at.epochMs())
        }
    val albums =
        dao.albumsLikedByCreateDateAsc().first().mapNotNull { album ->
            val at = album.album.bookmarkedAt ?: return@mapNotNull null
            MioLibBookmark(album.id, album.album.title, album.album.thumbnailUrl, at.epochMs(), album.album.year)
        }
    return MioLibraryPayload(songs.values.toList(), playlists, plays, artists, albums)
}

private fun MioLibraryPayload.toForeignLibrary(): ForeignLibrary =
    ForeignLibrary(
        family = ForeignLibraryFamily.INNERTUNE,
        songs =
            songs.map {
                ForeignSong(
                    id = it.id,
                    title = it.title,
                    artists = it.artists.map { artist -> ForeignArtist(artist.id, artist.name) },
                    albumId = it.albumId,
                    albumName = it.albumName,
                    durationSeconds = it.duration,
                    thumbnailUrl = it.thumbnail,
                    explicit = it.explicit,
                    liked = it.liked,
                    likedAt = it.likedAt,
                    inLibraryAt = it.inLibraryAt,
                    totalPlayTimeMs = it.playTimeMs,
                    isVideo = it.isVideo,
                )
            },
        playlists =
            playlists.map {
                ForeignPlaylist(
                    name = it.name,
                    browseId = null,
                    songIds = it.songIds,
                    isEditable = true,
                    description = it.description,
                    createdAt = it.createdAt,
                    thumbnailUrl = it.thumbnail,
                )
            },
        plays = plays.map { ForeignPlay(it.songId, it.at, it.ms) },
        artists = artists.map { ForeignBookmark(it.id, it.name, it.thumbnail, it.at) },
        albums = albums.map { ForeignBookmark(it.id, it.name, it.thumbnail, it.at, it.year) },
    )
