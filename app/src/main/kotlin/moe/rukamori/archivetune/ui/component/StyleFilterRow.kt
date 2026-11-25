/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.styles.SongStyleTagger
import moe.rukamori.archivetune.styles.StyleOption
import moe.rukamori.archivetune.styles.styleTagLabel

private val PillHeight = 44.dp
private val PillCircle = 36.dp

/** Sort module: "Data de adição" and the direction arrow in one pill, tinted like the style pills. */
@Composable
fun StyleSortModule(
    sortLabel: String,
    sortDescending: Boolean,
    onSortClick: () -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = PillHeight,
    sortMenu: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val onPill = colors.onSecondaryContainer
    val arrowRotation by animateFloatAsState(
        targetValue = if (sortDescending) 0f else 180f,
        label = "StyleSortDirection",
    )
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .height(height)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer),
        ) {
            Text(
                text = sortLabel,
                color = onPill,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .clickable(role = Role.Button, onClick = onSortClick)
                        .padding(start = 18.dp, end = 10.dp)
                        .padding(vertical = 14.dp),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .padding(end = 4.dp)
                        .size(height - 8.dp)
                        .clip(CircleShape)
                        .background(onPill.copy(alpha = 0.14f))
                        .clickable(role = Role.Button) { onSortDescendingChange(!sortDescending) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_downward),
                    contentDescription =
                        stringResource(
                            if (sortDescending) R.string.sort_order_descending else R.string.sort_order_ascending,
                        ),
                    tint = onPill,
                    modifier = Modifier.size(18.dp).rotate(arrowRotation),
                )
            }
        }
        sortMenu()
    }
}

/**
 * Style pills: selected ones first (inner "x" removes it and the ones after it), then the next options with
 * their song count. Same shape as [StyleSortModule]: text plus an inner circle.
 */
@Composable
fun StylePillsRow(
    path: List<String>,
    options: List<StyleOption>,
    progress: SongStyleTagger.Progress?,
    onSelect: (String) -> Unit,
    onRemoveFrom: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val strong = colors.secondaryContainer
    val soft = colors.secondaryContainer.copy(alpha = 0.45f)
    val onPill = colors.onSecondaryContainer
    val circle = onPill.copy(alpha = 0.14f)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(path.size, key = { "sel:${path[it]}" }) { index ->
            StylePill(
                label = styleTagLabel(path[index]),
                container = strong,
                contentColor = onPill,
                circleColor = circle,
                onClick = { onRemoveFrom(index) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    tint = onPill,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        items(options, key = { "opt:${it.tag}" }) { option ->
            StylePill(
                label = styleTagLabel(option.tag),
                container = soft,
                contentColor = onPill,
                circleColor = circle,
                onClick = { onSelect(option.tag) },
            ) {
                Text(
                    text = option.count.toString(),
                    color = onPill,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
        if (progress != null) {
            item(key = "progress") {
                Text(
                    text = stringResource(R.string.style_ai_progress, progress.done, progress.total),
                    color = onPill.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StylePill(
    label: String,
    container: Color,
    contentColor: Color,
    circleColor: Color,
    onClick: () -> Unit,
    circleContent: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .height(PillHeight)
                .clip(CircleShape)
                .background(container)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(start = 16.dp, end = 4.dp),
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(PillCircle).clip(CircleShape).background(circleColor),
        ) {
            circleContent()
        }
    }
}
