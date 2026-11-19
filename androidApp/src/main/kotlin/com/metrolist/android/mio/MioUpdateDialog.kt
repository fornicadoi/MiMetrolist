/**
 * MiMetrolist: aviso de nueva versión con las novedades de esa versión.
 */

package com.metrolist.android.mio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.metrolist.android.update.GitHubRelease
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_update_later
import com.metrolist.shared.generated.resources.mio_update_news
import com.metrolist.shared.generated.resources.mio_update_now
import com.metrolist.shared.generated.resources.mio_update_title
import com.metrolist.shared.generated.resources.mio_update_version
import com.metrolist.shared.generated.resources.mio_update_version_date
import com.metrolist.shared.ui.icons.MetrolistIcons
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val versionPattern = Regex("""MiMetrolist\s+(\S+?)(?:\+\w+)?\s""")

/** "14.0.1" from a release named "MiMetrolist 14.0.1+abc1234 (2026-11-19)". */
internal fun GitHubRelease.mioVersion(): String = name?.let { versionPattern.find("$it ")?.groupValues?.get(1) } ?: version

/** The "- ..." lines under "## Novedades" in the release notes. */
internal fun GitHubRelease.mioNews(): List<String> =
    body
        .orEmpty()
        .substringAfter("## Novedades", missingDelimiterValue = "")
        .substringBefore("\n## ")
        .lineSequence()
        .map(String::trim)
        .filter { it.startsWith("- ") }
        .map { it.removePrefix("- ").trim() }
        .filter(String::isNotEmpty)
        .take(12)
        .toList()

private fun GitHubRelease.mioBuiltAt(): String? =
    runCatching {
        val stamp = apk?.updatedAt ?: publishedAt ?: return null
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .format(Instant.parse(stamp).atZone(ZoneId.systemDefault()))
    }.getOrNull()

@Composable
internal fun MioUpdateDialog(
    release: GitHubRelease,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = onLater) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1C1C1C)) {
            Column(Modifier.padding(24.dp)) {
                Box(
                    Modifier.size(52.dp).background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MetrolistIcons.Outlined.NewReleases, null, tint = accent, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(Res.string.mio_update_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                val version = release.mioVersion()
                Text(
                    release.mioBuiltAt()?.let { stringResource(Res.string.mio_update_version_date, version, it) }
                        ?: stringResource(Res.string.mio_update_version, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
                val news = release.mioNews()
                if (news.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(Res.string.mio_update_news),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        news.forEach { line ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 7.dp).size(6.dp).background(accent, CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Text(line, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onUpdate,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                ) {
                    Text(stringResource(Res.string.mio_update_now), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.mio_update_later), color = Color.White.copy(alpha = 0.75f))
                }
            }
        }
    }
}
