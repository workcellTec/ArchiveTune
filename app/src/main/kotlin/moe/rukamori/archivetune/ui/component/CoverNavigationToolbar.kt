/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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
import moe.rukamori.archivetune.constants.FloatingBarJunctionCornerRadius
import moe.rukamori.archivetune.constants.FloatingBarOuterCornerRadius
import moe.rukamori.archivetune.constants.FloatingBarStandaloneCornerRadius
import moe.rukamori.archivetune.constants.MiniPlayerBottomSpacing
import moe.rukamori.archivetune.constants.NavigationBarHeight
import moe.rukamori.archivetune.constants.NavigationBarMaxWidth
import moe.rukamori.archivetune.constants.NavigationBarMode
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.ui.screens.Screens
import moe.rukamori.archivetune.ui.theme.PlayerColorExtractor

private const val CoverColorCacheSize = 24

/** How much of the artwork color is kept; the rest goes to black so white icons stay readable. */
private const val CoverColorStrength = 0.5f

private val IconsPillHeight = 56.dp
private val IconsPillItemWidth = 64.dp

/**
 * Dark navigation color derived from the cover art of the song that is playing, or null while
 * nothing is playing or the cover is not available yet. It follows the bottom color of the
 * mini player gradient so both pieces look like the same family.
 */
@Composable
fun rememberCoverNavigationColor(): Color? {
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
            object : LinkedHashMap<String, Color>(CoverColorCacheSize, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Color>?): Boolean =
                    size > CoverColorCacheSize
            }
        }
    var color by remember { mutableStateOf<Color?>(null) }
    val thumbnailUrl = metadata?.thumbnailUrl

    LaunchedEffect(thumbnailUrl, fallbackColor) {
        if (thumbnailUrl.isNullOrBlank()) {
            color = null
            return@LaunchedEffect
        }
        cache[thumbnailUrl]?.let {
            color = it
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
                val bitmap = result.image?.toBitmap() ?: return@runCatching null
                val palette =
                    withContext(Dispatchers.Default) {
                        Palette
                            .from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }
                val colors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
                val base = colors.getOrNull(2) ?: colors.firstOrNull() ?: return@runCatching null
                lerpColor(Color.Black, base, CoverColorStrength)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                null
            }
        if (extracted != null) {
            cache[thumbnailUrl] = extracted
        }
        color = extracted
    }
    return color
}

@Composable
internal fun CoverNavigationToolbar(
    items: List<Screens>,
    mode: NavigationBarMode,
    pureBlack: Boolean,
    modifier: Modifier,
    miniPlayerProximityProvider: () -> Float,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onSearchItemDoubleClick: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val cover = rememberCoverNavigationColor()
    val fallbackContainer = if (pureBlack) Color.Black else scheme.surfaceContainer
    val container by animateColorAsState(cover ?: fallbackContainer, label = "coverNavigationContainer")
    val hasCover = cover != null
    val selectedTint = if (hasCover) Color.White else scheme.primary
    val unselectedTint = if (hasCover) Color.White.copy(alpha = 0.72f) else scheme.onSurfaceVariant
    val indicator = if (hasCover) lerpColor(container, Color.White, 0.22f) else scheme.secondaryContainer

    val isPill = mode == NavigationBarMode.ICONS_PILL
    val proximity = if (mode.joinsMiniPlayer) miniPlayerProximityProvider() else 0f
    val junctionRadius = if (mode == NavigationBarMode.SINGLE_PIECE) 0f else FloatingBarJunctionCornerRadius.value
    val shape =
        if (isPill) {
            RoundedCornerShape(percent = 50)
        } else {
            RoundedCornerShape(
                topStart = lerp(FloatingBarStandaloneCornerRadius.value, junctionRadius, proximity).dp,
                topEnd = lerp(FloatingBarStandaloneCornerRadius.value, junctionRadius, proximity).dp,
                bottomStart = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarOuterCornerRadius.value, proximity).dp,
                bottomEnd = lerp(FloatingBarStandaloneCornerRadius.value, FloatingBarOuterCornerRadius.value, proximity).dp,
            )
        }
    val bridgeHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { MiniPlayerBottomSpacing.toPx() }
    val fillGap = mode == NavigationBarMode.SINGLE_PIECE

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .then(
                        if (isPill) {
                            Modifier.height(IconsPillHeight)
                        } else {
                            Modifier.widthIn(max = NavigationBarMaxWidth).fillMaxWidth().height(NavigationBarHeight)
                        },
                    ).drawBehind {
                        // Single piece: close the small gap between the mini player and the bar.
                        val gapAlpha = if (fillGap) proximity.coerceIn(0f, 1f) else 0f
                        if (gapAlpha > 0f) {
                            drawRect(
                                color = container.copy(alpha = gapAlpha),
                                topLeft = Offset(0f, -bridgeHeightPx),
                                size = Size(size.width, bridgeHeightPx),
                            )
                        }
                    },
        ) {
            Row(
                modifier =
                    Modifier
                        .then(if (isPill) Modifier else Modifier.fillMaxWidth().fillMaxHeight())
                        .clip(shape)
                        .background(container)
                        .selectableGroup()
                        .padding(
                            horizontal = if (isPill) 8.dp else 12.dp,
                            vertical = if (isPill) 6.dp else 8.dp,
                        ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { screen ->
                    CoverNavigationItem(
                        screen = screen,
                        selected = isSelected(screen),
                        showLabel = !isPill,
                        selectedTint = selectedTint,
                        unselectedTint = unselectedTint,
                        indicatorColor = indicator,
                        modifier = if (isPill) Modifier.size(IconsPillItemWidth, IconsPillHeight - 12.dp) else Modifier.weight(1f).fillMaxHeight(),
                        onItemClick = onItemClick,
                        onSearchItemDoubleClick = onSearchItemDoubleClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverNavigationItem(
    screen: Screens,
    selected: Boolean,
    showLabel: Boolean,
    selectedTint: Color,
    unselectedTint: Color,
    indicatorColor: Color,
    modifier: Modifier,
    onItemClick: (Screens, Boolean) -> Unit,
    onSearchItemDoubleClick: (() -> Unit)?,
) {
    val tint by animateColorAsState(if (selected) selectedTint else unselectedTint, label = "coverNavigationTint")
    val indicator by animateColorAsState(if (selected) indicatorColor else Color.Transparent, label = "coverNavigationIndicator")
    val onClick = rememberNavigationItemClick(screen, selected, onItemClick, onSearchItemDoubleClick)
    val title = stringResource(screen.titleId)
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .selectable(selected = selected, role = Role.Tab, onClick = onClick)
                .semantics { contentDescription = title },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = if (showLabel) 56.dp else 52.dp, height = if (showLabel) 30.dp else 36.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
        if (showLabel) {
            Text(
                text = title,
                color = tint,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
