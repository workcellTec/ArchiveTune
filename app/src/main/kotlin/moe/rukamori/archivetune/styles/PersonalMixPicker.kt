/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.styles

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.db.MusicDatabase
import moe.rukamori.archivetune.db.entities.Song
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Chooses the seed song of the home "Mix pra você". It picks a style that exists in the liked songs
 * (weighted by how many songs have it, optionally refined with a second style), then a seed inside that
 * style, favoring what is played often and never repeating the last few seeds.
 */
@Singleton
class PersonalMixPicker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val database: MusicDatabase,
        private val styleTagger: SongStyleTagger,
    ) {
        data class Pick(
            val seed: Song,
            /** Styles the mix follows (e.g. "Pop · Romântica"), or null when no style was used. */
            val styleLabel: String?,
        )

        private val recentSeeds = ArrayDeque<String>()

        suspend fun pick(avoidedSongIds: Set<String>): Pick? =
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
                if (played.isEmpty() && liked.isEmpty()) return@withContext null

                val playedIds = played.mapTo(HashSet()) { it.id }
                var pool = (liked + played).distinctBy { it.id }
                var label: String? = null

                if (liked.size >= MIN_LIKED_FOR_STYLE) {
                    val labels =
                        StyleLabels(
                            live = context.getString(R.string.style_tag_live),
                            acoustic = context.getString(R.string.style_tag_acoustic),
                            remix = context.getString(R.string.style_tag_remix),
                            explicit = context.getString(R.string.style_tag_explicit),
                            decade = { context.getString(R.string.style_decade, it) },
                        )
                    val index = buildStyleIndex(pool, styleTagger.tags.first(), labels)
                    val path = ArrayList<String>(2)
                    var matching = pool
                    repeat(MAX_STYLE_DEPTH) { depth ->
                        if (depth == 1 && Random.nextFloat() > REFINE_CHANCE) return@repeat
                        val options = styleOptions(matching, index, path)
                        val tag = options.weightedRandom { it.count.toDouble() }?.tag ?: return@repeat
                        val narrowed = filterByStylePath(matching, index, path + tag)
                        if (narrowed.size >= MIN_POOL_SIZE) {
                            path += tag
                            matching = narrowed
                        }
                    }
                    if (path.isNotEmpty()) {
                        pool = matching
                        label = path.joinToString(" · ") { styleTagLabel(it) }
                    }
                }

                var candidates = pool.filter { it.id !in recentSeeds }
                if (candidates.isEmpty()) {
                    recentSeeds.clear()
                    candidates = pool
                }
                // Songs played often are likelier seeds, but every candidate keeps a chance.
                val seed = candidates.weightedRandom { if (it.id in playedIds) 3.0 else 1.0 } ?: return@withContext null
                recentSeeds.addLast(seed.id)
                while (recentSeeds.size > RECENT_SEEDS) recentSeeds.removeFirst()
                Pick(seed, label)
            }

        private fun List<Song>.usable(avoided: Set<String>): List<Song> = filter { !it.song.isLocal && it.id !in avoided }

        private fun <T> List<T>.weightedRandom(weight: (T) -> Double): T? {
            if (isEmpty()) return null
            val total = sumOf { weight(it) }
            var roll = Random.nextDouble() * total
            for (item in this) {
                roll -= weight(item)
                if (roll <= 0) return item
            }
            return last()
        }

        private companion object {
            const val PLAYED_WINDOW_DAYS = 60L
            const val PLAYED_LIMIT = 25
            const val MIN_LIKED_FOR_STYLE = 8
            const val MIN_POOL_SIZE = 3
            const val MAX_STYLE_DEPTH = 2
            const val REFINE_CHANCE = 0.6f
            const val RECENT_SEEDS = 10
        }
    }
