/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.ComponentName
import android.content.Context
import moe.rukamori.archivetune.playback.MusicService

internal suspend fun requestPlaybackWidgetUpdate(context: Context) {
    // A BroadcastReceiver context may not bind services (ReceiverCallNotAllowedException),
    // so bind through the application context.
    val appContext = context.applicationContext
    val serviceIntent = android.content.Intent(appContext, MusicService::class.java)
    val connection =
        object : android.content.ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: android.os.IBinder?,
            ) {
                val service = (binder as? MusicService.MusicBinder)?.service
                service?.updateWidget()
                runCatching { appContext.unbindService(this) }
            }

            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }

    runCatching {
        appContext.bindService(serviceIntent, connection, 0)
    }.onFailure { error ->
        android.util.Log.w("PlaybackWidgetUpdate", "Could not bind MusicService to refresh widgets", error)
    }
}
