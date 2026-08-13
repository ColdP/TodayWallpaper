package btm.m.todaywallpaper.ui.screens

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Constraints
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.ContextCompat
import btm.m.todaywallpaper.data.preferences.FullscreenClockConfig
import btm.m.todaywallpaper.data.preferences.FullscreenClockPreferences
import btm.m.todaywallpaper.service.ClockMediaNotificationListenerService
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FullscreenClockActivity : ComponentActivity() {
    private var previousOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previousOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                FullscreenClockScreen(onExit = ::finish)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        requestedOrientation = previousOrientation
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}

private data class ClockMedia(
    val title: String = "暂无媒体",
    val artist: String = "打开音乐应用开始播放",
    val artwork: Bitmap? = null,
    val duration: Long = 0L,
    val position: Long = 0L,
    val playing: Boolean = false,
    val controller: MediaController? = null,
    val accessGranted: Boolean = false
)

private data class BatteryInfo(
    val level: Int = 0,
    val charging: Boolean = false,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val currentAmp: Float = 0f
)

private val clockColors = listOf(
    Color.White, Color(0xFFFF6B00), Color(0xFFFFF200), Color(0xFF8CF21A), Color(0xFF62D75D),
    Color(0xFF61D3AA), Color(0xFF45C1E0), Color(0xFF5796E4), Color(0xFFB449DF), Color(0xFFE62B32)
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FullscreenClockScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { FullscreenClockPreferences(context.applicationContext) }
    val config by prefs.config.collectAsState(initial = FullscreenClockConfig())
    val scope = rememberCoroutineScope()
    val now by produceState(initialValue = Date()) {
        while (true) { value = Date(); delay(1_000) }
    }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    val date = remember(now) { SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(now) }
    val media by rememberMediaState(context)
    val battery by rememberBatteryInfo(context)

    var wallpaperFullscreen by remember { mutableStateOf(false) }
    var musicFullscreen by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var deviceEditorOpen by remember { mutableStateOf(false) }
    var mediaPermissionDialog by remember { mutableStateOf(false) }
    var editingStyle by remember { mutableIntStateOf(0) }

    LaunchedEffect(config.selectedStyle, media.accessGranted) {
        if (config.selectedStyle == 1 && !media.accessGranted) {
            mediaPermissionDialog = true
        } else if (media.accessGranted) {
            mediaPermissionDialog = false
        }
    }

    fun closeTransient(): Boolean = when {
        wallpaperFullscreen -> { wallpaperFullscreen = false; true }
        musicFullscreen -> { musicFullscreen = false; true }
        editorOpen -> { editorOpen = false; true }
        deviceEditorOpen -> { deviceEditorOpen = false; true }
        else -> false
    }
    BackHandler { if (!closeTransient()) onExit() }

    var randomWallpaper by remember { mutableStateOf("") }
    LaunchedEffect(config.wallpaperMode, config.wallpaperSource, config.wallpaperCandidates) {
        if (config.wallpaperMode == "random") {
            randomWallpaper = config.wallpaperCandidates.randomOrNull() ?: config.wallpaperUri
        }
    }
    val wallpaperModel = when (config.wallpaperMode) {
        "current" -> context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
            .getString("widget_wallpaper_url", "").orEmpty()
        "random" -> randomWallpaper.ifBlank { config.wallpaperUri }
        else -> config.wallpaperUri
    }
    var wallpaperInk by remember { mutableStateOf(Color.White) }
    LaunchedEffect(wallpaperModel, config.style1Color) {
        if (config.style1Color == -1L && wallpaperModel.isNotBlank()) {
            wallpaperInk = withContext(Dispatchers.IO) { sampleWallpaperInk(context, wallpaperModel) }
        } else {
            wallpaperInk = Color(config.style1Color.toInt())
        }
    }
    val styleColor = if (config.selectedStyle == 2) Color(config.style3Color.toInt()) else Color(config.style1Color.toInt())
    val font = FontFamily.Default
    val clockWeight = FontWeight.Normal

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(config.selectedStyle, editorOpen, deviceEditorOpen, wallpaperFullscreen, musicFullscreen) {
                if (editorOpen || deviceEditorOpen || wallpaperFullscreen || musicFullscreen) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    var end = start
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        event.changes.firstOrNull { it.id == down.id }?.let { end = it.position }
                    } while (event.changes.any { it.pressed })
                    val dx = end.x - start.x
                    val dy = end.y - start.y
                    if (abs(dy) > 56.dp.toPx() && abs(dx) < abs(dy) * .9f) {
                        val next = (config.selectedStyle + if (dy < 0) 1 else 3) % 4
                        scope.launch { prefs.setStyle(next) }
                    }
                }
            }
    ) {
        when (config.selectedStyle) {
            0 -> WallpaperClock(
                time, wallpaperModel, wallpaperFullscreen, styleColor, if (config.style1Color == -1L) wallpaperInk else styleColor, font,
                clockWeight = clockWeight,
                onToggleFullscreen = { wallpaperFullscreen = !wallpaperFullscreen },
                onEdit = { wallpaperFullscreen = false; editingStyle = 0; editorOpen = true }
            )
            1 -> MusicClock(
                time = time,
                media = media,
                fullscreen = musicFullscreen,
                clockFont = font,
                onToggleFullscreen = { musicFullscreen = !musicFullscreen },
                onRequestMediaAccess = { mediaPermissionDialog = true }
            )
            2 -> ColorClock(time, date, styleColor, font, font, editorOpen) {
                editingStyle = 2; editorOpen = true
            }
            else -> DeviceClock(time, battery, config.deviceNameOverride, font) { deviceEditorOpen = true }
        }
        if (!wallpaperFullscreen && !musicFullscreen && !editorOpen && !deviceEditorOpen) {
            StylePips(config.selectedStyle) { scope.launch { prefs.setStyle(it) } }
        }
        if (editorOpen) {
            ClockEditor(
                selectedColor = if (editingStyle == 2) Color(config.style3Color.toInt()) else Color(config.style1Color.toInt()),
                onColor = { color -> scope.launch {
                    if (editingStyle == 2) prefs.setStyle3Color(color.toArgb().toLong()) else prefs.setStyle1Color(color.toArgb().toLong())
                } },
                onDone = { editorOpen = false }
            )
        }
        if (deviceEditorOpen) {
            DeviceNameEditor(
                initial = config.deviceNameOverride.ifBlank { deviceMarketName() },
                onDismiss = { deviceEditorOpen = false },
                onSave = { scope.launch { prefs.setDeviceName(it) }; deviceEditorOpen = false }
            )
        }
        if (mediaPermissionDialog && config.selectedStyle == 1) {
            MediaAccessDialog(
                onDismiss = { mediaPermissionDialog = false },
                onOpenSettings = {
                    mediaPermissionDialog = false
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
        }
    }
}

@Composable
private fun WallpaperClock(
    time: String,
    image: String,
    fullscreen: Boolean,
    cardColor: Color,
    fullscreenColor: Color,
    font: FontFamily,
    clockWeight: FontWeight,
    onToggleFullscreen: () -> Unit,
    onEdit: () -> Unit
) {
    if (fullscreen) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().combinedClickable(onClick = onToggleFullscreen, onLongClick = {})) {
                AsyncImage(image, "时钟壁纸", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            AdaptiveSingleLineText(
                text = time,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 48.dp, top = 30.dp).fillMaxWidth(.22f).height(86.dp)
                    .combinedClickable(onClick = {}, onLongClick = onEdit),
                color = fullscreenColor,
                fontFamily = font,
                maxFontSize = 80.sp,
                minFontSize = 24.sp,
                fontWeight = clockWeight,
                textAlign = TextAlign.End
            )
        }
    } else {
        Row(Modifier.fillMaxSize().padding(start = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            AdaptiveSingleLineText(
                text = time,
                modifier = Modifier.weight(1.12f).fillMaxHeight()
                    .combinedClickable(onClick = {}, onLongClick = onEdit),
                color = cardColor,
                fontFamily = font,
                maxFontSize = 178.sp,
                minFontSize = 44.sp,
                fontWeight = clockWeight,
                verticalAlignment = Alignment.CenterVertically
            )
            Box(Modifier.weight(.9f).fillMaxHeight(.68f).padding(end = 66.dp).clip(RoundedCornerShape(32.dp)).combinedClickable(onClick = onToggleFullscreen, onLongClick = {})) {
                if (image.isBlank()) Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) { Text("请在设置中选择图片", color = Color.White) }
                else AsyncImage(image, "壁纸卡片", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
    }
}

private suspend fun sampleWallpaperInk(context: Context, source: String): Color {
    val bitmap = runCatching {
        val stream = if (source.startsWith("content://") || source.startsWith("file://")) {
            context.contentResolver.openInputStream(android.net.Uri.parse(source))
        } else null
        stream?.use { BitmapFactory.decodeStream(it) }
            ?: if (source.startsWith("http")) {
                val result = ImageLoader(context).execute(
                    ImageRequest.Builder(context).data(source).size(64).allowHardware(false).build()
                ) as? SuccessResult
                (result?.drawable as? BitmapDrawable)?.bitmap
            } else BitmapFactory.decodeFile(source.removePrefix("file://"))
    }.getOrNull() ?: return Color.White
    val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
    var brightness = 0.0
    for (x in 0 until scaled.width) for (y in 0 until scaled.height) {
        val pixel = scaled.getPixel(x, y)
        brightness += (0.2126 * AndroidColor.red(pixel) + 0.7152 * AndroidColor.green(pixel) + 0.0722 * AndroidColor.blue(pixel)) / 255.0
    }
    val average = brightness / (scaled.width * scaled.height)
    if (scaled !== bitmap) scaled.recycle()
    if (source.startsWith("content://") || source.startsWith("file://")) {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
    return if (average > .58) Color.Black else Color.White
}

@Composable
private fun MusicClock(
    time: String,
    media: ClockMedia,
    fullscreen: Boolean,
    clockFont: FontFamily,
    onToggleFullscreen: () -> Unit,
    onRequestMediaAccess: () -> Unit
) {
    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color(0xFF171717))) {
            media.artwork?.let { androidx.compose.foundation.Image(it.asImageBitmap(), null, Modifier.fillMaxSize().blur(35.dp), contentScale = ContentScale.Crop, alpha = .38f) }
            AdaptiveSingleLineText(
                text = time,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 30.dp, top = 30.dp).fillMaxWidth(.24f).height(62.dp),
                color = Color.White,
                fontFamily = clockFont,
                maxFontSize = 42.sp,
                minFontSize = 18.sp,
                fontWeight = FontWeight.Normal
            )
            Column(Modifier.align(Alignment.Center).fillMaxWidth(.56f), horizontalAlignment = Alignment.CenterHorizontally) {
                Artwork(media.artwork, Modifier.size(260.dp).combinedClickable(onClick = onToggleFullscreen, onLongClick = {}))
                Spacer(Modifier.height(18.dp)); MediaInfo(media, true)
                if (!media.accessGranted) MediaAccessButton(onRequestMediaAccess)
            }
        }
    } else {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            AdaptiveSingleLineText(
                text = time,
                modifier = Modifier.weight(.76f).fillMaxHeight().padding(start = 5.dp),
                color = Color.White,
                fontFamily = clockFont,
                maxFontSize = 178.sp,
                minFontSize = 44.sp,
                fontWeight = FontWeight.Normal,
                verticalAlignment = Alignment.CenterVertically
            )
            Box(
                Modifier.weight(1.024f).fillMaxHeight(.68f).padding(end = 44.dp)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                if (media.artwork != null) {
                    androidx.compose.foundation.Image(
                        media.artwork.asImageBitmap(),
                        null,
                        Modifier.fillMaxSize().blur(28.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (media.artwork == null) .72f else .5f)))
                CompactMediaCard(
                    media = media,
                    onArtworkClick = onToggleFullscreen,
                    onRequestMediaAccess = onRequestMediaAccess,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 17.dp)
                )
            }
        }
    }
}

