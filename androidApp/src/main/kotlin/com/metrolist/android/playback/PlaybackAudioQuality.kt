package com.metrolist.android.playback

import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.shared.settings.Settings

internal fun playbackAudioQuality(): AudioQuality =
    try {
        AudioQuality.valueOf(Settings.shared.audioQuality)
    } catch (_: IllegalArgumentException) {
        AudioQuality.AUTO
    }

/** MiMetrolist: las descargas usan el audio más liviano si "Descargas ligeras" está activo. */
internal fun downloadAudioQuality(): AudioQuality = if (Settings.shared.mioDownloadLight) AudioQuality.LOW else playbackAudioQuality()
