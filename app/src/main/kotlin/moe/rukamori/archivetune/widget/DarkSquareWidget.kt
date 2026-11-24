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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import moe.rukamori.archivetune.R

/** Square dark card: like and sleep on top, cover with transport, progress, shuffle and repeat. */
class DarkSquareWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            DarkSquareContent(context)
        }
    }
}

@Composable
private fun DarkSquareContent(context: Context) {
    val state = rememberPlaybackState(context)
    val sleepActive = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()[MusicWidgetKeys.SLEEP_ACTIVE] ?: false
    val size = LocalSize.current
    val showFooter = size.height >= FooterMinHeight
    // Everything but the cover row: padding, top buttons, gaps, progress, times and shuffle row.
    val chrome = 28.dp + 36.dp + 10.dp + 14.dp + (if (showFooter) 50.dp else 0.dp)
    val coverSize = minOf(size.height - chrome, size.width * 0.48f).coerceAtLeast(56.dp)
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
      WidgetSurface(state, palette, context, contentPadding = 14.dp, backdrop = true) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetIconButton(
                    icon = if (state.isLiked) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = context.getString(if (state.isLiked) R.string.action_remove_like else R.string.action_like),
                    action = toggleLikeAction(),
                    tint = if (state.isLiked) DarkCardPink else palette.onSurfaceVariant,
                    modifier = GlanceModifier.size(36.dp),
                    container = palette.container,
                    cornerRadius = 18.dp,
                    iconSize = 18.dp,
                )
                Spacer(GlanceModifier.defaultWeight())
                WidgetIconButton(
                    icon = if (sleepActive) R.drawable.bedtime else R.drawable.timer,
                    contentDescription = context.getString(R.string.widget_sleep_timer),
                    action = sleepTimerAction(),
                    tint = if (sleepActive) palette.accent else palette.onSurfaceVariant,
                    modifier = GlanceModifier.size(36.dp),
                    container = palette.container,
                    cornerRadius = 18.dp,
                    iconSize = 18.dp,
                )
            }
            Spacer(GlanceModifier.height(10.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.artist,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = palette.onSurfaceVariant, fontSize = 11.sp),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.title,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = palette.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines = 2,
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WidgetIconButton(
                            icon = if (state.isPlaying) R.drawable.pause else R.drawable.play,
                            contentDescription = context.getString(if (state.isPlaying) R.string.widget_pause else R.string.play),
                            action = if (state.isAvailable) playPauseAction() else openArchiveTuneAction(context),
                            tint = palette.onAccent,
                            modifier = GlanceModifier.size(44.dp),
                            container = palette.accent,
                            cornerRadius = 22.dp,
                            iconSize = 22.dp,
                        )
                        if (state.isAvailable) {
                            Spacer(GlanceModifier.width(6.dp))
                            WidgetIconButton(
                                icon = R.drawable.skip_previous,
                                contentDescription = context.getString(R.string.widget_previous),
                                action = skipPreviousAction(),
                                tint = palette.onSurface,
                                modifier = GlanceModifier.size(32.dp),
                                container = palette.container,
                                cornerRadius = 16.dp,
                                iconSize = 16.dp,
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            WidgetIconButton(
                                icon = R.drawable.skip_next,
                                contentDescription = context.getString(R.string.next),
                                action = skipNextAction(),
                                tint = palette.onSurface,
                                modifier = GlanceModifier.size(32.dp),
                                container = palette.container,
                                cornerRadius = 16.dp,
                                iconSize = 16.dp,
                            )
                        }
                    }
                }
            }
            if (state.isAvailable) {
                Spacer(GlanceModifier.height(8.dp))
                WidgetProgress(state, palette)
            }
            if (showFooter) {
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = formatWidgetTime((state.durationMs * state.playbackPosition).toLong()),
                        style = TextStyle(color = palette.onSurfaceVariant, fontSize = 11.sp),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = formatWidgetTime(state.durationMs),
                        style = TextStyle(color = palette.onSurfaceVariant, fontSize = 11.sp),
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetShuffleButton(state, context, palette.accent, palette.onSurfaceVariant, GlanceModifier.size(32.dp))
                    Spacer(GlanceModifier.width(28.dp))
                    WidgetRepeatButton(state, context, palette.accent, palette.onSurfaceVariant, GlanceModifier.size(32.dp))
                }
            }
        }
      }
    }
}

private val FooterMinHeight = 222.dp
private val DarkCardPink = ColorProvider(Color(0xFFF2B8C6))
