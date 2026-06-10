package btm.m.todaywallpaper.ui.screens

import org.json.JSONObject

/**
 * Data class representing a Liquid Glass preset with all tuneable parameters.
 */
data class LiquidGlassPreset(
    val name: String,
    val blur: Float = 8f,
    val refractionHeight: Float = 20f,
    val refractionOffset: Float = 70f,
    val tintAlpha: Float = 0f,
    val dispersion: Float = 0f,
    val draggable: Boolean = false,
    val elastic: Boolean = false,
    val touchEffect: Boolean = false,
    val isBuiltIn: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("blur", blur.toDouble())
            put("refractionHeight", refractionHeight.toDouble())
            put("refractionOffset", refractionOffset.toDouble())
            put("tintAlpha", tintAlpha.toDouble())
            put("dispersion", dispersion.toDouble())
            put("draggable", draggable)
            put("elastic", elastic)
            put("touchEffect", touchEffect)
        }
    }

    fun toJsonString(): String = toJson().toString(2)

    companion object {
        fun fromJson(json: JSONObject): LiquidGlassPreset {
            return LiquidGlassPreset(
                name = json.optString("name", "Untitled"),
                blur = json.optDouble("blur", 8.0).toFloat(),
                refractionHeight = json.optDouble("refractionHeight", 20.0).toFloat(),
                refractionOffset = json.optDouble("refractionOffset", 70.0).toFloat(),
                tintAlpha = json.optDouble("tintAlpha", 0.0).toFloat(),
                dispersion = json.optDouble("dispersion", 0.0).toFloat(),
                draggable = json.optBoolean("draggable", false),
                elastic = json.optBoolean("elastic", false),
                touchEffect = json.optBoolean("touchEffect", false)
            )
        }

        fun fromJsonString(jsonStr: String): LiquidGlassPreset {
            return fromJson(JSONObject(jsonStr))
        }

        /**
         * Parse a JSON string that may be a single preset object or an array of presets.
         * Returns a list of presets.
         */
        fun parseJsonPresets(jsonStr: String): List<LiquidGlassPreset> {
            val trimmed = jsonStr.trim()
            return try {
                if (trimmed.startsWith("[")) {
                    val arr = org.json.JSONArray(trimmed)
                    (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
                } else {
                    listOf(fromJson(JSONObject(trimmed)))
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}