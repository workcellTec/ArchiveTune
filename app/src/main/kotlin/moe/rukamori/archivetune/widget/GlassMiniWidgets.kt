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
import androidx.glance.appwidget.provideContent
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
    internal val surfacePadding: Dp,
    internal val cardPadding: Dp,
) {
    /** 4x1: cover, text and controls in a row, up-next cards on the right. */
    Slim(maxCards = 3, surfacePadding = 4.dp, cardPadding = 6.dp),

    /** 3x2: cover and cards on top, title with controls below. */
    Medium(maxCards = 3, surfacePadding = 6.dp, cardPadding = 8.dp),

    /** 2x2: small cover and cards on top, title and compact controls below. */
    Compact(maxCards = 2, surfacePadding = 6.dp, cardPadding = 8.dp),
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
        WidgetSurface(state, palette, context, contentPadding = layout.surfacePadding, backdrop = true) {
            val inset = (layout.surfacePadding + layout.cardPadding) * 2
            val contentWidth = size.width - inset
            val contentHeight = size.height - inset
            val cards = if (state.isAvailable) upNext else emptyList()
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(layout.cardPadding),
            ) {
                when (layout) {
                    GlassMiniLayout.Slim -> SlimBody(state, palette, context, cards, contentWidth, contentHeight)
                    else -> StackedBody(state, palette, context, cards, layout, contentWidth, contentHeight)
                }
            }
        }
    }
}

/**
 * Picks the biggest card size that lets as many cards as possible (up to [maxCards]) fan out in
 * [areaWidth], so a wider widget shows more covers instead of leaving the space empty.
 */
private fun fitCards(
    areaWidth: Dp,
    maxSize: Dp,
    available: Int,
    maxCards: Int,
): NextCardPlan {
    var n = minOf(maxCards, available)
    while (n >= 1) {
        val size = minOf(maxSize, areaWidth / (1f + (n - 1) * CardStep))
        if (size >= MinCardSize) return NextCardPlan(size, n)
        n--
    }
    return NextCardPlan(0.dp, 0)
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
    val button = (height * 0.46f).coerceIn(MinButton, 30.dp)
    val play = button + 6.dp
    val transportWidth = button * 2 + play + 16.dp
    // Cards only get what is left once the cover and the controls have their room.
    val cardsArea = width - coverSize - transportWidth - 20.dp
    val plan = fitCards(cardsArea, minOf(height * 0.7f, 48.dp), upNext.size, GlassMiniLayout.Slim.maxCards)
    val showArtist = height >= 58.dp
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
            Spacer(GlanceModifier.height(if (showArtist) 6.dp else 4.dp))
            WidgetGlassTransport(state, palette, context, buttonSize = button, playSize = play)
        }
        if (plan.count > 0) {
            Spacer(GlanceModifier.width(8.dp))
            WidgetNextCards(upNext.take(plan.count), palette, context, plan.size)
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
    val button = if (compact) 30.dp else 32.dp
    val play = if (compact) 40.dp else 44.dp
    val topHeight = (height - play - 8.dp).coerceAtLeast(MinCover)
    val coverSize = minOf(topHeight, width * 0.46f)
    val textHeight = if (compact) 30.dp else 36.dp
    val rightWidth = width - coverSize - 10.dp
    // Cards sit above the title in the column right of the cover, so they only get what the text leaves.
    val plan =
        fitCards(
            rightWidth,
            minOf(topHeight - textHeight - 4.dp, coverSize * 0.62f, 60.dp),
            upNext.size,
            3,
        )
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth().height(topHeight)) {
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
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                if (plan.count > 0) {
                    WidgetNextCards(upNext.take(plan.count), palette, context, plan.size)
                }
                Spacer(GlanceModifier.defaultWeight())
                WidgetTrackText(
                    state,
                    palette,
                    GlanceModifier.fillMaxWidth(),
                    titleSize = if (compact) 12.sp else 14.sp,
                )
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        // Previous at the left edge, pause in the middle, next at the right edge.
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isAvailable) {
                Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.CenterStart) {
                    WidgetSkipButton(
                        forward = false,
                        palette = palette,
                        context = context,
                        modifier = GlanceModifier.size(button),
                        container = GlassFill,
                        cornerRadius = button / 2,
                        iconSize = button * 0.5f,
                    )
                }
            }
            WidgetPlayCircle(state, palette, context, play)
            if (state.isAvailable) {
                Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.CenterEnd) {
                    WidgetSkipButton(
                        forward = true,
                        palette = palette,
                        context = context,
                        modifier = GlanceModifier.size(button),
                        container = GlassFill,
                        cornerRadius = button / 2,
                        iconSize = button * 0.5f,
                    )
                }
            }
        }
    }
}

private val MinCover = 40.dp
private val MinButton = 24.dp
private val MinCardSize = 28.dp
private const val CardStep = 0.74f
