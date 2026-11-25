/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.styles

import moe.rukamori.archivetune.db.entities.Song

/** Kinds of style tags. A tag is stored as `<kind>=<label>`, e.g. `g=Pop`. */
object StyleKind {
    const val GENRE = 'g'
    const val MOOD = 'm'
    const val ERA = 'e'
    const val LANGUAGE = 'l'
    const val ARTIST = 'a'
    const val VERSION = 'v'
    const val EXPLICIT = 'x'
}

/** Localized labels for the tags that are computed without AI. */
class StyleLabels(
    val live: String,
    val acoustic: String,
    val remix: String,
    val explicit: String,
    val decade: (Int) -> String,
)

data class StyleOption(
    val tag: String,
    val count: Int,
)

fun styleTagKey(
    kind: Char,
    label: String,
): String? {
    val clean =
        label
            .replace(Regex("[|;=\\r\\n]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_LABEL_LENGTH)
    if (clean.isEmpty()) return null
    return "$kind=${clean.replaceFirstChar { it.uppercase() }}"
}

fun styleTagLabel(tag: String): String = tag.substringAfter('=')

fun styleTagKind(tag: String): Char = tag.firstOrNull() ?: ' '

private const val MAX_LABEL_LENGTH = 24
private const val MAX_OPTIONS = 14
private const val MIN_COUNT = 2
private const val MIN_COUNT_ARTIST = 3
private val LiveRegex = Regex("""\b(live|ao vivo)\b""")

/** Tags that need no AI: artist, decade, version (live, acoustic, remix) and explicit. */
fun heuristicTags(
    song: Song,
    labels: StyleLabels,
): List<String> {
    val tags = ArrayList<String>(6)
    song.artists.forEach { artist -> styleTagKey(StyleKind.ARTIST, artist.name)?.let(tags::add) }
    song.song.year
        ?.takeIf { it in 1950..2099 }
        ?.let { styleTagKey(StyleKind.ERA, labels.decade(it / 10 * 10)) }
        ?.let(tags::add)
    val text = "${song.song.title} ${song.song.albumName.orEmpty()}".lowercase()
    if (LiveRegex.containsMatchIn(text)) styleTagKey(StyleKind.VERSION, labels.live)?.let(tags::add)
    if ("acústic" in text || "acustic" in text || "acoustic" in text) {
        styleTagKey(StyleKind.VERSION, labels.acoustic)?.let(tags::add)
    }
    if ("remix" in text) styleTagKey(StyleKind.VERSION, labels.remix)?.let(tags::add)
    if (song.song.explicit) styleTagKey(StyleKind.EXPLICIT, labels.explicit)?.let(tags::add)
    return tags
}

/** Song id to all of its tags (AI tags plus the ones computed locally). */
fun buildStyleIndex(
    songs: List<Song>,
    aiTags: Map<String, List<String>>,
    labels: StyleLabels,
): Map<String, Set<String>> =
    songs.associate { song ->
        val tags = LinkedHashSet<String>()
        aiTags[song.id]?.let(tags::addAll)
        tags.addAll(heuristicTags(song, labels))
        song.id to tags
    }

fun filterByStylePath(
    songs: List<Song>,
    index: Map<String, Set<String>>,
    path: List<String>,
): List<Song> =
    if (path.isEmpty()) {
        songs
    } else {
        songs.filter { index[it.id]?.containsAll(path) == true }
    }

/**
 * Next options to show after [path]. Only tags that exist among the matching songs appear, and tags that
 * every matching song has are skipped because they would not narrow anything. At the root, AI genres come
 * first when there are any.
 */
fun styleOptions(
    matching: List<Song>,
    index: Map<String, Set<String>>,
    path: List<String>,
): List<StyleOption> {
    val counts = HashMap<String, Int>()
    matching.forEach { song ->
        index[song.id]?.forEach { tag -> if (tag !in path) counts[tag] = (counts[tag] ?: 0) + 1 }
    }
    var candidates =
        counts.entries.filter { (tag, count) ->
            val min = if (styleTagKind(tag) == StyleKind.ARTIST) MIN_COUNT_ARTIST else MIN_COUNT
            count >= min && count < matching.size
        }
    if (path.isEmpty()) {
        val genres = candidates.filter { styleTagKind(it.key) == StyleKind.GENRE }
        if (genres.isNotEmpty()) candidates = genres
    }
    return candidates
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { styleTagLabel(it.key) })
        .take(MAX_OPTIONS)
        .map { StyleOption(it.key, it.value) }
}
