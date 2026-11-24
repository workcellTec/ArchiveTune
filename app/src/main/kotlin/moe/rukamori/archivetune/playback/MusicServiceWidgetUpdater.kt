/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.widget.CoverPillWidget
import moe.rukamori.archivetune.widget.CoverPlayWidget
import moe.rukamori.archivetune.widget.CoverWideWidget
import moe.rukamori.archivetune.widget.GlassWidget
import moe.rukamori.archivetune.widget.HardwareMiniWidget
import moe.rukamori.archivetune.widget.HardwareWidget
import moe.rukamori.archivetune.widget.ModernWidget
import moe.rukamori.archivetune.widget.PillWidget
import moe.rukamori.archivetune.widget.QueueWidget
import moe.rukamori.archivetune.widget.SquareCardWidget
import moe.rukamori.archivetune.widget.ControlsWidget
import moe.rukamori.archivetune.widget.StatsWidget
import moe.rukamori.archivetune.widget.VolumeWidget
import moe.rukamori.archivetune.widget.encodeWeeklyStats
import moe.rukamori.archivetune.widget.MusicWidgetKeys
import moe.rukamori.archivetune.extensions.SilentHandler
import moe.rukamori.archivetune.utils.reportException
import moe.rukamori.archivetune.widget.WidgetMediaItem
import moe.rukamori.archivetune.widget.encodeWidgetMediaItems
import java.io.File

