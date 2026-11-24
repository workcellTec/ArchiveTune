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

class PillWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            PillContent(context)
        }
    }
}

@Composable
private fun PillContent(context: Context) {
    val state = rememberPlaybackState(context)
    val size = LocalSize.current
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = 6.dp, backdrop = false) {
            val innerHeight = size.height - 12.dp
            val artSize = (innerHeight - 12.dp).coerceIn(32.dp, 56.dp)
            val showTransport = state.isAvailable && size.width >= 300.dp
            Row(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(innerHeight / 2)
                        .background(palette.container)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetArtwork(
                    artPath = state.artPath,
                    context = context,
                    contentDescription = null,
                    targetSize = artSize,
                    shape = WidgetArtShape.Circle,
                    palette = palette,
                    modifier = GlanceModifier.size(artSize),
                )
                Spacer(GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    WidgetTrackText(state, palette, GlanceModifier.fillMaxWidth(), titleSize = 14.sp)
                    if (state.isAvailable && innerHeight >= 52.dp) {
                        Spacer(GlanceModifier.height(4.dp))
                        WidgetProgress(state, palette)
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                if (showTransport) {
                    WidgetSkipButton(
                        forward = false,
                        palette = palette,
                        context = context,
                        modifier = GlanceModifier.size(36.dp),
                        iconSize = 20.dp,
                    )
                }
                WidgetPlayCircle(state, palette, context, artSize.coerceAtMost(44.dp))
                if (showTransport) {
                    WidgetSkipButton(
                        forward = true,
                        palette = palette,
                        context = context,
                        modifier = GlanceModifier.size(36.dp),
                        iconSize = 20.dp,
                    )
                }
            }
        }
    }
}
