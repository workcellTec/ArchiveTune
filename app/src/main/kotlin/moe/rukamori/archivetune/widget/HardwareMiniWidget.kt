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

class HardwareMiniWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            HardwareMiniContent(context)
        }
    }
}

@Composable
private fun HardwareMiniContent(context: Context) {
    val state = rememberPlaybackState(context)
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { ambient ->
        val palette = ambient.copy(surface = TransparentFill)
        WidgetSurface(state, palette, context, contentPadding = 0.dp, backdrop = false) {
            Box(modifier = GlanceModifier.fillMaxSize()) {
                Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    WidgetVinyl(state.artPath, context, 56.dp, GlanceModifier.padding(end = 14.dp))
                }
                Column(
                    modifier =
                        GlanceModifier
                            .fillMaxSize()
                            .padding(top = 22.dp)
                            .cornerRadius(22.dp)
                            .background(SlabFill)
                            .padding(10.dp),
                ) {
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WidgetArtwork(
                            artPath = state.artPath,
                            context = context,
                            contentDescription = null,
                            targetSize = 36.dp,
                            shape = WidgetArtShape.Rounded,
                            cornerRadius = 10.dp,
                            palette = palette,
                            modifier = GlanceModifier.size(36.dp),
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        WidgetPlayCircle(state, palette, context, 34.dp)
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    WidgetHeadline(state.title, palette, fontSize = 13.sp)
                    Spacer(GlanceModifier.defaultWeight())
                    WidgetProgress(state, palette)
                }
            }
        }
    }
}
