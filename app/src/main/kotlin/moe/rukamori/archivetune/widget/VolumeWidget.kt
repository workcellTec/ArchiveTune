/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.ColorFilter
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import moe.rukamori.archivetune.R

class VolumeWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            VolumeContent(context)
        }
    }
}

private val DotSizes = listOf(6.dp, 9.dp, 13.dp, 18.dp, 13.dp, 9.dp, 6.dp)

@Composable
private fun VolumeContent(context: Context) {
    val state = rememberPlaybackState(context)
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val volume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).coerceIn(0, maxVolume)
    val fraction = volume / maxVolume.toFloat()
    val filled = (fraction * DotSizes.size).let { kotlin.math.round(it).toInt() }
    WidgetTheme(null, WidgetSurfaceStyle.Ambient, context) { ambient ->
        val palette = ambient.bento()
        WidgetSurface(state, palette, context, contentPadding = 14.dp, backdrop = false) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                WidgetSupporting(context.getString(R.string.widget_volume), palette, fontSize = 13.sp)
                Spacer(GlanceModifier.defaultWeight())
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DotSizes.forEachIndexed { index, dot ->
                        Box(
                            modifier =
                                GlanceModifier
                                    .size(dot)
                                    .cornerRadius(dot / 2)
                                    .background(if (index < filled) palette.accent else BentoDim),
                        ) {}
                        if (index != DotSizes.lastIndex) Spacer(GlanceModifier.width(4.dp))
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WidgetIconButton(
                        icon = R.drawable.remove,
                        contentDescription = context.getString(R.string.widget_volume_down),
                        action = volumeDownAction(),
                        tint = palette.onSurface,
                        modifier = GlanceModifier.size(30.dp),
                        container = SlabInset,
                        cornerRadius = 15.dp,
                        iconSize = 16.dp,
                    )
                    Text(
                        text = "${kotlin.math.round(fraction * 100).toInt()}%",
                        modifier = GlanceModifier.defaultWeight(),
                        style = TextStyle(
                            color = palette.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                    WidgetIconButton(
                        icon = R.drawable.add,
                        contentDescription = context.getString(R.string.widget_volume_up),
                        action = volumeUpAction(),
                        tint = palette.onSurface,
                        modifier = GlanceModifier.size(30.dp),
                        container = SlabInset,
                        cornerRadius = 15.dp,
                        iconSize = 16.dp,
                    )
                }
            }
        }
    }
}
