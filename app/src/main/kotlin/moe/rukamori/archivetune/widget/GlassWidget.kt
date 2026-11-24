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
        WidgetSurface(state, palette, context, contentPadding = 10.dp, backdrop = true) {
            val contentHeight = size.height - 44.dp
            val coverSize = (size.height - 44.dp).coerceIn(48.dp, 112.dp)
            // Space left for the cards once title, label and transport are accounted for.
            val cardSize = (contentHeight - GlassFixedHeight).coerceAtMost(56.dp)
            val textWidth = size.width - 44.dp - coverSize - 12.dp
            val cardCount = if (state.isAvailable) nextCardsFit(textWidth, cardSize, upNext.size) else 0
            Row(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(26.dp)
                        .background(GlassFill)
                        .padding(12.dp),
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
                    WidgetTrackText(
                        state,
                        palette,
                        GlanceModifier.fillMaxWidth(),
                        titleSize = if (cardCount > 0) 19.sp else 22.sp,
                        titleLines = if (cardCount > 0) 1 else 2,
                    )
                    if (cardCount > 0) {
                        Spacer(GlanceModifier.height(6.dp))
                        WidgetOverline(context.getString(R.string.widget_up_next), palette, context)
                        Spacer(GlanceModifier.height(4.dp))
                        WidgetNextCards(upNext.take(cardCount), palette, context, cardSize)
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    WidgetGlassTransport(state, palette, context, buttonSize = 36.dp, playSize = 44.dp)
                }
            }
        }
    }
}

/** Title, artist, label, spacing and transport, in dp. */
private val GlassFixedHeight = 134.dp
