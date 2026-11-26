/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.player.immersive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.media3.common.Player
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.extensions.toggleRepeatMode
import moe.rukamori.archivetune.ui.component.ListDialog
import moe.rukamori.archivetune.ui.player.rememberCurrentDisliked
import moe.rukamori.archivetune.ui.player.heartPop
import moe.rukamori.archivetune.utils.makeTimeString

private val DetailedContent = Color.White
private val DetailedSecondary = Color.White.copy(alpha = 0.72f)

@Composable
fun ImmersiveDetailedScreen(
    state: ImmersivePlayerScreenState,
    accent: Color,
    disableBlur: Boolean,
    backdropBlurAmount: Int,
    contentBottomPadding: Dp,
    onCollapse: () -> Unit,
    onAction: (ImmersivePlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val model =
        when (state) {
            is ImmersivePlayerScreenState.Success -> state.model
            is ImmersivePlayerScreenState.Error -> state.previous
            else -> null
        }
    if (model == null) {
        Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            if (state == ImmersivePlayerScreenState.Loading) CircularProgressIndicator(color = DetailedContent)
        }
        return
    }
    if (model.showArtistDialog) {
        val artists = remember(model.artists) { model.artists.filter { !it.id.isNullOrBlank() }.distinctBy { it.id } }
        ListDialog(onDismiss = { onAction(ImmersivePlayerAction.DismissArtistDialog) }) {
            items(artists, key = { requireNotNull(it.id) }, contentType = { "artist" }) { artist ->
                ListItem(
                    headlineContent = { Text(text = artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().clickable {
                        onAction(ImmersivePlayerAction.OpenArtist(requireNotNull(artist.id)))
                    },
                )
            }
        }
    }
    BoxWithConstraints(modifier = modifier.background(Color.Black)) {
        val landscape = maxWidth > maxHeight
        val stageHeight = maxHeight * 0.64f
        key(model.mediaId, model.canvas) {
            ImmersiveBackdrop(
                mediaId = model.mediaId,
                canvasFrame = model.canvasFrame,
                onAction = onAction,
                artworkUrl = model.artworkUrl,
                canvas = model.canvas,
                isPlaying = model.isPlaying,
                artWidth = if (landscape) maxWidth * 0.48f else maxWidth,
                artHeight = if (landscape) maxHeight else stageHeight,
                backdropOffsetY = if (landscape) 0.dp else stageHeight,
                disableBlur = disableBlur,
                backdropBlurAmount = backdropBlurAmount,
            )
        }
        DetailedHeader(
            sourceTitle = model.sourceTitle,
            onCollapse = onCollapse,
            onMenu = { onAction(ImmersivePlayerAction.OpenMenu) },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
        )
        DetailedBody(
            model = model,
            accent = accent,
            compact = landscape,
            onAction = onAction,
            modifier =
                (if (landscape) Modifier.fillMaxHeight().fillMaxWidth(0.55f).align(Alignment.CenterEnd) else Modifier.fillMaxWidth().align(Alignment.BottomCenter))
                    .padding(start = 24.dp, end = 24.dp, bottom = contentBottomPadding + if (landscape) 4.dp else 12.dp),
        )
    }
}

@Composable
private fun DetailedHeader(
    sourceTitle: String,
    onCollapse: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = sourceTitle.ifBlank { stringResource(R.string.now_playing) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        IconButton(onClick = onCollapse) {
            Icon(painterResource(R.drawable.expand_more), stringResource(R.string.close), tint = DetailedContent, modifier = Modifier.size(30.dp))
        }
        Text(
            text = stringResource(R.string.playing_from, source),
            color = DetailedContent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMenu) {
            Icon(painterResource(R.drawable.more_horiz), stringResource(R.string.more_label), tint = DetailedContent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DetailedBody(
    model: ImmersivePlayerUiModel,
    accent: Color,
    compact: Boolean,
    onAction: (ImmersivePlayerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artistText = remember(model.artists) { model.artists.joinToString(separator = " • ") { it.name } }
    val disliked = rememberCurrentDisliked()
    val connection = LocalPlayerConnection.current
    Column(
        modifier = modifier,
        verticalArrangement = if (compact) Arrangement.SpaceEvenly else Arrangement.Bottom,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = model.title,
                    color = DetailedContent,
                    fontSize = if (compact) 21.sp else 26.sp,
                    lineHeight = if (compact) 25.sp else 31.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee().clickable(enabled = model.albumId != null) { onAction(ImmersivePlayerAction.OpenAlbum) },
                )
                Text(
                    text = artistText,
                    color = DetailedSecondary,
                    fontSize = if (compact) 14.sp else 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee().clickable(enabled = model.artists.any { !it.id.isNullOrBlank() }) { onAction(ImmersivePlayerAction.OpenArtists) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassCircle(size = 32.dp, emphasized = disliked, onClick = { connection?.toggleDislike() }) {
                    Icon(
                        painterResource(R.drawable.v12_heart_broken),
                        stringResource(if (disliked) R.string.action_remove_dislike else R.string.action_dislike),
                        tint = DetailedContent.copy(alpha = if (disliked) 1f else 0.9f),
                        modifier = Modifier.size(17.dp),
                    )
                }
                GlassCircle(size = 32.dp, emphasized = model.isLiked, onClick = { onAction(ImmersivePlayerAction.ToggleLike) }) {
                    Icon(
                        painter = painterResource(if (model.isLiked) R.drawable.v12_favorite else R.drawable.favorite_border),
                        contentDescription = stringResource(if (model.isLiked) R.string.action_remove_like else R.string.action_like),
                        tint = DetailedContent,
                        modifier = Modifier.heartPop(model.isLiked).size(17.dp),
                    )
                }
                GlassCircle(size = 32.dp, onClick = { onAction(ImmersivePlayerAction.OpenMenu) }) {
                    Icon(painterResource(R.drawable.more_horiz), stringResource(R.string.more_label), tint = DetailedContent, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(if (compact) 2.dp else 20.dp))
        val displayPosition = model.seekPositionMs ?: model.positionMs
        AccentSlider(
            value = displayPosition,
            max = model.durationMs.coerceAtLeast(1L),
            accent = accent,
            label = stringResource(R.string.music_player),
            onValueChange = { onAction(ImmersivePlayerAction.PreviewSeek(it)) },
            onValueChangeFinished = { onAction(ImmersivePlayerAction.CommitSeek) },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(makeTimeString(displayPosition), color = DetailedSecondary, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.remaining_time, makeTimeString((model.durationMs - displayPosition).coerceAtLeast(0L))),
                color = DetailedSecondary,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(if (compact) 2.dp else 18.dp))
        DetailedTransport(model = model, compact = compact, onAction = onAction)
        Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
    }
}

/** Frosted-glass circle: soft vertical white fill plus a hairline border that fades from the top-left. */
@Composable
private fun GlassCircle(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    strong: Boolean = false,
    content: @Composable () -> Unit,
) {
    val topAlpha = if (strong) 0.34f else if (emphasized) 0.30f else 0.20f
    val bottomAlpha = if (strong) 0.14f else if (emphasized) 0.14f else 0.08f
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = topAlpha), Color.White.copy(alpha = bottomAlpha))))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.04f))),
                    CircleShape,
                ).clickable(onClick = onClick),
    ) { content() }
}

