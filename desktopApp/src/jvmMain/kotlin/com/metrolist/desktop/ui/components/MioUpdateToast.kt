/**
 * MiMetrolist: aviso de actualización con diseño propio, en la esquina inferior derecha.
 */

package com.metrolist.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.state.app.UiState
import com.metrolist.desktop.update.GitHubDesktopRelease
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.close
import com.metrolist.shared.generated.resources.mio_update_later
import com.metrolist.shared.generated.resources.mio_update_ready
import com.metrolist.shared.generated.resources.mio_update_ready_at
import com.metrolist.shared.generated.resources.mio_update_title_native
import com.metrolist.shared.generated.resources.mio_update_title_nightly
import com.metrolist.shared.generated.resources.mio_update_view
import com.metrolist.shared.generated.resources.updates_available_title
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.settings.SettingsPageId
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Title shared by the notice and the updates page. */
@Composable
internal fun GitHubDesktopRelease.mioUpdateTitle(): String =
    when {
        isMioNativeNightly -> stringResource(Res.string.mio_update_title_native)
        isNightly -> stringResource(Res.string.mio_update_title_nightly)
        else -> stringResource(Res.string.updates_available_title, version)
    }

private fun GitHubDesktopRelease.builtAt(): String? =
    runCatching {
        val stamp = nightlyAsset?.updatedAt ?: publishedAt ?: return null
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .format(Instant.parse(stamp).atZone(ZoneId.systemDefault()))
    }.getOrNull()

@Composable
fun MioUpdateToast(modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<GitHubDesktopRelease?>(null) }
    val notice = UiState.mioUpdateNotice
    if (notice != null) shown = notice
    AnimatedVisibility(
        visible = notice != null,
        modifier = modifier,
        enter = slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(320)),
        exit = slideOutVertically(tween(220)) { it / 2 } + fadeOut(tween(220)),
    ) {
        val release = shown ?: return@AnimatedVisibility
        val accent = MaterialTheme.colorScheme.primary
        Surface(
            modifier = Modifier.width(400.dp).shadow(24.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1C1C1C),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        ) {
            Column(Modifier.padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(42.dp).background(accent.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(MetrolistIcons.Outlined.NewReleases, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f).padding(top = 2.dp)) {
                        Text(
                            release.mioUpdateTitle(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            release.builtAt()?.let { stringResource(Res.string.mio_update_ready_at, it) }
                                ?: stringResource(Res.string.mio_update_ready),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = { UiState.mioUpdateNotice = null }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            MetrolistIcons.Default.Close,
                            contentDescription = stringResource(Res.string.close),
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { UiState.mioUpdateNotice = null }) {
                        Text(stringResource(Res.string.mio_update_later), color = Color.White.copy(alpha = 0.75f))
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        onClick = {
                            UiState.mioUpdateNotice = null
                            UiState.selectedSettingsPage = SettingsPageId.UPDATES
                            UiState.showSettings = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                    ) {
                        Text(stringResource(Res.string.mio_update_view), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
