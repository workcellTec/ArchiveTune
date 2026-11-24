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

class GlassWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlassContent(context)
        }
    }
}

@Composable
private fun GlassContent(context: Context) {
    val state = rememberPlaybackState(context)
    val upNext = rememberWidgetUpNext()
    val size = LocalSize.current
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = 8.dp, backdrop = true) {
            // 8dp surface padding + 10dp card padding on each side.
            val contentHeight = size.height - 36.dp
            val coverSize = minOf(contentHeight, size.width * 0.46f).coerceAtLeast(48.dp)
            val textWidth = size.width - 36.dp - coverSize - 12.dp
            val plan =
                if (state.isAvailable) {
                    planNextCards(textWidth, contentHeight - GlassFixedHeight, upNext.size)
                } else {
                    NextCardPlan(0.dp, 0)
                }
            val withCards = plan.count > 0
            Row(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(26.dp)
                        .background(GlassFill)
                        .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetArtwork(
                    artPath = state.artPath,
                    context = context,
                    contentDescription = null,
                    targetSize = coverSize,
                    shape = WidgetArtShape.Rounded,
                    cornerRadius = 20.dp,
                    palette = palette,
                    modifier = GlanceModifier.size(coverSize),
                )
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                    if (withCards) {
                        WidgetNextCards(upNext.take(plan.count), palette, context, plan.size)
                        Spacer(GlanceModifier.defaultWeight())
                    }
                    WidgetTrackText(
                        state,
                        palette,
                        GlanceModifier.fillMaxWidth(),
                        titleSize = if (withCards) 17.sp else 22.sp,
                        titleLines = if (withCards) 1 else 2,
                    )
                    if (!withCards) Spacer(GlanceModifier.defaultWeight()) else Spacer(GlanceModifier.height(8.dp))
                    WidgetGlassTransport(state, palette, context, buttonSize = 36.dp, playSize = 40.dp)
                }
            }
        }
    }
}

/** Title, artist, spacing and transport, in dp (the cards take everything above). */
private val GlassFixedHeight = 96.dp
