/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.styles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.Song
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Builds the queue of the home "Mix pra você": a shuffled blend of songs the user likes (liked + played
 * often) across every style, skipping disliked songs and avoiding what the last mixes already used.
 */
@Singleton
class PersonalMixPicker
    @Inject
    constructor(
        private val database: MusicDatabase,
    ) {
        private val recentIds = ArrayDeque<String>()

        suspend fun pick(avoidedSongIds: Set<String>): List<Song>? =
            withContext(Dispatchers.IO) {
                val since =
                    LocalDateTime
                        .now()
                        .minusDays(PLAYED_WINDOW_DAYS)
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli()
                val played =
                    database
                        .mostPlayedSongs(fromTimeStamp = since, limit = PLAYED_LIMIT)
                        .first()
                        .usable(avoidedSongIds)
                val liked = database.likedSongsByCreateDateAscNoVideo().first().usable(avoidedSongIds)
                val pool = (liked + played).distinctBy { it.id }
                if (pool.isEmpty()) return@withContext null

                val fresh = pool.filter { it.id !in recentIds }.shuffled()
                val picked =
                    if (fresh.size >= QUEUE_SIZE) {
                        fresh.take(QUEUE_SIZE)
                    } else {
                        (fresh + pool.filter { it.id in recentIds }.shuffled()).take(QUEUE_SIZE)
                    }
                picked.forEach { recentIds.addLast(it.id) }
                while (recentIds.size > RECENT_LIMIT) recentIds.removeFirst()
                picked
            }

        private fun List<Song>.usable(avoided: Set<String>): List<Song> = filter { !it.song.isLocal && it.id !in avoided }

        private companion object {
            const val PLAYED_WINDOW_DAYS = 60L
            const val PLAYED_LIMIT = 25
            const val QUEUE_SIZE = 50
            const val RECENT_LIMIT = 150
        }
    }
