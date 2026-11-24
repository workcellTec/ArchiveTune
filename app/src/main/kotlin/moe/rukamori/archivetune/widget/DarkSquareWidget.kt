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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
    val coverSize = (size.height - 28.dp - 36.dp - 10.dp - 14.dp - (if (showFooter) 50.dp else 0.dp)).coerceIn(56.dp, 104.dp)
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .widgetRootCorners()
                .background(DarkCardSurface)
                .clickable(openArchiveTuneAction(context))
                .padding(14.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetIconButton(
                    icon = if (state.isLiked) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = context.getString(if (state.isLiked) R.string.action_remove_like else R.string.action_like),
                    action = toggleLikeAction(),
                    tint = if (state.isLiked) DarkCardPink else DarkCardIcon,
                    modifier = GlanceModifier.size(36.dp),
                    container = DarkCardButton,
                    cornerRadius = 18.dp,
                    iconSize = 18.dp,
                )
                Spacer(GlanceModifier.defaultWeight())
                WidgetIconButton(
                    icon = if (sleepActive) R.drawable.bedtime else R.drawable.timer,
                    contentDescription = context.getString(R.string.widget_sleep_timer),
                    action = sleepTimerAction(),
                    tint = if (sleepActive) DarkCardAccent else DarkCardIcon,
                    modifier = GlanceModifier.size(36.dp),
                    container = DarkCardButton,
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
                    palette = DarkCardPalette,
                    modifier = GlanceModifier.size(coverSize),
                )
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.artist,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = DarkCardDim, fontSize = 11.sp),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.title,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = DarkCardText, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines = 2,
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WidgetIconButton(
                            icon = if (state.isPlaying) R.drawable.pause else R.drawable.play,
                            contentDescription = context.getString(if (state.isPlaying) R.string.widget_pause else R.string.play),
                            action = if (state.isAvailable) playPauseAction() else openArchiveTuneAction(context),
                            tint = DarkCardText,
                            modifier = GlanceModifier.size(40.dp),
                            container = DarkCardPlay,
                            cornerRadius = 20.dp,
                            iconSize = 20.dp,
                        )
                        if (state.isAvailable) {
                            Spacer(GlanceModifier.width(6.dp))
                            WidgetIconButton(
                                icon = R.drawable.skip_previous,
                                contentDescription = context.getString(R.string.widget_previous),
                                action = skipPreviousAction(),
                                tint = DarkCardText,
                                modifier = GlanceModifier.size(32.dp),
                                container = DarkCardButton,
                                cornerRadius = 16.dp,
                                iconSize = 16.dp,
                            )
                            Spacer(GlanceModifier.width(6.dp))
                            WidgetIconButton(
                                icon = R.drawable.skip_next,
                                contentDescription = context.getString(R.string.next),
                                action = skipNextAction(),
                                tint = DarkCardText,
                                modifier = GlanceModifier.size(32.dp),
                                container = DarkCardButton,
                                cornerRadius = 16.dp,
                                iconSize = 16.dp,
                            )
                        }
                    }
                }
            }
            if (state.isAvailable) {
                Spacer(GlanceModifier.height(8.dp))
                WidgetProgress(state, DarkCardPalette)
            }
            if (showFooter) {
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = formatWidgetTime((state.durationMs * state.playbackPosition).toLong()),
                        style = TextStyle(color = DarkCardDim, fontSize = 11.sp),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = formatWidgetTime(state.durationMs),
                        style = TextStyle(color = DarkCardDim, fontSize = 11.sp),
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetShuffleButton(state, context, DarkCardAccent, DarkCardIcon, GlanceModifier.size(32.dp))
                    Spacer(GlanceModifier.width(28.dp))
                    WidgetRepeatButton(state, context, DarkCardAccent, DarkCardIcon, GlanceModifier.size(32.dp))
                }
            }
        }
    }
}

private val FooterMinHeight = 222.dp
private val DarkCardSurface = ColorProvider(Color(0xFF1B1D24))
private val DarkCardButton = ColorProvider(Color(0xFF2E333D))
private val DarkCardPlay = ColorProvider(Color(0xFF3B4150))
private val DarkCardText = ColorProvider(Color.White)
private val DarkCardDim = ColorProvider(Color(0xFF9AA1AE))
private val DarkCardIcon = ColorProvider(Color(0xFFB4BAC6))
private val DarkCardAccent = ColorProvider(Color(0xFFFFDD55))
private val DarkCardPink = ColorProvider(Color(0xFFF2B8C6))
private val DarkCardTrack = ColorProvider(Color(0xFF2E333D))
private val DarkCardProgress = ColorProvider(Color(0xFFC9B88C))

private val DarkCardPalette =
    fixedWidgetPalette(
        surface = DarkCardSurface,
        onSurface = DarkCardText,
        onSurfaceVariant = DarkCardDim,
        accent = DarkCardProgress,
        onAccent = DarkCardSurface,
        container = DarkCardButton,
        track = DarkCardTrack,
        artworkFallback = DarkCardButton,
    )
