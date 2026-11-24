/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import moe.rukamori.archivetune.playback.MusicService

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.PLAY_PAUSE)
    }
}

class SkipNextAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.SKIP_NEXT)
    }
}

class SkipPrevAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.SKIP_PREV)
    }
}

class PlayQueueItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val mediaId = parameters[WidgetActionKeys.MediaId]?.takeIf { it.isNotBlank() } ?: return
        sendWidgetAction(context, WidgetServiceActions.PLAY_QUEUE_ITEM) {
            putExtra(WidgetServiceActions.EXTRA_MEDIA_ID, mediaId)
            putExtra(WidgetServiceActions.EXTRA_QUEUE_INDEX, parameters[WidgetActionKeys.QueueIndex] ?: NoQueueIndex)
        }
    }
}

class PlaySongAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val mediaId = parameters[WidgetActionKeys.MediaId]?.takeIf { it.isNotBlank() } ?: return
        sendWidgetAction(context, WidgetServiceActions.PLAY_SONG) {
            putExtra(WidgetServiceActions.EXTRA_MEDIA_ID, mediaId)
        }
    }
}

class SleepTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.SLEEP_TIMER)
    }
}

class ToggleLikeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.TOGGLE_LIKE)
    }
}

class ToggleShuffleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.TOGGLE_SHUFFLE)
    }
}

class ToggleRepeatAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        sendWidgetAction(context, WidgetServiceActions.TOGGLE_REPEAT)
    }
}

class VolumeUpAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        adjustWidgetVolume(context, AudioManager.ADJUST_RAISE, flags = 0)
    }
}

class VolumeDownAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        adjustWidgetVolume(context, AudioManager.ADJUST_LOWER, flags = 0)
    }
}

class ShowVolumePanelAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        adjustWidgetVolume(context, AudioManager.ADJUST_SAME, flags = AudioManager.FLAG_SHOW_UI)
    }
}

internal object WidgetServiceActions {
    const val SLEEP_TIMER = "moe.rukamori.archivetune.WIDGET_SLEEP_TIMER"
    const val TOGGLE_LIKE = "moe.rukamori.archivetune.WIDGET_TOGGLE_LIKE"
    const val TOGGLE_SHUFFLE = "moe.rukamori.archivetune.WIDGET_TOGGLE_SHUFFLE"
    const val TOGGLE_REPEAT = "moe.rukamori.archivetune.WIDGET_TOGGLE_REPEAT"
    const val PLAY_PAUSE = "moe.rukamori.archivetune.WIDGET_PLAY_PAUSE"
    const val SKIP_NEXT = "moe.rukamori.archivetune.WIDGET_SKIP_NEXT"
    const val SKIP_PREV = "moe.rukamori.archivetune.WIDGET_SKIP_PREV"
    const val PLAY_QUEUE_ITEM = "moe.rukamori.archivetune.WIDGET_PLAY_QUEUE_ITEM"
    const val PLAY_SONG = "moe.rukamori.archivetune.WIDGET_PLAY_SONG"
    const val EXTRA_MEDIA_ID = "moe.rukamori.archivetune.extra.WIDGET_MEDIA_ID"
    const val EXTRA_QUEUE_INDEX = "moe.rukamori.archivetune.extra.WIDGET_QUEUE_INDEX"
}

private const val TAG = "MusicWidgetActions"

/** Moves the music stream volume (or just shows the system panel) and redraws the widgets that display it. */
private suspend fun adjustWidgetVolume(
    context: Context,
    direction: Int,
    flags: Int,
) {
    runCatching {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, flags)
    }.onFailure { error ->
        Log.e(TAG, "Failed to adjust volume", error)
    }
    VolumeWidget().updateAll(context)
}

private inline fun sendWidgetAction(
    context: Context,
    action: String,
    configure: Intent.() -> Unit = {},
) {
    val intent = Intent(action).setClass(context, MusicService::class.java).apply(configure)
    runCatching {
        context.startService(intent)
    }.onFailure { error ->
        Log.e(TAG, "Failed to send widget action: $action", error)
    }
}
