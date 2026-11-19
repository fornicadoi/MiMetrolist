package com.metrolist.desktop.storage

import com.metrolist.desktop.di.composeStateModule
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.state.coreModule
import com.metrolist.shared.youtube.di.sharedModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes

/**
 * MiMetrolist: descarga una canción real con el mismo código que la app de escritorio.
 * Solo corre con MIO_LIVE_DOWNLOAD=1 (flujo "Mi pruebas"); escribe el resultado en MIO_RESULT_FILE.
 */
class MioLiveDownloadTest {
    private class Tee(
        private val first: OutputStream,
        private val second: OutputStream,
    ) : OutputStream() {
        override fun write(b: Int) {
            first.write(b)
            second.write(b)
        }

        override fun write(
            b: ByteArray,
            off: Int,
            len: Int,
        ) {
            first.write(b, off, len)
            second.write(b, off, len)
        }

        override fun flush() {
            first.flush()
            second.flush()
        }
    }

    @Test
    fun downloadsOneSong() {
        if (System.getenv("MIO_LIVE_DOWNLOAD") != "1") return
        val report = File(System.getenv("MIO_RESULT_FILE") ?: "build/mio-download.txt").apply { parentFile?.mkdirs() }
        val captured = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(Tee(originalOut, captured), true))
        startKoin { modules(coreModule, sharedModule, composeStateModule) }
        val lines = mutableListOf<String>()
        var outcome = "ERROR"
        try {
            runBlocking {
                val query = System.getenv("MIO_QUERY").orEmpty().ifBlank { "Daft Punk Get Lucky" }
                val song =
                    GlobalYouTubeRepository.instance
                        .searchFiltered(query, SearchFilter.SONG, allowOfflineFallback = false)
                        .items
                        .filterIsInstance<SongItem>()
                        .firstOrNull()
                if (song == null) {
                    lines += "detalle=La búsqueda de \"$query\" no devolvió canciones"
                    return@runBlocking
                }
                lines += "cancion=${song.title} - ${song.artists.joinToString(", ") { it.name }} (${song.id})"
                val started = System.currentTimeMillis()
                DownloadManager.download(song)
                val state =
                    withTimeoutOrNull(5.minutes) {
                        DownloadManager.downloadStates
                            .first { it[song.id] == DownloadState.DOWNLOADED || it[song.id] == DownloadState.FAILED }[song.id]
                    }
                lines += "segundos=${(System.currentTimeMillis() - started) / 1000}"
                if (state != DownloadState.DOWNLOADED) {
                    lines += "detalle=Estado final: ${state ?: "sin terminar en 5 minutos"}"
                    return@runBlocking
                }
                val path = DownloadManager.getLocalAudioPath(song.id)?.let(::File)
                if (path == null || path.length() < 100_000) {
                    lines += "detalle=El archivo descargado falta o es muy pequeño (${path?.length() ?: 0} bytes)"
                    return@runBlocking
                }
                val seconds =
                    FFmpegFrameGrabber(path).use { grabber ->
                        grabber.start()
                        grabber.lengthInTime / 1_000_000
                    }
                lines += "archivo=${path.name} (${path.length() / 1024} KB, $seconds s de audio)"
                outcome = if (seconds >= 30) "OK" else "ERROR"
                if (outcome != "OK") lines += "detalle=El audio dura solo $seconds s"
            }
        } catch (error: Throwable) {
            lines += "detalle=${error::class.simpleName}: ${error.message}"
        } finally {
            System.setOut(originalOut)
            stopKoin()
        }
        if (outcome != "OK" && isYouTubeBlock(captured.toString() + lines.joinToString("\n"))) outcome = "BLOQUEO"
        report.writeText((listOf("resultado=$outcome") + lines).joinToString("\n") + "\n")
        println(report.readText())
        if (outcome == "ERROR") fail(lines.joinToString("; "))
    }

    private fun isYouTubeBlock(log: String): Boolean =
        listOf(
            "not a bot",
            "Sign in to confirm",
            "LOGIN_REQUIRED",
            "429",
            "Too Many Requests",
            "unusual traffic",
        ).any { log.contains(it, ignoreCase = true) }
}