@Composable
private fun Artwork(bitmap: Bitmap?, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
        if (bitmap != null) androidx.compose.foundation.Image(bitmap.asImageBitmap(), "专辑封面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(56.dp))
    }
}

@Composable
private fun MediaInfo(media: ClockMedia, large: Boolean) {
    val controls = media.controller?.transportControls
    var seek by remember(media.position, media.duration) { mutableFloatStateOf(if (media.duration > 0) media.position.toFloat() / media.duration else 0f) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (large) Alignment.CenterHorizontally else Alignment.Start) {
        AdaptiveSingleLineText(
            media.title,
            Modifier.fillMaxWidth().height(if (large) 38.dp else 30.dp),
            Color.White,
            FontFamily.SansSerif,
            if (large) 28.sp else 21.sp,
            10.sp,
            FontWeight.Bold,
            verticalAlignment = Alignment.CenterVertically
        )
        AdaptiveSingleLineText(
            media.artist,
            Modifier.fillMaxWidth().height(if (large) 30.dp else 25.dp),
            Color.White.copy(alpha = .75f),
            FontFamily.SansSerif,
            if (large) 20.sp else 16.sp,
            9.sp,
            FontWeight.Normal,
            verticalAlignment = Alignment.CenterVertically
        )
        IosMediaProgress(
            progress = seek,
            enabled = controls != null && media.duration > 0,
            onProgressChange = { seek = it },
            onSeekFinished = { controls?.seekTo((seek * media.duration).toLong()) },
            modifier = Modifier.fillMaxWidth().height(if (large) 34.dp else 26.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton({ controls?.skipToPrevious() }, enabled = controls != null) { Icon(Icons.Rounded.SkipPrevious, "上一首", tint = Color.White) }
            IconButton({ if (media.playing) controls?.pause() else controls?.play() }, enabled = controls != null) { Icon(if (media.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "播放暂停", tint = Color.White) }
            IconButton({ controls?.skipToNext() }, enabled = controls != null) { Icon(Icons.Rounded.SkipNext, "下一首", tint = Color.White) }
        }
    }
}

@Composable
private fun CompactMediaCard(
    media: ClockMedia,
    onArtworkClick: () -> Unit,
    onRequestMediaAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val controls = media.controller?.transportControls
    var seek by remember(media.position, media.duration) {
        mutableFloatStateOf(if (media.duration > 0) media.position.toFloat() / media.duration else 0f)
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Artwork(
                media.artwork,
                Modifier.fillMaxHeight(.86f).aspectRatio(1f)
                    .combinedClickable(onClick = onArtworkClick, onLongClick = {})
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                AdaptiveSingleLineText(
                    media.title,
                    Modifier.fillMaxWidth().height(32.dp),
                    Color.White,
                    FontFamily.SansSerif,
                    22.sp,
                    11.sp,
                    FontWeight.Bold,
                    verticalAlignment = Alignment.CenterVertically
                )
                AdaptiveSingleLineText(
                    media.artist,
                    Modifier.fillMaxWidth().height(27.dp),
                    Color.White.copy(alpha = .76f),
                    FontFamily.SansSerif,
                    16.sp,
                    9.sp,
                    verticalAlignment = Alignment.CenterVertically
                )
            }
        }
        IosMediaProgress(
            progress = seek,
            enabled = controls != null && media.duration > 0,
            onProgressChange = { seek = it },
            onSeekFinished = { controls?.seekTo((seek * media.duration).toLong()) },
            modifier = Modifier.fillMaxWidth().height(26.dp)
        )
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton({ controls?.skipToPrevious() }, enabled = controls != null) {
                Icon(Icons.Rounded.SkipPrevious, "上一首", tint = Color.White, modifier = Modifier.size(27.dp))
            }
            IconButton({ if (media.playing) controls?.pause() else controls?.play() }, enabled = controls != null) {
                Icon(if (media.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "播放暂停", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            IconButton({ controls?.skipToNext() }, enabled = controls != null) {
                Icon(Icons.Rounded.SkipNext, "下一首", tint = Color.White, modifier = Modifier.size(27.dp))
            }
        }
        if (!media.accessGranted) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MediaAccessButton(onRequestMediaAccess) }
        }
    }
}

@Composable
private fun IosMediaProgress(
    progress: Float,
    enabled: Boolean,
    onProgressChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Canvas(
        modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun update(x: Float) = onProgressChange((x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                update(down.position.x)
                var pressed = true
                while (pressed) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    update(change.position.x)
                    change.consume()
                    pressed = change.pressed
                }
                onSeekFinished()
            }
        }
    ) {
        val centerY = size.height / 2f
        val endX = size.width * clampedProgress
        val trackWidth = 4.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = .28f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = if (enabled) Color.White else Color.White.copy(alpha = .55f),
            start = Offset(0f, centerY),
            end = Offset(endX, centerY),
            strokeWidth = trackWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = Offset(endX.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx()), centerY)
        )
    }
}

