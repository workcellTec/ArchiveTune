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

class SquareCardWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            SquareCardContent(context)
        }
    }
}

@Composable
private fun SquareCardContent(context: Context) {
    val state = rememberPlaybackState(context)
    val size = LocalSize.current
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = 12.dp, backdrop = true) {
            // The cover takes everything the title row (38dp play button) leaves free.
            val coverSize = minOf(size.width - 24.dp, size.height - 24.dp - 38.dp - 10.dp).coerceAtLeast(56.dp)
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
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
                }
                Spacer(GlanceModifier.height(10.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WidgetTrackText(state, palette, GlanceModifier.defaultWeight(), titleSize = 15.sp)
                    Spacer(GlanceModifier.width(6.dp))
                    WidgetPlayCircle(state, palette, context, 38.dp)
                }
            }
        }
    }
}
