/**
 * MiMetrolist: pantalla del modo enfoque (teléfono y PC).
 */

package com.metrolist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_focus_break
import com.metrolist.shared.generated.resources.mio_focus_break_length
import com.metrolist.shared.generated.resources.mio_focus_calm_music
import com.metrolist.shared.generated.resources.mio_focus_close
import com.metrolist.shared.generated.resources.mio_focus_done
import com.metrolist.shared.generated.resources.mio_focus_done_detail
import com.metrolist.shared.generated.resources.mio_focus_less
import com.metrolist.shared.generated.resources.mio_focus_minutes
import com.metrolist.shared.generated.resources.mio_focus_more
import com.metrolist.shared.generated.resources.mio_focus_pause
import com.metrolist.shared.generated.resources.mio_focus_pause_on_break
import com.metrolist.shared.generated.resources.mio_focus_resume
import com.metrolist.shared.generated.resources.mio_focus_round
import com.metrolist.shared.generated.resources.mio_focus_rounds
import com.metrolist.shared.generated.resources.mio_focus_skip
import com.metrolist.shared.generated.resources.mio_focus_start
import com.metrolist.shared.generated.resources.mio_focus_stop
import com.metrolist.shared.generated.resources.mio_focus_title
import com.metrolist.shared.generated.resources.mio_focus_work
import com.metrolist.shared.generated.resources.mio_focus_work_length
import com.metrolist.shared.mio.MioFocus
import com.metrolist.shared.mio.MioFocusPhase
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource

/**
 * Shows the focus screen while [MioFocus.visible]; the session keeps running when it is closed.
 * [inWindow] draws it over the current window instead of in a dialog, as the desktop app does with its overlays.
 */
@Composable
fun MioFocusHost(
    nowPlaying: String? = null,
    inWindow: Boolean = false,
) {
    if (!MioFocus.visible) return
    if (inWindow) {
        MioFocusContent(nowPlaying)
    } else {
        Dialog(
            onDismissRequest = { MioFocus.visible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) { MioFocusContent(nowPlaying) }
    }
}

@Composable
private fun MioFocusContent(nowPlaying: String?) {
    run {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { } }
                .safeDrawingPadding(),
        ) {
            IconButton(
                onClick = { MioFocus.visible = false },
                // Below the desktop window buttons, so it is never mistaken for closing the app.
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 16.dp),
            ) {
                Icon(MetrolistIcons.Default.Close, stringResource(Res.string.mio_focus_close), tint = Color.White)
            }
            Column(
                modifier = Modifier.align(Alignment.Center).widthIn(max = 440.dp).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.mio_focus_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Spacer(Modifier.height(28.dp))
                when (MioFocus.phase) {
                    MioFocusPhase.IDLE -> FocusSetup()
                    MioFocusPhase.DONE -> FocusDone()
                    else -> FocusRunning(nowPlaying)
                }
            }
        }
    }
}

@Composable
private fun FocusSetup() {
    val settings = Settings.shared
    val minutes = Res.string.mio_focus_minutes
    FocusStepper(stringResource(Res.string.mio_focus_work_length), stringResource(minutes, settings.mioFocusWorkMinutes)) {
        settings.mioFocusWorkMinutes = (settings.mioFocusWorkMinutes + it * 5).coerceIn(5, 180)
    }
    FocusStepper(stringResource(Res.string.mio_focus_break_length), stringResource(minutes, settings.mioFocusBreakMinutes)) {
        settings.mioFocusBreakMinutes = (settings.mioFocusBreakMinutes + it).coerceIn(1, 60)
    }
    FocusStepper(stringResource(Res.string.mio_focus_rounds), settings.mioFocusRounds.toString()) {
        settings.mioFocusRounds = (settings.mioFocusRounds + it).coerceIn(1, 12)
    }
    FocusSwitch(stringResource(Res.string.mio_focus_calm_music), settings.mioFocusCalmMusic) { settings.mioFocusCalmMusic = it }
    FocusSwitch(stringResource(Res.string.mio_focus_pause_on_break), settings.mioFocusPauseOnBreak) {
        settings.mioFocusPauseOnBreak = it
    }
    Spacer(Modifier.height(28.dp))
    Button(
        onClick = MioFocus::start,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
    ) {
        Text(stringResource(Res.string.mio_focus_start), fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun FocusRunning(nowPlaying: String?) {
    val accent = MaterialTheme.colorScheme.primary
    val isWork = MioFocus.phase == MioFocusPhase.WORK
    val ringColor = if (isWork) accent else Color(0xFF64B5F6)
    val progress = if (MioFocus.totalSeconds > 0) MioFocus.remainingSeconds.toFloat() / MioFocus.totalSeconds else 0f
    Text(
        stringResource(if (isWork) Res.string.mio_focus_work else Res.string.mio_focus_break),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = ringColor,
    )
    Text(
        stringResource(Res.string.mio_focus_round, MioFocus.round, Settings.shared.mioFocusRounds),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.6f),
    )
    Spacer(Modifier.height(24.dp))
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = ringColor,
            trackColor = Color.White.copy(alpha = 0.1f),
            strokeWidth = 10.dp,
        )
        val minutes = MioFocus.remainingSeconds / 60
        val seconds = MioFocus.remainingSeconds % 60
        Text(
            "$minutes:${seconds.toString().padStart(2, '0')}",
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
    }
    if (!nowPlaying.isNullOrBlank()) {
        Spacer(Modifier.height(20.dp))
        Text(
            nowPlaying,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = MioFocus::stop) { Text(stringResource(Res.string.mio_focus_stop), color = Color.White) }
        Button(
            onClick = MioFocus::togglePause,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier.height(52.dp),
        ) {
            Text(
                stringResource(if (MioFocus.paused) Res.string.mio_focus_resume else Res.string.mio_focus_pause),
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedButton(onClick = MioFocus::skip) { Text(stringResource(Res.string.mio_focus_skip), color = Color.White) }
    }
}

@Composable
private fun FocusDone() {
    Text(
        stringResource(Res.string.mio_focus_done),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(Res.string.mio_focus_done_detail, Settings.shared.mioFocusRounds),
        color = Color.White.copy(alpha = 0.75f),
    )
    Spacer(Modifier.height(28.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = {
            MioFocus.stop()
            MioFocus.visible = false
        }) { Text(stringResource(Res.string.mio_focus_close), color = Color.White) }
        Button(
            onClick = MioFocus::start,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) { Text(stringResource(Res.string.mio_focus_start), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun FocusStepper(
    label: String,
    value: String,
    onStep: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        StepButton("−", stringResource(Res.string.mio_focus_less)) { onStep(-1) }
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(76.dp),
        )
        StepButton("+", stringResource(Res.string.mio_focus_more)) { onStep(1) }
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.size(40.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FocusSwitch(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
