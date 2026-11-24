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

class CoverWideWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            CoverWideContent(context)
        }
    }
}

@Composable
private fun CoverWideContent(context: Context) {
    val state = rememberPlaybackState(context)
    val size = LocalSize.current
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = 0.dp, backdrop = false) {
            Box(modifier = GlanceModifier.fillMaxSize()) {
                WidgetCoverFill(state, palette, context, maxOf(size.width, size.height))
                Column(modifier = GlanceModifier.fillMaxSize().padding(14.dp)) {
                    Box(
                        modifier =
                            GlanceModifier
                                .cornerRadius(12.dp)
                                .background(GlassFill)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        WidgetOverline(
                            context.getString(R.string.widget_now_playing),
                            palette,
                            context,
                            color = palette.onSurface,
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        WidgetTrackText(state, palette, GlanceModifier.defaultWeight(), titleSize = 18.sp)
                        Spacer(GlanceModifier.width(8.dp))
                        WidgetGlassTransport(state, palette, context, buttonSize = 38.dp, playSize = 46.dp)
                    }
                }
            }
        }
    }
}
