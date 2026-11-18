/**
 * MiMetrolist: modo enfoque. Alterna rondas de concentración y descanso,
 * pone música tranquila si se quiere y pausa al terminar.
 */

package com.metrolist.shared.mio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_focus_music_query
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.GlobalYouTubeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import kotlin.time.TimeSource

enum class MioFocusPhase { IDLE, WORK, BREAK, DONE }

object MioFocus {
    var visible by mutableStateOf(false)
    var phase by mutableStateOf(MioFocusPhase.IDLE)
        private set
    var round by mutableIntStateOf(0)
        private set
    var remainingSeconds by mutableIntStateOf(0)
        private set
    var totalSeconds by mutableIntStateOf(0)
        private set
    var paused by mutableStateOf(false)
        private set

    /** Platform playback hooks, set once by each app. */
    var playSongs: ((List<SongItem>) -> Unit)? = null
    var setPlaying: ((Boolean) -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var skipRequested = false

    val running: Boolean get() = phase == MioFocusPhase.WORK || phase == MioFocusPhase.BREAK

    fun start() {
        job?.cancel()
        paused = false
        job = scope.launch { runSession() }
    }

    fun stop() {
        job?.cancel()
        job = null
        paused = false
        phase = MioFocusPhase.IDLE
    }

    fun togglePause() {
        if (!running) return
        paused = !paused
        if (phase == MioFocusPhase.WORK || !Settings.shared.mioFocusPauseOnBreak) setPlaying?.invoke(!paused)
    }

    fun skip() {
        if (running) skipRequested = true
    }

    private suspend fun runSession() {
        val settings = Settings.shared
        val rounds = settings.mioFocusRounds.coerceIn(1, 12)
        startMusic(settings.mioFocusCalmMusic)
        for (current in 1..rounds) {
            round = current
            countdown(MioFocusPhase.WORK, settings.mioFocusWorkMinutes.coerceIn(1, 180) * 60)
            if (current == rounds) break
            if (settings.mioFocusPauseOnBreak) setPlaying?.invoke(false)
            countdown(MioFocusPhase.BREAK, settings.mioFocusBreakMinutes.coerceIn(1, 60) * 60)
            if (settings.mioFocusPauseOnBreak) setPlaying?.invoke(true)
        }
        setPlaying?.invoke(false)
        phase = MioFocusPhase.DONE
    }

    private suspend fun startMusic(calm: Boolean) {
        if (!calm) {
            setPlaying?.invoke(true)
            return
        }
        val songs =
            try {
                withContext(Dispatchers.Default) {
                    GlobalYouTubeRepository.instance
                        .searchFiltered(getString(Res.string.mio_focus_music_query), SearchFilter.SONG)
                        .items
                        .filterIsInstance<SongItem>()
                        .filterNot { it.isUnavailable }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
        if (songs.isEmpty()) setPlaying?.invoke(true) else playSongs?.invoke(songs.shuffled())
    }

    private suspend fun countdown(
        next: MioFocusPhase,
        seconds: Int,
    ) {
        phase = next
        totalSeconds = seconds
        remainingSeconds = seconds
        skipRequested = false
        var leftMs = seconds * 1000L
        var mark = TimeSource.Monotonic.markNow()
        while (leftMs > 0 && !skipRequested) {
            delay(200)
            val elapsed = mark.elapsedNow().inWholeMilliseconds
            mark = TimeSource.Monotonic.markNow()
            if (!paused) leftMs -= elapsed
            remainingSeconds = ((leftMs + 999) / 1000).toInt().coerceAtLeast(0)
        }
        if (paused) {
            paused = false
            if (next == MioFocusPhase.WORK || !Settings.shared.mioFocusPauseOnBreak) setPlaying?.invoke(true)
        }
    }
}
