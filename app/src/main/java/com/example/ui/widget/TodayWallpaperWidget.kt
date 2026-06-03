package btm.m.todaywallpaper.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import btm.m.todaywallpaper.ui.screens.getLunarDateHelper
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ========== Shared widget content composable ==========

@Composable
private fun WidgetContent(
    bitmapProvider: ImageProvider?,
    year: String,
    monthDay: String,
    weekday: String,
    lunarDate: String?,
    author: String?,
    source: String?,
    isCompact: Boolean
) {
    val white = Color.White
    val white90 = Color.White.copy(alpha = 0.9f)
    val white85 = Color.White.copy(alpha = 0.85f)
    val white72 = Color.White.copy(alpha = 0.72f)
    val white70 = Color.White.copy(alpha = 0.7f)
    val white60 = Color.White.copy(alpha = 0.6f)

    Box(
        modifier = GlanceModifier.fillMaxSize()
    ) {
        // Background wallpaper image
        if (bitmapProvider != null) {
            Image(
                provider = bitmapProvider,
                contentDescription = "Wallpaper",
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dark gradient overlay
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0x88000000)),
            content = {}
        )

        // Content overlay
        if (isCompact) {
            // 2x1 compact: just date + weekday in a single row
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthDay,
                        style = TextStyle(
                            color = ColorProvider(day = white, night = white),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column {
                        Text(
                            text = weekday,
                            style = TextStyle(
                                color = ColorProvider(day = white85, night = white85),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = year,
                            style = TextStyle(
                                color = ColorProvider(day = white70, night = white70),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        } else {
            // 2x2 and 4x2: full date display
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = year,
                        style = TextStyle(
                            color = ColorProvider(day = white90, night = white90),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = monthDay,
                        style = TextStyle(
                            color = ColorProvider(day = white, night = white),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = weekday,
                        style = TextStyle(
                            color = ColorProvider(day = white90, night = white90),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (lunarDate != null) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = lunarDate,
                            style = TextStyle(
                                color = ColorProvider(day = white72, night = white72),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (author != null) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = author,
                            style = TextStyle(
                                color = ColorProvider(day = white60, night = white60),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ========== Abstract base widget ==========

abstract class TodayWallpaperBaseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        val wallpaperUrl = prefs.getString("widget_wallpaper_url", null)
        val language = prefs.getString("language", "zh") ?: "zh"
        val widgetAuthor = prefs.getString("widget_wallpaper_author", null)
        val widgetSource = prefs.getString("widget_wallpaper_source", null)

        val bitmapProvider = loadBitmapProvider(context, wallpaperUrl)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val monthDay = "${calendar.get(Calendar.MONTH) + 1}/${String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))}"
        val weekdayFmt = if (language == "zh") SimpleDateFormat("EEEE", Locale.CHINA) else SimpleDateFormat("EEEE", Locale.ENGLISH)
        val weekday = weekdayFmt.format(calendar.time)
        val lunarDate = if (language == "zh") {
            "[农历] ${getLunarDateHelper(calendar)}"
        } else null

        provideContent {
            WidgetContent(
                bitmapProvider = bitmapProvider,
                year = year,
                monthDay = monthDay,
                weekday = weekday,
                lunarDate = lunarDate,
                author = widgetAuthor,
                source = widgetSource,
                isCompact = isCompact()
            )
        }
    }

    abstract fun isCompact(): Boolean

    private suspend fun loadBitmapProvider(context: Context, url: String?): ImageProvider? {
        if (url.isNullOrBlank()) return null
        return try {
            val cachedFile = File(context.cacheDir, "widget_wallpaper.jpg")
            if (cachedFile.exists() && System.currentTimeMillis() - cachedFile.lastModified() < 3_600_000) {
                // Use cached image if less than 1 hour old
                val cachedBitmap = android.graphics.BitmapFactory.decodeFile(cachedFile.absolutePath)
                if (cachedBitmap != null) return ImageProvider(cachedBitmap)
            }
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = withContext(Dispatchers.IO) { loader.execute(request) }
            if (result is SuccessResult) {
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    withContext(Dispatchers.IO) {
                        FileOutputStream(cachedFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                    }
                    ImageProvider(bitmap)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("TodayWidget", "Failed to load widget wallpaper: ${e.message}")
            null
        }
    }
}

// ========== Three widget variants ==========

class TodayWallpaperWidget2x2 : TodayWallpaperBaseWidget() {
    override fun isCompact() = false
}

class TodayWallpaperWidget2x1 : TodayWallpaperBaseWidget() {
    override fun isCompact() = true
}

class TodayWallpaperWidget4x2 : TodayWallpaperBaseWidget() {
    override fun isCompact() = false
}

// ========== Receivers (one per widget variant) ==========

class TodayWallpaperWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWallpaperWidget2x2()
}

class TodayWallpaperWidget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWallpaperWidget2x1()
}

class TodayWallpaperWidget4x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWallpaperWidget4x2()
}

// ========== Helper to update all widgets ==========

object TodayWallpaperWidgetUpdater {
    suspend fun updateAll(context: Context) {
        TodayWallpaperWidget2x2().updateAll(context)
        TodayWallpaperWidget2x1().updateAll(context)
        TodayWallpaperWidget4x2().updateAll(context)
    }
}
