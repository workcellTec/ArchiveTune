/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import moe.rukamori.archivetune.lyrics.translation.StandardLyricsTranslationRepository
import java.util.Locale

/** Translates remote text (e.g. developer news, written in English) into the app language. */
private object RemoteTextTranslator {
    private val repository = StandardLyricsTranslationRepository()
    private val cache =
        object : LinkedHashMap<String, String>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) = size > MAX_CACHED_TEXTS
        }

    fun cached(
        text: String,
        language: String,
    ): String? = synchronized(cache) { cache["$language|$text"] }

    suspend fun translate(
        text: String,
        language: String,
    ): String? {
        cached(text, language)?.let { return it }
        val translated =
            try {
                repository.translate(text, language)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                return null
            }
        if (translated.isBlank()) return null
        synchronized(cache) { cache["$language|$text"] = translated }
        return translated
    }

    private const val MAX_CACHED_TEXTS = 200
}

/**
 * Returns [text] translated into the device language, or [text] itself while it is loading, when the
 * device language is English, or when translating fails. The result is cached per text and language.
 */
@Composable
fun rememberTranslatedText(text: String): String {
    val language = remember { Locale.getDefault().language }
    if (language == "en" || text.isBlank()) return text
    var translated by remember(text, language) { mutableStateOf(RemoteTextTranslator.cached(text, language)) }
    LaunchedEffect(text, language) {
        if (translated == null) translated = RemoteTextTranslator.translate(text, language)
    }
    return translated ?: text
}
