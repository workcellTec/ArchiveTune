/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider

/** Up-next songs decoded from the widget state. */
@Composable
internal fun rememberWidgetUpNext(): List<WidgetMediaItem> {
    val preferences = currentState<Preferences>()
    return remember(preferences) { preferences.widgetUpNext() }
}

/** Card size and count that fill the free area of a widget. */
internal data class NextCardPlan(
    val size: Dp,
    val count: Int,
)

/**
 * Sizes the up-next cards from the free area: as tall as the height allows (up to [MaxNextCardSize]),
 * never wider than the area, and as many overlapped cards as then fit the width. A zero count means
 * the area is too small and the cards should be left out.
 */
internal fun planNextCards(
    availableWidth: Dp,
    availableHeight: Dp,
    available: Int,
): NextCardPlan {
    val size = minOf(availableHeight, availableWidth, MaxNextCardSize)
    val count = nextCardsFit(availableWidth, size, available)
    return if (count == 0) NextCardPlan(0.dp, 0) else NextCardPlan(size, count)
}

/** How many overlapped cards fit in [availableWidth] (0 when not even one fits). */
internal fun nextCardsFit(
    availableWidth: Dp,
    cardSize: Dp,
    available: Int,
): Int {
    if (available <= 0 || cardSize < MinNextCardSize) return 0
    val step = cardSize * NextCardStepFraction
    val extra = ((availableWidth - cardSize) / step).toInt() + 1
    return extra.coerceIn(1, MaxNextCards).coerceAtMost(available)
}

/**
 * Square cover cards fanned out side by side: the first one sits in front and every following
 * card is a bit smaller, dimmer and tucked behind the previous one. Tapping a card plays it.
 */
@Composable
internal fun WidgetNextCards(
    items: List<WidgetMediaItem>,
    palette: WidgetPalette,
    context: Context,
    cardSize: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    if (items.isEmpty()) return
    val step = cardSize * NextCardStepFraction
    val totalWidth = step * (items.size - 1) + cardSize
    Box(modifier = modifier.width(totalWidth).height(cardSize)) {
        // Back-most card is drawn first so the first one ends up on top.
        for (index in items.indices.reversed()) {
            val item = items[index]
            val size = cardSize * (1f - NextCardShrink * index)
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(start = step * index),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier =
                        GlanceModifier
                            .size(size)
                            .cornerRadius(NextCardRadius)
                            .background(NextCardRing)
                            .padding(NextCardRingWidth)
                            .clickable(playQueueItemAction(item)),
                ) {
                    WidgetArtwork(
                        artPath = item.artPath,
                        context = context,
                        contentDescription = item.title,
                        targetSize = size,
                        shape = WidgetArtShape.Rounded,
                        cornerRadius = NextCardRadius - NextCardRingWidth,
                        palette = palette,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                    if (index > 0) {
                        Box(
                            modifier =
                                GlanceModifier
                                    .fillMaxSize()
                                    .cornerRadius(NextCardRadius - NextCardRingWidth)
                                    .background(ColorProvider(Color.Black.copy(alpha = NextCardDim * index))),
                        ) {}
                    }
                }
            }
        }
    }
}

private val NextCardRadius = 14.dp
private val NextCardRingWidth = 2.dp
private val NextCardRing = ColorProvider(Color.White.copy(alpha = 0.28f))
private val MinNextCardSize = 40.dp
private val MaxNextCardSize = 88.dp
private const val NextCardStepFraction = 0.74f
private const val NextCardShrink = 0.07f
private const val NextCardDim = 0.16f
private const val MaxNextCards = 4
