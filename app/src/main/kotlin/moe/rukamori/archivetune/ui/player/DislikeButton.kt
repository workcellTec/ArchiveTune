/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R

/** True while the song that is playing right now is marked as "don't like". */
@Composable
fun rememberCurrentDisliked(): Boolean {
    val playerConnection = LocalPlayerConnection.current ?: return false
    val disliked by playerConnection.isCurrentDisliked.collectAsState(initial = false)
    return disliked
}

/** Subtle heart "pop" played when [liked] turns on; does nothing on first composition. */
@Composable
fun Modifier.heartPop(liked: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    var seenFirstValue by remember { mutableStateOf(false) }
    LaunchedEffect(liked) {
        if (!seenFirstValue) {
            seenFirstValue = true
            return@LaunchedEffect
        }
        if (liked) {
            scale.animateTo(1.3f, tween(durationMillis = 100))
            scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
        } else {
            scale.snapTo(1f)
        }
    }
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Broken-heart "don't like" button. Style parameters let each player design reuse its own
 * size, shape and colors while the behavior (skip + avoid for 30 days) stays in one place.
 */
@Composable
fun DislikeIconButton(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    tint: Color = MaterialTheme.colorScheme.onSurface,
    activeTint: Color = MaterialTheme.colorScheme.error,
    containerColor: Color = Color.Transparent,
    activeContainerColor: Color = containerColor,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val disliked = rememberCurrentDisliked()
    Box(
        modifier =
            modifier
                .size(size)
                .clip(shape)
                .background(if (disliked) activeContainerColor else containerColor)
                .clickable { playerConnection.toggleDislike() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (disliked) R.drawable.heart_broken_on else R.drawable.heart_broken),
            contentDescription = stringResource(if (disliked) R.string.action_remove_dislike else R.string.action_dislike),
            tint = if (disliked) activeTint else tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
