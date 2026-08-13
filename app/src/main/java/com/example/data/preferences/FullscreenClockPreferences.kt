package btm.m.todaywallpaper.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fullscreenClockDataStore by preferencesDataStore("fullscreen_clock")

data class FullscreenClockConfig(
    val selectedStyle: Int = 0,
    val style1Color: Long = -1L,
    val style3Color: Long = 0xFF61D3AA.toInt().toLong(),
    val wallpaperMode: String = "current",
    val wallpaperSource: String = "favorites",
    val wallpaperUri: String = "",
    val wallpaperCandidates: List<String> = emptyList(),
    val deviceNameOverride: String = ""
)

class FullscreenClockPreferences(private val context: Context) {
    private object Keys {
        val style = intPreferencesKey("selected_style")
        val style1Color = stringPreferencesKey("style_1_color")
        val style3Color = stringPreferencesKey("style_3_color")
        val wallpaperMode = stringPreferencesKey("wallpaper_mode")
        val wallpaperSource = stringPreferencesKey("wallpaper_source")
        val wallpaperUri = stringPreferencesKey("wallpaper_uri")
        val wallpaperCandidates = stringSetPreferencesKey("wallpaper_candidates")
        val deviceName = stringPreferencesKey("device_name_override")
    }

    val config: Flow<FullscreenClockConfig> = context.fullscreenClockDataStore.data.map { prefs ->
        FullscreenClockConfig(
            selectedStyle = (prefs[Keys.style] ?: 0).coerceIn(0, 3),
            style1Color = prefs[Keys.style1Color]?.toLongOrNull() ?: -1L,
            style3Color = prefs[Keys.style3Color]?.toLongOrNull() ?: 0xFF61D3AA.toInt().toLong(),
            wallpaperMode = prefs[Keys.wallpaperMode] ?: "current",
            wallpaperSource = prefs[Keys.wallpaperSource] ?: "favorites",
            wallpaperUri = prefs[Keys.wallpaperUri].orEmpty(),
            wallpaperCandidates = prefs[Keys.wallpaperCandidates].orEmpty().toList(),
            deviceNameOverride = prefs[Keys.deviceName].orEmpty()
        )
    }

    suspend fun setStyle(value: Int) = context.fullscreenClockDataStore.edit { it[Keys.style] = value }
    suspend fun setStyle1Color(value: Long) = context.fullscreenClockDataStore.edit { it[Keys.style1Color] = value.toString() }
    suspend fun setStyle3Color(value: Long) = context.fullscreenClockDataStore.edit { it[Keys.style3Color] = value.toString() }
    suspend fun setWallpaper(
        mode: String,
        source: String = "favorites",
        uri: String = "",
        candidates: List<String> = emptyList()
    ) = context.fullscreenClockDataStore.edit {
        it[Keys.wallpaperMode] = mode
        it[Keys.wallpaperSource] = source
        it[Keys.wallpaperUri] = uri
        it[Keys.wallpaperCandidates] = candidates.filter(String::isNotBlank).toSet()
    }
    suspend fun setWallpaperUri(value: String) = setWallpaper(
        mode = if (value.isBlank()) "current" else "upload",
        uri = value
    )
    suspend fun setDeviceName(value: String) = context.fullscreenClockDataStore.edit { it[Keys.deviceName] = value }
}