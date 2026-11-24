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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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

/** Light cream card with an audio-wave progress, an orange play button and like/shuffle on the sides. */
class LightWaveWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            LightWaveContent(context)
        }
    }
}

@Composable
private fun LightWaveContent(context: Context) {
    val state = rememberPlaybackState(context)
    val size = LocalSize.current
    val contentWidth = size.width - 28.dp
    val showTimes = size.height >= TimesMinHeight
    val coverSize = minOf((size.height - 28.dp) * 0.5f, size.width * 0.36f).coerceIn(56.dp, 128.dp)
    val titleLines = if (size.height >= 210.dp) 2 else 1
    Box(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .widgetRootCorners()
                .background(LightCardSurface)
                .clickable(openArchiveTuneAction(context))
                .padding(14.dp),
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetArtwork(
                    artPath = state.artPath,
                    context = context,
                    contentDescription = null,
                    targetSize = coverSize,
                    shape = WidgetArtShape.Rounded,
                    cornerRadius = 18.dp,
                    palette = LightCardPalette,
                    modifier = GlanceModifier.size(coverSize),
                )
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = context.getString(R.string.widget_now_playing),
                        style = TextStyle(color = LightCardAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.title,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = LightCardText, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        maxLines = titleLines,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.artist,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = LightCardDim, fontSize = 12.sp),
                        maxLines = 1,
                    )
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            WidgetWaveform(
                context = context,
                seed = state.mediaId ?: state.title,
                progress = state.playbackPosition,
                width = contentWidth,
                height = if (showTimes) 34.dp else 28.dp,
                playedColor = LightCardAccentArgb,
                restColor = LightCardWaveRestArgb,
            )
            if (showTimes) {
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                    Text(
                        text = formatWidgetTime((state.durationMs * state.playbackPosition).toLong()),
                        style = TextStyle(color = LightCardDim, fontSize = 11.sp),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = formatWidgetTime(state.durationMs),
                        style = TextStyle(color = LightCardDim, fontSize = 11.sp),
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetShuffleButton(state, context, LightCardAccent, LightCardIcon, GlanceModifier.size(36.dp))
                Spacer(GlanceModifier.defaultWeight())
                if (state.isAvailable) {
                    WidgetSkipButton(
                        forward = false,
                        palette = LightCardPalette,
                        context = context,
                        modifier = GlanceModifier.size(40.dp),
                        iconSize = 24.dp,
                    )
                    Spacer(GlanceModifier.defaultWeight())
                }
                WidgetPlayCircle(state, LightCardPalette, context, 48.dp)
                Spacer(GlanceModifier.defaultWeight())
                if (state.isAvailable) {
                    WidgetSkipButton(
                        forward = true,
                        palette = LightCardPalette,
                        context = context,
                        modifier = GlanceModifier.size(40.dp),
                        iconSize = 24.dp,
                    )
                    Spacer(GlanceModifier.defaultWeight())
                }
                WidgetIconButton(
                    icon = if (state.isLiked) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = context.getString(if (state.isLiked) R.string.action_remove_like else R.string.action_like),
                    action = toggleLikeAction(),
                    tint = LightCardAccent,
                    modifier = GlanceModifier.size(36.dp),
                    iconSize = 22.dp,
                )
            }
        }
    }
}

private val TimesMinHeight = 190.dp
private val LightCardAccentColor = Color(0xFFF26A2E)
private val LightCardAccentArgb = LightCardAccentColor.toArgb()
private val LightCardWaveRestArgb = Color(0xFFE8D6C8).toArgb()
private val LightCardSurface = ColorProvider(Color(0xFFFFF4EC))
private val LightCardAccent = ColorProvider(LightCardAccentColor)
private val LightCardText = ColorProvider(Color(0xFF2A1F1A))
private val LightCardDim = ColorProvider(Color(0xFF7A6A60))
private val LightCardIcon = ColorProvider(Color(0xFF5A4A40))

private val LightCardPalette =
    fixedWidgetPalette(
        surface = LightCardSurface,
        onSurface = LightCardIcon,
        onSurfaceVariant = LightCardDim,
        accent = LightCardAccent,
        onAccent = ColorProvider(Color.White),
        container = ColorProvider(Color(0xFFFFE6D6)),
        track = ColorProvider(Color(0xFFE8D6C8)),
        artworkFallback = ColorProvider(Color(0xFFFBD9BE)),
    )
