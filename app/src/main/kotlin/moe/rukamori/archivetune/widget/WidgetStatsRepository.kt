/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.widget

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.db.MusicDatabase
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

internal class WidgetStatsRepository
    @Inject
    constructor(
        private val database: MusicDatabase,
    ) {
        /** Listening time in milliseconds for each of the last [WeekDays] days, oldest first (today is last). */
        suspend fun weeklyListeningMillis(nowMs: Long = System.currentTimeMillis()): List<Long> =
            withContext(Dispatchers.IO) {
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                (WeekDays - 1 downTo 0).map { offset ->
                    val date = today.minusDays(offset.toLong())
                    val fromMs = date.atStartOfDay(zone).toInstant().toEpochMilli() - 1L
                    val toMs = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli().coerceAtMost(nowMs)
                    if (toMs <= fromMs) {
                        0L
                    } else {
                        database.listeningTotals(fromTimestamp = fromMs, toTimestamp = toMs).first().totalTimeListened
                    }
                }
            }
    }

internal const val WeekDays = 7

internal fun List<Long>.encodeWeeklyStats(): String = joinToString(separator = ",")

internal fun Preferences.widgetWeeklyStats(): List<Long> =
    this[MusicWidgetKeys.WEEKLY_STATS]
        .orEmpty()
        .split(",")
        .mapNotNull { it.toLongOrNull() }
        .takeIf { it.size == WeekDays }
        ?: List(WeekDays) { 0L }