internal class MusicServiceWidgetUpdater(
    private val service: MusicService,
    private val player: Player,
    private val scopeProvider: () -> CoroutineScope,
    private val isSleepTimerActive: () -> Boolean,
    private val loadWeeklyStats: suspend () -> List<Long>,
) {
    private val widgetManager = GlanceAppWidgetManager(service)
    private var stateJob: Job? = null
    private var progressJob: Job? = null

    fun update() {
        stateJob?.cancel()
        stateJob =
            scopeProvider().launch(SilentHandler) {
                pushState()
            }
    }

    fun updateProgressTracking() {
        progressJob?.cancel()
        if (player.isPlaying && player.duration > 0) {
            progressJob =
                scopeProvider().launch(SilentHandler) {
                    val installedTargets = findInstalledTargets(progressWidgets)
                    if (installedTargets.isEmpty()) return@launch

                    while (isActive && player.isPlaying) {
                        delay(WIDGET_PROGRESS_UPDATE_INTERVAL_MILLIS)
                        if (player.isPlaying) {
                            updateProgress(installedTargets, player.playbackProgress())
                        }
                    }
                }
        }
    }

    private suspend fun pushState() {
        val installedTargets = findInstalledTargets(playbackWidgets)
        if (installedTargets.isEmpty()) return

        val mediaItem = player.currentMediaItem
        val meta = mediaItem?.mediaMetadata
        val title = meta?.title?.toString() ?: service.getString(R.string.no_track_playing)
        val artist = meta?.artist?.toString().orEmpty()
        val isPlaying = player.isPlaying
        val playbackPosition = player.playbackProgress()
        val queueEntries =
            if (installedTargets.any { it.target.requiresQueue }) collectUpNextEntries() else null

        val artFile = meta?.artworkUri?.let { cacheAlbumArt(it) }
        val dominantColor = artFile?.let { extractDominantColor(it) }
        Log.d(
            TAG,
            "pushState media=${mediaItem?.mediaId} art=${meta?.artworkUri} file=${artFile?.name} targets=${installedTargets.size}",
        )
        val upNext = queueEntries?.map { entry -> entry.toWidgetMediaItem() }
        val weeklyStats = if (installedTargets.any { it.target.requiresStats }) loadWeeklyStatsSafely() else null
        val snapshot =
            WidgetSnapshot(
                mediaId = mediaItem?.mediaId,
                title = title,
                artist = artist,
                isPlaying = isPlaying,
                isAvailable = mediaItem != null,
                playbackPosition = playbackPosition,
                artPath = artFile?.absolutePath,
                dominantColor = dominantColor,
                upNext = upNext,
                sleepActive = isSleepTimerActive(),
                weeklyStats = weeklyStats,
            )

        installedTargets.forEach { target ->
            updateWidget(target, snapshot)
        }
        pruneArtCache(snapshot.referencedArtPaths())
    }

    private fun collectUpNextEntries(): List<QueueEntry> {
        val timeline = player.currentTimeline
        val itemCount = player.mediaItemCount
        if (timeline.isEmpty || itemCount == 0) return emptyList()

        val shuffle = player.shuffleModeEnabled
        val entries = mutableListOf<QueueEntry>()
        var index = player.currentMediaItemIndex
        while (entries.size < UP_NEXT_LIMIT) {
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffle)
            if (index == C.INDEX_UNSET || index >= itemCount) break
            val item = player.getMediaItemAt(index)
            val metadata = item.mediaMetadata
            entries +=
                QueueEntry(
                    index = index,
                    mediaId = item.mediaId,
                    title = metadata.title?.toString().orEmpty(),
                    artist = metadata.artist?.toString().orEmpty(),
                    artworkUri = metadata.artworkUri,
                )
        }
        return entries
    }

    private suspend fun QueueEntry.toWidgetMediaItem(): WidgetMediaItem =
        WidgetMediaItem(
            id = mediaId,
            title = title.ifBlank { service.getString(R.string.no_track_playing) },
            subtitle = artist,
            artPath = artworkUri?.let { cacheAlbumArt(it) }?.absolutePath,
            queueIndex = index,
        )

    private suspend fun loadWeeklyStatsSafely(): String? =
        try {
            loadWeeklyStats().encodeWeeklyStats()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            reportException(error)
            null
        }

    private suspend fun updateProgress(
        installedTargets: List<InstalledWidgetTarget>,
        progress: Float,
    ) {
        installedTargets.forEach { installedTarget ->
            installedTarget.ids.forEach { id ->
                updateAppWidgetState(service, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutableWidgetPreferences().apply {
                        this[MusicWidgetKeys.PLAYBACK_POSITION] = progress
                    }
                }
                installedTarget.target.widget.update(service, id)
            }
        }
    }

    private suspend fun updateWidget(
        installedTarget: InstalledWidgetTarget,
        snapshot: WidgetSnapshot,
    ) {
        installedTarget.ids.forEach { id ->
            updateAppWidgetState(service, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutableWidgetPreferences().apply {
                    writeSnapshot(snapshot)
                }
            }
            installedTarget.target.widget.update(service, id)
        }
    }

    private suspend fun findInstalledTargets(targets: List<WidgetTarget>): List<InstalledWidgetTarget> =
        targets.mapNotNull { target ->
            widgetManager
                .getGlanceIds(target.widgetClass)
                .takeIf { ids -> ids.isNotEmpty() }
                ?.let { ids -> InstalledWidgetTarget(target, ids) }
        }

    private fun Preferences.toMutableWidgetPreferences(): MutablePreferences =
        mutablePreferencesOf().also { mutable ->
            this[MusicWidgetKeys.TRACK_TITLE]?.let { mutable[MusicWidgetKeys.TRACK_TITLE] = it }
            this[MusicWidgetKeys.TRACK_ARTIST]?.let { mutable[MusicWidgetKeys.TRACK_ARTIST] = it }
            this[MusicWidgetKeys.ART_PATH]?.let { mutable[MusicWidgetKeys.ART_PATH] = it }
            this[MusicWidgetKeys.IS_PLAYING]?.let { mutable[MusicWidgetKeys.IS_PLAYING] = it }
            this[MusicWidgetKeys.IS_AVAILABLE]?.let { mutable[MusicWidgetKeys.IS_AVAILABLE] = it }
            this[MusicWidgetKeys.DOMINANT_COLOR]?.let { mutable[MusicWidgetKeys.DOMINANT_COLOR] = it }
            this[MusicWidgetKeys.PLAYBACK_POSITION]?.let { mutable[MusicWidgetKeys.PLAYBACK_POSITION] = it }
            this[MusicWidgetKeys.CURRENT_MEDIA_ID]?.let { mutable[MusicWidgetKeys.CURRENT_MEDIA_ID] = it }
            this[MusicWidgetKeys.UP_NEXT]?.let { mutable[MusicWidgetKeys.UP_NEXT] = it }
            this[MusicWidgetKeys.SLEEP_ACTIVE]?.let { mutable[MusicWidgetKeys.SLEEP_ACTIVE] = it }
            this[MusicWidgetKeys.WEEKLY_STATS]?.let { mutable[MusicWidgetKeys.WEEKLY_STATS] = it }
        }

    private fun MutablePreferences.writeSnapshot(snapshot: WidgetSnapshot) {
        this[MusicWidgetKeys.TRACK_TITLE] = snapshot.title
        this[MusicWidgetKeys.TRACK_ARTIST] = snapshot.artist
        this[MusicWidgetKeys.IS_PLAYING] = snapshot.isPlaying
        this[MusicWidgetKeys.IS_AVAILABLE] = snapshot.isAvailable
        this[MusicWidgetKeys.PLAYBACK_POSITION] = snapshot.playbackPosition

        val mediaId = snapshot.mediaId
        if (!mediaId.isNullOrBlank()) {
            this[MusicWidgetKeys.CURRENT_MEDIA_ID] = mediaId
        } else {
            remove(MusicWidgetKeys.CURRENT_MEDIA_ID)
        }

        val artPath = snapshot.artPath
        if (artPath != null) {
            this[MusicWidgetKeys.ART_PATH] = artPath
        } else {
            remove(MusicWidgetKeys.ART_PATH)
        }

        val dominantColor = snapshot.dominantColor
        if (dominantColor != null) {
            this[MusicWidgetKeys.DOMINANT_COLOR] = dominantColor
        } else {
            remove(MusicWidgetKeys.DOMINANT_COLOR)
        }

        this[MusicWidgetKeys.SLEEP_ACTIVE] = snapshot.sleepActive
        snapshot.weeklyStats?.let { stats -> this[MusicWidgetKeys.WEEKLY_STATS] = stats }
        snapshot.upNext?.let { items -> writeMediaItems(MusicWidgetKeys.UP_NEXT, items) }
    }

    private fun MutablePreferences.writeMediaItems(
        key: Preferences.Key<String>,
        items: List<WidgetMediaItem>,
    ) {
        if (items.isEmpty()) {
            remove(key)
        } else {
            this[key] = items.encodeWidgetMediaItems()
        }
    }

    private suspend fun cacheAlbumArt(uri: Uri): File? =
        withContext(Dispatchers.IO) {
            val dest = File(service.cacheDir, "$ART_FILE_PREFIX${Integer.toHexString(uri.toString().hashCode())}.jpg")
            if (dest.isFile && dest.length() > 0L) return@withContext dest

            if (uri.scheme == "content" || uri.scheme == "file") {
                return@withContext try {
                    service.contentResolver.openInputStream(uri)?.use { src ->
                        dest.outputStream().use { dst -> src.copyTo(dst) }
                    }
                    if (dest.exists() && dest.length() > 0) dest else null
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "cacheAlbumArt failed for $uri", error)
                    null
                }
            }

            if (uri.scheme == "https" || uri.scheme == "http") {
                return@withContext try {
                    val loader = service.applicationContext.imageLoader
                    val request =
                        ImageRequest
                            .Builder(service.applicationContext)
                            .data(uri.toString())
                            .size(512, 512)
                            .allowHardware(false)
                            .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = result.image.toBitmap()
                        dest.outputStream().use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
                        }
                        if (dest.exists() && dest.length() > 0) dest else null
                    } else {
                        null
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "cacheAlbumArt failed for $uri", error)
                    null
                }
            }

            null
        }

    private suspend fun pruneArtCache(referencedPaths: Set<String>) {
        withContext(Dispatchers.IO) {
            val files =
                service.cacheDir.listFiles { file -> file.isFile && file.name.startsWith(ART_FILE_PREFIX) }
                    ?: return@withContext
            if (files.size <= MAX_CACHED_ART_FILES) return@withContext
            files
                .filterNot { file -> file.absolutePath in referencedPaths }
                .sortedBy { file -> file.lastModified() }
                .take(files.size - MAX_CACHED_ART_FILES)
                .forEach { file -> file.delete() }
        }
    }

    private suspend fun extractDominantColor(file: File): Int? =
        withContext(Dispatchers.Default) {
            try {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
                val palette = Palette.from(bitmap).generate()
                palette.getDarkVibrantColor(
                    palette.getDominantColor(android.graphics.Color.DKGRAY),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        }

    private fun Player.playbackProgress(): Float =
        if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    private fun WidgetSnapshot.referencedArtPaths(): Set<String> =
        buildSet {
            artPath?.let(::add)
            upNext?.forEach { item -> item.artPath?.let(::add) }
        }

    private data class QueueEntry(
        val index: Int,
        val mediaId: String,
        val title: String,
        val artist: String,
        val artworkUri: Uri?,
    )

    private data class WidgetSnapshot(
        val mediaId: String?,
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val isAvailable: Boolean,
        val playbackPosition: Float,
        val artPath: String?,
        val dominantColor: Int?,
        val upNext: List<WidgetMediaItem>?,
        val sleepActive: Boolean,
        val weeklyStats: String?,
    )

    private data class WidgetTarget(
        val widgetClass: Class<out GlanceAppWidget>,
        val widget: GlanceAppWidget,
        val requiresQueue: Boolean = false,
        val requiresStats: Boolean = false,
    )

    private data class InstalledWidgetTarget(
        val target: WidgetTarget,
        val ids: List<GlanceId>,
    )

    private companion object {
        const val WIDGET_PROGRESS_UPDATE_INTERVAL_MILLIS = 30_000L
        const val UP_NEXT_LIMIT = 8
        const val MAX_CACHED_ART_FILES = 64
        const val ART_FILE_PREFIX = "widget_art_"
        const val TAG = "WidgetUpdater"

        val playbackWidgets =
            listOf(
                WidgetTarget(CoverPillWidget::class.java, CoverPillWidget()),
                WidgetTarget(CoverPlayWidget::class.java, CoverPlayWidget()),
                WidgetTarget(SquareCardWidget::class.java, SquareCardWidget()),
                WidgetTarget(CoverWideWidget::class.java, CoverWideWidget()),
                WidgetTarget(GlassWidget::class.java, GlassWidget()),
                WidgetTarget(HardwareWidget::class.java, HardwareWidget()),
                WidgetTarget(HardwareMiniWidget::class.java, HardwareMiniWidget()),
                WidgetTarget(PillWidget::class.java, PillWidget()),
                WidgetTarget(ModernWidget::class.java, ModernWidget()),
                WidgetTarget(ControlsWidget::class.java, ControlsWidget()),
                WidgetTarget(VolumeWidget::class.java, VolumeWidget()),
                WidgetTarget(
                    widgetClass = StatsWidget::class.java,
                    widget = StatsWidget(),
                    requiresStats = true,
                ),
                WidgetTarget(
                    widgetClass = QueueWidget::class.java,
                    widget = QueueWidget(),
                    requiresQueue = true,
                ),
            )

        val progressWidgets =
            listOf(
                WidgetTarget(HardwareWidget::class.java, HardwareWidget()),
                WidgetTarget(HardwareMiniWidget::class.java, HardwareMiniWidget()),
                WidgetTarget(PillWidget::class.java, PillWidget()),
                WidgetTarget(ModernWidget::class.java, ModernWidget()),
                WidgetTarget(QueueWidget::class.java, QueueWidget()),
            )
    }
}
