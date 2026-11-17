/**
 * MiMetrolist: título grande y cuadrícula "Explorar" del buscador.
 */

package com.metrolist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.mio_explore_title
import com.metrolist.shared.generated.resources.mio_genres
import com.metrolist.shared.generated.resources.mio_search_title
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.theme.mioCorner
import org.jetbrains.compose.resources.stringResource

internal fun mioExploreEnabled(): Boolean = Settings.shared.mioStyle

internal fun mioSearchTitleVisible(
    query: String,
    isQuerySubmitted: Boolean,
): Boolean = Settings.shared.mioStyle && !isQuerySubmitted && query.isEmpty()

private val TileColors =
    listOf(
        Color(0xFFC2410C),
        Color(0xFF7C3AED),
        Color(0xFF1D4ED8),
        Color(0xFF0F766E),
        Color(0xFFB45309),
        Color(0xFFBE185D),
        Color(0xFF0369A1),
        Color(0xFF6D28D9),
        Color(0xFF15803D),
        Color(0xFFA21CAF),
        Color(0xFFB91C1C),
        Color(0xFF4B5563),
    )

@Composable
internal fun MioSearchTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.mio_search_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
    )
}

/** Cuadrícula de géneros: al tocar uno se busca ese género. */
@Composable
internal fun MioExploreGrid(
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val genres =
        stringResource(Res.string.mio_genres)
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(Res.string.mio_explore_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            genres.chunked(2).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { columnIndex, genre ->
                        val color = TileColors[(rowIndex * 2 + columnIndex) % TileColors.size]
                        MioGenreTile(
                            name = genre,
                            color = color,
                            onClick = { onGenreClick(genre) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MioGenreTile(
    name: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(96.dp)
                .clip(RoundedCornerShape(mioCorner(12.dp)))
                .background(Brush.linearGradient(listOf(color, lerp(color, Color.Black, 0.35f))))
                .clickable(onClick = onClick)
                .padding(12.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
