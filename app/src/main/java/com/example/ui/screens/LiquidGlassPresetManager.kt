package btm.m.todaywallpaper.ui.screens

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Manages Liquid Glass presets stored in SharedPreferences.
 * Includes 5 built-in presets that cannot be deleted or exported.
 */
object LiquidGlassPresetManager {

    private const val PREFS_NAME = "liquid_glass_presets"
    private const val KEY_PRESETS = "presets_json"
    private const val KEY_ACTIVE_PRESET = "active_preset_name"

    /** 5 built-in presets provided by default */
    val BUILT_IN_PRESETS = listOf(
        LiquidGlassPreset(
            name = "通透",
            blur = 1.2845947f,
            refractionHeight = 28.991964f,
            refractionOffset = 28.212656f,
            tintAlpha = 0f,
            dispersion = 0.32948583f,
            draggable = false,
            elastic = true,
            touchEffect = true,
            isBuiltIn = true
        ),
        LiquidGlassPreset(
            name = "柔和",
            blur = 3.2318993f,
            refractionHeight = 28.991964f,
            refractionOffset = 27.92235f,
            tintAlpha = 0.15267576f,
            dispersion = 0.32948583f,
            draggable = false,
            elastic = true,
            touchEffect = true,
            isBuiltIn = true
        ),
        LiquidGlassPreset(
            name = "高斯模糊",
            blur = 6.589717f,
            refractionHeight = 12f,
            refractionOffset = 20f,
            tintAlpha = 0.14533053f,
            dispersion = 0f,
            draggable = false,
            elastic = false,
            touchEffect = false,
            isBuiltIn = true
        ),
        LiquidGlassPreset(
            name = "低透明度",
            blur = 31.51733f,
            refractionHeight = 12f,
            refractionOffset = 20f,
            tintAlpha = 0.5975866f,
            dispersion = 0f,
            draggable = false,
            elastic = false,
            touchEffect = false,
            isBuiltIn = true
        ),
        LiquidGlassPreset(
            name = "不透明",
            blur = 31.51733f,
            refractionHeight = 12f,
            refractionOffset = 20f,
            tintAlpha = 1f,
            dispersion = 0f,
            draggable = false,
            elastic = false,
            touchEffect = false,
            isBuiltIn = true
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns all presets: built-in presets first, then user presets.
     */
    fun getAllPresets(context: Context): List<LiquidGlassPreset> {
        val userPresets = getUserPresets(context)
        return BUILT_IN_PRESETS + userPresets
    }

    /**
     * Returns only user-created (non-built-in) presets.
     */
    fun getUserPresets(context: Context): List<LiquidGlassPreset> {
        val jsonStr = getPrefs(context).getString(KEY_PRESETS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(jsonStr)
            (0 until arr.length()).map { LiquidGlassPreset.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePreset(context: Context, preset: LiquidGlassPreset) {
        // Cannot overwrite built-in presets by name
        if (BUILT_IN_PRESETS.any { it.name == preset.name }) return
        val presets = getUserPresets(context).toMutableList()
        val existingIndex = presets.indexOfFirst { it.name == preset.name }
        if (existingIndex >= 0) {
            presets[existingIndex] = preset
        } else {
            presets.add(preset)
        }
        persistUserPresets(context, presets)
    }

    fun deletePreset(context: Context, name: String) {
        // Cannot delete built-in presets
        if (BUILT_IN_PRESETS.any { it.name == name }) return
        val presets = getUserPresets(context).toMutableList()
        presets.removeAll { it.name == name }
        persistUserPresets(context, presets)
        if (getActivePresetName(context) == name) {
            setActivePresetName(context, null)
        }
    }

    fun renamePreset(context: Context, oldName: String, newName: String): Boolean {
        if (oldName == newName) return true
        // Cannot rename built-in presets
        if (BUILT_IN_PRESETS.any { it.name == oldName }) return false
        if (BUILT_IN_PRESETS.any { it.name == newName }) return false
        val presets = getUserPresets(context).toMutableList()
        if (presets.any { it.name == newName }) return false
        val idx = presets.indexOfFirst { it.name == oldName }
        if (idx < 0) return false
        presets[idx] = presets[idx].copy(name = newName)
        persistUserPresets(context, presets)
        if (getActivePresetName(context) == oldName) {
            setActivePresetName(context, newName)
        }
        return true
    }

    fun getActivePresetName(context: Context): String? {
        return getPrefs(context).getString(KEY_ACTIVE_PRESET, null)
    }

    fun setActivePresetName(context: Context, name: String?) {
        getPrefs(context).edit().putString(KEY_ACTIVE_PRESET, name).apply()
    }

    fun getActivePreset(context: Context): LiquidGlassPreset? {
        val name = getActivePresetName(context) ?: return null
        return getAllPresets(context).find { it.name == name }
    }

    private fun persistUserPresets(context: Context, presets: List<LiquidGlassPreset>) {
        val arr = JSONArray()
        presets.forEach { arr.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_PRESETS, arr.toString()).apply()
    }
}