@Composable
private fun MediaAccessButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
        Icon(Icons.Rounded.Notifications, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("开启媒体读取权限", maxLines = 1, fontSize = 12.sp)
    }
}

@Composable
private fun MediaAccessDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.MusicNote, null) },
        title = { Text("允许读取当前媒体") },
        text = { Text("音乐卡片需要通知使用权，以读取系统当前媒体会话的歌曲、封面、进度和播放状态。应用不会保存通知内容；拒绝后仍可使用时钟。") },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("前往开启") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("暂不") } }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ColorClock(time: String, date: String, color: Color, font: FontFamily, dateFont: FontFamily, editing: Boolean, onEdit: () -> Unit) {
    if (!editing) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(92.dp))
            AdaptiveSingleLineText(
                date,
                Modifier.fillMaxWidth(.82f).height(54.dp),
                color,
                dateFont,
                42.sp,
                14.sp,
                FontWeight.Bold,
                textAlign = TextAlign.Center,
                verticalAlignment = Alignment.CenterVertically
            )
            Spacer(Modifier.height(30.dp))
            AdaptiveSingleLineText(
                time,
                Modifier.fillMaxWidth(.88f).fillMaxHeight(.5f).combinedClickable(onClick = {}, onLongClick = onEdit),
                color,
                font,
                230.sp,
                54.sp,
                FontWeight.Bold,
                textAlign = TextAlign.Center,
                verticalAlignment = Alignment.CenterVertically
            )
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            AdaptiveSingleLineText(
                text = time,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 30.dp)
                    .fillMaxWidth(.55f).fillMaxHeight(.48f)
                    .clip(RoundedCornerShape(55.dp)).background(Color.Transparent),
                color = color,
                fontFamily = font,
                maxFontSize = 170.sp,
                minFontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                verticalAlignment = Alignment.CenterVertically
            )
        }
    }
}

