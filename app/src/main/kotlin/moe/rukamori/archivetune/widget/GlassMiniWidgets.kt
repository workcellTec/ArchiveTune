/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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

/** Smaller scales of the glass widget: same blurred cover, frosted card and up-next cards. */
enum class GlassMiniLayout(
    val maxCards: Int,
) {
    /** 4x1: cover, text and controls in a row, up-next cards on the right. */
    Slim(maxCards = 3),

    /** 3x2: cover and cards on top, title with controls below. */
    Medium(maxCards = 3),

    /** 2x2: small cover and cards on top, title and compact controls below. */
    Compact(maxCards = 2),
}

open class GlassMiniWidget(
    private val layout: GlassMiniLayout,
) : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent { GlassMiniContent(context, layout) }
    }
}

class GlassSlimWidget : GlassMiniWidget(GlassMiniLayout.Slim)

class GlassMediumWidget : GlassMiniWidget(GlassMiniLayout.Medium)

class GlassCompactWidget : GlassMiniWidget(GlassMiniLayout.Compact)

@Composable
private fun GlassMiniContent(
    context: Context,
    layout: GlassMiniLayout,
) {
    val state = rememberPlaybackState(context)
    val upNext = rememberWidgetUpNext()
    val size = LocalSize.current
    WidgetTheme(state.dominantColor, WidgetSurfaceStyle.Ambient, context) { palette ->
        WidgetSurface(state, palette, context, contentPadding = SurfacePadding, backdrop = true) {
            // Surface padding + card padding on each side.
            val contentWidth = size.width - (SurfacePadding + CardPadding) * 2
            val contentHeight = size.height - (SurfacePadding + CardPadding) * 2
            val cards = if (state.isAvailable) upNext else emptyList()
            Box(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(CardRadius)
                        .background(GlassFill)
                        .padding(CardPadding),
            ) {
                when (layout) {
                    GlassMiniLayout.Slim -> SlimBody(state, palette, context, cards, contentWidth, contentHeight)
                    GlassMiniLayout.Medium ->
                        StackedBody(state, palette, context, cards, layout, contentWidth, contentHeight)
                    GlassMiniLayout.Compact ->
                        StackedBody(state, palette, context, cards, layout, contentWidth, contentHeight)
                }
            }
        }
    }
}

@Composable
private fun SlimBody(
    state: WidgetPlaybackState,
    palette: WidgetPalette,
    context: Context,
    upNext: List<WidgetMediaItem>,
    width: Dp,
    height: Dp,
) {
    val coverSize = height.coerceAtLeast(MinCover)
    val button = (height * 0.44f).coerceIn(MinButton, 30.dp)
    val play = button + 6.dp
    val cardSize = (height * 0.62f).coerceIn(MinCardSize, 46.dp)
    val cardCount =
        nextCardsFit(width * 0.3f, cardSize, upNext.size).coerceAtMost(GlassMiniLayout.Slim.maxCards)
    val showArtist = height >= 56.dp
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        WidgetArtwork(
            artPath = state.artPath,
            context = context,
            contentDescription = null,
            targetSize = coverSize,
            shape = WidgetArtShape.Rounded,
            cornerRadius = 16.dp,
            palette = palette,
            modifier = GlanceModifier.size(coverSize),
        )
        Spacer(GlanceModifier.width(10.dp))
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetTrackText(
                state,
                palette,
                GlanceModifier.fillMaxWidth(),
                titleSize = 14.sp,
                showArtist = showArtist,
            )
            Spacer(GlanceModifier.height(6.dp))
            WidgetGlassTransport(state, palette, context, buttonSize = button, playSize = play)
        }
        if (cardCount > 0) {
            Spacer(GlanceModifier.width(8.dp))
            WidgetNextCards(upNext.take(cardCount), palette, context, cardSize)
        }
    }
}

@Composable
private fun StackedBody(
    state: WidgetPlaybackState,
    palette: WidgetPalette,
    context: Context,
    upNext: List<WidgetMediaItem>,
    layout: GlassMiniLayout,
    width: Dp,
    height: Dp,
) {
    val compact = layout == GlassMiniLayout.Compact
    val button = if (compact) 26.dp else 28.dp
    val play = if (compact) 32.dp else 34.dp
    val titleSize = if (compact) 13.sp else 14.sp
    // Title + artist block (compact stacks the controls under it, medium puts them beside it).
    val textHeight = 36.dp
    val bottomHeight = if (compact) textHeight + 6.dp + play else maxOf(textHeight, play)
    val topHeight = (height - bottomHeight - 8.dp).coerceAtLeast(MinCover)
    val coverSize = minOf(topHeight, width * 0.5f)
    val cardSize = (coverSize * if (compact) 0.8f else 0.7f).coerceAtLeast(MinCardSize)
    val cardCount =
        nextCardsFit(width - coverSize - 8.dp, cardSize, upNext.size).coerceAtMost(layout.maxCards)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(topHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WidgetArtwork(
                artPath = state.artPath,
                context = context,
                contentDescription = null,
                targetSize = coverSize,
                shape = WidgetArtShape.Rounded,
                cornerRadius = if (compact) 14.dp else 18.dp,
                palette = palette,
                modifier = GlanceModifier.size(coverSize),
            )
            if (cardCount > 0) {
                Spacer(GlanceModifier.width(8.dp))
                WidgetNextCards(upNext.take(cardCount), palette, context, cardSize)
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (compact) {
            WidgetTrackText(state, palette, GlanceModifier.fillMaxWidth(), titleSize = titleSize)
            Spacer(GlanceModifier.height(6.dp))
            WidgetGlassTransport(state, palette, context, buttonSize = button, playSize = play)
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetTrackText(state, palette, GlanceModifier.defaultWeight(), titleSize = titleSize)
                Spacer(GlanceModifier.width(6.dp))
                WidgetGlassTransport(state, palette, context, buttonSize = button, playSize = play)
            }
        }
    }
}

private val SurfacePadding = 6.dp
private val CardPadding = 8.dp
private val CardRadius = 24.dp
private val MinCover = 40.dp
private val MinButton = 24.dp
private val MinCardSize = 30.dp
