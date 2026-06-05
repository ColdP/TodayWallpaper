package btm.m.todaywallpaper.ui.screens

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AutoSwitchReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AutoSwitchReceiver"
        private const val REQUEST_CODE = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        val enabled = sp.getBoolean("auto_switch_enabled", false)
        if (!enabled) return

        val sourceType = sp.getString("auto_switch_source_type", "current") ?: "current"
        val mode = sp.getString("auto_switch_mode", "interval") ?: "interval"
        val pexelsApiKey = sp.getString("pexels_api_key", "") ?: ""
        val homeType = sp.getString("home_wallpaper_type", "PexelsCurated") ?: "PexelsCurated"

        // Determine the effective source type
        val effectiveType = if (sourceType == "current") homeType else sourceType

        // Use goAsync() for long-running operations in BroadcastReceiver
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageUrl = fetchRandomWallpaperUrl(context, effectiveType, pexelsApiKey)
                if (imageUrl != null) {
                    setWallpaperFromUrl(context, imageUrl)
                    Log.d(TAG, "Auto-switch wallpaper set successfully from source: $effectiveType")
                } else {
                    Log.e(TAG, "Failed to fetch wallpaper URL for source: $effectiveType")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during auto-switch wallpaper: ${e.message}", e)
            } finally {
                // Reschedule next alarm if still enabled
                if (enabled) {
                    rescheduleAlarm(context, sp)
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun fetchRandomWallpaperUrl(context: Context, type: String, pexelsApiKey: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (type.startsWith("collection_")) {
                    fetchFromLocalCollection(context, type)
                } else if (type.startsWith("Nekosia")) {
                    fetchFromNekosia(type)
                } else {
                    fetchFromPexels(type, pexelsApiKey)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching wallpaper: ${e.message}", e)
                null
            }
        }
    }

    private fun fetchFromLocalCollection(context: Context, type: String): String? {
        val collectionId = type.removePrefix("collection_").toIntOrNull() ?: return null
        try {
            val db = btm.m.todaywallpaper.data.local.WallpaperDatabase.getDatabase(context)
            val dao = db.wallpaperDao
            val items = runBlockingResult {
                dao.getItemsForCollectionSync(collectionId)
            }
            if (items.isEmpty()) return null
            return items.random().imageUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from local collection: ${e.message}", e)
            return null
        }
    }

    private fun fetchFromNekosia(type: String): String? {
        val categoryName = type.removePrefix("Nekosia:")
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("https://api.nekosia.cat/api/v1/images/$categoryName")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val success = json.optBoolean("success", false)
        if (!success) return null
        val image = json.optJSONObject("image") ?: return null
        val original = image.optJSONObject("original") ?: return null
        return original.optString("url")
    }

    private fun fetchFromPexels(type: String, apiKey: String): String? {
        if (apiKey.isEmpty()) return null

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        if (type == "PexelsCurated") {
            val page = (1..50).random()
            val request = Request.Builder()
                .url("https://api.pexels.com/v1/curated?page=$page&per_page=20")
                .addHeader("Authorization", apiKey)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val photos = json.optJSONArray("photos") ?: return null
            if (photos.length() == 0) return null
            val randomIndex = (0 until photos.length()).random()
            val photo = photos.getJSONObject(randomIndex)
            val src = photo.optJSONObject("src")
            return src?.optString("original") ?: src?.optString("large2x")
        } else {
            val queryMap = mapOf(
                "PexelsSpace" to "cosmic space",
                "PexelsMinimalist" to "minimalist wallpaper",
                "PexelsNature" to "scenery wallpaper"
            )
            val query = queryMap[type] ?: type
            val page = (1..10).random()
            val request = Request.Builder()
                .url("https://api.pexels.com/v1/search?query=$query&page=$page&per_page=20")
                .addHeader("Authorization", apiKey)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val photos = json.optJSONArray("photos") ?: return null
            if (photos.length() == 0) return null
            val randomIndex = (0 until photos.length()).random()
            val photo = photos.getJSONObject(randomIndex)
            val src = photo.optJSONObject("src")
            return src?.optString("original") ?: src?.optString("large2x")
        }
    }

    private suspend fun setWallpaperFromUrl(context: Context, imageUrl: String) {
        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            val drawable = result.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setBitmap(bitmap)
            }
        }
    }

    private fun rescheduleAlarm(context: Context, sp: android.content.SharedPreferences) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AutoSwitchReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mode = sp.getString("auto_switch_mode", "interval") ?: "interval"
        val triggerAtMillis = if (mode == "interval") {
            val hours = sp.getInt("auto_switch_interval_hours", 1)
            val minutes = sp.getInt("auto_switch_interval_minutes", 0)
            val intervalMillis = (hours * 3600L + minutes * 60L) * 1000L
            System.currentTimeMillis() + intervalMillis
        } else {
            val dailyHour = sp.getInt("auto_switch_daily_hour", 8)
            val dailyMinute = sp.getInt("auto_switch_daily_minute", 0)
            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, dailyHour)
                set(java.util.Calendar.MINUTE, dailyMinute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            calendar.timeInMillis
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
    }

    private fun <T> runBlockingResult(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            block()
        }
    }
}