@Composable
private fun StylePips(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxHeight().padding(end = 22.dp).wrapContentWidth().padding(vertical = 0.dp), verticalArrangement = Arrangement.Center) {
        // Alignment is supplied by the parent through a full-screen overlay below.
    }
    Column(Modifier.fillMaxSize().padding(end = 22.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
        repeat(4) { index ->
            Box(Modifier.padding(4.dp).size(if (index == selected) 12.dp else 10.dp).clip(CircleShape).background(if (index == selected) Color.White else Color.Gray).combinedClickable(onClick = { onSelect(index) }, onLongClick = {}))
        }
    }
}

@Composable
private fun ClockEditor(selectedColor: Color, onColor: (Color) -> Unit, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.align(Alignment.CenterEnd).padding(end = 42.dp).fillMaxWidth(.39f).fillMaxHeight(.63f), shape = RoundedCornerShape(48.dp), color = Color(0xFF9F9F9F), border = BorderStroke(1.dp, Color.White.copy(.45f))) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("时钟颜色", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onDone, Modifier.background(Color.White, CircleShape)) { Icon(Icons.Rounded.Check, "完成", tint = Color.Black) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    clockColors.forEach { color ->
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(color)
                                .then(if (color == selectedColor) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                                .combinedClickable(onClick = { onColor(color) }, onLongClick = {})
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceClock(time: String, battery: BatteryInfo, overrideName: String, deviceFont: FontFamily, onEdit: () -> Unit) {
    val context = LocalContext.current
    val memory = rememberMemoryInfo(context)
    val cpu by rememberCpuUsage()
    val storage = remember { storageInfo() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 450.dp
        val horizontalPadding = if (compact) 14.dp else 38.dp
        val rightPadding = if (compact) 42.dp else 92.dp
        val contentTop = if (compact) 52.dp else 108.dp
        val contentBottom = if (compact) 14.dp else 46.dp
        val gap = if (compact) 10.dp else 22.dp
        val cardPadding = if (compact) 12.dp else 28.dp
        val cardRadius = if (compact) 20.dp else 38.dp
        val metricHeight = if (compact) 25.dp else 38.dp
        AdaptiveSingleLineText(
            time,
            Modifier.padding(start = horizontalPadding, top = 12.dp).fillMaxWidth(.2f).height(if (compact) 30.dp else 54.dp),
            Color.White,
            deviceFont,
            if (compact) 28.sp else 38.sp,
            14.sp,
            FontWeight.Normal
        )
        Row(
            Modifier.fillMaxSize().padding(start = horizontalPadding, end = rightPadding, top = contentTop, bottom = contentBottom),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            Column(Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(gap)) {
                HardwareCard(Modifier.fillMaxWidth().weight(1.28f).combinedClickable(onClick = {}, onLongClick = onEdit), cardPadding, cardRadius) {
                    AdaptiveSingleLineText(
                        overrideName.ifBlank { deviceMarketName() },
                        Modifier.fillMaxWidth().height(if (compact) 28.dp else 48.dp),
                        Color.White,
                        deviceFont,
                        if (compact) 21.sp else 34.sp,
                        10.sp,
                        FontWeight.Bold,
                        verticalAlignment = Alignment.CenterVertically
                    )
                    Spacer(Modifier.height(if (compact) 3.dp else 10.dp))
                    AdaptiveSingleLineText(
                        "Android ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}",
                        Modifier.fillMaxWidth().height(if (compact) 18.dp else 26.dp),
                        Color.White,
                        deviceFont,
                        if (compact) 12.sp else 17.sp,
                        7.sp,
                        FontWeight.Normal
                    )
                    AdaptiveSingleLineText(
                        systemBuildName(),
                        Modifier.fillMaxWidth().height(if (compact) 18.dp else 26.dp),
                        Color.White,
                        deviceFont,
                        if (compact) 12.sp else 17.sp,
                        7.sp,
                        FontWeight.Normal
                    )
                }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    HardwareCard(Modifier.weight(1.25f).fillMaxHeight(), cardPadding, cardRadius) {
                        Meter("内存", memory.first, memory.second, deviceFont, compact)
                        Spacer(Modifier.height(if (compact) 4.dp else 11.dp))
                        Meter("存储空间", storage.first, storage.second, deviceFont, compact)
                    }
                    HardwareCard(Modifier.weight(.85f).fillMaxHeight(), cardPadding, cardRadius) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                cpu / 100f,
                                Modifier.size(if (compact) 58.dp else 105.dp),
                                color = Color.White,
                                trackColor = Color.Gray,
                                strokeWidth = if (compact) 3.dp else 5.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CPU占用", fontSize = if (compact) 10.sp else 16.sp, fontWeight = FontWeight.Bold)
                                Text("%.1f%%".format(cpu), fontSize = if (compact) 12.sp else 18.sp)
                            }
                        }
                    }
                }
            }
            HardwareCard(Modifier.weight(1f).fillMaxHeight(), cardPadding, cardRadius) {
                val power = battery.voltage * battery.currentAmp
                Metric("电量", "${battery.level}%", if (battery.charging) Color(0xFF2FD266) else Color.White, deviceFont, metricHeight, compact)
                LinearProgressIndicator(
                    battery.level / 100f,
                    Modifier.fillMaxWidth().height(if (compact) 5.dp else 8.dp).clip(CircleShape),
                    color = if (battery.charging) Color(0xFF2FD266) else Color.White,
                    trackColor = Color.Gray
                )
                Spacer(Modifier.height(if (compact) 5.dp else 28.dp))
                Metric("充电模式", if (battery.charging) "AC快充" else "未在充电", Color.LightGray, deviceFont, metricHeight, compact)
                Metric("电池温度", "%.1f°C".format(battery.temperature), Color.LightGray, deviceFont, metricHeight, compact)
                Metric(if (battery.charging) "功率" else "功耗", "%+.2fW".format(power), Color.LightGray, deviceFont, metricHeight, compact)
                Metric("电压", "%.2fV".format(battery.voltage), Color.LightGray, deviceFont, metricHeight, compact)
                Metric("电流", "%+.2fA".format(battery.currentAmp), Color.LightGray, deviceFont, metricHeight, compact)
            }
        }
    }
}

