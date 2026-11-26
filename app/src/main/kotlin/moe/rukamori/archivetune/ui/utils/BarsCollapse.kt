/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Whether the floating bottom bars (navigation + mini player) are retracted because the user is scrolling down. */
@Stable
class BarsCollapseState {
    var collapsed by mutableStateOf(false)
}

/**
 * Collapses on a sustained scroll down (finger moving up) and restores on a sustained scroll up,
 * ignoring horizontal gestures and tiny movements.
 */
@Composable
fun rememberBarsCollapseConnection(
    state: BarsCollapseState,
    enabled: () -> Boolean,
): NestedScrollConnection {
    val thresholdPx = with(LocalDensity.current) { 28.dp.toPx() }
    return remember(state, thresholdPx) {
        object : NestedScrollConnection {
            private var accumulated = 0f

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!enabled() || abs(available.y) <= abs(available.x)) return Offset.Zero
                if (accumulated != 0f && (accumulated > 0f) != (available.y > 0f)) accumulated = 0f
                accumulated += available.y
                if (accumulated <= -thresholdPx) {
                    state.collapsed = true
                    accumulated = 0f
                } else if (accumulated >= thresholdPx) {
                    state.collapsed = false
                    accumulated = 0f
                }
                return Offset.Zero
            }
        }
    }
}
