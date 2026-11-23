/**
 * MiMetrolist: portadas guardadas con las descargas, para verlas sin conexión.
 */

package com.metrolist.android.mio

import android.content.Context
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.metrolist.android.playback.DownloadedArtwork
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.youtube.playback.PlaybackNetworkStateStore
import java.io.File

internal object MioOfflineArtwork {
    private const val PLAYLIST_DIRECTORY = "mio-playlist-artwork"

    /** Keeps the cover of a downloaded playlist; song covers are already kept by the download itself. */
    suspend fun retainPlaylistCover(
        context: Context,
        database: MusicDatabase,
        playlistId: String,
    ) {
        if (!PlaybackNetworkStateStore.current.connected) return
        val cover = database.dao().findPlaylistEntity(playlistId)?.thumbnailUrl ?: return
        DownloadedArtwork(context, PLAYLIST_DIRECTORY).retain(cover)
    }

    private fun savedFile(
        context: Context,
        url: String,
    ): File? =
        listOf(DownloadedArtwork(context), DownloadedArtwork(context, PLAYLIST_DIRECTORY))
            .firstNotNullOfOrNull { store -> store.file(url).takeIf { it.length() > 0 } }

    /** Serves saved covers when there is no connection, or when the network copy cannot be loaded. */
    class ImageInterceptor(
        context: Context,
    ) : Interceptor {
        private val appContext = context.applicationContext

        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val url = chain.request.data as? String ?: return chain.proceed()
            if (!url.startsWith("http")) return chain.proceed()
            if (!PlaybackNetworkStateStore.current.connected) {
                savedFile(appContext, url)?.let { file ->
                    return chain.withRequest(chain.request.newBuilder().data(file).build()).proceed()
                }
            }
            val result = chain.proceed()
            if (result is SuccessResult) return result
            val file = savedFile(appContext, url) ?: return result
            return chain.withRequest(chain.request.newBuilder().data(file).build()).proceed()
        }
    }
}