@Composable
private fun HardwareCard(
    modifier: Modifier,
    padding: Dp,
    radius: Dp,
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier,
    shape = RoundedCornerShape(radius),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF474747))
) {
    Column(Modifier.fillMaxSize().padding(padding), content = content)
}

@Composable
private fun Meter(title: String, free: Float, total: Float, fontFamily: FontFamily, compact: Boolean) {
    val lineHeight = if (compact) 17.dp else 24.dp
    AdaptiveSingleLineText(title, Modifier.fillMaxWidth().height(lineHeight), Color.White, fontFamily, if (compact) 12.sp else 17.sp, 7.sp, FontWeight.Bold)
    LinearProgressIndicator(if (total > 0) (total - free) / total else 0f, Modifier.fillMaxWidth().height(if (compact) 4.dp else 5.dp), color = Color.White, trackColor = Color.Gray)
    Row(Modifier.fillMaxWidth().height(lineHeight), horizontalArrangement = Arrangement.SpaceBetween) {
        AdaptiveSingleLineText("%.1f GB (空闲)".format(free), Modifier.weight(1f), Color.White, fontFamily, if (compact) 8.sp else 10.sp, 5.sp)
        AdaptiveSingleLineText("%.1f GB (总共)".format(total), Modifier.weight(1f), Color.White, fontFamily, if (compact) 8.sp else 10.sp, 5.sp, textAlign = TextAlign.End)
    }
}

