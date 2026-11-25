/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class PlaybackWidgetReceiver : GlanceAppWidgetReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refresh(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refresh(context)
    }

    /**
     * Redraws from the last saved state first, so a widget the launcher re-inflated (after the app
     * process was killed) leaves the loading spinner even when MusicService is not running, then
     * asks the service for fresh data.
     */
    private fun refresh(context: Context) {
        val pending = goAsync()
        scope.launch {
            try {
                runCatching { glanceAppWidget.updateAll(context) }
                requestPlaybackWidgetUpdate(context)
            } finally {
                pending.finish()
            }
        }
    }
}
