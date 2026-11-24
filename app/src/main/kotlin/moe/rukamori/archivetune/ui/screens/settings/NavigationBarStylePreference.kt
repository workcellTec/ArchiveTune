/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.MiniPlayerBottomSpacing
import moe.rukamori.archivetune.constants.NavigationBarBackgroundStyle
import moe.rukamori.archivetune.constants.NavigationBarBackgroundStyleKey
import moe.rukamori.archivetune.constants.NavigationBarHeight
import moe.rukamori.archivetune.constants.NavigationBarHorizontalPadding
import moe.rukamori.archivetune.constants.NavigationBarMode
import moe.rukamori.archivetune.constants.PureBlackKey
import moe.rukamori.archivetune.ui.component.FloatingNavigationToolbar
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.player.MiniPlayer
import moe.rukamori.archivetune.ui.screens.Screens
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference

/** The preview is laid out at the size of a phone screen and then scaled down to fit the card. */
private val PreviewDesignWidth = 392.dp
private val PreviewHeight = 250.dp

@Composable
private fun navigationBarModeLabel(mode: NavigationBarMode): String =
    when (mode) {
        NavigationBarMode.CONNECTED -> stringResource(R.string.navigation_bar_mode_connected)
        NavigationBarMode.STANDALONE -> stringResource(R.string.navigation_bar_mode_standalone)
        NavigationBarMode.APPLE_MUSIC -> stringResource(R.string.navigation_bar_mode_apple_music)
        NavigationBarMode.COVER_COLOR -> stringResource(R.string.navigation_bar_mode_cover_color)
        NavigationBarMode.SINGLE_PIECE -> stringResource(R.string.navigation_bar_mode_single_piece)
        NavigationBarMode.ICONS_PILL -> stringResource(R.string.navigation_bar_mode_icons_pill)
        NavigationBarMode.GRADIENT -> stringResource(R.string.navigation_bar_mode_gradient)
    }

/**
 * Navigation bar style picker. Tapping the entry opens a panel right below it with one preview and
 * a row of styles; picking a style applies it and the preview redraws with the real mini player
 * and the real navigation bar.
 */
@Composable
fun NavigationBarStylePreference(
    selectedMode: NavigationBarMode,
    onModeSelected: (NavigationBarMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    PreferenceEntry(
        modifier = modifier,
        title = { Text(stringResource(R.string.navigation_bar_mode)) },
        description = navigationBarModeLabel(selectedMode),
        icon = { Icon(painterResource(R.drawable.palette), null) },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        },
        onClick = { expanded = !expanded },
        content = {
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NavigationBarMode.entries.forEach { mode ->
                            FilterChip(
                                selected = mode == selectedMode,
                                onClick = { onModeSelected(mode) },
                                label = { Text(navigationBarModeLabel(mode)) },
                            )
                        }
                    }
                    NavigationBarLivePreview(selectedMode)
                }
            }
        },
    )
}

/** The real mini player and the real navigation bar, drawn over a simplified home screen. */
@Composable
private fun NavigationBarLivePreview(mode: NavigationBarMode) {
    val scheme = MaterialTheme.colorScheme
    val playerConnection = LocalPlayerConnection.current
    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)
    val (backgroundStyle) =
        rememberEnumPreference(NavigationBarBackgroundStyleKey, defaultValue = NavigationBarBackgroundStyle.THEME)
    val hazeState = remember { HazeState() }
    val metadata = playerConnection?.mediaMetadata?.collectAsState()?.value

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection) {
        val player = playerConnection?.player ?: return@LaunchedEffect
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .scaledToFitWidth(PreviewDesignWidth, PreviewHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp)).background(scheme.surfaceContainerHigh))
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(scheme.primaryContainer))
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = NavigationBarHorizontalPadding, end = NavigationBarHorizontalPadding, bottom = 10.dp),
        ) {
            if (metadata == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(70.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.navigation_bar_preview_play_something),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            } else {
                MiniPlayer(
                    position = position,
                    duration = duration,
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth(),
                    pureBlack = pureBlack,
                    navigationProximityProvider = { 1f },
                    navigationBarMode = mode,
                )
            }
            Spacer(Modifier.height(MiniPlayerBottomSpacing))
            FloatingNavigationToolbar(
                items = Screens.MainScreens,
                pureBlack = pureBlack,
                backgroundStyle = backgroundStyle,
                hazeState = hazeState,
                modifier = Modifier.height(NavigationBarHeight),
                miniPlayerProximityProvider = { 1f },
                navigationBarMode = mode,
                isSelected = { it == Screens.Library },
                onItemClick = { _, _ -> },
            )
        }
    }
}

/** Measures the content at [designWidth] x [designHeight] and scales it to the width it is given. */
private fun Modifier.scaledToFitWidth(
    designWidth: Dp,
    designHeight: Dp,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(Constraints.fixed(designWidth.roundToPx(), designHeight.roundToPx()))
        val scale = constraints.maxWidth.toFloat() / placeable.width
        layout(constraints.maxWidth, (placeable.height * scale).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
