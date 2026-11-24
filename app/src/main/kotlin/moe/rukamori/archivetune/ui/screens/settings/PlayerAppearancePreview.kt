/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.HidePlayerThumbnailKey
import moe.rukamori.archivetune.constants.MiniPlayerBackgroundStyle
import moe.rukamori.archivetune.constants.MiniPlayerBackgroundStyleKey
import moe.rukamori.archivetune.constants.PlayerBackgroundStyle
import moe.rukamori.archivetune.constants.PlayerBackgroundStyleKey
import moe.rukamori.archivetune.constants.PlayerButtonsStyle
import moe.rukamori.archivetune.constants.PlayerButtonsStyleKey
import moe.rukamori.archivetune.constants.PlayerDesignStyle
import moe.rukamori.archivetune.constants.PlayerDesignStyleKey
import moe.rukamori.archivetune.constants.SliderStyle
import moe.rukamori.archivetune.constants.SliderStyleKey
import moe.rukamori.archivetune.constants.ThumbnailCornerRadiusKey
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.ui.player.StyledPlaybackSlider
import moe.rukamori.archivetune.ui.theme.PlayerColorExtractor
import moe.rukamori.archivetune.utils.rememberEnumPreference
import moe.rukamori.archivetune.utils.rememberPreference

private enum class PreviewTab { PLAYER, MINI }

private val PreviewHeight = 196.dp

