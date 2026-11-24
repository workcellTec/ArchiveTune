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
import moe.rukamori.archivetune.widget.MusicWidgetKeys

class ControlsWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            ControlsContent(context)
        }
    }
}

@Composable
private fun ControlsContent(context: Context) {
    val preferences = currentState<Preferences>()
    val state = androidx.compose.runtime.remember(preferences, context) { preferences.toWidgetPlaybackState(context) }
    val sleepActive = preferences[MusicWidgetKeys.SLEEP_ACTIVE] ?: false
    WidgetTheme(null, WidgetSurfaceStyle.Ambient, context) { ambient ->
        val palette = ambient.bento()
        WidgetSurface(state, palette, context, contentPadding = 8.dp, backdrop = false) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                WidgetIconButton(
                    icon = R.drawable.dark_mode,
                    contentDescription = context.getString(R.string.widget_sleep_timer),
                    action = sleepTimerAction(),
                    tint = if (sleepActive) palette.onAccent else palette.onSurface,
                    modifier = GlanceModifier.size(46.dp),
                    container = if (sleepActive) palette.accent else SlabInset,
                    cornerRadius = 23.dp,
                    iconSize = 22.dp,
                )
                Spacer(GlanceModifier.defaultWeight())
                Row(
                    modifier =
                        GlanceModifier
                            .cornerRadius(28.dp)
                            .background(ColorProvider(Color.White))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetIconButton(
                        icon = R.drawable.skip_previous,
                        contentDescription = context.getString(R.string.widget_previous),
                        action = skipPreviousAction(),
                        tint = BentoOnAccent,
                        modifier = GlanceModifier.size(36.dp),
                        iconSize = 22.dp,
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    WidgetPlayCircle(state, palette, context, 44.dp)
                    Spacer(GlanceModifier.width(4.dp))
                    WidgetIconButton(
                        icon = R.drawable.skip_next,
                        contentDescription = context.getString(R.string.next),
                        action = skipNextAction(),
                        tint = BentoOnAccent,
                        modifier = GlanceModifier.size(36.dp),
                        iconSize = 22.dp,
                    )
                }
                Spacer(GlanceModifier.defaultWeight())
                WidgetIconButton(
                    icon = R.drawable.volume_up,
                    contentDescription = context.getString(R.string.widget_volume),
                    action = showVolumePanelAction(),
                    tint = palette.onSurface,
                    modifier = GlanceModifier.size(46.dp),
                    container = SlabInset,
                    cornerRadius = 23.dp,
                    iconSize = 22.dp,
                )
            }
        }
    }
}
