/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.NavigationBarMode
import moe.rukamori.archivetune.ui.component.PreferenceEntry
import moe.rukamori.archivetune.ui.component.rememberCoverNavigationColor

private val PreviewHeight = 92.dp
private val MockMiniHeight = 26.dp
private val MockBarHeight = 28.dp

@Composable
private fun navigationBarModeLabel(mode: NavigationBarMode): String =
    when (mode) {
        NavigationBarMode.CONNECTED -> stringResource(R.string.navigation_bar_mode_connected)
        NavigationBarMode.STANDALONE -> stringResource(R.string.navigation_bar_mode_standalone)
        NavigationBarMode.APPLE_MUSIC -> stringResource(R.string.navigation_bar_mode_apple_music)
        NavigationBarMode.COVER_COLOR -> stringResource(R.string.navigation_bar_mode_cover_color)
        NavigationBarMode.SINGLE_PIECE -> stringResource(R.string.navigation_bar_mode_single_piece)
        NavigationBarMode.ICONS_PILL -> stringResource(R.string.navigation_bar_mode_icons_pill)
    }

/**
 * Navigation bar style picker. Tapping the entry opens the list right below it, and every style
 * shows a small drawing of how the mini player and the bar will look.
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NavigationBarMode.entries.forEach { mode ->
                        NavigationBarStyleOption(
                            mode = mode,
                            selected = mode == selectedMode,
                            onClick = { onModeSelected(mode) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun NavigationBarStyleOption(
    mode: NavigationBarMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
                    shape = shape,
                ).selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = navigationBarModeLabel(mode),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.primary else scheme.onSurface,
        )
        NavigationBarStyleMock(mode)
    }
}

@Composable
private fun NavigationBarStyleMock(mode: NavigationBarMode) {
    val scheme = MaterialTheme.colorScheme
    val cover = rememberCoverNavigationColor() ?: lerp(Color.Black, scheme.primary, 0.5f)
    val usesCover = mode.usesCoverColor
    val barColor = if (usesCover) cover else scheme.surfaceContainer
    val miniColor = if (usesCover) lerp(cover, Color.White, 0.08f) else scheme.surfaceContainerHigh
    val foreground = if (usesCover) Color.White.copy(alpha = 0.85f) else scheme.onSurfaceVariant
    val big = 14.dp
    val small = 4.dp

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PreviewHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surface)
                .padding(8.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        when (mode) {
            NavigationBarMode.CONNECTED, NavigationBarMode.COVER_COLOR -> {
                MockMini(miniColor, foreground, RoundedCornerShape(big, big, small, small))
                Spacer(Modifier.height(3.dp))
                MockBar(barColor, foreground, RoundedCornerShape(small, small, big, big), Modifier.fillMaxWidth())
            }
            NavigationBarMode.STANDALONE -> {
                MockMini(miniColor, foreground, RoundedCornerShape(big))
                Spacer(Modifier.height(6.dp))
                MockBar(barColor, foreground, RoundedCornerShape(big), Modifier.fillMaxWidth())
            }
            NavigationBarMode.APPLE_MUSIC -> {
                MockMini(miniColor, foreground, RoundedCornerShape(big))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    MockBar(scheme.onSurface.copy(alpha = 0.12f), foreground, CircleShape, Modifier.weight(1f), icons = 2)
                    Box(
                        modifier = Modifier.size(MockBarHeight).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        MockIcon(foreground)
                    }
                }
            }
            NavigationBarMode.SINGLE_PIECE -> {
                Column(modifier = Modifier.clip(RoundedCornerShape(big)).background(barColor)) {
                    MockMini(miniColor, foreground, RoundedCornerShape(0.dp))
                    MockBar(barColor, foreground, RoundedCornerShape(0.dp), Modifier.fillMaxWidth())
                }
            }
            NavigationBarMode.ICONS_PILL -> {
                MockMini(miniColor, foreground, RoundedCornerShape(big))
                Spacer(Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    MockBar(barColor, foreground, CircleShape, Modifier.width(96.dp), icons = 3)
                }
            }
        }
    }
}

@Composable
private fun MockMini(
    color: Color,
    foreground: Color,
    shape: Shape,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(MockMiniHeight).clip(shape).background(color).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(foreground.copy(alpha = 0.5f)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.width(54.dp).height(4.dp).clip(CircleShape).background(foreground))
            Box(Modifier.width(34.dp).height(3.dp).clip(CircleShape).background(foreground.copy(alpha = 0.5f)))
        }
        Box(Modifier.size(14.dp).clip(CircleShape).background(foreground))
    }
}

@Composable
private fun MockBar(
    color: Color,
    foreground: Color,
    shape: Shape,
    modifier: Modifier,
    icons: Int = 3,
) {
    Row(
        modifier = modifier.height(MockBarHeight).clip(shape).background(color).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        repeat(icons) { MockIcon(foreground) }
    }
}

@Composable
private fun MockIcon(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}
