/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.height
import androidx.glance.layout.width
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * Audio-wave style progress bar. Widgets cannot animate or draw, so the bars are rendered into a
 * bitmap: the shape is derived from [seed] (so each song keeps its own wave) and the bars up to
 * [progress] use [playedColor].
 */
@Composable
internal fun WidgetWaveform(
    context: Context,
    seed: String,
    progress: Float,
    width: Dp,
    height: Dp,
    playedColor: Int,
    restColor: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val density = context.resources.displayMetrics.density
    val widthPx = (width.value * density).toInt().coerceIn(WaveMinPx, WaveMaxPx)
    val heightPx = (height.value * density).toInt().coerceIn(WaveMinPx, WaveMaxPx)
    val playedBars = (progress.coerceIn(0f, 1f) * WaveBars).toInt()
    val bitmap =
        remember(seed, playedBars, widthPx, heightPx, playedColor, restColor) {
            renderWaveform(seed, playedBars, widthPx, heightPx, playedColor, restColor)
        }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.width(width).height(height),
    )
}

private fun renderWaveform(
    seed: String,
    playedBars: Int,
    widthPx: Int,
    heightPx: Int,
    playedColor: Int,
    restColor: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val slot = widthPx.toFloat() / WaveBars
    val barWidth = slot * 0.58f
    val radius = barWidth / 2f
    var state = seed.hashCode().toLong() xor WaveSeedSalt
    for (index in 0 until WaveBars) {
        state = state * 6364136223846793005L + 1442695040888963407L
        val noise = ((state ushr 33) % 1000L) / 1000f
        val envelope = 0.55f + 0.45f * abs(sin(index * PI / WaveBars * 2.3 + (noise * 0.6)))
        val barHeight = max(heightPx * 0.16f, heightPx * (0.28f + 0.72f * noise) * envelope.toFloat())
        val left = index * slot + (slot - barWidth) / 2f
        val top = (heightPx - barHeight) / 2f
        paint.color = if (index < playedBars) playedColor else restColor
        canvas.drawRoundRect(RectF(left, top, left + barWidth, top + barHeight), radius, radius, paint)
    }
    return bitmap
}

private const val WaveBars = 44
private const val WaveMinPx = 32
private const val WaveMaxPx = 1200
private const val WaveSeedSalt = 0x5DEECE66DL
