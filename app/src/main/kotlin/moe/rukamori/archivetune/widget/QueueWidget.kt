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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed

class QueueWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            QueueContent(context)
        }
    }
}

@Composable
private fun QueueContent(context: Context) {
    val preferences = currentState<Preferences>()
    val state = androidx.compose.runtime.remember(preferences, context) { preferences.toWidgetPlaybackState(context) }
    val upNext = androidx.compose.runtime.remember(preferences) { preferences.widgetUpNext() }
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = 12.dp, backdrop = true) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(
                    modifier =
                        GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(22.dp)
                            .background(GlassFill)
                            .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetArtwork(
                        artPath = state.artPath,
                        context = context,
                        contentDescription = null,
                        targetSize = 56.dp,
                        shape = WidgetArtShape.Rounded,
                        cornerRadius = 14.dp,
                        palette = palette,
                        modifier = GlanceModifier.size(56.dp),
                    )
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        WidgetOverline(context.getString(R.string.widget_now_playing), palette, context)
                        WidgetHeadline(state.title, palette, fontSize = 15.sp)
                        WidgetSupporting(state.artist, palette)
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    WidgetPlayCircle(state, palette, context, 40.dp)
                }
                if (state.isAvailable) {
                    Spacer(GlanceModifier.height(8.dp))
                    WidgetProgress(state, palette)
                }
                Spacer(GlanceModifier.height(10.dp))
                WidgetOverline(context.getString(R.string.widget_up_next), palette, context)
                Spacer(GlanceModifier.height(4.dp))
                if (upNext.isEmpty()) {
                    WidgetEmptyMessage(
                        icon = R.drawable.queue_music,
                        message =
                            context.getString(
                                if (state.isAvailable) R.string.widget_queue_empty else R.string.widget_tap_to_open,
                            ),
                        palette = palette,
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    )
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        itemsIndexed(upNext, itemId = { _, item -> item.widgetItemId() }) { index, item ->
                            WidgetMediaRow(
                                item = item,
                                palette = palette,
                                context = context,
                                action = playQueueItemAction(item),
                                rank = index + 1,
                                artSize = 36.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}
