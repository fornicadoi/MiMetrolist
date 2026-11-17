/**
 * MiMetrolist: grupo de ajustes "Mi estilo" dentro de Apariencia.
 */

@file:Suppress("FunctionName", "ktlint:standard:no-wildcard-imports")

package com.metrolist.shared.ui.settings

import androidx.compose.runtime.Composable
import com.metrolist.shared.generated.resources.*
import com.metrolist.shared.settings.MioAccent
import com.metrolist.shared.settings.MioCorners
import com.metrolist.shared.settings.MioMiniPlayer
import com.metrolist.shared.settings.MioNavBar
import com.metrolist.shared.settings.Settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val accentLabels: Map<MioAccent, StringResource> =
    mapOf(
        MioAccent.VERDE to Res.string.mio_accent_verde,
        MioAccent.AZUL to Res.string.mio_accent_azul,
        MioAccent.MORADO to Res.string.mio_accent_morado,
        MioAccent.ROSA to Res.string.mio_accent_rosa,
        MioAccent.NARANJA to Res.string.mio_accent_naranja,
        MioAccent.ROJO to Res.string.mio_accent_rojo,
        MioAccent.CIAN to Res.string.mio_accent_cian,
        MioAccent.DORADO to Res.string.mio_accent_dorado,
        MioAccent.BLANCO to Res.string.mio_accent_blanco,
        MioAccent.CANCION to Res.string.mio_accent_cancion,
    )

private val cornerLabels: Map<MioCorners, StringResource> =
    mapOf(
        MioCorners.RECTAS to Res.string.mio_corners_rectas,
        MioCorners.SUAVES to Res.string.mio_corners_suaves,
        MioCorners.REDONDAS to Res.string.mio_corners_redondas,
        MioCorners.MUY_REDONDAS to Res.string.mio_corners_muy_redondas,
    )

private val miniPlayerLabels: Map<MioMiniPlayer, StringResource> =
    mapOf(
        MioMiniPlayer.TARJETA to Res.string.mio_mini_player_tarjeta,
        MioMiniPlayer.ORIGINAL to Res.string.mio_mini_player_original,
    )

private val navBarLabels: Map<MioNavBar, StringResource> =
    mapOf(
        MioNavBar.DEGRADADO to Res.string.mio_nav_bar_degradado,
        MioNavBar.SOLIDA to Res.string.mio_nav_bar_solida,
        MioNavBar.ORIGINAL to Res.string.mio_nav_bar_original,
    )

@Composable
private fun <E : Enum<E>> choicesOf(labels: Map<E, StringResource>): List<SettingsChoice> =
    labels.map { (value, label) -> SettingsChoice(value.name, stringResource(label)) }

@Composable
internal fun SettingsPageRegistryBuilder.mioSettingsGroup() {
    val settings = Settings.shared
    group(
        id = "mioStyle",
        title = stringResource(Res.string.mio_group_title),
        description = stringResource(Res.string.mio_group_description),
    ) {
        switch(
            id = "mioStyleEnabled",
            title = stringResource(Res.string.mio_style),
            description = stringResource(Res.string.mio_style_description),
            checked = settings.mioStyle,
        ) { settings.mioStyle = it }

        val enabled = settings.mioStyle
        val accentChoices = choicesOf(accentLabels)
        choice(
            id = "mioAccent",
            title = stringResource(Res.string.mio_accent),
            valueLabel = accentChoices.first { it.id == settings.mioAccent.name }.label,
            choices = accentChoices,
            selectedChoiceId = settings.mioAccent.name,
            enabled = enabled,
        ) { settings.mioAccent = MioAccent.valueOf(it) }

        switch(
            id = "mioOled",
            title = stringResource(Res.string.mio_oled),
            description = stringResource(Res.string.mio_oled_description),
            checked = settings.mioOled,
            enabled = enabled,
        ) { settings.mioOled = it }

        val cornerChoices = choicesOf(cornerLabels)
        choice(
            id = "mioCorners",
            title = stringResource(Res.string.mio_corners),
            valueLabel = cornerChoices.first { it.id == settings.mioCorners.name }.label,
            choices = cornerChoices,
            selectedChoiceId = settings.mioCorners.name,
            enabled = enabled,
        ) { settings.mioCorners = MioCorners.valueOf(it) }

        switch(
            id = "mioBoldTitles",
            title = stringResource(Res.string.mio_bold_titles),
            description = stringResource(Res.string.mio_bold_titles_description),
            checked = settings.mioBoldTitles,
            enabled = enabled,
        ) { settings.mioBoldTitles = it }

        switch(
            id = "mioHomeGreeting",
            title = stringResource(Res.string.mio_home_greeting),
            description = stringResource(Res.string.mio_home_greeting_description),
            checked = settings.mioHomeGreeting,
            enabled = enabled,
        ) { settings.mioHomeGreeting = it }

        switch(
            id = "mioHomeShortcuts",
            title = stringResource(Res.string.mio_home_shortcuts),
            description = stringResource(Res.string.mio_home_shortcuts_description),
            checked = settings.mioHomeShortcuts,
            enabled = enabled,
        ) { settings.mioHomeShortcuts = it }

        val miniPlayerChoices = choicesOf(miniPlayerLabels)
        choice(
            id = "mioMiniPlayer",
            title = stringResource(Res.string.mio_mini_player),
            valueLabel = miniPlayerChoices.first { it.id == settings.mioMiniPlayer.name }.label,
            choices = miniPlayerChoices,
            selectedChoiceId = settings.mioMiniPlayer.name,
            enabled = enabled,
        ) { settings.mioMiniPlayer = MioMiniPlayer.valueOf(it) }

        val navBarChoices = choicesOf(navBarLabels)
        choice(
            id = "mioNavBar",
            title = stringResource(Res.string.mio_nav_bar),
            valueLabel = navBarChoices.first { it.id == settings.mioNavBar.name }.label,
            choices = navBarChoices,
            selectedChoiceId = settings.mioNavBar.name,
            enabled = enabled,
        ) { settings.mioNavBar = MioNavBar.valueOf(it) }
    }
}
