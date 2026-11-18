/**
 * MiMetrolist: "Tu año en música", un resumen animado del año a partir del historial local.
 */

package com.metrolist.shared.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.shared.db.HistoryStatisticsSnapshot
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.historyStatisticsSnapshot
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_recap_card_subtitle
import com.metrolist.shared.generated.resources.mio_recap_card_title
import com.metrolist.shared.generated.resources.mio_recap_empty
import com.metrolist.shared.generated.resources.mio_recap_hour
import com.metrolist.shared.generated.resources.mio_recap_hour_title
import com.metrolist.shared.generated.resources.mio_recap_intro
import com.metrolist.shared.generated.resources.mio_recap_minutes
import com.metrolist.shared.generated.resources.mio_recap_minutes_title
import com.metrolist.shared.generated.resources.mio_recap_outro
import com.metrolist.shared.generated.resources.mio_recap_plays
import com.metrolist.shared.generated.resources.mio_recap_top_artist_title
import com.metrolist.shared.generated.resources.mio_recap_top_artists_title
import com.metrolist.shared.generated.resources.mio_recap_top_songs_title
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.icons.MetrolistIcons
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

internal fun mioRecapEnabled(): Boolean = Settings.shared.mioStyle

private data class RecapData(
    val year: Int,
    val snapshot: HistoryStatisticsSnapshot,
    val peakHour: Int?,
)

private val SlideColors =
    listOf(
        listOf(Color(0xFF1E3A8A), Color(0xFF0B0B0F)),
        listOf(Color(0xFF7C2D12), Color(0xFF0B0B0F)),
        listOf(Color(0xFF14532D), Color(0xFF0B0B0F)),
        listOf(Color(0xFF581C87), Color(0xFF0B0B0F)),
        listOf(Color(0xFF831843), Color(0xFF0B0B0F)),
        listOf(Color(0xFF134E4A), Color(0xFF0B0B0F)),
        listOf(Color(0xFF3F3F46), Color(0xFF0B0B0F)),
    )

/** Tarjeta que abre el resumen; va arriba de la pantalla de estadísticas. */
@Composable
internal fun MioYearRecapCard(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val year = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFFDB2777), Color(0xFFF59E0B))))
                .clickable { open = true }
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.mio_recap_card_title, year),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text(
                stringResource(Res.string.mio_recap_card_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Icon(MetrolistIcons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
    }
    if (open) MioYearRecapDialog(onDismiss = { open = false })
}

@Composable
private fun MioYearRecapDialog(onDismiss: () -> Unit) {
    var data by remember { mutableStateOf<RecapData?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            val tz = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val year = now.toLocalDateTime(tz).year
            val start = LocalDate(year, 1, 1).atStartOfDayIn(tz).toEpochMilliseconds()
            val db = KoinPlatform.getKoin().get<MusicDatabase>()
            val snapshot = db.historyStatisticsSnapshot(start, now.toEpochMilliseconds())
            val peak = snapshot.hourly.maxByOrNull { it.playCount }?.bucket?.toIntOrNull()
            data = RecapData(year, snapshot, peak)
        } catch (e: Exception) {
            failed = true
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0F))) {
            val recap = data
            when {
                recap != null && recap.snapshot.totals.playCount > 0 -> RecapStories(recap, onDismiss)
                recap != null || failed -> RecapMessage(stringResource(Res.string.mio_recap_empty), onDismiss)
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            }
        }
    }
}

@Composable
private fun RecapMessage(
    text: String,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().clickable(onClick = onDismiss).padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun RecapStories(
    recap: RecapData,
    onDismiss: () -> Unit,
) {
    val slides = remember(recap) { buildSlides(recap) }
    var index by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(index) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 6000, easing = LinearEasing))
        if (index < slides.lastIndex) index++ else onDismiss()
    }
    val colors = SlideColors[index % SlideColors.size]
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors))
            .clickable { if (index < slides.lastIndex) index++ else onDismiss() },
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                slides.indices.forEach { i ->
                    val fill = if (i < index) 1f else if (i == index) progress.value else 0f
                    Box(
                        Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.3f)),
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Color.White))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(MetrolistIcons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }
            AnimatedContent(
                targetState = index,
                transitionSpec = { (fadeIn(tween(400)) + scaleIn(initialScale = 0.92f)).togetherWith(fadeOut(tween(200))) },
                modifier = Modifier.fillMaxSize(),
            ) { i ->
                Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    slides[i]()
                }
            }
        }
    }
}

private fun buildSlides(recap: RecapData): List<@Composable () -> Unit> {
    val s = recap.snapshot
    val minutes = s.totals.playTimeMs / 60_000
    val topArtist = s.artists.firstOrNull()
    val slides = mutableListOf<@Composable () -> Unit>()
    slides += { BigText(stringResource(Res.string.mio_recap_intro, recap.year)) }
    if (minutes > 0) {
        slides += {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Caption(stringResource(Res.string.mio_recap_minutes_title))
                Text(
                    minutes.toString(),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                )
                Caption(stringResource(Res.string.mio_recap_minutes))
            }
        }
    }
    if (topArtist != null) {
        slides += {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Caption(stringResource(Res.string.mio_recap_top_artist_title))
                Spacer(Modifier.height(20.dp))
                AsyncImage(
                    url = topArtist.thumbnailUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(200.dp).clip(CircleShape),
                )
                Spacer(Modifier.height(20.dp))
                BigText(topArtist.name)
                Caption(stringResource(Res.string.mio_recap_plays, topArtist.playCount))
            }
        }
    }
    if (s.songs.isNotEmpty()) {
        slides += {
            RankedList(
                title = stringResource(Res.string.mio_recap_top_songs_title),
                rows = s.songs.take(5).map { Triple(it.title, it.artists, it.thumbnailUrl) },
                circle = false,
            )
        }
    }
    if (s.artists.size > 1) {
        slides += {
            RankedList(
                title = stringResource(Res.string.mio_recap_top_artists_title),
                rows = s.artists.take(5).map { Triple(it.name, "", it.thumbnailUrl) },
                circle = true,
            )
        }
    }
    recap.peakHour?.let { hour ->
        slides += {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Caption(stringResource(Res.string.mio_recap_hour_title))
                Text(
                    stringResource(Res.string.mio_recap_hour, hour.toString().padStart(2, '0')),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
    slides += { BigText(stringResource(Res.string.mio_recap_outro)) }
    return slides
}

@Composable
private fun BigText(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.85f),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RankedList(
    title: String,
    rows: List<Triple<String, String, String?>>,
    circle: Boolean,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        rows.forEachIndexed { i, (name, subtitle, image) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${i + 1}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(40.dp),
                )
                AsyncImage(
                    url = image,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(if (circle) CircleShape else RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
