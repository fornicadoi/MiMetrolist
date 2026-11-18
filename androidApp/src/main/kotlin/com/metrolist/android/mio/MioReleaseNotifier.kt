/**
 * MiMetrolist: avisa con una notificación cuando un artista que sigues publica algo nuevo.
 * Revisa al abrir la app (cada 12 h como mínimo) y una vez al día en segundo plano.
 */

package com.metrolist.android.mio

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.metrolist.android.App
import com.metrolist.android.MainActivity
import com.metrolist.android.R
import com.metrolist.android.utils.launchAsync
import com.metrolist.shared.api.MioReleaseChecker
import com.metrolist.shared.settings.Settings
import kotlinx.coroutines.CancellationException
import timber.log.Timber

object MioReleaseNotifier {
    private const val CHANNEL_ID = "mio_releases"
    private const val REQUEST_CODE = 7302
    const val ACTION_CHECK = "com.metrolist.android.mio.ACTION_CHECK_RELEASES"

    fun scheduleDaily(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, MioReleaseReceiver::class.java).setAction(ACTION_CHECK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        manager.cancel(pending)
        if (!Settings.shared.mioReleaseAlerts) return
        manager.setInexactRepeating(
            AlarmManager.RTC,
            MioAlarmScheduler.nextTrigger(13, 0),
            AlarmManager.INTERVAL_DAY,
            pending,
        )
    }

    suspend fun checkAndNotify(context: Context) {
        if (!MioReleaseChecker.shouldCheck()) return
        val releases = MioReleaseChecker.check()
        if (releases.isEmpty()) return
        val nm = NotificationManagerCompat.from(context)
        nm.createNotificationChannel(
            NotificationChannelCompat
                .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Lanzamientos nuevos")
                .setDescription("Avisos cuando un artista que sigues publica música nueva")
                .build(),
        )
        if (!nm.areNotificationsEnabled()) return
        releases.take(5).forEachIndexed { index, release ->
            val open =
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE + index + 1,
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .setData(android.net.Uri.parse("https://music.youtube.com/browse/${release.album.id}")),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Nuevo de ${release.artistName}")
                    .setContentText(release.album.title)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
            try {
                nm.notify("mio_release_${release.album.id}".hashCode(), notification)
            } catch (e: SecurityException) {
                Timber.w(e, "Sin permiso para notificar lanzamientos")
            }
        }
    }
}

class MioReleaseReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                MioReleaseNotifier.scheduleDaily(context)
                return
            }

            MioReleaseNotifier.ACTION_CHECK -> Unit

            else -> return
        }
        launchAsync {
            try {
                (context.applicationContext as App).startup.await()
                MioReleaseNotifier.checkAndNotify(context)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "No se pudieron revisar los lanzamientos")
            }
        }
    }
}
