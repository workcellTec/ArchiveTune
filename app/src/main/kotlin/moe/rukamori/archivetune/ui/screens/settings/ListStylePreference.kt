/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.ListStyle
import moe.rukamori.archivetune.ui.component.PreferenceEntry

@Composable
private fun listStyleLabel(style: ListStyle): String =
    when (style) {
        ListStyle.DEFAULT -> stringResource(R.string.list_style_default)
        ListStyle.CONNECTED -> stringResource(R.string.list_style_connected)
        ListStyle.STANDALONE -> stringResource(R.string.list_style_standalone)
        ListStyle.APPLE_MUSIC -> stringResource(R.string.list_style_apple_music)
        ListStyle.COVER_COLOR -> stringResource(R.string.list_style_cover_color)
        ListStyle.SINGLE_PIECE -> stringResource(R.string.list_style_single_piece)
        ListStyle.ICONS_PILL -> stringResource(R.string.list_style_icons_pill)
        ListStyle.GRADIENT -> stringResource(R.string.list_style_gradient)
    }

/** Picker for the look of the queue and the player menu; tapping the entry opens the style chips. */
@Composable
fun ListStylePreference(
    selected: ListStyle,
    onSelected: (ListStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    PreferenceEntry(
        modifier = modifier,
        title = { Text(stringResource(R.string.list_style)) },
        description = "${listStyleLabel(selected)} · ${stringResource(R.string.list_style_description)}",
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ListStyle.entries.forEach { style ->
                        FilterChip(
                            selected = style == selected,
                            onClick = { onSelected(style) },
                            label = { Text(listStyleLabel(style)) },
                        )
                    }
                }
            }
        },
    )
}
