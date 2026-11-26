/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.constants.MiniPlayerBottomSpacing
import moe.rukamori.archivetune.constants.MiniPlayerHeight
import moe.rukamori.archivetune.constants.NavigationBarHeight
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.ui.theme.PlayerColorExtractor

private const val CoverPaletteCacheSize = 24

/** Same layers the mini player gradient uses: color alpha over black, then a 32% black scrim. */
private const val GradientScrimKeep = 0.68f

private val TotalGradientHeight = MiniPlayerHeight + MiniPlayerBottomSpacing + NavigationBarHeight

/** Share of the joint gradient (mini player + bar) where the mini player ends. */
val MiniPlayerGradientEnd: Float = MiniPlayerHeight / TotalGradientHeight

/** Share of the joint gradient where the navigation bar starts. */
val NavigationGradientStart: Float = (MiniPlayerHeight + MiniPlayerBottomSpacing) / TotalGradientHeight

/**
 * One gradient that runs through the mini player and the navigation bar, so the bar picks up
 * exactly where the mini player stops. The colors and layers are the ones the mini player uses.
 */
class CoverGradient(
    first: Color,
    second: Color,
    third: Color,
    /** Light theme: tint over white instead of black, so the gradient follows the cover's brightness. */
    val light: Boolean = false,
) {
    private val points =
        listOf(
            0f to effective(first, 0.95f),
            0.52f to effective(second, 0.82f),
            1f to effective(third, 0.72f),
        )

    fun colorAt(t: Float): Color {
        val position = t.coerceIn(0f, 1f)
        val upper = points.indexOfFirst { it.first >= position }.coerceAtLeast(1)
        val (startT, startColor) = points[upper - 1]
        val (endT, endColor) = points[upper]
        return lerp(startColor, endColor, ((position - startT) / (endT - startT)).coerceIn(0f, 1f))
    }

    /** Vertical brush that paints the slice [start]..[end] of the joint gradient over a box. */
    fun brush(
        start: Float,
        end: Float,
    ): Brush {
        val stops = mutableListOf(0f to colorAt(start))
        points.forEach { (t, color) ->
            if (t > start && t < end) stops += Pair((t - start) / (end - start), color)
        }
        stops += Pair(1f, colorAt(end))
        return Brush.verticalGradient(colorStops = stops.toTypedArray())
    }

    private fun effective(
        color: Color,
        alpha: Float,
    ): Color = lerp(if (light) Color.White else Color.Black, color.copy(alpha = 1f), alpha * GradientScrimKeep)

    companion object {
        fun from(
            colors: List<Color>,
            light: Boolean = false,
        ): CoverGradient? {
            val first = colors.firstOrNull() ?: return null
            val second = colors.getOrElse(1) { first }
            val third = colors.getOrElse(2) { second }
            return CoverGradient(first, second, third, light)
        }
    }
}

/** Gradient colors taken from the cover of the song that is playing; empty while unavailable. */
@Composable
fun rememberCoverGradient(light: Boolean = false): CoverGradient? {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val metadataFlow =
        remember(playerConnection) {
            playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
        }
    val metadata by metadataFlow.collectAsState()
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val cache =
        remember {
            object : LinkedHashMap<String, List<Color>>(CoverPaletteCacheSize, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Color>>?): Boolean =
                    size > CoverPaletteCacheSize
            }
        }
    var colors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val thumbnailUrl = metadata?.thumbnailUrl

    LaunchedEffect(thumbnailUrl, fallbackColor) {
        if (thumbnailUrl.isNullOrBlank()) {
            colors = emptyList()
            return@LaunchedEffect
        }
        cache[thumbnailUrl]?.let {
            colors = it
            return@LaunchedEffect
        }
        val request =
            ImageRequest
                .Builder(context)
                .data(thumbnailUrl)
                .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                .allowHardware(false)
                .build()
        val extracted =
            runCatching {
                val result = withContext(Dispatchers.IO) { context.imageLoader.execute(request) }
                val bitmap = result.image?.toBitmap() ?: return@runCatching emptyList()
                val palette =
                    withContext(Dispatchers.Default) {
                        Palette
                            .from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }
                PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                emptyList()
            }
        if (extracted.isNotEmpty()) cache[thumbnailUrl] = extracted
        colors = extracted
    }
    return remember(colors, light) { CoverGradient.from(colors, light) }
}

/** Solid dark color for the navigation bar: the bottom tone of the gradient. */
@Composable
fun rememberCoverNavigationColor(): Color? = rememberCoverGradient()?.colorAt(1f)
