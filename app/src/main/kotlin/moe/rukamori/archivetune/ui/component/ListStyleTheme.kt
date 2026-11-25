/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.constants.ListStyle
import moe.rukamori.archivetune.constants.ListStyleKey
import moe.rukamori.archivetune.utils.rememberEnumPreference

/** Full-sheet gradient of the "Gradiente" list style; null for every other style. */
val LocalListBackdrop = compositionLocalOf<Brush?> { null }

/** True while a list style other than the default is applied. */
val LocalListStyled = compositionLocalOf { false }

private val OnDark = Color.White
private val OnDarkVariant = Color.White.copy(alpha = 0.72f)

/**
 * Re-tints the colors and corners the queue and the player menu read from [MaterialTheme], so both
 * follow the style picked in settings without each component knowing about it. Styles that paint
 * with the cover fall back to the normal look while no cover color is available.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListStyleTheme(
    enabled: Boolean = true,
    content: @Composable (backdrop: Brush?) -> Unit,
) {
    val style by rememberEnumPreference(ListStyleKey, defaultValue = ListStyle.DEFAULT)
    val scheme = MaterialTheme.colorScheme
    val gradient = rememberCoverGradient()
    val styled =
        remember(style, enabled, scheme, gradient) {
            if (enabled) scheme.styledFor(style, gradient) else null
        }
    val shapes = MaterialTheme.shapes
    val styledShapes =
        remember(style, shapes) {
            if (style == ListStyle.ICONS_PILL && styled != null) {
                shapes.copy(
                    small = RoundedCornerShape(50),
                    medium = RoundedCornerShape(28.dp),
                    large = RoundedCornerShape(36.dp),
                    extraLarge = RoundedCornerShape(44.dp),
                )
            } else {
                shapes
            }
        }
    // The wrapper keeps the same structure whether or not a style applies, so toggling it never
    // recreates the content below (the player menu flips [enabled] while it is already open).
    MaterialExpressiveTheme(
        colorScheme = styled?.scheme ?: scheme,
        motionScheme = MaterialTheme.motionScheme,
        typography = MaterialTheme.typography,
        shapes = styledShapes,
    ) {
        CompositionLocalProvider(
            LocalListBackdrop provides styled?.backdrop,
            LocalListStyled provides (styled != null),
        ) {
            content(styled?.backdrop)
        }
    }
}

private class StyledScheme(
    val scheme: ColorScheme,
    val backdrop: Brush? = null,
)

private fun ColorScheme.tinted(
    sheet: Color,
    low: Color,
    high: Color,
    highest: Color,
    accent: Color,
    onAccent: Color,
    highlight: Color,
): ColorScheme =
    copy(
        surface = sheet,
        surfaceContainerLowest = sheet,
        surfaceContainerLow = low,
        surfaceContainer = low,
        surfaceContainerHigh = high,
        surfaceContainerHighest = highest,
        onSurface = OnDark,
        onSurfaceVariant = OnDarkVariant,
        primary = accent,
        onPrimary = onAccent,
        secondaryContainer = highlight,
        onSecondaryContainer = OnDark,
        primaryContainer = highlight,
        onPrimaryContainer = OnDark,
    )

private fun ColorScheme.styledFor(
    style: ListStyle,
    gradient: CoverGradient?,
): StyledScheme? =
    when (style) {
        ListStyle.DEFAULT -> null
        ListStyle.CONNECTED ->
            StyledScheme(
                copy(
                    surfaceContainerLow = surfaceContainerHigh,
                    surfaceContainer = surfaceContainerHigh,
                ),
            )
        ListStyle.STANDALONE ->
            StyledScheme(
                copy(
                    surfaceContainerLow = surfaceContainerHigh,
                    surfaceContainer = surfaceContainerHigh,
                    surfaceContainerHigh = surfaceContainerHighest,
                ),
            )
        ListStyle.APPLE_MUSIC -> {
            val sheet = Color(0xFF14161C).copy(alpha = 0.88f)
            StyledScheme(
                tinted(
                    sheet = sheet,
                    low = Color.White.copy(alpha = 0.09f),
                    high = Color.White.copy(alpha = 0.13f),
                    highest = Color.White.copy(alpha = 0.18f),
                    accent = Color.White.copy(alpha = 0.92f),
                    onAccent = Color(0xFF10202A),
                    highlight = Color.White.copy(alpha = 0.24f),
                ),
            )
        }
        ListStyle.COVER_COLOR, ListStyle.ICONS_PILL, ListStyle.SINGLE_PIECE -> {
            gradient ?: return null
            val sheet = gradient.colorAt(if (style == ListStyle.SINGLE_PIECE) 0.3f else 0.52f)
            val flat = style == ListStyle.SINGLE_PIECE
            StyledScheme(
                tinted(
                    sheet = sheet,
                    low = lerp(sheet, Color.White, if (flat) 0.06f else 0.08f),
                    high = lerp(sheet, Color.White, if (flat) 0.10f else 0.14f),
                    highest = lerp(sheet, Color.White, if (flat) 0.15f else 0.20f),
                    accent = lerp(gradient.colorAt(0f), Color.White, 0.72f),
                    onAccent = lerp(sheet, Color.Black, 0.4f),
                    highlight = lerp(sheet, Color.White, 0.26f),
                ),
            )
        }
        ListStyle.GRADIENT -> {
            gradient ?: return null
            val sheet = gradient.colorAt(0.3f)
            StyledScheme(
                tinted(
                    sheet = sheet,
                    low = Color.Black.copy(alpha = 0.26f),
                    high = Color.Black.copy(alpha = 0.32f),
                    highest = Color.Black.copy(alpha = 0.38f),
                    accent = lerp(gradient.colorAt(0f), Color.White, 0.72f),
                    onAccent = lerp(sheet, Color.Black, 0.4f),
                    highlight = Color.Black.copy(alpha = 0.40f),
                ),
                backdrop = gradient.brush(0f, 1f),
            )
        }
    }
