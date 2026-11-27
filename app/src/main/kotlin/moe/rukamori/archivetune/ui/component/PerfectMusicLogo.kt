package moe.rukamori.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.util.lerp
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt

/** Logo do Perfect Music: acompanha o matiz do tema (que segue a cor do álbum). */
@Composable
fun PerfectMusicLogo(modifier: Modifier = Modifier) {
    val accent by animateColorAsState(
        targetValue = perfectLogoAccent(MaterialTheme.colorScheme.primary),
        animationSpec = tween(durationMillis = LogoColorMillis),
        label = "perfectLogoAccent",
    )
    PerfectMusicLogo(accent = accent, modifier = modifier)
}

/** Cor de destaque da logo: mantém o matiz do tema (que acompanha o álbum) com saturação/brilho fixos. */
private fun perfectLogoAccent(themeColor: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(themeColor.toArgb(), hsl)
    val saturation = lerp(0.12f, 0.88f, (hsl[1] / 0.5f).coerceIn(0f, 1f))
    return Color.hsl(hsl[0], saturation, 0.54f)
}

@Composable
fun PerfectMusicLogo(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(accent.toArgb(), it) }
    val light = Color.hsl(hsl[0], hsl[1], 0.64f)
    val mid = Color.hsl(hsl[0], hsl[1] * 0.88f, 0.38f)
    val back = Color.hsl(hsl[0], hsl[1] * 0.66f, 0.19f)
    val reflection = Color.hsl(hsl[0], hsl[1] * 0.62f, 0.28f).copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        val unit = size.minDimension / LogoViewport
        withTransform({
            scale(unit, unit, pivot = Offset.Zero)
            translate(-LogoOriginX, -LogoOriginY)
        }) {
            drawRoundRect(back, Offset(130f, 80f), Size(215f, 215f), CornerRadius(56f))
            drawRoundRect(mid, Offset(102f, 112f), Size(213f, 213f), CornerRadius(56f))
            drawRoundRect(LogoFrontPlate, Offset(75f, 148f), Size(210f, 207f), CornerRadius(54f))
            drawRoundRect(
                color = accent,
                topLeft = Offset(75f, 148f),
                size = Size(210f, 207f),
                cornerRadius = CornerRadius(54f),
                style = Stroke(width = 7f),
            )
            LogoBarCenters.forEachIndexed { index, centerX ->
                val barHeight = LogoBarHeights[index]
                val barWidth = if (index % 8 == 0) 12f else 13f
                drawRoundRect(
                    color = if (index == LogoPeakBar) light else accent,
                    topLeft = Offset(centerX - barWidth / 2f, LogoBaseline - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6f),
                )
                drawRoundRect(
                    color = reflection,
                    topLeft = Offset(centerX - barWidth / 2f, LogoBaseline + 6f),
                    size = Size(barWidth, (barHeight * 0.72f).roundToInt().toFloat()),
                    cornerRadius = CornerRadius(6f),
                )
            }
        }
    }
}

private const val LogoColorMillis = 600
private const val LogoViewport = 300f
private const val LogoOriginX = 60f
private const val LogoOriginY = 70f
private const val LogoBaseline = 246f
private const val LogoPeakBar = 4
private val LogoFrontPlate = Color(0xFF19191C)
private val LogoBarCenters = floatArrayOf(104f, 124f, 144f, 164f, 184f, 204f, 224f, 242f, 262f)
private val LogoBarHeights = floatArrayOf(10f, 24f, 44f, 68f, 88f, 60f, 76f, 36f, 18f)
