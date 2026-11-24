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

class StatsWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            StatsContent(context)
        }
    }
}

private val BarColors =
    listOf(
        Color(0xFF64B5F6),
        Color(0xFFFFDD55),
        Color(0xFFFFBF5A),
        Color(0xFF6EE0A5),
        Color(0xFFFFDD55),
        Color(0xFFFFBF5A),
        Color(0xFF64B5F6),
    )

@Composable
private fun StatsContent(context: Context) {
    val preferences = currentState<Preferences>()
    val state = androidx.compose.runtime.remember(preferences, context) { preferences.toWidgetPlaybackState(context) }
    val week = androidx.compose.runtime.remember(preferences) { preferences.widgetWeeklyStats() }
    val locale = context.resources.configuration.locales[0]
    val today = java.time.LocalDate.now()
    val maxMs = week.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    WidgetTheme(null, WidgetSurfaceStyle.Ambient, context) { ambient ->
        val palette = ambient.bento()
        WidgetSurface(state, palette, context, contentPadding = 14.dp, backdrop = false) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                WidgetSupporting(context.getString(R.string.widget_listening_today), palette, fontSize = 13.sp)
                WidgetHeadline(formatTodayListening(context, week.last()), palette, fontSize = 20.sp)
                Spacer(GlanceModifier.defaultWeight())
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    week.forEachIndexed { index, ms ->
                        val label =
                            today
                                .minusDays((WeekDays - 1 - index).toLong())
                                .dayOfWeek
                                .getDisplayName(java.time.format.TextStyle.NARROW, locale)
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Box(
                                modifier =
                                    GlanceModifier
                                        .width(9.dp)
                                        .height((36.dp * (ms.toFloat() / maxMs)).coerceAtLeast(6.dp))
                                        .cornerRadius(5.dp)
                                        .background(ColorProvider(BarColors[index % BarColors.size])),
                            ) {}
                            Spacer(GlanceModifier.height(3.dp))
                            Text(
                                text = label,
                                style = TextStyle(color = palette.onSurfaceVariant, fontSize = 9.sp),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTodayListening(
    context: Context,
    millis: Long,
): String {
    val totalMinutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis).coerceAtLeast(0L)
    if (totalMinutes <= 0L) return context.getString(R.string.widget_listening_time_empty)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        context.getString(R.string.widget_listening_time_hours_minutes, hours, minutes)
    } else {
        context.getString(R.string.widget_listening_time_minutes, minutes)
    }
}
