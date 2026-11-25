/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.rukamori.archivetune.constants.DislikedSongsKey

/**
 * Songs the user marked as "don't like". The first dislike avoids the song for [AVOID_DURATION_MS];
 * disliking the same song again after that has expired makes it avoided until the user undoes it
 * (button again or liking it). Artists behind several disliked songs are avoided as well.
 * Stored in DataStore as one line per song (`id|expiresAtMs|artistId,artistId|strikes`)
 * so no database migration is needed.
 */
class DislikeStore(
    private val dataStore: DataStore<Preferences>,
) {
    data class Entry(
        val songId: String,
        val expiresAt: Long,
        val artistIds: List<String>,
        val strikes: Int = 1,
    )

    /** Snapshot of the active dislikes, used synchronously by playback filters. */
    data class Snapshot(
        val songIds: Set<String> = emptySet(),
        val artistIds: Set<String> = emptySet(),
    ) {
        fun isAvoided(
            songId: String?,
            artistIds: Collection<String>,
        ): Boolean =
            (songId != null && songId in songIds) ||
                artistIds.any { it in this.artistIds }
    }

    val snapshots: Flow<Snapshot> =
        dataStore.data.map { preferences -> snapshotOf(parse(preferences[DislikedSongsKey]), System.currentTimeMillis()) }

    /** Returns how many times this song was disliked in a row (2 or more means avoided until undone). */
    suspend fun add(
        songId: String,
        artistIds: List<String>,
    ): Int {
        if (!songId.isSafeToken()) return 1
        val now = System.currentTimeMillis()
        var result = 1
        dataStore.edit { preferences ->
            val all = parse(preferences[DislikedSongsKey])
            // Expired entries are kept for a year so repeated dislikes of the same song can be counted.
            val entries =
                all
                    .filter { it.songId != songId && (it.expiresAt == FOREVER || it.expiresAt + COUNT_MEMORY_MS > now) }
                    .toMutableList()
            val strikes = (all.firstOrNull { it.songId == songId }?.strikes ?: 0) + 1
            result = strikes
            entries +=
                Entry(
                    songId = songId,
                    expiresAt = if (strikes >= 2) FOREVER else now + AVOID_DURATION_MS,
                    artistIds = artistIds.filter { it.isSafeToken() },
                    strikes = strikes,
                )
            preferences[DislikedSongsKey] = encode(entries.takeLast(MAX_ENTRIES))
        }
        return result
    }

    suspend fun remove(songId: String) {
        dataStore.edit { preferences ->
            val current = parse(preferences[DislikedSongsKey])
            if (current.none { it.songId == songId }) return@edit
            preferences[DislikedSongsKey] = encode(current.filter { it.songId != songId })
        }
    }

    companion object {
        const val AVOID_DURATION_MS = 30L * 24 * 60 * 60 * 1000
        private const val COUNT_MEMORY_MS = 365L * 24 * 60 * 60 * 1000
        private const val FOREVER = Long.MAX_VALUE

        /** Number of active dislikes by the same artist that makes the artist avoided too. */
        const val ARTIST_STRIKES = 3
        private const val MAX_ENTRIES = 2000

        private fun String.isSafeToken() = isNotBlank() && none { it == '|' || it == ',' || it == '\n' }

        internal fun parse(raw: String?): List<Entry> =
            raw
                .orEmpty()
                .lineSequence()
                .mapNotNull { line ->
                    val parts = line.split('|')
                    if (parts.size < 2) return@mapNotNull null
                    val expiresAt = parts[1].toLongOrNull() ?: return@mapNotNull null
                    Entry(
                        songId = parts[0],
                        expiresAt = expiresAt,
                        artistIds = parts.getOrNull(2)?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
                        strikes = parts.getOrNull(3)?.toIntOrNull() ?: 1,
                    )
                }.toList()

        internal fun encode(entries: List<Entry>): String =
            entries.joinToString("\n") { "${it.songId}|${it.expiresAt}|${it.artistIds.joinToString(",")}|${it.strikes}" }

        internal fun snapshotOf(
            entries: List<Entry>,
            now: Long,
        ): Snapshot {
            val active = entries.filter { it.expiresAt > now }
            val avoidedArtists =
                active
                    .flatMap { it.artistIds.distinct() }
                    .groupingBy { it }
                    .eachCount()
                    .filterValues { it >= ARTIST_STRIKES }
                    .keys
            return Snapshot(active.mapTo(HashSet()) { it.songId }, avoidedArtists)
        }
    }
}
