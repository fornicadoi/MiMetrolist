/**
 * MiMetrolist: grupo de ajustes "Mi estilo" dentro de Apariencia.
 */

@file:Suppress("FunctionName", "ktlint:standard:no-wildcard-imports")

package com.metrolist.shared.ui.settings

import androidx.compose.runtime.Composable
import com.metrolist.shared.generated.resources.*
import com.metrolist.shared.settings.MioAccent
import com.metrolist.shared.settings.MioAlarmSource
import com.metrolist.shared.settings.MioCorners
import com.metrolist.shared.settings.MioMiniPlayer
import com.metrolist.shared.settings.MioNavBar
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.utils.isDesktop
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
            id = "mioDailyMixes",
            title = stringResource(Res.string.mio_daily_mixes),
            description = stringResource(Res.string.mio_daily_mixes_description),
            checked = settings.mioDailyMixes,
            enabled = enabled,
        ) { settings.mioDailyMixes = it }

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

        switch(
            id = "mioDownloadLight",
            title = stringResource(Res.string.mio_download_light),
            description = stringResource(Res.string.mio_download_light_description),
            checked = settings.mioDownloadLight,
        ) { settings.mioDownloadLight = it }
    }
}

private val alarmSourceLabels: Map<MioAlarmSource, StringResource> =
    mapOf(
        MioAlarmSource.ME_GUSTA to Res.string.mio_alarm_source_me_gusta,
        MioAlarmSource.DESCARGADAS to Res.string.mio_alarm_source_descargadas,
        MioAlarmSource.TOP to Res.string.mio_alarm_source_top,
    )

@Composable
internal fun SettingsPageRegistryBuilder.mioAlarmGroup() {
    if (isDesktop) return
    val settings = Settings.shared
    group(id = "mioAlarm", title = stringResource(Res.string.mio_alarm_group)) {
        switch(
            id = "mioReleaseAlerts",
            title = stringResource(Res.string.mio_release_alerts),
            description = stringResource(Res.string.mio_release_alerts_description),
            checked = settings.mioReleaseAlerts,
        ) { settings.mioReleaseAlerts = it }
        switch(
            id = "mioAlarmEnabled",
            title = stringResource(Res.string.mio_alarm_enabled),
            description = stringResource(Res.string.mio_alarm_enabled_description),
            checked = settings.mioAlarmEnabled,
        ) { settings.mioAlarmEnabled = it }
        val on = settings.mioAlarmEnabled
        slider(
            id = "mioAlarmHour",
            title = stringResource(Res.string.mio_alarm_hour),
            valueLabel = settings.mioAlarmHour.toString().padStart(2, '0'),
            value = settings.mioAlarmHour.toFloat(),
            range = 0f..23f,
            steps = 22,
            enabled = on,
        ) { settings.mioAlarmHour = it.toInt().coerceIn(0, 23) }
        slider(
            id = "mioAlarmMinute",
            title = stringResource(Res.string.mio_alarm_minute),
            valueLabel = settings.mioAlarmMinute.toString().padStart(2, '0'),
            value = settings.mioAlarmMinute.toFloat(),
            range = 0f..55f,
            steps = 10,
            enabled = on,
        ) { settings.mioAlarmMinute = (it / 5f).toInt().times(5).coerceIn(0, 55) }
        val sources = choicesOf(alarmSourceLabels)
        choice(
            id = "mioAlarmSource",
            title = stringResource(Res.string.mio_alarm_source),
            valueLabel = sources.first { it.id == settings.mioAlarmSource.name }.label,
            choices = sources,
            selectedChoiceId = settings.mioAlarmSource.name,
            enabled = on,
        ) { settings.mioAlarmSource = MioAlarmSource.valueOf(it) }
    }
}

@Composable
internal fun SettingsPageRegistryBuilder.mioAutoGroup() {
    if (isDesktop) return
    val settings = Settings.shared
    group(id = "mioAuto", title = stringResource(Res.string.mio_auto_group)) {
        switch(
            id = "mioAutoDownload",
            title = stringResource(Res.string.mio_auto_download),
            description = stringResource(Res.string.mio_auto_download_description),
            checked = settings.mioAutoDownload,
        ) { settings.mioAutoDownload = it }
        slider(
            id = "mioAutoDownloadMax",
            title = stringResource(Res.string.mio_auto_download_max),
            valueLabel = settings.mioAutoDownloadMaxSongs.toString(),
            value = settings.mioAutoDownloadMaxSongs.toFloat(),
            range = 50f..1000f,
            steps = 18,
            enabled = settings.mioAutoDownload,
        ) { settings.mioAutoDownloadMaxSongs = ((it / 50f).toInt() * 50).coerceIn(50, 1000) }
        switch(
            id = "mioAutoBackup",
            title = stringResource(Res.string.mio_auto_backup),
            description = stringResource(Res.string.mio_auto_backup_description),
            checked = settings.mioAutoBackup,
        ) {
            settings.mioAutoBackup = it
            if (!it) settings.mioAutoBackupFolder = ""
        }
    }
}

@Composable
internal fun SettingsPageRegistryBuilder.mioPanelsGroup() {
    if (!isDesktop) return
    val settings = Settings.shared
    group(id = "mioPanels", title = stringResource(Res.string.mio_panels_group)) {
        switch(
            id = "mioQueueFloating",
            title = stringResource(Res.string.mio_panels_queue_floating),
            description = stringResource(Res.string.mio_panels_floating_description),
            checked = settings.mioQueueFloating,
        ) { settings.mioQueueFloating = it }
        switch(
            id = "mioLyricsFloating",
            title = stringResource(Res.string.mio_panels_lyrics_floating),
            description = stringResource(Res.string.mio_panels_floating_description),
            checked = settings.mioLyricsFloating,
        ) { settings.mioLyricsFloating = it }
        switch(
            id = "mioFloatOnTop",
            title = stringResource(Res.string.mio_panels_on_top),
            description = stringResource(Res.string.mio_panels_on_top_description),
            checked = settings.mioFloatOnTop,
        ) { settings.mioFloatOnTop = it }
        switch(
            id = "mioPanesLeft",
            title = stringResource(Res.string.mio_panels_left),
            description = stringResource(Res.string.mio_panels_left_description),
            checked = settings.mioPanesLeft,
        ) { settings.mioPanesLeft = it }
    }
}
