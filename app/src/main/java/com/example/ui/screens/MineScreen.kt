package btm.m.todaywallpaper.ui.screens

import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import btm.m.todaywallpaper.data.model.CollectionItem
import btm.m.todaywallpaper.data.model.FavoriteWallpaper
import btm.m.todaywallpaper.data.model.WallpaperCollection
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MineScreen(
    viewModel: WallpaperViewModel,
    onViewDetail: (String, String, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Observed states from DB/Repository via ViewModel
    val favorites by viewModel.favorites.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val activeCollectionItems by viewModel.activeCollectionItems.collectAsState()

    // Preferences states
    val currentLang by viewModel.language.collectAsState()
    val homeType by viewModel.homeWallpaperType.collectAsState()
    val categories by viewModel.categories.collectAsState()
    // UI Interactive States
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var selectedCollectionForDetail by remember { mutableStateOf<WallpaperCollection?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAboutPage by remember { mutableStateOf(false) }

    LaunchedEffect(showAboutPage) {
        viewModel.setAboutPageVisible(showAboutPage)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.setAboutPageVisible(false)
        }
    }

    var collectionBackProgress by remember { mutableStateOf(0f) }
    var isCollectionBackSwiping by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCollectionForDetail) {
        if (selectedCollectionForDetail != null) {
            // Reset only when entering a new collection, NOT when leaving
            collectionBackProgress = 0f
            isCollectionBackSwiping = false
        }
    }

    if (selectedCollectionForDetail != null) {
        PredictiveBackHandler { progressFlow ->
            try {
                isCollectionBackSwiping = true
                progressFlow.collect { backEvent ->
                    collectionBackProgress = backEvent.progress
                }
                isCollectionBackSwiping = false
                collectionBackProgress = 1f
                selectedCollectionForDetail = null
            } catch (_: Exception) {
                isCollectionBackSwiping = false
                collectionBackProgress = 0f
            }
        }
    }

    val currentUsername by viewModel.username.collectAsState()
    val currentAvatarUrl by viewModel.avatarUrl.collectAsState()
    val currentProfileSubtitle by viewModel.profileSubtitle.collectAsState()
    
    var tempAvatarUrlForDialog by remember { mutableStateOf("") }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    val fileName = "avatar_$timeStamp.jpg"
                    context.filesDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("avatar_") && file.name.endsWith(".jpg")) {
                            file.delete()
                        }
                    }
                    val avatarFile = java.io.File(context.filesDir, fileName)
                    java.io.FileOutputStream(avatarFile).use { output ->
                        inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }
                    tempAvatarUrlForDialog = avatarFile.absolutePath
                    Toast.makeText(context, viewModel.getTranslation("已加载本地图片预览", "Local image loaded for preview"), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, viewModel.getTranslation("无法读取该图片文件", "Failed to load selected image"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    AnimatedContent(
        targetState = showAboutPage,
        transitionSpec = {
            (fadeIn(tween(220)) + slideInVertically(initialOffsetY = { it / 8 }))
                .togetherWith(fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 8 }))
        },
        label = "MineAboutTransition"
    ) { isAboutPage ->
        if (isAboutPage) {
            AboutScreen(
                viewModel = viewModel,
                onBack = { showAboutPage = false },
                modifier = modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Crossfade(
                    targetState = selectedCollectionForDetail,
                    animationSpec = tween(durationMillis = 300),
                    label = "MinePanelTransition"
                ) { activeCollection ->
                    if (activeCollection == null) {
                        // Main Dashboard Mine Panel
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Profile Header Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                tempAvatarUrlForDialog = currentAvatarUrl ?: ""
                                showEditProfileDialog = true
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (currentAvatarUrl != null) {
                                    AsyncImage(
                                        model = currentAvatarUrl,
                                        contentDescription = "User avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "User avatar icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUsername,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentProfileSubtitle.ifEmpty {
                                    viewModel.getTranslation("每一次收藏，皆是对美学的赞美", "Each bookmark praises outstanding aesthetics")
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // SECTION 1: CUSTOM ALBUM COLLECTIONS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(
                            icon = Icons.Outlined.Collections,
                            text = viewModel.getTranslation("本地自定义图集", "Custom Albums"),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { showCreateCollectionDialog = true },
                            modifier = Modifier.testTag("mine_add_collection_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Album",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (collections.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                    RoundedCornerShape(14.dp)
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp),
                            onClick = { showCreateCollectionDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = viewModel.getTranslation("+ 创建你的第一个专属图集", "+ Create your first custom album"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else {
                        // User Albums List
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(collections, key = { it.id }) { album ->
                                AlbumCardItem(album = album, viewModel = viewModel) {
                                    selectedCollectionForDetail = album
                                    viewModel.fetchActiveCollectionItems(album.id)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // SECTION 2: BOOKMARKED FAVORITES (HORIZONTAL PREVIEWS)
                    SectionTitle(
                        icon = Icons.Outlined.Favorite,
                        text = viewModel.getTranslation("我喜欢的壁纸", "My Favorites"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (favorites.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                    RoundedCornerShape(16.dp)
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = viewModel.getTranslation("暂无喜欢的壁纸，在首页或分类中双击收藏吧", "No favorites yet. Double-tap to add!"),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(favorites, key = { it.id }) { wallpaper ->
                                FavoritePreviewCard(wallpaper = wallpaper) {
                                    onViewDetail(
                                        wallpaper.id,
                                        wallpaper.imageUrl,
                                        wallpaper.authorName,
                                        wallpaper.source
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // SECTION 3: SYSTEM PREFERENCES & CONTROLS
                    SectionTitle(
                        icon = Icons.Filled.Settings,
                        text = viewModel.getTranslation("偏好与系统设置", "System Preferences"),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Language modifier row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleLanguage() }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = "Language",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = viewModel.getTranslation("语言 (Language)", "Language (語言)"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (currentLang == "zh") "简体中文" else "English",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                            // Homepage source setting Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = android.content.Intent(context, btm.m.todaywallpaper.ui.screens.StyleSettingActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = "Theme Set",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = viewModel.getTranslation("首页沉浸壁纸风格", "Homepage Immersive Style"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                val matchedCategory = categories.find { it.key == homeType }
                                val readableHomeType = if (matchedCategory != null) {
                                    if (currentLang == "zh") matchedCategory.zhTitle else matchedCategory.enTitle
                                } else if (homeType.startsWith("collection_")) {
                                    val collId = homeType.removePrefix("collection_").toIntOrNull()
                                    val matchedCollection = collections.find { it.id == collId }
                                    matchedCollection?.name ?: viewModel.getTranslation("自定义图集", "Custom Collection")
                                } else {
                                    when (homeType) {
                                        "PexelsCurated" -> viewModel.getTranslation("Pexels 每日精选", "Pexels Curated Scenery")
                                        "PexelsSpace" -> viewModel.getTranslation("Pexels 极地太空", "Pexels Galactic Space")
                                        "PexelsMinimalist" -> viewModel.getTranslation("Pexels 留白极简", "Pexels Minimal Art")
                                        "PexelsNature" -> viewModel.getTranslation("Pexels 山川地质", "Pexels Natural Planet")
                                        "Nekosia:cute" -> viewModel.getTranslation("Nekosia 治愈萌系", "Nekosia Kawaii Anime")
                                        "Nekosia:girl" -> viewModel.getTranslation("Nekosia 唯美少女", "Nekosia Beauty Girl")
                                        "Nekosia:maid" -> viewModel.getTranslation("Nekosia 优雅女仆", "Nekosia Classic Maid")
                                        "Nekosia:vtuber" -> viewModel.getTranslation("Nekosia 虚拟主播", "Nekosia VTubers")
                                        else -> homeType
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = readableHomeType,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(max = 140.dp),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Arrow",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                            // Liquid Glass Adjustment row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = android.content.Intent(context, btm.m.todaywallpaper.ui.screens.LiquidGlassSettingActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.BlurOn,
                                        contentDescription = "Liquid Glass",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = viewModel.getTranslation("Liquid Glass 调整", "Liquid Glass Adjustment"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val liquidGlassBlurSp = context.getSharedPreferences("app_gallery_prefs", android.content.Context.MODE_PRIVATE)
                                    val currentBlur = liquidGlassBlurSp.getFloat("liquid_glass_blur", 8f)
                                    Text(
                                        text = "${currentBlur.toInt()}dp",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Arrow",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                            // Custom Splash Screen row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = android.content.Intent(context, btm.m.todaywallpaper.ui.screens.SplashSettingActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "Custom Splash",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = viewModel.getTranslation("自定义开屏界面", "Custom Splash Screen"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val splashSp = context.getSharedPreferences("app_gallery_prefs", android.content.Context.MODE_PRIVATE)
                                    val splashModeVal = splashSp.getString("splash_mode", "app_icon") ?: "app_icon"
                                    val modeLabel = when (splashModeVal) {
                                        "app_icon" -> viewModel.getTranslation("默认", "Default")
                                        "select" -> viewModel.getTranslation("已选图", "Selected")
                                        "random" -> viewModel.getTranslation("随机", "Random")
                                        "upload" -> viewModel.getTranslation("已上传", "Uploaded")
                                        else -> viewModel.getTranslation("默认", "Default")
                                    }
                                    Text(
                                        text = modeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (splashModeVal != "app_icon") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Arrow",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                            // Auto Switch Wallpaper row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = android.content.Intent(context, btm.m.todaywallpaper.ui.screens.AutoSwitchWallpaperActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Autorenew,
                                        contentDescription = "Auto Switch",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = viewModel.getTranslation("自动切换桌面壁纸", "Auto Switch Wallpaper"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val autoSwitchSp = context.getSharedPreferences("app_gallery_prefs", android.content.Context.MODE_PRIVATE)
                                    val isAutoSwitchOn = autoSwitchSp.getBoolean("auto_switch_enabled", false)
                                    Text(
                                        text = if (isAutoSwitchOn) viewModel.getTranslation("已开启", "On") else viewModel.getTranslation("已关闭", "Off"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAutoSwitchOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Arrow",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                            // Pexels API Key Config row
                            val pexelsApiKey by viewModel.pexelsApiKey.collectAsState()
                            var showPexelsKeyEditDialog by remember { mutableStateOf(false) }

                            // WebView result launcher for API key capture
                            val webviewResultLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartActivityForResult()
                            ) { result ->
                                if (result.resultCode == android.app.Activity.RESULT_OK) {
                                    val detectedKey = result.data?.getStringExtra("detected_api_key")
                                    if (!detectedKey.isNullOrEmpty()) {
                                        viewModel.updatePexelsApiKey(detectedKey)
                                        Toast.makeText(context, viewModel.getTranslation("API Key 已自动保存！", "API Key saved automatically!"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPexelsKeyEditDialog = true }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.VpnKey,
                                        contentDescription = "Pexels Key",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Pexels API Key",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (pexelsApiKey.isEmpty()) viewModel.getTranslation("未配置 (点击设置)", "Unconfigured (Click to Setup)") else viewModel.getTranslation("已配置", "Configured"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pexelsApiKey.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Arrow",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (showPexelsKeyEditDialog) {
                                var tempKeyInput by remember { mutableStateOf(pexelsApiKey) }
                                AlertDialog(
                                    onDismissRequest = { showPexelsKeyEditDialog = false },
                                    title = {
                                        Text(
                                            text = "Pexels API Key",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    },
                                    text = {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = viewModel.getTranslation(
                                                    "在下方输入您的 Pexels API 密钥以解锁高质量实景风光壁纸库。留空并保存则将其清除：",
                                                    "Enter your Pexels API key below to query scenic landscape backdrops live. Leave empty and save to reset:"
                                                ),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 18.sp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = tempKeyInput,
                                                onValueChange = { tempKeyInput = it },
                                                placeholder = { Text("Pexels API Key...") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().testTag("mine_pexels_api_key_field"),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            // Register API Key via WebView button
                                            OutlinedButton(
                                                onClick = {
                                                    showPexelsKeyEditDialog = false
                                                    val intent = android.content.Intent(context, PexelsWebViewActivity::class.java)
                                                    webviewResultLauncher.launch(intent)
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.OpenInBrowser,
                                                    contentDescription = "Register",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = viewModel.getTranslation("还没有 API Key？点击注册", "Don't have an API Key? Register here"),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = viewModel.getTranslation("打开 Pexels API 页面，注册后自动获取", "Open Pexels API page, key will be auto-captured"),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.updatePexelsApiKey(tempKeyInput.trim())
                                                showPexelsKeyEditDialog = false
                                            },
                                            modifier = Modifier.testTag("mine_save_key_button")
                                        ) {
                                            Text(text = viewModel.getTranslation("保存配置", "Save"))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showPexelsKeyEditDialog = false }) {
                                            Text(text = viewModel.getTranslation("取消", "Cancel"))
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 4: ABOUT ENTRY
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { showAboutPage = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = viewModel.getTranslation("关于", "About"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    text = viewModel.getTranslation("项目介绍、链接与设备信息", "Project info, links & device details"),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(98.dp))
                }
                    } else {
                        // Albums Details Subpanel screen
                        CollectionDetailsView(
                            collection = activeCollection,
                            items = activeCollectionItems,
                            viewModel = viewModel,
                            backProgress = collectionBackProgress,
                            isBackSwiping = isCollectionBackSwiping,
                            onBack = { selectedCollectionForDetail = null },
                            onViewDetail = onViewDetail
                        )
                    }
                }

                if (showCreateCollectionDialog) {
                    CreateCollectionDialog(
                        viewModel = viewModel,
                        onDismiss = { showCreateCollectionDialog = false },
                        onConfirm = { name, desc, selectedUris ->
                            viewModel.createNewCollection(name, desc) { collectionId ->
                                if (selectedUris.isNotEmpty()) {
                                    // Batch upload selected images
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                        selectedUris.forEachIndexed { index, uri ->
                                            try {
                                                val stream = context.contentResolver.openInputStream(uri)
                                                if (stream != null) {
                                                    val folder = java.io.File(context.filesDir, "local_collections")
                                                    if (!folder.exists()) folder.mkdirs()
                                                    val photoId = "local_${System.currentTimeMillis()}_$index"
                                                    val file = java.io.File(folder, "img_${photoId}.jpg")
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                        java.io.FileOutputStream(file).use { out ->
                                                            stream.use { input -> input.copyTo(out) }
                                                        }
                                                    }
                                                    val localPath = "file://${file.absolutePath}"
                                                    val wallpaper = btm.m.todaywallpaper.ui.viewmodel.UnifiedWallpaper(
                                                        id = photoId,
                                                        imageUrl = localPath,
                                                        thumbnailUrl = localPath,
                                                        author = null,
                                                        authorUrl = "",
                                                        source = "Local Gallery",
                                                        category = null
                                                    )
                                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        viewModel.addWallpaperToCollectionId(collectionId, wallpaper)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Skip failed images silently
                                            }
                                        }
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            showCreateCollectionDialog = false
                                            Toast.makeText(context, viewModel.getTranslation("图集创建成功！已添加 ${selectedUris.size} 张图片", "Album created! Added ${selectedUris.size} images"), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    showCreateCollectionDialog = false
                                    Toast.makeText(context, viewModel.getTranslation("图集创建成功！", "Album created successfully!"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                if (showEditProfileDialog) {
                    var inputUsername by remember { mutableStateOf(currentUsername) }
                    var inputSubtitle by remember { mutableStateOf(currentProfileSubtitle) }

                    AlertDialog(
                        onDismissRequest = { showEditProfileDialog = false },
                        title = {
                            Text(
                                text = viewModel.getTranslation("自定义个人档案", "Custom Profile"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                        .clickable { avatarPickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (tempAvatarUrlForDialog.isNotEmpty()) {
                                        AsyncImage(
                                            model = tempAvatarUrlForDialog,
                                            contentDescription = "Avatar Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = "Select photo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.45f))
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = viewModel.getTranslation("点击上传", "Upload"),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = viewModel.getTranslation("提示：可点击上方方框上传本地相册图片", "Click the box above to upload from your gallery"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )

                                OutlinedTextField(
                                    value = inputUsername,
                                    onValueChange = { inputUsername = it },
                                    label = { Text(text = viewModel.getTranslation("用户名", "Username")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = tempAvatarUrlForDialog,
                                    onValueChange = { tempAvatarUrlForDialog = it },
                                    label = { Text(text = viewModel.getTranslation("自定义头像链接 / 本地路径", "Avatar Image URL / Local path")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = inputSubtitle,
                                    onValueChange = { inputSubtitle = it },
                                    label = { Text(text = viewModel.getTranslation("个性签名", "Custom Subtitle")) },
                                    placeholder = { Text(text = viewModel.getTranslation("每一次收藏，皆是对美学的赞美", "Each bookmark praises outstanding aesthetics")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (inputUsername.isNotBlank()) {
                                        viewModel.updateUsername(inputUsername.trim())
                                    }
                                    viewModel.updateAvatar(tempAvatarUrlForDialog.trim().ifEmpty { null })
                                    viewModel.updateProfileSubtitle(inputSubtitle.trim())
                                    showEditProfileDialog = false
                                }
                            ) {
                                Text(text = viewModel.getTranslation("保存", "Save"))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditProfileDialog = false }) {
                                Text(text = viewModel.getTranslation("取消", "Cancel"))
                            }
                        }
                    )
                }
        }
    }

}



}



@Composable
fun AlbumCardItem(
    album: WallpaperCollection,
    viewModel: WallpaperViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(130.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("album_card_${album.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (album.coverUrl != null) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }

            // Dark subtle overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Collection Title info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = album.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                
                if (!album.description.isNullOrEmpty()) {
                    Text(
                        text = album.description,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritePreviewCard(
    wallpaper: FavoriteWallpaper,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag("fav_item_${wallpaper.id}"),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = "Fav picture preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // minimal bottom text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = wallpaper.source,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionDetailsView(
    collection: WallpaperCollection,
    items: List<CollectionItem>,
    viewModel: WallpaperViewModel,
    backProgress: Float = 0f,
    isBackSwiping: Boolean = false,
    onBack: () -> Unit,
    onViewDetail: (String, String, String?, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDeleteConfirmationVisible by remember { mutableStateOf(false) }

    var editItemDialogData by remember { mutableStateOf<CollectionItem?>(null) }
    var editItemTitle by remember { mutableStateOf("") }
    var editItemSource by remember { mutableStateOf("") }

    // Long-press action dialog state (delete / set as cover)
    var longPressActionItem by remember { mutableStateOf<CollectionItem?>(null) }

    // Cover picker: set a custom cover from local storage
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val folder = java.io.File(context.filesDir, "local_collections")
                        if (!folder.exists()) folder.mkdirs()
                        val coverId = "cover_${System.currentTimeMillis()}"
                        val file = java.io.File(folder, "img_${coverId}.jpg")
                        withContext(Dispatchers.IO) {
                            java.io.FileOutputStream(file).use { out ->
                                stream.use { input -> input.copyTo(out) }
                            }
                        }
                        val localPath = "file://${file.absolutePath}"
                        viewModel.updateCollectionCover(collection.id, localPath)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, viewModel.getTranslation("封面已更新！", "Cover updated!"), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, viewModel.getTranslation("封面设置失败", "Failed to set cover"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        val folder = java.io.File(context.filesDir, "local_collections")
                        if (!folder.exists()) {
                            folder.mkdirs()
                        }
                        val photoId = "local_${System.currentTimeMillis()}"
                        val file = java.io.File(folder, "img_${photoId}.jpg")
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            java.io.FileOutputStream(file).use { out ->
                                stream.use { input ->
                                    input.copyTo(out)
                                }
                            }
                        }
                        val localPath = "file://${file.absolutePath}"
                        // Show edit dialog before saving
                        editItemTitle = ""
                        editItemSource = ""
                        editItemDialogData = CollectionItem(
                            id = 0, // temp, will be auto-generated
                            collectionId = collection.id,
                            wallpaperId = photoId,
                            imageUrl = localPath,
                            thumbnailUrl = localPath,
                            authorName = null,
                            source = "Local Gallery",
                            addedAt = System.currentTimeMillis()
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, viewModel.getTranslation("图片上传失败", "Failed to upload image"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
                .graphicsLayer(
                scaleX = 1f - (backProgress * 0.08f),
                scaleY = 1f - (backProgress * 0.08f),
                translationX = with(androidx.compose.ui.platform.LocalDensity.current) { (backProgress * 120).dp.toPx() },
                alpha = 1f - (backProgress * 0.2f),
                clip = backProgress > 0f,
                shape = RoundedCornerShape((backProgress * 24).dp)
            )
            .statusBarsPadding()
    ) {
        // Sub-panel Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("album_detail_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${items.size} ${viewModel.getTranslation("张壁纸", "wallpapers")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Set custom cover button
            IconButton(
                onClick = { coverPickerLauncher.launch("image/*") },
                modifier = Modifier.testTag("set_cover_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Set Cover",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Upload local photo button
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.testTag("upload_photo_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Upload Photo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Option to delete this full collection
            IconButton(
                onClick = { isDeleteConfirmationVisible = true },
                modifier = Modifier.testTag("delete_album_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Album",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Collections,
                    contentDescription = "Empty collection",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = viewModel.getTranslation("图集为空，您可以点击下方按钮上传本地图片，或者在大图页面点击添加", "Album is empty. You can upload local images, or add them inside preview!"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.getTranslation("上传本地图片", "Upload Local Image"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = "Album wallpaper preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Click to view detail, long press to show action dialog
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .combinedClickable(
                                        onClick = {
                                            onViewDetail(
                                                item.wallpaperId,
                                                item.imageUrl,
                                                item.authorName,
                                                item.source
                                            )
                                        },
                                        onLongClick = {
                                            longPressActionItem = item
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    // Long-press action dialog: delete from album / set as cover
    longPressActionItem?.let { actionItem ->
        AlertDialog(
            onDismissRequest = { longPressActionItem = null },
            title = {
                Text(
                    text = viewModel.getTranslation("图片操作", "Image Actions"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Set as album cover option
                    Surface(
                        onClick = {
                            viewModel.updateCollectionCover(collection.id, actionItem.imageUrl)
                            longPressActionItem = null
                            Toast.makeText(context, viewModel.getTranslation("已设为图集封面！", "Set as album cover!"), Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = "Set as Cover",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = viewModel.getTranslation("设置为图集封面", "Set as Album Cover"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = viewModel.getTranslation("此图片将作为图集封面显示", "This image will be used as the album cover"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Delete from album option
                    Surface(
                        onClick = {
                            viewModel.removeWallpaperFromCollection(collection.id, actionItem.wallpaperId)
                            viewModel.fetchActiveCollectionItems(collection.id)
                            longPressActionItem = null
                            Toast.makeText(context, viewModel.getTranslation("图片已移出图集！", "Image removed from album!"), Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = viewModel.getTranslation("从图集中删除", "Remove from Album"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = viewModel.getTranslation("此图片将从当前图集移除", "This image will be removed from the album"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressActionItem = null }) {
                    Text(viewModel.getTranslation("取消", "Cancel"))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Edit item metadata dialog (for upload-time edit only)
    editItemDialogData?.let { editItem ->
        AlertDialog(
            onDismissRequest = { editItemDialogData = null },
            title = {
                Text(
                    text = viewModel.getTranslation("编辑图片信息", "Edit Image Info"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = viewModel.getTranslation("为本地上传的图片添加标题和来源信息：", "Add title and source for this local image:"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editItemTitle,
                        onValueChange = { editItemTitle = it },
                        label = { Text(viewModel.getTranslation("图片标题 / 作者", "Image Title / Author")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editItemSource,
                        onValueChange = { editItemSource = it },
                        label = { Text(viewModel.getTranslation("来源渠道", "Source")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalAuthor = editItemTitle.trim().ifEmpty { null }
                        val finalSource = editItemSource.trim().ifEmpty { "Local Gallery" }
                        if (editItem.id == 0) {
                            // New upload - save to collection
                            val wallpaper = btm.m.todaywallpaper.ui.viewmodel.UnifiedWallpaper(
                                id = editItem.wallpaperId,
                                imageUrl = editItem.imageUrl,
                                thumbnailUrl = editItem.thumbnailUrl,
                                author = finalAuthor,
                                authorUrl = "",
                                source = finalSource,
                                category = null
                            )
                            viewModel.addWallpaperToCollectionId(editItem.collectionId, wallpaper)
                            Toast.makeText(context, viewModel.getTranslation("图片上传成功！", "Image uploaded successfully!"), Toast.LENGTH_SHORT).show()
                        } else {
                            // Existing item - update metadata
                            viewModel.updateCollectionItemMeta(editItem.id, finalAuthor, finalSource)
                            viewModel.fetchActiveCollectionItems(editItem.collectionId)
                            Toast.makeText(context, viewModel.getTranslation("信息已更新！", "Info updated!"), Toast.LENGTH_SHORT).show()
                        }
                        editItemDialogData = null
                    }
                ) {
                    Text(viewModel.getTranslation("保存", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editItemDialogData = null }) {
                    Text(viewModel.getTranslation("取消", "Cancel"))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Modal dialogue to confirm album deletion
    if (isDeleteConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isDeleteConfirmationVisible = false },
            title = { Text(text = viewModel.getTranslation("确定删除此图集吗？", "Delete Album?")) },
            text = { Text(text = viewModel.getTranslation("在删除后，自定义图集以及包含的所有关系都将消失。", "Deleting this album is permanent.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCollectionId(collection.id)
                        isDeleteConfirmationVisible = false
                        onBack()
                        Toast.makeText(context, viewModel.getTranslation("图集已删除！", "Album deleted!"), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = viewModel.getTranslation("彻底删除", "Delete"), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteConfirmationVisible = false }) {
                    Text(text = viewModel.getTranslation("取消", "Cancel"))
                }
            }
        )
    }
}

@Composable
fun CreateCollectionDialog(
    viewModel: WallpaperViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, List<Uri>) -> Unit
) {
    var collectionName by remember { mutableStateOf("") }
    var collectionDesc by remember { mutableStateOf("") }
    val selectedUris = remember { mutableStateListOf<Uri>() }
    val context = LocalContext.current

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedUris.clear()
        selectedUris.addAll(uris)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = viewModel.getTranslation("创建新壁纸图集", "Create New Album"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = { collectionName = it },
                    label = { Text(text = viewModel.getTranslation("图集名称 (必填)", "Album Name *")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_album_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = collectionDesc,
                    onValueChange = { collectionDesc = it },
                    label = { Text(text = viewModel.getTranslation("图集描述 (选填)", "Description (Optional)")) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Batch image upload section
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                Text(
                    text = viewModel.getTranslation("批量添加图片（可选）", "Batch add images (optional)"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedButton(
                    onClick = { multiImagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedUris.isEmpty()) {
                            viewModel.getTranslation("选择本地图片", "Select local images")
                        } else {
                            viewModel.getTranslation("已选择 ${selectedUris.size} 张图片", "${selectedUris.size} images selected")
                        }
                    )
                }

                // Show selected image count if any
                if (selectedUris.isNotEmpty()) {
                    Text(
                        text = viewModel.getTranslation(
                            "已选择 ${selectedUris.size} 张图片，创建后将自动添加到图集中",
                            "${selectedUris.size} images will be added to the album after creation"
                        ),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (collectionName.isNotBlank()) {
                        onConfirm(collectionName, collectionDesc.ifBlank { null }, selectedUris.toList())
                    }
                },
                enabled = collectionName.isNotBlank(),
                modifier = Modifier.testTag("dialog_album_confirm_btn")
            ) {
                Text(text = viewModel.getTranslation("创建", "Create"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = viewModel.getTranslation("取消", "Cancel"))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}


