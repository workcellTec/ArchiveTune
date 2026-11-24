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

class ModernWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            ModernContent(context)
        }
    }
}

@Composable
private fun ModernContent(context: Context) {
    val state = rememberPlaybackState(context)
    val upNext = rememberWidgetUpNext()
    val size = LocalSize.current
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = 12.dp, backdrop = true) {
            val contentHeight = size.height - 24.dp
            val coverSize = (size.height - 24.dp).coerceIn(56.dp, 130.dp)
            val cardSize = (contentHeight - ModernFixedHeight).coerceAtMost(52.dp)
            val textWidth = size.width - 24.dp - coverSize - 14.dp
            val cardCount = if (state.isAvailable) nextCardsFit(textWidth, cardSize, upNext.size) else 0
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                WidgetArtwork(
                    artPath = state.artPath,
                    context = context,
                    contentDescription = null,
                    targetSize = coverSize,
                    shape = WidgetArtShape.Rounded,
                    cornerRadius = 24.dp,
                    palette = palette,
                    modifier = GlanceModifier.size(coverSize),
                )
                Spacer(GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                    WidgetTrackText(
                        state,
                        palette,
                        GlanceModifier.fillMaxWidth(),
                        titleSize = if (cardCount > 0) 19.sp else 22.sp,
                        titleLines = if (cardCount > 0) 1 else 2,
                    )
                    if (cardCount > 0) {
                        Spacer(GlanceModifier.height(8.dp))
                        WidgetNextCards(upNext.take(cardCount), palette, context, cardSize)
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    if (state.isAvailable) {
                        WidgetProgress(state, palette)
                        Spacer(GlanceModifier.height(8.dp))
                    }
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (state.isAvailable) {
                            WidgetSkipButton(
                                forward = false,
                                palette = palette,
                                context = context,
                                modifier = GlanceModifier.size(38.dp),
                                container = GlassFill,
                                cornerRadius = 19.dp,
                                iconSize = 20.dp,
                            )
                            Spacer(GlanceModifier.width(8.dp))
                        }
                        WidgetPlayWide(state, palette, context, GlanceModifier.defaultWeight(), height = 38.dp)
                        if (state.isAvailable) {
                            Spacer(GlanceModifier.width(8.dp))
                            WidgetSkipButton(
                                forward = true,
                                palette = palette,
                                context = context,
                                modifier = GlanceModifier.size(38.dp),
                                container = GlassFill,
                                cornerRadius = 19.dp,
                                iconSize = 20.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Title, artist, spacing, progress and transport, in dp. */
private val ModernFixedHeight = 122.dp
