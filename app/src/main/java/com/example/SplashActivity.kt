package btm.m.todaywallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.request.ImageRequest
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Get the app's launcher icon as a Bitmap, supporting adaptive icons.
 */
private fun getAppIconBitmap(context: Context, size: Int = 128): Bitmap? {
    return try {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        drawableToBitmap(drawable, size)
    } catch (e: Exception) {
        Log.e("SplashActivity", "Failed to get app icon: ${e.message}", e)
        null
    }
}

private fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sp = getSharedPreferences("app_gallery_prefs", MODE_PRIVATE)
        val splashMode = sp.getString("splash_mode", "app_icon") ?: "app_icon"

        if (splashMode == "app_icon") {
            // Default mode: native splash with ic_launcher_foreground center
            setContentView(R.layout.splash_placeholder)
            window.decorView.setBackgroundResource(R.drawable.splash_background)
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMain()
            }, 1200)
        } else {
            // Custom mode: show custom image via Compose
            window.setBackgroundDrawableResource(android.R.color.black)
            enableEdgeToEdge()
            setContent {
                MyApplicationTheme {
                    CustomSplashScreen(mode = splashMode, onTimeout = { navigateToMain() })
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

@Composable
fun CustomSplashScreen(mode: String, onTimeout: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loadedBitmap = loadSplashBitmap(context, mode)
        bitmap = loadedBitmap
        isLoading = false

        val startTime = System.currentTimeMillis()
        val minDisplayTime = 1200L
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < minDisplayTime) {
            kotlinx.coroutines.delay(minDisplayTime - elapsed)
        }
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (bitmap != null) {
            // Full-screen custom splash image
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Splash Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Bottom-left branding: full app icon (ic_launcher) + "Today Wallpaper"
            // No rounded rect background, just icon and text directly on the image
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val appIconBitmap = remember { getAppIconBitmap(context, 128) }
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap.asImageBitmap(),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Today Wallpaper",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!isLoading) {
            // Fallback: image failed to load
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val appIconBitmap = remember { getAppIconBitmap(context, 192) }
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap.asImageBitmap(),
                            contentDescription = "App Icon",
                            modifier = Modifier.size(72.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Today Wallpaper",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Loading: show centered app icon
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val appIconBitmap = remember { getAppIconBitmap(context, 192) }
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap.asImageBitmap(),
                        contentDescription = "Loading",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

private suspend fun loadSplashBitmap(context: Context, mode: String): android.graphics.Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
            val source = sp.getString("splash_source", "favorites") ?: "favorites"

            val imageUrl = when (mode) {
                "select" -> sp.getString("splash_selected_url", "") ?: ""
                "upload" -> sp.getString("splash_upload_path", "") ?: ""
                "random" -> fetchRandomImageUrl(context, source)
                else -> ""
            }

            if (imageUrl.isNullOrEmpty()) return@withContext null

            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            Log.e("SplashActivity", "Failed to load splash image: ${e.message}", e)
            null
        }
    }
}

private suspend fun fetchRandomImageUrl(context: Context, source: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            when {
                source == "favorites" -> {
                    val db = btm.m.todaywallpaper.data.local.WallpaperDatabase.getDatabase(context)
                    val dao = db.wallpaperDao
                    val favorites = dao.getAllFavoritesSync()
                    favorites.randomOrNull()?.imageUrl
                }
                source.startsWith("collection_") -> {
                    val collId = source.removePrefix("collection_").toIntOrNull() ?: return@withContext null
                    val db = btm.m.todaywallpaper.data.local.WallpaperDatabase.getDatabase(context)
                    val dao = db.wallpaperDao
                    val items = dao.getItemsForCollectionSync(collId)
                    items.randomOrNull()?.imageUrl
                }
                source.startsWith("category_") -> {
                    val categoryKey = source.removePrefix("category_")
                    val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
                    val pexelsApiKey = sp.getString("pexels_api_key", "") ?: ""
                    if (categoryKey.startsWith("nekosia_")) {
                        fetchFromNekosia(categoryKey.removePrefix("nekosia_"))
                    } else {
                        val queryMap = mapOf(
                            "nature" to "scenery landscape wallpaper",
                            "space" to "cosmic galaxy astronomy",
                            "urban" to "tokyo new york cyberpunk design",
                            "minimalist" to "clean minimalist backgrounds"
                        )
                        val query = queryMap[categoryKey] ?: categoryKey
                        fetchFromPexels(query, pexelsApiKey)
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Failed to fetch random splash: ${e.message}", e)
            null
        }
    }
}

private fun fetchFromNekosia(categoryName: String): String? {
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
    if (!json.optBoolean("success", false)) return null
    val image = json.optJSONObject("image") ?: return null
    val original = image.optJSONObject("original") ?: return null
    return original.optString("url")
}

private fun fetchFromPexels(query: String, apiKey: String): String? {
    if (apiKey.isEmpty()) return null
    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
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
    val photo = photos.getJSONObject((0 until photos.length()).random())
    val src = photo.optJSONObject("src") ?: return null
    return src.optString("original").ifEmpty { src.optString("large2x") }
}