@Composable
private fun Metric(label: String, value: String, valueColor: Color, fontFamily: FontFamily, height: Dp, compact: Boolean) {
    Row(Modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.CenterVertically) {
        AdaptiveSingleLineText(label, Modifier.weight(.56f), Color.White, fontFamily, if (compact) 11.sp else 18.sp, 6.sp, FontWeight.Bold, verticalAlignment = Alignment.CenterVertically)
        AdaptiveSingleLineText(value, Modifier.weight(.44f), valueColor, fontFamily, if (compact) 11.sp else 18.sp, 6.sp, textAlign = TextAlign.End, verticalAlignment = Alignment.CenterVertically)
    }
}

@Composable
private fun DeviceNameEditor(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("设备名称") }, text = { OutlinedTextField(value, { value = it }, singleLine = true) }, confirmButton = { TextButton({ if (value.isNotBlank()) onSave(value.trim()) }) { Text("保存") } }, dismissButton = { TextButton(onDismiss) { Text("取消") } })
}

@Composable
private fun rememberMediaState(context: Context): State<ClockMedia> = produceState(ClockMedia(), context) {
    while (true) { value = queryMedia(context); delay(1_000) }
}

private fun queryMedia(context: Context): ClockMedia {
    val accessGranted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    if (!accessGranted) return ClockMedia(accessGranted = false)
    return runCatching {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(context, ClockMediaNotificationListenerService::class.java)
        val controller = manager.getActiveSessions(component).maxByOrNull { if (it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) 1 else 0 }
            ?: return ClockMedia(accessGranted = true)
        val metadata = controller.metadata
        val state = controller.playbackState
        ClockMedia(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty().ifBlank { "未知歌曲" },
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty().ifBlank { metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty().ifBlank { "未知艺术家" } },
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
            duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            position = state?.position ?: 0L,
            playing = state?.state == android.media.session.PlaybackState.STATE_PLAYING,
            controller = controller,
            accessGranted = true
        )
    }.getOrDefault(ClockMedia(accessGranted = true))
}

