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

@Composable
internal fun rememberPlaybackState(context: Context): WidgetPlaybackState {
    val preferences = currentState<Preferences>()
    return androidx.compose.runtime.remember(preferences, context) { preferences.toWidgetPlaybackState(context) }
}

internal val GlassFill = ColorProvider(Color.White.copy(alpha = 0.16f))
internal val SlabFill = ColorProvider(Color(0xFF26262B))
internal val SlabInset = ColorProvider(Color(0xFF34343B))
internal val BentoSurface = ColorProvider(Color(0xFF0E0E10))
internal val BentoAccent = ColorProvider(Color(0xFFFFDD55))
internal val BentoOnAccent = ColorProvider(Color(0xFF1A1A1E))
internal val BentoDim = ColorProvider(Color.White.copy(alpha = 0.18f))
internal val TransparentFill = ColorProvider(Color.Transparent)

/** Neutral dark look used by the "bento" widgets (volume, stats, controls). */
internal fun WidgetPalette.bento(): WidgetPalette =
    copy(
        surface = BentoSurface,
        accent = BentoAccent,
        onAccent = BentoOnAccent,
        container = SlabInset,
        track = BentoDim,
    )

/** Full-bleed cover with a dark gradient at the bottom so text stays readable. */
@Composable
internal fun WidgetCoverFill(
    state: WidgetPlaybackState,
    palette: WidgetPalette,
    context: Context,
    targetSize: Dp,
) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        WidgetArtwork(
            artPath = state.artPath,
            context = context,
            contentDescription = null,
            targetSize = targetSize,
            shape = WidgetArtShape.Square,
            palette = palette,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Box(modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_scrim_bottom))) {}
    }
}

@Composable
internal fun WidgetPlayCircle(
    state: WidgetPlaybackState,
    palette: WidgetPalette,
    context: Context,
    size: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    WidgetIconButton(
        icon = if (state.isPlaying) R.drawable.pause else R.drawable.play,
        contentDescription = context.getString(if (state.isPlaying) R.string.widget_pause else R.string.play),
        action = if (state.isAvailable) playPauseAction() else openArchiveTuneAction(context),
        tint = palette.onAccent,
        modifier = modifier.size(size),
        container = palette.accent,
        cornerRadius = size / 2,
        iconSize = size * 0.5f,
    )
}

@Composable
internal fun WidgetPlayWide(
    state: WidgetPlaybackState,
    palette: WidgetPalette,
    context: Context,
    modifier: GlanceModifier = GlanceModifier,
    height: Dp = 40.dp,
) {
    Box(
        modifier =
            modifier
                .height(height)
                .cornerRadius(height / 2)
                .background(palette.accent)
                .clickable(if (state.isAvailable) playPauseAction() else openArchiveTuneAction(context)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (state.isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = context.getString(if (state.isPlaying) R.string.widget_pause else R.string.play),
            colorFilter = ColorFilter.tint(palette.onAccent),
            modifier = GlanceModifier.size(height * 0.55f),
        )
    }
}

/** Previous / play-pause / next with translucent round buttons. */
@Composable
internal fun WidgetGlassTransport(
    state: WidgetPlaybackState,
    palette: WidgetPalette,
    context: Context,
    modifier: GlanceModifier = GlanceModifier,
    buttonSize: Dp = 40.dp,
    playSize: Dp = 48.dp,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (state.isAvailable) {
            WidgetSkipButton(
                forward = false,
                palette = palette,
                context = context,
                modifier = GlanceModifier.size(buttonSize),
                container = GlassFill,
                cornerRadius = buttonSize / 2,
                iconSize = buttonSize * 0.5f,
            )
            Spacer(GlanceModifier.width(8.dp))
        }
        WidgetPlayCircle(state, palette, context, playSize)
        if (state.isAvailable) {
            Spacer(GlanceModifier.width(8.dp))
            WidgetSkipButton(
                forward = true,
                palette = palette,
                context = context,
                modifier = GlanceModifier.size(buttonSize),
                container = GlassFill,
                cornerRadius = buttonSize / 2,
                iconSize = buttonSize * 0.5f,
            )
        }
    }
}
