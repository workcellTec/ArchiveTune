/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.LocalPlayerConnection
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** True while the song that is playing right now is marked as "don't like". */
@Composable
fun rememberCurrentDisliked(): Boolean {
    val playerConnection = LocalPlayerConnection.current ?: return false
    val disliked by playerConnection.isCurrentDisliked.collectAsState(initial = false)
    return disliked
}

private val HeartBurstRed = Color(0xFFFF3B4F)
private val HeartBurstLight = Color(0xFFFFB3BC)

/**
 * Played the moment [liked] turns on: the heart pulses, a soft red glow expands and small red sparkles
 * fly outwards. Nothing plays on first composition or when the like is removed.
 */
@Composable
fun Modifier.heartPop(liked: Boolean): Modifier {
    val scale = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    var seenFirstValue by remember { mutableStateOf(false) }
    LaunchedEffect(liked) {
        if (!seenFirstValue) {
            seenFirstValue = true
            return@LaunchedEffect
        }
        if (liked) {
            coroutineScope {
                launch {
                    burst.snapTo(0f)
                    burst.animateTo(1f, tween(durationMillis = 700, easing = LinearOutSlowInEasing))
                }
                launch {
                    scale.animateTo(1.4f, tween(durationMillis = 110))
                    scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
                }
            }
        } else {
            scale.snapTo(1f)
            burst.snapTo(0f)
        }
    }
    return this
        .drawBehind {
            val progress = burst.value
            if (progress <= 0f || progress >= 1f) return@drawBehind
            val fade = 1f - progress
            val center = Offset(size.width / 2f, size.height / 2f)
            val base = size.minDimension
            // Soft red pulse behind the heart.
            drawCircle(
                color = HeartBurstRed.copy(alpha = 0.38f * fade),
                radius = base * (0.35f + 0.55f * progress),
                center = center,
            )
            // Sparkles flying outwards.
            val count = 10
            for (i in 0 until count) {
                val angle = (2.0 * PI * i / count).toFloat() + 0.3f
                val reach = base * (if (i % 2 == 0) 0.85f else 0.62f)
                val distance = base * 0.3f + reach * progress
                val sparkle = Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance)
                val radius = base * (if (i % 2 == 0) 0.075f else 0.05f) * (1f - 0.6f * progress)
                drawCircle(
                    color = (if (i % 3 == 0) HeartBurstLight else HeartBurstRed).copy(alpha = fade),
                    radius = radius,
                    center = sparkle,
                )
            }
        }.graphicsLayer {
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
