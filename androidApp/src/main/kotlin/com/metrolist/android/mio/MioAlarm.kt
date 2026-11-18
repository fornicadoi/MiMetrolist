/**
 * MiMetrolist: despertador con música. Programa una alarma diaria que reproduce
 * tus "me gusta", tus descargas o tu top 100 en orden aleatorio, subiendo el volumen poco a poco.
 */

package com.metrolist.android.mio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.metrolist.android.App
import com.metrolist.android.MainActivity
import com.metrolist.android.playback.PlayerConnection
import com.metrolist.android.utils.launchAsync
import com.metrolist.android.widget.widgetBuiltinSongs
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.model.BuiltinPlaylist
import com.metrolist.shared.settings.MioAlarmSource
import com.metrolist.shared.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import timber.log.Timber
import java.util.Calendar

object MioAlarmScheduler {
    private const val REQUEST_CODE = 7301
    const val ACTION_ALARM = "com.metrolist.android.mio.ACTION_ALARM"

    private fun alarmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, MioAlarmReceiver::class.java).setAction(ACTION_ALARM),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Próxima hora de la alarma en milisegundos. */
    fun nextTrigger(
        hour: Int,
        minute: Int,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }

    fun update(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        manager.cancel(pending)
        val settings = Settings.shared
        if (!settings.mioAlarmEnabled) return
        val trigger = nextTrigger(settings.mioAlarmHour, settings.mioAlarmMinute)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        try {
            if (canExact) {
                val show =
                    PendingIntent.getActivity(
                        context,
                        REQUEST_CODE,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } catch (e: SecurityException) {
            Timber.w(e, "No se pudo programar el despertador exacto")
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }
}

class MioAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                MioAlarmScheduler.update(context)
                return
            }

            MioAlarmScheduler.ACTION_ALARM -> Unit

            else -> return
        }
        // Deja programada la alarma de mañana antes de sonar.
        MioAlarmScheduler.update(context)
        launchAsync {
            try {
                val app = context.applicationContext as App
                app.startup.await()
                val builtin =
                    when (Settings.shared.mioAlarmSource) {
                        MioAlarmSource.ME_GUSTA -> BuiltinPlaylist.LIKED
                        MioAlarmSource.DESCARGADAS -> BuiltinPlaylist.DOWNLOADED
                        MioAlarmSource.TOP -> BuiltinPlaylist.TOP
                    }
                val songs =
                    withContext(Dispatchers.IO) {
                        GlobalContext.get().get<MusicDatabase>().widgetBuiltinSongs(builtin)
                    }.shuffled()
                if (songs.isEmpty()) return@launchAsync
                val connection = GlobalContext.get().get<PlayerConnection>()
                connection.awaitConnected()
                connection.setVolume(0.15f)
                connection.playQueue(songs, queueTitle = null, finite = true)
                // Sube el volumen poco a poco durante 40 segundos.
                for (step in 1..8) {
                    delay(5_000)
                    connection.setVolume((0.15f + step * 0.85f / 8).coerceAtMost(1f))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "El despertador no pudo empezar a sonar")
            }
        }
    }
}