/**
 * Live preview of the player appearance options. It only reads the saved preferences and the
 * current song, so it never touches playback. It is a simplified rendering of the real players.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerAppearancePreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val metadataFlow = remember(playerConnection) { playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null) }
    val metadata by metadataFlow.collectAsState()

    val (designStyle) = rememberEnumPreference(PlayerDesignStyleKey, defaultValue = PlayerDesignStyle.V4)
    val (background) = rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val (miniBackground) = rememberEnumPreference(MiniPlayerBackgroundStyleKey, defaultValue = MiniPlayerBackgroundStyle.THEME)
    val (buttonsStyle) = rememberEnumPreference(PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    val (sliderStyle) = rememberEnumPreference(SliderStyleKey, defaultValue = SliderStyle.Standard)
    val (cornerRadius) = rememberPreference(ThumbnailCornerRadiusKey, defaultValue = 8f)
    val (hideThumbnail) = rememberPreference(HidePlayerThumbnailKey, defaultValue = false)

    var tab by rememberSaveable { mutableStateOf(PreviewTab.PLAYER) }
    var expanded by rememberSaveable { mutableStateOf(true) }

    val artworkUrl = metadata?.thumbnailUrl
    val title = metadata?.title ?: "Purple Rain"
    val artist = metadata?.artists?.joinToString { it.name }?.takeIf { it.isNotBlank() } ?: "Prince & The Revolution"

    // Dominant color of the cover, the same way the real player seeds its gradients.
    var seed by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(artworkUrl) {
        seed =
            if (artworkUrl == null) {
                null
            } else {
                runCatching {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(artworkUrl)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                            .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                            .allowHardware(false)
                            .build()
                    val bitmap = withContext(Dispatchers.IO) { context.imageLoader.execute(request).image?.toBitmap() }
                    bitmap?.let {
                        withContext(Dispatchers.Default) {
                            val palette = Palette.from(it).maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT).generate()
                            (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb?.let { rgb -> Color(rgb) }
                        }
                    }
                }.getOrNull()
            }
    }

    val scheme = MaterialTheme.colorScheme
    val base = seed ?: scheme.primary
    val previewState =
        PreviewState(
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            base = base,
            cornerRadius = cornerRadius.dp,
            hideThumbnail = hideThumbnail,
            tonalButtons = buttonsStyle == PlayerButtonsStyle.SECONDARY,
            sliderStyle = sliderStyle,
        )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.media_notification_preview_title),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = tab == PreviewTab.PLAYER,
                onClick = { tab = PreviewTab.PLAYER },
                label = { Text(stringResource(R.string.appearance_preview_tab_player)) },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = tab == PreviewTab.MINI,
                onClick = { tab = PreviewTab.MINI },
                label = { Text(stringResource(R.string.appearance_preview_tab_mini)) },
            )
            Spacer(Modifier.width(4.dp))
            androidx.compose.material3.IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = null,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(6.dp))
                when (tab) {
                    PreviewTab.PLAYER -> PlayerPreviewCard(previewState, designStyle, background)
                    PreviewTab.MINI -> MiniPlayerPreviewCard(previewState, miniBackground)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.appearance_preview_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class PreviewState(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val base: Color,
    val cornerRadius: Dp,
    val hideThumbnail: Boolean,
    val tonalButtons: Boolean,
    val sliderStyle: SliderStyle,
)

@Composable
private fun PreviewCover(
    state: PreviewState,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(state.cornerRadius.coerceAtMost(size / 2))
    Box(
        modifier = modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (state.artworkUrl != null && !state.hideThumbnail) {
            AsyncImage(
                model = state.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size / 3),
            )
        }
    }
}

@Composable
private fun PreviewBackground(
    style: PlayerBackgroundStyle,
    state: PreviewState,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surfaceContainer
    Box(modifier = modifier) {
        when (style) {
            PlayerBackgroundStyle.DEFAULT, PlayerBackgroundStyle.CUSTOM ->
                Box(Modifier.fillMaxSize().background(surface))
            PlayerBackgroundStyle.GRADIENT ->
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(state.base, Color.Black))))
            PlayerBackgroundStyle.COLORING ->
                Box(Modifier.fillMaxSize().background(lerp(state.base, Color.Black, 0.45f)))
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.BLUR_GRADIENT -> {
                if (state.artworkUrl != null) {
                    AsyncImage(
                        model = state.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(28.dp),
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(surface))
                }
                val scrim =
                    if (style == PlayerBackgroundStyle.BLUR_GRADIENT) {
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.75f)))
                    } else {
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.35f)))
                    }
                Box(Modifier.fillMaxSize().background(scrim))
            }
            PlayerBackgroundStyle.GLOW, PlayerBackgroundStyle.GLOW_ANIMATED ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .background(Brush.radialGradient(listOf(state.base.copy(alpha = 0.75f), Color.Transparent))),
                )
        }
    }
}

private fun PlayerBackgroundStyle.isDark(): Boolean = this != PlayerBackgroundStyle.DEFAULT && this != PlayerBackgroundStyle.CUSTOM

@Composable
private fun PlayerPreviewCard(
    state: PreviewState,
    design: PlayerDesignStyle,
    background: PlayerBackgroundStyle,
) {
    val scheme = MaterialTheme.colorScheme
    val immersive = design == PlayerDesignStyle.V7 || design == PlayerDesignStyle.V8
    val onBg = if (background.isDark() || immersive) Color.White else scheme.onSurface
    val onBgVariant = onBg.copy(alpha = 0.72f)
    val accent = if (background.isDark() || immersive) Color.White else scheme.primary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PreviewHeight)
                .clip(RoundedCornerShape(24.dp)),
    ) {
        if (immersive) {
            if (state.artworkUrl != null && !state.hideThumbnail) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PreviewBackground(background, state, Modifier.fillMaxSize())
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))),
            )
        } else {
            PreviewBackground(background, state, Modifier.fillMaxSize())
        }
        when {
            immersive ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    PreviewTexts(state, onBg, onBgVariant, 20.sp)
                    Spacer(Modifier.height(8.dp))
                    PreviewSlider(state, onBg)
                    Spacer(Modifier.height(4.dp))
                    PreviewControls(state, onBg, accent, Modifier.fillMaxWidth(), Arrangement.Center)
                }
            design == PlayerDesignStyle.V5 ->
                Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PreviewCover(state, 64.dp)
                        Spacer(Modifier.width(12.dp))
                        PreviewTexts(state, onBg, onBgVariant, 16.sp, Modifier.weight(1f))
                    }
                    PreviewSlider(state, accent)
                    PreviewControls(state, onBg, accent, Modifier.fillMaxWidth(), Arrangement.Center)
                }
            design == PlayerDesignStyle.V9 || design == PlayerDesignStyle.V10 || design == PlayerDesignStyle.V11 ->
                Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PreviewCover(state, 84.dp)
                        Spacer(Modifier.width(14.dp))
                        PreviewTexts(state, onBg, onBgVariant, 22.sp, Modifier.weight(1f))
                    }
                    PreviewSlider(state, accent)
                    PreviewControls(state, onBg, accent, Modifier.fillMaxWidth(), Arrangement.SpaceBetween, big = true)
                }
            else ->
                Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PreviewCover(state, 84.dp)
                        Spacer(Modifier.width(14.dp))
                        PreviewTexts(state, onBg, onBgVariant, 18.sp, Modifier.weight(1f))
                    }
                    PreviewSlider(state, accent)
                    PreviewControls(state, onBg, accent, Modifier.fillMaxWidth(), Arrangement.Center)
                }
        }
    }
}

@Composable
private fun PreviewTexts(
    state: PreviewState,
    color: Color,
    variant: Color,
    titleSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = state.title,
            color = color,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.artist,
            color = variant,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewSlider(
    state: PreviewState,
    color: Color,
) {
    StyledPlaybackSlider(
        sliderStyle = state.sliderStyle,
        value = 0.35f,
        valueRange = 0f..1f,
        onValueChange = {},
        onValueChangeFinished = {},
        activeColor = color,
        isPlaying = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PreviewControls(
    state: PreviewState,
    onBg: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    arrangement: Arrangement.Horizontal = Arrangement.Center,
    big: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val playContainer = if (state.tonalButtons) scheme.secondaryContainer else accent
    val playContent = if (state.tonalButtons) scheme.onSecondaryContainer else scheme.surface
    val side = if (state.tonalButtons) scheme.secondaryContainer.copy(alpha = 0.8f) else onBg.copy(alpha = 0.14f)
    val sideContent = if (state.tonalButtons) scheme.onSecondaryContainer else onBg
    Row(
        modifier = modifier,
        horizontalArrangement = if (arrangement == Arrangement.SpaceBetween) Arrangement.SpaceBetween else Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewButton(R.drawable.skip_previous, side, sideContent, 40.dp, CircleShape)
        PreviewButton(
            R.drawable.pause,
            playContainer,
            playContent,
            if (big) 64.dp else 48.dp,
            if (big) RoundedCornerShape(20.dp) else CircleShape,
        )
        PreviewButton(R.drawable.skip_next, side, sideContent, 40.dp, CircleShape)
    }
}

@Composable
private fun PreviewButton(
    icon: Int,
    container: Color,
    content: Color,
    size: Dp,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Box(
        modifier = Modifier.size(size).clip(shape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(size / 2))
    }
}

@Composable
private fun MiniPlayerPreviewCard(
    state: PreviewState,
    style: MiniPlayerBackgroundStyle,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)
    val darkBackground = style == MiniPlayerBackgroundStyle.BLUR || style == MiniPlayerBackgroundStyle.GLOW
    val content = if (darkBackground) Color.White else scheme.onSurface
    Box(modifier = Modifier.fillMaxWidth().height(76.dp).clip(shape)) {
        when (style) {
            MiniPlayerBackgroundStyle.THEME -> Box(Modifier.fillMaxSize().background(scheme.surfaceContainerHigh))
            MiniPlayerBackgroundStyle.GRADIENT ->
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(state.base.copy(alpha = 0.8f), scheme.surfaceContainerHigh))))
            MiniPlayerBackgroundStyle.GLOW ->
                Box(Modifier.fillMaxSize().background(Color.Black).background(Brush.radialGradient(listOf(state.base.copy(alpha = 0.7f), Color.Transparent))))
            MiniPlayerBackgroundStyle.ACRYLIC -> Box(Modifier.fillMaxSize().background(scheme.surfaceContainerHigh.copy(alpha = 0.85f)))
            MiniPlayerBackgroundStyle.BLUR -> {
                if (state.artworkUrl != null) {
                    AsyncImage(
                        model = state.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(24.dp),
                    )
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            }
            MiniPlayerBackgroundStyle.GLASS -> Box(Modifier.fillMaxSize().background(scheme.onSurface.copy(alpha = 0.12f)))
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewCover(state, 48.dp)
            Spacer(Modifier.width(12.dp))
            PreviewTexts(state, content, content.copy(alpha = 0.72f), 15.sp, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            PreviewControls(state, content, if (darkBackground) Color.White else scheme.primary, Modifier, Arrangement.Center)
        }
    }
}
