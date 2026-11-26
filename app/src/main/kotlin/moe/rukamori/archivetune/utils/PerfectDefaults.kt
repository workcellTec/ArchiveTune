/*
 * Perfect Music — padrões visuais e de reprodução do Brendon.
 * Aplicados uma única vez, na primeira instalação, antes do onboarding.
 */

package moe.rukamori.archivetune.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import moe.rukamori.archivetune.constants.LaunchCountKey
import moe.rukamori.archivetune.constants.OnboardingCompletedKey

object PerfectDefaults {
    private val AppliedKey = booleanPreferencesKey("perfect_defaults_applied")

    /** Só estilo e reprodução: nada de contas, chaves, históricos ou dados pessoais. */
    private val defaults: List<Pair<Preferences.Key<*>, Any>> =
        listOf(
        booleanPreferencesKey("playVideoAsOfficialAudio") to true,
        booleanPreferencesKey("networkMetered") to false,
        booleanPreferencesKey("preloadNextSong") to true,
        booleanPreferencesKey("crossfadeEnabled") to true,
        booleanPreferencesKey("audioOffload") to false,
        booleanPreferencesKey("skipSilence") to true,
        booleanPreferencesKey("audioNormalization") to false,
        booleanPreferencesKey("usbBitPerfect") to false,
        booleanPreferencesKey("permanentShuffle") to true,
        booleanPreferencesKey("autoDownloadOnLike") to true,
        booleanPreferencesKey("autoSkipNextOnError") to true,
        booleanPreferencesKey("wakelock") to true,
        booleanPreferencesKey("archiveTuneCanvas") to true,
        booleanPreferencesKey("canvasWifiOnly") to true,
        booleanPreferencesKey("show_codec_on_player") to true,
        booleanPreferencesKey("pureBlack") to true,
        booleanPreferencesKey("disableBlur") to false,
        booleanPreferencesKey("forceHighRefreshRate") to true,
        booleanPreferencesKey("aodModeEnabled") to true,
        booleanPreferencesKey("aodShowAlbum") to true,
        booleanPreferencesKey("SwipeToSong") to true,
        booleanPreferencesKey("cropThumbnailToSquare") to false,
        booleanPreferencesKey("showHomeCategoryChips") to true,
        booleanPreferencesKey("smartTrimmer") to true,
        booleanPreferencesKey("enableUpdateNotification") to true,
        booleanPreferencesKey("import_local_first") to false,
        floatPreferencesKey("crossfadeDuration") to 8.0f,
        floatPreferencesKey("blurRadius") to 60.0f,
        floatPreferencesKey("swipeSensitivity") to 0.8474988f,
        stringPreferencesKey("audioQuality") to "HIGHEST",
        stringPreferencesKey("playerDesignStyle") to "V7",
        stringPreferencesKey("playerBackgroundStyle") to "DEFAULT",
        stringPreferencesKey("miniPlayerBackgroundStyle") to "GRADIENT",
        stringPreferencesKey("navigationBarBackgroundStyle") to "THEME",
        stringPreferencesKey("player_buttons_style") to "DEFAULT",
        stringPreferencesKey("sliderStyle") to "Thick",
        stringPreferencesKey("lyricsBackgroundStyle") to "FOLLOW_THEME",
        stringPreferencesKey("canvasSource") to "ALL",
        stringPreferencesKey("mediaNotificationActions") to "DISLIKE,LIKE",
        stringPreferencesKey("darkMode") to "ON",
        booleanPreferencesKey("autoPlaylistSongSortDescending") to true,
        stringPreferencesKey("navigationBarMode") to "GRADIENT",
        stringPreferencesKey("quickPicksDisplayMode") to "CARD",
        stringPreferencesKey("albumFilter") to "LIBRARY",
        stringPreferencesKey("searchSource") to "ONLINE",
        booleanPreferencesKey("showPlayerVolumeBar") to true,
        stringPreferencesKey("listStyle") to "GRADIENT",
        booleanPreferencesKey("hidePlayerThumbnail") to false,
        intPreferencesKey("listStyleBlur") to 121,
        booleanPreferencesKey("dynamicTheme") to true,
        )

    /**
     * Instalações novas recebem os padrões; quem já usa o app só é marcado, sem ter nada
     * sobrescrito. Depois disso tudo continua editável nas configurações.
     */
    suspend fun applyIfNeeded(context: Context) {
        val current = context.dataStore.data.first()
        if (current[AppliedKey] == true) return
        val freshInstall =
            current[OnboardingCompletedKey] != true && (current[LaunchCountKey] ?: 0) <= 0
        context.dataStore.edit { preferences ->
            if (freshInstall) {
                defaults.forEach { (key, value) -> preferences.putUnchecked(key, value) }
            }
            preferences[AppliedKey] = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun androidx.datastore.preferences.core.MutablePreferences.putUnchecked(
        key: Preferences.Key<*>,
        value: Any,
    ) {
        this[key as Preferences.Key<Any>] = value
    }
}
