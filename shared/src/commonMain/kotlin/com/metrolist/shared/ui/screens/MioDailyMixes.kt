/**
 * MiMetrolist: mezclas diarias armadas con el historial local (cambian cada día).
 */

package com.metrolist.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.historyStatisticsSnapshot
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_mix_name
import com.metrolist.shared.generated.resources.mio_mixes_title
import com.metrolist.shared.generated.resources.mio_mix_recent
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.images.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import kotlin.random.Random
import kotlin.time.Clock

internal fun mioMixesEnabled(): Boolean = Settings.shared.mioStyle && Settings.shared.mioDailyMixes

private data class DailyMix(
    val number: Int,
    val leadArtist: String?,
    val subtitle: String,
    val cover: String?,
    val songs: List<SongItem>,
)

private val MixColors =
    listOf(
        Color(0xFF0EA5E9),
        Color(0xFFF97316),
        Color(0xFF22C55E),
        Color(0xFFE11D48),
        Color(0xFFA855F7),
        Color(0xFFEAB308),
    )

private const val DAY_MS = 86_400_000L

/** Arma hasta 6 mezclas: una por cada artista que más escuchas, más "Lo que escuchas ahora". */
private suspend fun buildDailyMixes(): List<DailyMix> =
    withContext(Dispatchers.Default) {
        val db = KoinPlatform.getKoin().get<MusicDatabase>()
        val now = Clock.System.now().toEpochMilliseconds()
        val snapshot = db.historyStatisticsSnapshot(now - 120 * DAY_MS, now)
        val songs = snapshot.songItems.filter { it.artists.isNotEmpty() }
        if (songs.size < 8) return@withContext emptyList()
        val random = Random(now / DAY_MS)
        val byArtist = songs.groupBy { it.artists.first().name }
        val leaders =
            byArtist.entries
                .sortedByDescending { it.value.size }
                .take(5)
        val mixes = mutableListOf<DailyMix>()
        leaders.forEachIndexed { i, (artist, own) ->
            // Canciones del artista principal mezcladas con otros artistas que escuchas.
            val others = songs.filter { it.artists.first().name != artist }.shuffled(random).take(18)
            val mixSongs = (own.shuffled(random).take(12) + others).distinctBy { it.id }.shuffled(random)
            val featured =
                others
                    .map { it.artists.first().name }
                    .distinct()
                    .take(3)
            mixes +=
                DailyMix(
                    number = i + 1,
                    leadArtist = artist,
                    subtitle = (listOf(artist) + featured).joinToString(", "),
                    cover = own.first().thumbnail,
                    songs = mixSongs,
                )
        }
        val recent = db.historyStatisticsSnapshot(now - 14 * DAY_MS, now).songItems.take(40)
        if (recent.size >= 8) {
            mixes +=
                DailyMix(
                    number = 0,
                    leadArtist = null,
                    subtitle = recent.flatMap { it.artists.map { a -> a.name } }.distinct().take(4).joinToString(", "),
                    cover = recent.first().thumbnail,
                    songs = recent.shuffled(random),
                )
        }
        mixes
    }

@Composable
internal fun MioDailyMixesRow(
    onPlaySongs: (List<SongItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mixes by remember { mutableStateOf<List<DailyMix>>(emptyList()) }
    LaunchedEffect(Unit) {
        mixes = runCatching { buildDailyMixes() }.getOrDefault(emptyList())
    }
    AnimatedVisibility(visible = mixes.isNotEmpty(), enter = fadeIn(tween(450)) + expandVertically(tween(450))) {
        MixesContent(mixes, onPlaySongs, modifier)
    }
}

@Composable
private fun MixesContent(
    mixes: List<DailyMix>,
    onPlaySongs: (List<SongItem>) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            stringResource(Res.string.mio_mixes_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(mixes, key = { "mix-${it.number}" }) { mix ->
                MixCard(mix, onClick = { onPlaySongs(mix.songs) })
            }
        }
    }
}

@Composable
private fun MixCard(
    mix: DailyMix,
    onClick: () -> Unit,
) {
    val color = MixColors[(if (mix.number == 0) MixColors.lastIndex else mix.number - 1) % MixColors.size]
    val label =
        if (mix.number == 0) stringResource(Res.string.mio_mix_recent) else stringResource(Res.string.mio_mix_name, mix.number)
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        Box(Modifier.size(150.dp).clip(RoundedCornerShape(6.dp))) {
            AsyncImage(url = mix.cover, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.95f)))),
            )
            Box(Modifier.fillMaxWidth().height(6.dp).background(color).align(Alignment.TopCenter))
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            mix.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
