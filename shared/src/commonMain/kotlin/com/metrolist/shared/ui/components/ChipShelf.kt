package com.metrolist.shared.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.chip_content_description
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.tv.tvFocusHighlight
import com.metrolist.shared.ui.utils.carouselScroll
import org.jetbrains.compose.resources.stringResource

private val ChipShelfHeight = 48.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ChipShelf(
    options: List<T>,
    selectedOption: T?,
    onSelectionChange: (T?) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    allLabel: String? = null,
    pinnedLabel: String? = null,
    onPinnedClick: () -> Unit = {},
    selectionRequired: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp),
    leadingContent: (@Composable () -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ChipShelfHeight)
                .clipToBounds()
                .focusGroup()
                .selectableGroup()
                .carouselScroll(scrollState)
                .horizontalScroll(scrollState)
                .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.invoke()
        pinnedLabel?.let { text ->
            ShelfChip(
                text = text,
                selected = true,
                onClick = onPinnedClick,
            )
        }
        allLabel?.let { text ->
            ShelfChip(
                text = text,
                selected = selectedOption == null,
                onClick = { onSelectionChange(null) },
            )
        }
        options.forEach { option ->
            val selected = selectedOption == option
            ShelfChip(
                text = label(option),
                selected = selected,
                onClick = {
                    onSelectionChange(
                        if (selected && !selectionRequired) null else option,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShelfChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerHeight = ButtonDefaults.ExtraSmallContainerHeight
    val chipDescription = stringResource(Res.string.chip_content_description, text)
    ExpressiveFilterChip(
        selected = selected,
        onClick = onClick,
        modifier =
            Modifier.height(containerHeight).clearAndSetSemantics {
                contentDescription = chipDescription
                role = Role.Button
                this.selected = selected
                onClick {
                    onClick()
                    true
                }
            },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (Settings.shared.mioStyle) {
        MioPillChip(selected, onClick, label, modifier, enabled)
        return
    }
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        enabled = enabled,
        shapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.ExtraSmallContainerHeight),
        colors =
            if (Settings.shared.mioStyle) {
                // MiMetrolist: píldoras grises; la seleccionada usa el color de acento.
                ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
        modifier =
            modifier
                .height(ButtonDefaults.ExtraSmallContainerHeight)
                .tvFocusHighlight(ToggleButtonDefaults.shape)
                .semantics { this.selected = selected },
    ) {
        label()
    }
}

/** MiMetrolist: píldora con borde gris; la seleccionada se rellena con el color de acento. */
@Composable
private fun MioPillChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    val container = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier =
            modifier
                .height(32.dp)
                .clip(CircleShape)
                .background(container)
                .then(if (selected) Modifier else Modifier.border(1.dp, Color(0xFF777777), CircleShape))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp)
                .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) { label() }
    }
}
