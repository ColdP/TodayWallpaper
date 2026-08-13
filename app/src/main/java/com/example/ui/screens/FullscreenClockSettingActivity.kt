package btm.m.todaywallpaper.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.data.preferences.FullscreenClockConfig
import btm.m.todaywallpaper.data.preferences.FullscreenClockPreferences
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperUiState
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class FullscreenClockSettingActivity : ComponentActivity() {
    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { FullscreenClockSettingScreen(viewModel, ::finish) } }
    }
}

private data class ClockImageCandidate(val id: String, val url: String, val thumbnail: String)

@Composable
private fun FullscreenClockSettingScreen(viewModel: WallpaperViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { FullscreenClockPreferences(context.applicationContext) }
    val config by preferences.config.collectAsState(initial = FullscreenClockConfig())
    val favorites by viewModel.favorites.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val categoryState by viewModel.categoryGridState.collectAsState()
    val collectionItems by viewModel.activeCollectionItems.collectAsState()
    val showApiKeyPrompt by viewModel.showApiKeyPrompt.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        scope.launch { preferences.setWallpaper("upload", uri = uri.toString()) }
        Toast.makeText(context, "全屏时钟图片已保存", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(config.wallpaperSource) {
        when {
            config.wallpaperSource.startsWith("category_") -> viewModel.loadCategoryWallpapers(config.wallpaperSource.removePrefix("category_"))
            config.wallpaperSource.startsWith("collection_") -> config.wallpaperSource.removePrefix("collection_").toIntOrNull()?.let(viewModel::fetchActiveCollectionItems)
        }
    }

    val candidates = remember(config.wallpaperSource, favorites, categoryState, collectionItems) {
        when {
            config.wallpaperSource == "favorites" -> favorites.map { ClockImageCandidate(it.id, it.imageUrl, it.thumbnailUrl) }
            config.wallpaperSource.startsWith("category_") -> ((categoryState as? WallpaperUiState.Success)?.data ?: emptyList()).map { ClockImageCandidate(it.id, it.imageUrl, it.thumbnailUrl) }
            config.wallpaperSource.startsWith("collection_") -> collectionItems.map { ClockImageCandidate(it.wallpaperId, it.imageUrl, it.thumbnailUrl) }
            else -> emptyList()
        }
    }
    LaunchedEffect(config.wallpaperMode, config.wallpaperSource, candidates) {
        if (config.wallpaperMode == "random" && candidates.isNotEmpty()) {
            val urls = candidates.map { it.url }
            val selected = config.wallpaperUri.takeIf(urls::contains) ?: urls.random()
            preferences.setWallpaper("random", config.wallpaperSource, selected, urls)
        }
    }
    val preview = config.wallpaperUri.ifBlank { context.getSharedPreferences("app_gallery_prefs", 0).getString("widget_wallpaper_url", "").orEmpty() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
            Text("全屏时钟自定义图片", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 7f).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                AsyncImage(preview, "全屏时钟图片预览", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.scrim.copy(alpha = .68f)) {
                    Text(modeLabel(config.wallpaperMode), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
                }
            }
            Text("图片模式", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    ClockModeRow(Icons.Rounded.Wallpaper, "跟随当前首页壁纸", "首页壁纸变化后自动使用最新图片", config.wallpaperMode == "current") { scope.launch { preferences.setWallpaper("current") } }
                    ClockModeRow(Icons.Rounded.Image, "从来源中选择一张", "从喜欢、分类或图集中指定", config.wallpaperMode == "select") { scope.launch { preferences.setWallpaper("select", config.wallpaperSource, config.wallpaperUri) } }
                    ClockModeRow(Icons.Rounded.Shuffle, "从来源中随机选择", "进入时钟时从所选来源随机取图", config.wallpaperMode == "random") { scope.launch { preferences.setWallpaper("random", config.wallpaperSource, candidates.randomOrNull()?.url.orEmpty(), candidates.map { it.url }) } }
                    ClockModeRow(Icons.Rounded.Upload, "从相册上传自定义图片", "使用系统图片选择器并持久保留权限", config.wallpaperMode == "upload") { picker.launch(arrayOf("image/*")) }
                }
            }
            if (config.wallpaperMode == "select" || config.wallpaperMode == "random") {
                Text("图片来源", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceChip("favorites", "喜欢", config.wallpaperMode, config.wallpaperSource, preferences, scope)
                    categories.forEach { SourceChip("category_${it.key}", it.zhTitle, config.wallpaperMode, config.wallpaperSource, preferences, scope) }
                    collections.forEach { SourceChip("collection_${it.id}", it.name, config.wallpaperMode, config.wallpaperSource, preferences, scope) }
                }
                if (candidates.isEmpty()) {
                    Text(if (categoryState is WallpaperUiState.Loading) "正在加载图片..." else "该来源暂无可用图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(Modifier.fillMaxWidth().height(132.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(candidates, key = { it.id }) { item ->
                            Box(Modifier.width(178.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)).clickable { scope.launch { preferences.setWallpaper(config.wallpaperMode, config.wallpaperSource, item.url, if (config.wallpaperMode == "random") candidates.map { it.url } else emptyList()) } }) {
                                AsyncImage(item.thumbnail.ifBlank { item.url }, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                if (config.wallpaperUri == item.url) Icon(Icons.Rounded.CheckCircle, "已选择", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp))
                            }
                        }
                    }
                    if (config.wallpaperMode == "random") Button(onClick = { candidates.randomOrNull()?.let { item -> scope.launch { preferences.setWallpaper("random", config.wallpaperSource, item.url, candidates.map { it.url }) } } }, Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Shuffle, null); Spacer(Modifier.width(8.dp)); Text("立即重新随机")
                    }
                }
            }
            HorizontalDivider()
            Text("音乐卡片需要通知使用权才能读取系统当前媒体会话。拒绝权限时仍可使用时钟。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Icon(Icons.Rounded.MusicNote, null); Spacer(Modifier.width(8.dp)); Text("管理媒体读取权限") }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showApiKeyPrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissApiKeyPrompt(proceedAnyway = false) },
            title = { Text("配置 Pexels API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("读取 Pexels 分类图片需要 API Key。也可以暂时取消，继续使用喜欢和图集中的本地图片。", fontSize = 13.sp)
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        singleLine = true,
                        placeholder = { Text("Pexels API Key") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.savePexelsKeyAndProceed(apiKeyInput) }) { Text("保存并加载") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissApiKeyPrompt(proceedAnyway = false) }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SourceChip(key: String, label: String, mode: String, selectedKey: String, preferences: FullscreenClockPreferences, scope: kotlinx.coroutines.CoroutineScope) {
    FilterChip(selected = key == selectedKey, onClick = { scope.launch { preferences.setWallpaper(mode, key, "") } }, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = if (key == selectedKey) ({ Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }) else null)
}

@Composable
private fun ClockModeRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }; RadioButton(selected, onClick)
    }
}

private fun modeLabel(mode: String) = when (mode) { "select" -> "指定图片"; "random" -> "随机来源"; "upload" -> "相册图片"; else -> "跟随首页" }