@Composable
private fun DetailedTransport(
    model: ImmersivePlayerUiModel,
    compact: Boolean,
    onAction: (ImmersivePlayerAction) -> Unit,
) {
    val connection = LocalPlayerConnection.current
    val shuffle by (connection?.shuffleModeEnabled ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) }).collectAsState()
    val repeat by (connection?.repeatMode ?: remember { kotlinx.coroutines.flow.MutableStateFlow(Player.REPEAT_MODE_OFF) }).collectAsState()
    val playSize = if (compact) 52.dp else 60.dp
    val sideSize = if (compact) 40.dp else 46.dp
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ToggleIcon(
            iconRes = R.drawable.v12_shuffle,
            description = stringResource(R.string.shuffle),
            active = shuffle,
            onClick = { connection?.let { it.player.shuffleModeEnabled = !shuffle } },
        )
        GlassCircle(size = sideSize, onClick = { onAction(ImmersivePlayerAction.SkipPrevious) }) {
            Icon(
                painterResource(R.drawable.v12_skip_previous),
                stringResource(R.string.widget_previous),
                tint = DetailedContent.copy(alpha = if (model.canSkipPrevious) 1f else 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(playSize + 20.dp)) {
            Box(
                modifier =
                    Modifier
                        .size(playSize + 20.dp)
                        .background(
                            Brush.radialGradient(
                                0.55f to Color.White.copy(alpha = 0.16f),
                                0.8f to Color.White.copy(alpha = 0.06f),
                                1f to Color.Transparent,
                            ),
                            CircleShape,
                        ),
            )
            GlassCircle(size = playSize, strong = true, onClick = { onAction(ImmersivePlayerAction.TogglePlayPause) }) {
                if (model.isBuffering) {
                    CircularProgressIndicator(color = DetailedContent, strokeWidth = 3.dp, modifier = Modifier.size(playSize * 0.45f))
                } else {
                    AnimatedContent(
                        targetState = model.isPlaying,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.75f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.75f))
                        },
                        label = "detailedPlayPause",
                    ) { playing ->
                        Icon(
                            painter = painterResource(if (playing) R.drawable.v12_pause else R.drawable.v12_play),
                            contentDescription = stringResource(if (playing) R.string.widget_pause else R.string.play),
                            tint = DetailedContent,
                            modifier = Modifier.size(playSize * 0.5f),
                        )
                    }
                }
            }
        }
        GlassCircle(size = sideSize, onClick = { onAction(ImmersivePlayerAction.SkipNext) }) {
            Icon(
                painterResource(R.drawable.v12_skip_next),
                stringResource(R.string.next),
                tint = DetailedContent.copy(alpha = if (model.canSkipNext) 1f else 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
        ToggleIcon(
            iconRes = if (repeat == Player.REPEAT_MODE_ONE) R.drawable.v12_repeat_one else R.drawable.v12_repeat,
            description = stringResource(R.string.repeat_mode_off),
            active = repeat != Player.REPEAT_MODE_OFF,
            onClick = { connection?.player?.toggleRepeatMode() },
        )
    }
}

@Composable
private fun ToggleIcon(
    iconRes: Int,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painterResource(iconRes),
                description,
                tint = DetailedContent.copy(alpha = if (active) 1f else 0.78f),
                modifier = Modifier.size(22.dp),
            )
        }
        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(if (active) DetailedContent else Color.Transparent))
    }
}

@Composable
private fun AccentSlider(
    value: Long,
    max: Long,
    accent: Color,
    label: String,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val fraction = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onValueChangeFinished)
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    setProgress { target ->
                        change((max * target.coerceIn(0f, 1f)).toLong())
                        finish()
                        true
                    }
                }.pointerInput(max) {
                    detectTapGestures { offset ->
                        change((max * (offset.x / size.width).coerceIn(0f, 1f)).toLong())
                        finish()
                    }
                }.pointerInput(max) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { c, _ -> change((max * (c.position.x / size.width).coerceIn(0f, 1f)).toLong()) },
                        onDragEnd = { finish() },
                        onDragCancel = { finish() },
                    )
                },
    ) {
        val cy = size.height / 2f
        val stroke = 3.dp.toPx()
        val thumb = 6.dp.toPx()
        val x = (size.width - 2 * thumb) * fraction + thumb
        drawLine(Color.White.copy(alpha = 0.22f), Offset(0f, cy), Offset(size.width, cy), stroke, StrokeCap.Round)
        drawLine(accent, Offset(0f, cy), Offset(x, cy), stroke, StrokeCap.Round)
        drawCircle(Color.White, thumb, Offset(x, cy))
    }
}
