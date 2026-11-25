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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.remember
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
    val colors = remember(state.dominantColor) { lightWaveColors(state.dominantColor) }
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
                .background(colors.palette.surface)
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
                    palette = colors.palette,
                    modifier = GlanceModifier.size(coverSize),
                )
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = context.getString(R.string.widget_now_playing),
                        style = TextStyle(color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.title,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = colors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                        maxLines = titleLines,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = state.artist,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(color = colors.dim, fontSize = 12.sp),
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
                playedColor = colors.accentArgb,
                restColor = colors.waveRestArgb,
            )
            if (showTimes) {
                Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                    Text(
                        text = formatWidgetTime((state.durationMs * state.playbackPosition).toLong()),
                        style = TextStyle(color = colors.dim, fontSize = 11.sp),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = formatWidgetTime(state.durationMs),
                        style = TextStyle(color = colors.dim, fontSize = 11.sp),
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetShuffleButton(state, context, colors.accent, colors.icon, GlanceModifier.size(36.dp))
                Spacer(GlanceModifier.defaultWeight())
                if (state.isAvailable) {
                    WidgetSkipButton(
                        forward = false,
                        palette = colors.palette,
                        context = context,
                        modifier = GlanceModifier.size(40.dp),
                        iconSize = 24.dp,
                    )
                    Spacer(GlanceModifier.defaultWeight())
                }
                WidgetPlayCircle(state, colors.palette, context, 48.dp)
                Spacer(GlanceModifier.defaultWeight())
                if (state.isAvailable) {
                    WidgetSkipButton(
                        forward = true,
                        palette = colors.palette,
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
                    tint = colors.accent,
                    modifier = GlanceModifier.size(36.dp),
                    iconSize = 22.dp,
                )
            }
        }
    }
}

private val TimesMinHeight = 190.dp

/** Colors of the light card, tinted by the cover's dominant color (orange when there is no cover). */
private class LightWaveColors(
    surface: Color,
    container: Color,
    track: Color,
    accent: Color,
    text: Color,
    dim: Color,
    icon: Color,
) {
    val accentArgb = accent.toArgb()
    val waveRestArgb = track.toArgb()
    val accent = ColorProvider(accent)
    val text = ColorProvider(text)
    val dim = ColorProvider(dim)
    val icon = ColorProvider(icon)
    val palette =
        fixedWidgetPalette(
            surface = ColorProvider(surface),
            onSurface = this.icon,
            onSurfaceVariant = this.dim,
            accent = this.accent,
            onAccent = ColorProvider(if (accent.luminance() > 0.5f) Color.Black else Color.White),
            container = ColorProvider(container),
            track = ColorProvider(track),
            artworkFallback = ColorProvider(container),
        )
}

private fun lightWaveColors(dominantColor: Int?): LightWaveColors {
    val base = dominantColor?.let { Color(it).copy(alpha = 1f) } ?: Color(0xFFF26A2E)
    val surface = base.mix(Color.White, 0.90f)
    var accent = base.mix(Color.Black, 0.22f)
    var step = 0
    while (accent.contrastWith(surface) < MinAccentContrast && step < 5) {
        accent = accent.mix(Color.Black, 0.25f)
        step++
    }
    return LightWaveColors(
        surface = surface,
        container = base.mix(Color.White, 0.78f),
        track = base.mix(Color.White, 0.70f),
        accent = accent,
        text = base.mix(Color.Black, 0.84f),
        dim = base.mix(Color.Black, 0.55f),
        icon = base.mix(Color.Black, 0.65f),
    )
}

private const val MinAccentContrast = 3f

private fun Color.mix(
    other: Color,
    fraction: Float,
): Color = lerp(this, other, fraction)

private fun Color.contrastWith(other: Color): Float {
    val lighter = maxOf(luminance(), other.luminance())
    val darker = minOf(luminance(), other.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
