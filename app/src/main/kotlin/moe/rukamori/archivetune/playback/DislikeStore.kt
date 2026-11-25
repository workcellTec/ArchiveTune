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
 * Songs the user marked as "don't like". They are avoided for [AVOID_DURATION_MS]
 * and the artists behind several of them are avoided as well. Stored in DataStore as one line
 * per song (`id|expiresAtMs|artistId,artistId`) so no database migration is needed.
 */
class DislikeStore(
    private val dataStore: DataStore<Preferences>,
) {
    data class Entry(
        val songId: String,
        val expiresAt: Long,
        val artistIds: List<String>,
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

    suspend fun add(
        songId: String,
        artistIds: List<String>,
    ) {
        if (!songId.isSafeToken()) return
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            val entries =
                parse(preferences[DislikedSongsKey])
                    .filter { it.expiresAt > now && it.songId != songId }
                    .toMutableList()
            entries += Entry(songId, now + AVOID_DURATION_MS, artistIds.filter { it.isSafeToken() })
            preferences[DislikedSongsKey] = encode(entries.takeLast(MAX_ENTRIES))
        }
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
                    )
                }.toList()

        internal fun encode(entries: List<Entry>): String =
            entries.joinToString("\n") { "${it.songId}|${it.expiresAt}|${it.artistIds.joinToString(",")}" }

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
