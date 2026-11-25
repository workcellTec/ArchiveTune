/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
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
import androidx.glance.text.TextAlign

/**
 * 3x2 glass widget with bigger up-next cards, centered title and artist under them and the three
 * transport buttons grouped in the middle of the bottom edge.
 */
class GlassCenterWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent { GlassCenterContent(context) }
    }
}

@Composable
private fun GlassCenterContent(context: Context) {
    val state = rememberPlaybackState(context)
    val upNext = rememberWidgetUpNext()
    val size = LocalSize.current
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = 6.dp, backdrop = true) {
            val inset = 28.dp
            val width = size.width - inset
            val height = size.height - inset
            val cards = if (state.isAvailable) upNext else emptyList()
            Box(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
                CenterBody(state, palette, context, cards, width, height)
            }
        }
    }
}

@Composable
private fun CenterBody(
    state: WidgetPlaybackState,
    palette: WidgetPalette,
    context: Context,
    upNext: List<WidgetMediaItem>,
    width: Dp,
    height: Dp,
) {
    val button = 32.dp
    val play = 44.dp
    val topHeight = (height - play - 6.dp).coerceAtLeast(MinCover)
    val coverSize = minOf(topHeight, width * 0.42f)
    val rightWidth = width - coverSize - 10.dp
    val textHeight = 34.dp
    // Cards use the full right column width, only limited by the room the text leaves.
    val plan =
        fitCards(
            rightWidth,
            minOf(topHeight - textHeight - 4.dp, coverSize * 0.9f, 80.dp),
            upNext.size,
            3,
        )
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth().height(topHeight)) {
            WidgetArtwork(
                artPath = state.artPath,
                context = context,
                contentDescription = null,
                targetSize = coverSize,
                shape = WidgetArtShape.Rounded,
                cornerRadius = 18.dp,
                palette = palette,
                modifier = GlanceModifier.size(coverSize),
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (plan.count > 0) {
                    WidgetNextCards(upNext.take(plan.count), palette, context, plan.size)
                }
                Spacer(GlanceModifier.defaultWeight())
                WidgetTrackText(
                    state,
                    palette,
                    GlanceModifier.fillMaxWidth(),
                    titleSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(GlanceModifier.defaultWeight())
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WidgetGlassTransport(state, palette, context, buttonSize = button, playSize = play)
        }
    }
}
