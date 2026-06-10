package btm.m.todaywallpaper.ui.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import btm.m.todaywallpaper.BuildConfig
import btm.m.todaywallpaper.R
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class UpdateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                UpdateScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val updateLogUrl: String,
    val fileSize: Long,
    val isForceUpdate: Boolean
)

enum class DownloadState {
    IDLE, DOWNLOADING, PAUSED, COMPLETED, ERROR
}

@Composable
fun UpdateScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val view = androidx.compose.ui.platform.LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(darkTheme) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {}
    }

    // Predictive back gesture
    var backProgress by remember { mutableStateOf(0f) }
    var isBackSwiping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        backProgress = 0f
        isBackSwiping = false
    }

    androidx.activity.compose.PredictiveBackHandler { progressFlow ->
        try {
            isBackSwiping = true
            progressFlow.collect { backEvent ->
                backProgress = backEvent.progress
            }
            isBackSwiping = false
            backProgress = 1f
            onBack()
        } catch (e: Exception) {
            isBackSwiping = false
            backProgress = 0f
        }
    }

    val scale = 1f - (backProgress * 0.08f)
    val translationXDp = (backProgress * 120).dp
    val alphaVal = 1f - (backProgress * 0.2f)
    val cornerRadius = (backProgress * 24).dp

    // Version info
    val currentVersionCode = BuildConfig.VERSION_CODE.toLong()
    val currentVersionName = BuildConfig.VERSION_NAME

    // Update check state
    var isChecking by remember { mutableStateOf(true) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkError by remember { mutableStateOf<String?>(null) }

    // Download state
    var downloadState by remember { mutableStateOf(DownloadState.IDLE) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var downloadedFileUri by remember { mutableStateOf<Uri?>(null) }

    // App icon
    val appIconBitmap = remember(context) {
        try {
            context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    val isLatest = updateInfo?.let { it.versionCode <= currentVersionCode } ?: false

    // Check for updates on launch
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("https://today-wallpaper.sgp1.cdn.digitaloceanspaces.com/update/update.json")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val info = UpdateInfo(
                            versionCode = json.getLong("versionCode"),
                            versionName = json.getString("versionName"),
                            apkUrl = json.getString("apkUrl"),
                            updateLogUrl = json.optString("updateLogUrl", ""),
                            fileSize = json.optLong("fileSize", 0),
                            isForceUpdate = json.optBoolean("isForceUpdate", false)
                        )
                        withContext(Dispatchers.Main) {
                            updateInfo = info
                            isChecking = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            checkError = "Empty response"
                            isChecking = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        checkError = "HTTP ${response.code}"
                        isChecking = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    checkError = e.localizedMessage ?: "Network error"
                    isChecking = false
                }
            }
        }
    }

    // Poll download progress AND detect completion (primary mechanism)
    LaunchedEffect(downloadState, downloadId) {
        if (downloadState == DownloadState.DOWNLOADING && downloadId != -1L) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (downloadState == DownloadState.DOWNLOADING && downloadId != -1L) {
                kotlinx.coroutines.delay(500)
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (bytesTotal > 0) {
                            downloadProgress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                        }
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloadState = DownloadState.COMPLETED
                                downloadProgress = 1f
                                cursor.close()
                                installApk(context, downloadId)
                                break
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloadState = DownloadState.ERROR
                                cursor.close()
                                break
                            }
                        }
                        cursor.close()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // DownloadManager broadcast receiver as secondary fallback
    DisposableEffect(downloadId) {
        if (downloadId == -1L) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId && downloadState == DownloadState.DOWNLOADING) {
                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloadState = DownloadState.COMPLETED
                            downloadProgress = 1f
                            installApk(ctx, downloadId)
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloadState = DownloadState.ERROR
                        }
                        cursor.close()
                    }
                }
            }
        }
        try {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        } catch (_: Exception) {}
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = with(LocalDensity.current) { translationXDp.toPx() },
                    alpha = alphaVal,
                    clip = cornerRadius > 0.dp,
                    shape = RoundedCornerShape(cornerRadius)
                ),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Title Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "软件更新",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Main info card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                // App Icon + Name row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (appIconBitmap != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = appIconBitmap,
                                                contentDescription = "App Icon",
                                                modifier = Modifier.size(42.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                    }
                                    Column {
                                        Text(
                                            text = context.getString(R.string.app_name),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Status
                                        if (isChecking) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "正在检查更新...",
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        } else if (checkError != null) {
                                            Text(
                                                text = "检查更新失败: $checkError",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        } else if (isLatest) {
                                            Text(
                                                text = "当前已是最新版本 ✓",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text(
                                                text = "有更新可用",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFFF9800)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Current Version
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "当前版本",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "$currentVersionName ($currentVersionCode)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Latest Version (only if update available)
                                if (!isLatest && updateInfo != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "最新版本",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "${updateInfo!!.versionName} (${updateInfo!!.versionCode})",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                }

                                // View changelog for new version
                                if (!isLatest && updateInfo != null && updateInfo!!.updateLogUrl.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                val intent = Intent(context, ChangelogActivity::class.java)
                                                intent.putExtra("versionCode", updateInfo!!.versionCode.toInt())
                                                intent.putExtra("versionName", updateInfo!!.versionName)
                                                intent.putExtra("changelogUrl", updateInfo!!.updateLogUrl)
                                                context.startActivity(intent)
                                            }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "查看新版本日志",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bottom buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        // Update button (only if update available)
                        if (!isLatest && updateInfo != null && checkError == null) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = downloadProgress,
                                animationSpec = tween(300),
                                label = "download_progress"
                            )

                            Button(
                                onClick = {
                                    when (downloadState) {
                                        DownloadState.IDLE, DownloadState.ERROR -> {
                                            // Start download
                                            downloadState = DownloadState.DOWNLOADING
                                            downloadProgress = 0f
                                            startDownload(context, updateInfo!!) { id ->
                                                downloadId = id
                                            }
                                        }
                                        DownloadState.DOWNLOADING -> {
                                            // Pause download
                                            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                            // DownloadManager doesn't support pause, so we remove and mark as paused
                                            dm.remove(downloadId)
                                            downloadState = DownloadState.PAUSED
                                        }
                                        DownloadState.PAUSED -> {
                                            // Resume download by restarting
                                            downloadState = DownloadState.DOWNLOADING
                                            downloadProgress = 0f
                                            startDownload(context, updateInfo!!) { id ->
                                                downloadId = id
                                            }
                                        }
                                        DownloadState.COMPLETED -> {
                                            installApk(context, downloadId)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                if (downloadState == DownloadState.DOWNLOADING || downloadState == DownloadState.PAUSED) {
                                                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                    dm.remove(downloadId)
                                                    downloadState = DownloadState.IDLE
                                                    downloadProgress = 0f
                                                    downloadId = -1L
                                                    Toast.makeText(context, "下载已取消", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    },
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (downloadState == DownloadState.DOWNLOADING)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                if (downloadState == DownloadState.DOWNLOADING) {
                                    // Show progress bar inside button using LinearProgressIndicator
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Background
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(26.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                        // Progress fill from left to right
                                        LinearProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(26.dp)),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            trackColor = Color.Transparent,
                                        )
                                        Text(
                                            text = "${(animatedProgress * 100).toInt()}% 点击暂停 · 长按取消",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else when (downloadState) {
                                    DownloadState.IDLE -> Text("更新软件", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    DownloadState.PAUSED -> Text("继续下载 (${(downloadProgress * 100).toInt()}%) · 长按取消", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    DownloadState.COMPLETED -> Text("安装更新", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    DownloadState.ERROR -> Text("重新下载", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    else -> {}
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // View current version changelog
                        OutlinedButton(
                            onClick = {
                                val currentLogUrl = "https://today-wallpaper.sgp1.cdn.digitaloceanspaces.com/update/${BuildConfig.VERSION_CODE}.md"
                                val intent = Intent(context, ChangelogActivity::class.java)
                                intent.putExtra("versionCode", BuildConfig.VERSION_CODE)
                                intent.putExtra("versionName", BuildConfig.VERSION_NAME)
                                intent.putExtra("changelogUrl", currentLogUrl)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("查看当前版本日志", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun startDownload(context: Context, updateInfo: UpdateInfo, onDownloadId: (Long) -> Unit) {
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "TodayWallpaper-v${updateInfo.versionName}-universal.apk"

        // Use app-private external files dir for reliable file access on all Android versions
        val downloadDir = File(context.getExternalFilesDir(null), "updates")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val targetFile = File(downloadDir, fileName)

        val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl))
            .setTitle("TodayWallpaper 更新")
            .setDescription("正在下载 TodayWallpaper v${updateInfo.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationUri(Uri.fromFile(targetFile))

        val id = dm.enqueue(request)
        onDownloadId(id)
    } catch (e: Exception) {
        Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun installApk(context: Context, downloadId: Long) {
    try {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dm.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val localUriStr = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            cursor.close()

            if (localUriStr != null) {
                val localUri = Uri.parse(localUriStr)
                val filePath = localUri.path
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        launchInstallIntent(context, file)
                        return
                    }
                }
            }
        }

        // Fallback: search in app-private updates dir
        val updatesDir = File(context.getExternalFilesDir(null), "updates")
        if (updatesDir.exists()) {
            val apkFiles = updatesDir.listFiles { f -> f.name.endsWith(".apk") }
            if (apkFiles != null && apkFiles.isNotEmpty()) {
                val latestApk = apkFiles.maxByOrNull { it.lastModified() }!!
                launchInstallIntent(context, latestApk)
                return
            }
        }

        Toast.makeText(context, "安装失败: 无法找到下载文件", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun launchInstallIntent(context: Context, apkFile: File) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        intent.setDataAndType(
            Uri.fromFile(apkFile),
            "application/vnd.android.package-archive"
        )
    }
    context.startActivity(intent)
}