@Composable
private fun rememberBatteryInfo(context: Context): State<BatteryInfo> = produceState(BatteryInfo(), context) {
    fun parse(intent: Intent?): BatteryInfo {
        if (intent == null) return BatteryInfo()
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val bm = context.getSystemService(BatteryManager::class.java)
        val microAmp = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL || plugged != 0
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val rawCurrent = if (microAmp == Int.MIN_VALUE) 0f else kotlin.math.abs(microAmp) / 1_000_000f
        return BatteryInfo(
            level = (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale).coerceIn(0, 100),
            charging = charging,
            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f,
            currentAmp = if (charging) rawCurrent else -rawCurrent
        )
    }
    value = parse(context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
    val receiver = object : BroadcastReceiver() { override fun onReceive(c: Context?, intent: Intent?) { value = parse(intent) } }
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_EXPORTED
    )
    awaitDispose { runCatching { context.unregisterReceiver(receiver) } }
}

@Composable
private fun rememberMemoryInfo(context: Context): Pair<Float, Float> {
    val info = remember { ActivityManager.MemoryInfo() }
    context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
    val gb = 1024f * 1024f * 1024f
    return info.availMem / gb to info.totalMem / gb
}

@Composable
private fun rememberCpuUsage(): State<Float> = produceState(0f) {
    while (true) { value = withContext(Dispatchers.IO) { sampleCpu() }; delay(2_000) }
}

