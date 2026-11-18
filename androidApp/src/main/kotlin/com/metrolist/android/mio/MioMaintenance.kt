/**
 * MiMetrolist: tareas automáticas (vienen apagadas por defecto).
 * - Descarga automática por Wi-Fi de tus "me gusta", con un máximo de canciones.
 * - Copia de seguridad semanal de la biblioteca en una carpeta que eliges.
 */

package com.metrolist.android.mio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.DocumentsContract
import com.metrolist.android.backup.BackupRestoreManager
import com.metrolist.android.playback.AudioCacheManager
import com.metrolist.android.playback.SongDownloadManager
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.SongLikeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MioMaintenance {
    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

    fun isOnWifi(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Descarga tus "me gusta" más recientes hasta el máximo elegido y quita los que ya no entran. */
    suspend fun autoDownload(context: Context) {
        val settings = Settings.shared
        if (!settings.mioAutoDownload || !isOnWifi(context)) return
        val manager = GlobalContext.get().get<SongDownloadManager>()
        val target = SongLikeState.likedSongsFlow.value.take(settings.mioAutoDownloadMaxSongs)
        val targetIds = target.map { it.id }.toSet()
        val previous = settings.mioAutoDownloadedIds.split(',').filter(String::isNotBlank).toSet()
        // Solo se borran canciones que esta función descargó y que ya no están en tus "me gusta" recientes.
        val cache = GlobalContext.get().get<AudioCacheManager>()
        previous.filterNot { it in targetIds }.forEach { id -> runCatching { cache.removeDownload(id) } }
        manager.downloadAll(target)
        settings.mioAutoDownloadedIds = targetIds.joinToString(",")
    }

    /** Guarda una copia de la biblioteca una vez por semana en la carpeta elegida. */
    suspend fun autoBackup(context: Context) {
        val settings = Settings.shared
        if (!settings.mioAutoBackup) return
        val folder = settings.mioAutoBackupFolder.takeIf(String::isNotBlank)?.let(Uri::parse) ?: return
        if (System.currentTimeMillis() - settings.mioAutoBackupLastAt < WEEK_MS) return
        withContext(Dispatchers.IO) {
            try {
                val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                val parent = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
                val file =
                    DocumentsContract.createDocument(
                        context.contentResolver,
                        parent,
                        "application/octet-stream",
                        "MiMetrolist-copia-$stamp.backup",
                    ) ?: return@withContext
                val database = GlobalContext.get().get<MusicDatabase>()
                BackupRestoreManager(context.applicationContext, database)
                    .backup(file)
                    .onSuccess { settings.mioAutoBackupLastAt = System.currentTimeMillis() }
                    .onFailure { Timber.w(it, "La copia de seguridad automática falló") }
            } catch (e: Exception) {
                Timber.w(e, "No se pudo crear la copia de seguridad automática")
            }
        }
    }

    /** Todo lo que corre una vez al día y al abrir la app. */
    suspend fun runAll(context: Context) {
        runCatching { MioReleaseNotifier.checkAndNotify(context) }
        runCatching { autoDownload(context) }
        runCatching { autoBackup(context) }
    }
}