private fun sampleCpu(): Float = runCatching {
    fun read(): Pair<Long, Long> {
        val values = java.io.File("/proc/stat").useLines { it.first() }.trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
        return values.drop(3).take(2).sum() to values.sum()
    }
    val first = read(); Thread.sleep(350); val second = read()
    val total = second.second - first.second; val idle = second.first - first.first
    if (total > 0) ((total - idle) * 100f / total).coerceIn(0f, 100f) else 0f
}.getOrDefault(0f)

private fun storageInfo(): Pair<Float, Float> {
    val stat = StatFs(android.os.Environment.getDataDirectory().path)
    val gb = 1024f * 1024f * 1024f
    return stat.availableBytes / gb to stat.totalBytes / gb
}

private fun deviceMarketName(): String = systemProperty("ro.product.marketname").ifBlank { "${Build.BRAND} ${Build.MODEL}" }
private fun systemBuildName(): String {
    val incremental = systemProperty("ro.mi.os.version.incremental")
    val brand = Build.BRAND.lowercase()
    val osName = when { brand.contains("xiaomi") || brand.contains("redmi") -> "HyperOS"; brand.contains("samsung") -> "One UI"; brand.contains("oppo") -> "ColorOS"; brand.contains("vivo") -> "OriginOS"; brand.contains("honor") -> "MagicOS"; brand.contains("huawei") -> "HarmonyOS"; else -> Build.BRAND }
    return "$osName ${incremental.ifBlank { Build.DISPLAY }}"
}
private fun systemProperty(key: String): String = runCatching { Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, key) as String }.getOrDefault("")

@Composable
private fun AdaptiveSingleLineText(
    text: String,
    modifier: Modifier,
    color: Color,
    fontFamily: FontFamily,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx().toInt().coerceAtLeast(1) }
        val maxHeightPx = with(density) { maxHeight.toPx().toInt().coerceAtLeast(1) }
        val resolvedSize = remember(text, fontFamily, fontWeight, maxFontSize, minFontSize, maxWidthPx, maxHeightPx) {
            val requestedMin = minFontSize.value.coerceAtLeast(1f)
            val requestedMax = maxFontSize.value.coerceAtLeast(requestedMin)
            val constraints = Constraints(maxWidth = maxWidthPx, maxHeight = maxHeightPx)

            fun fits(size: Float): Boolean {
                val result = measurer.measure(
                    AnnotatedString(text),
                    TextStyle(fontSize = size.sp, fontFamily = fontFamily, fontWeight = fontWeight),
                    maxLines = 1,
                    softWrap = false,
                    constraints = constraints
                )
                return !result.didOverflowWidth && !result.didOverflowHeight &&
                    result.size.width <= maxWidthPx && result.size.height <= maxHeightPx
            }

            var low = 1f
            var high = requestedMax
            if (fits(requestedMin)) {
                low = requestedMin
            } else if (fits(1f)) {
                high = requestedMin
            } else {
                high = 1f
            }
            repeat(12) {
                val candidate = (low + high) / 2f
                if (fits(candidate)) low = candidate else high = candidate
            }
            low.sp
        }
        val contentAlignment = when {
            verticalAlignment == Alignment.Top -> Alignment.TopStart
            verticalAlignment == Alignment.Bottom -> Alignment.BottomStart
            else -> Alignment.CenterStart
        }
        Box(Modifier.fillMaxSize(), contentAlignment = contentAlignment) {
            Text(
                text = text,
                color = color,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontSize = resolvedSize,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}