package btm.m.todaywallpaper.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import btm.m.todaywallpaper.data.model.FavoriteWallpaper
import btm.m.todaywallpaper.data.model.WallpaperCollection
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

class SplashSettingActivity : ComponentActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SplashSettingScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun SplashSettingScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang by viewModel.language.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val collections by viewModel.collections.collectAsState()

    // Splash preference states
    // Modes: "app_icon", "select", "random", "upload"
    var splashMode by remember { mutableStateOf("app_icon") }
    var selectedSource by remember { mutableStateOf("favorites") } // for "select" and "random"
    var selectedImageUrl by remember { mutableStateOf("") } // for "select" mode - chosen image URL
    var selectedImageId by remember { mutableStateOf("") } // for "select" mode - chosen image ID
    var uploadedImagePath by remember { mutableStateOf("") } // for "upload" mode

    // Load saved preferences
    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        splashMode = sp.getString("splash_mode", "app_icon") ?: "app_icon"
        selectedSource = sp.getString("splash_source", "favorites") ?: "favorites"
        selectedImageUrl = sp.getString("splash_selected_url", "") ?: ""
        selectedImageId = sp.getString("splash_selected_id", "") ?: ""
        uploadedImagePath = sp.getString("splash_upload_path", "") ?: ""
    }

    fun savePreferences() {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("splash_mode", splashMode)
            .putString("splash_source", selectedSource)
            .putString("splash_selected_url", selectedImageUrl)
            .putString("splash_selected_id", selectedImageId)
            .putString("splash_upload_path", uploadedImagePath)
            .apply()
    }

    // Gallery picker for upload
    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    // Delete old upload
                    context.filesDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("splash_upload_")) {
                            file.delete()
                        }
                    }
                    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                    val file = java.io.File(context.filesDir, "splash_upload_$timeStamp.jpg")
                    java.io.FileOutputStream(file).use { output ->
                        inputStream.use { input ->
                            input.copyTo(output)
                        }
                    }
                    uploadedImagePath = "file://${file.absolutePath}"
                    splashMode = "upload"
                    savePreferences()
                    Toast.makeText(context, viewModel.getTranslation("开屏图已保存", "Splash image saved"), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, viewModel.getTranslation("图片加载失败", "Failed to load image"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Source items for browse (favorites list for specific selection)
    var browseItems by remember { mutableStateOf<List<FavoriteWallpaper>>(emptyList()) }
    var showBrowseDialog by remember { mutableStateOf(false) }
    var browseCollectionItems by remember { mutableStateOf<List<btm.m.todaywallpaper.data.model.CollectionItem>>(emptyList()) }

    // Status bar styling
    val view = androidx.compose.ui.platform.LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(darkTheme) {
        val window = (view.context as? Activity)?.window
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
    val alpha = 1f - (backProgress * 0.2f)
    val cornerRadius = (backProgress * 24).dp

    // Built-in options (same as StyleSettingActivity)
    val basicOptions = remember {
        listOf(
            "PexelsCurated" to ("Pexels 山川每日精选" to "Pexels Curated Scenery"),
            "PexelsSpace" to ("Pexels 浩瀚太空星际" to "Pexels Galactic Space"),
            "PexelsMinimalist" to ("Pexels 优雅留白极简" to "Pexels Minimal Art"),
            "PexelsNature" to ("Pexels 壮丽山川自然" to "Pexels Natural Planet"),
            "Nekosia:cute" to ("Nekosia 萌系治愈二次元" to "Nekosia Kawaii Cute"),
            "Nekosia:girl" to ("Nekosia 唯美二次元少女" to "Nekosia Beauty Girl"),
            "Nekosia:maid" to ("Nekosia 黑白经典女仆" to "Nekosia Classic Maid"),
            "Nekosia:vtuber" to ("Nekosia 虚拟次元偶像" to "Nekosia VTubers")
        )
    }

    // Source name helper
    @Composable
    fun getSourceName(source: String): String {
        return when {
            source == "favorites" -> viewModel.getTranslation("喜欢的壁纸", "Favorites")
            source.startsWith("category_") -> {
                val key = source.removePrefix("category_")
                val cat = categories.find { it.key == key }
                if (cat != null) {
                    if (currentLang == "zh") cat.zhTitle else cat.enTitle
                } else key
            }
            source.startsWith("collection_") -> {
                val id = source.removePrefix("collection_").toIntOrNull()
                val coll = collections.find { it.id == id }
                coll?.name ?: source
            }
            else -> source
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
                    translationX = with(androidx.compose.ui.platform.LocalDensity.current) { translationXDp.toPx() },
                    alpha = alpha,
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
                        text = viewModel.getTranslation("自定义开屏界面", "Custom Splash Screen"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // ===== Mode Selection =====
                    Text(
                        text = viewModel.getTranslation("开屏模式", "Splash Mode"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // 1. App Icon (default)
                            SplashModeRow(
                                icon = Icons.Default.PhoneAndroid,
                                title = viewModel.getTranslation("应用图标（默认）", "App Icon (Default)"),
                                subtitle = viewModel.getTranslation("显示应用启动图标", "Show app launch icon"),
                                isSelected = splashMode == "app_icon",
                                onClick = {
                                    splashMode = "app_icon"
                                    savePreferences()
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )

                            // 2. Select specific image
                            SplashModeRow(
                                icon = Icons.Default.Image,
                                title = viewModel.getTranslation("选择开屏图", "Select Splash Image"),
                                subtitle = viewModel.getTranslation("从喜欢/分类/图集中选择一张", "Pick one from favorites/categories/collections"),
                                isSelected = splashMode == "select",
                                onClick = {
                                    splashMode = "select"
                                    savePreferences()
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )

                            // 3. Random from source
                            SplashModeRow(
                                icon = Icons.Default.Shuffle,
                                title = viewModel.getTranslation("随机开屏图", "Random Splash Image"),
                                subtitle = viewModel.getTranslation("每次启动随机展示一张", "Show a random image each launch"),
                                isSelected = splashMode == "random",
                                onClick = {
                                    splashMode = "random"
                                    savePreferences()
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )

                            // 4. Upload
                            SplashModeRow(
                                icon = Icons.Default.Upload,
                                title = viewModel.getTranslation("上传开屏图", "Upload Splash Image"),
                                subtitle = viewModel.getTranslation("从相册选择自定义图片", "Choose an image from gallery"),
                                isSelected = splashMode == "upload",
                                onClick = {
                                    uploadLauncher.launch("image/*")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ===== Source Selection (for select and random modes) =====
                    if (splashMode == "select" || splashMode == "random") {
                        Text(
                            text = viewModel.getTranslation("图片来源", "Image Source"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

                        // Favorites source
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                StyleRowItem(
                                    title = viewModel.getTranslation("喜欢的壁纸", "Favorites"),
                                    isSelected = selectedSource == "favorites",
                                    onClick = {
                                        selectedSource = "favorites"
                                        selectedImageUrl = ""
                                        selectedImageId = ""
                                        savePreferences()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Built-in categories
                        Text(
                            text = viewModel.getTranslation("内置分类", "Built-in Categories"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                val builtinCategories = listOf(
                                    "nature" to ("山海自然" to "Mountain & Nature"),
                                    "space" to ("浩瀚星空" to "Cosmic Space"),
                                    "urban" to ("都市霓虹" to "Urban Streets"),
                                    "minimalist" to ("极简主义" to "Minimal Space"),
                                    "nekosia_cute" to ("可爱插画" to "Kawaii Cutie"),
                                    "nekosia_girl" to ("唯美少女" to "Anime Girl"),
                                    "nekosia_maid" to ("优雅女仆" to "Classic Maid"),
                                    "nekosia_vtuber" to ("虚拟直播" to "VTuber Universe")
                                )
                                builtinCategories.forEachIndexed { index, (key, titlePair) ->
                                    val title = if (currentLang == "zh") titlePair.first else titlePair.second
                                    val sourceKey = "category_$key"
                                    StyleRowItem(
                                        title = title,
                                        isSelected = selectedSource == sourceKey,
                                        onClick = {
                                            selectedSource = sourceKey
                                            selectedImageUrl = ""
                                            selectedImageId = ""
                                            savePreferences()
                                        }
                                    )
                                    if (index < builtinCategories.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom categories
                        val customItems = categories.filter { it.key.startsWith("custom_pexels_") }
                        if (customItems.isNotEmpty()) {
                            Text(
                                text = viewModel.getTranslation("自定义分类", "Custom Categories"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                ) {
                                    customItems.forEachIndexed { index, item ->
                                        val name = if (currentLang == "zh") item.zhTitle else item.enTitle
                                        val sourceKey = "category_${item.key}"
                                        StyleRowItem(
                                            title = name,
                                            isSelected = selectedSource == sourceKey,
                                            onClick = {
                                                selectedSource = sourceKey
                                                selectedImageUrl = ""
                                                selectedImageId = ""
                                                savePreferences()
                                            }
                                        )
                                        if (index < customItems.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Collections
                        Text(
                            text = viewModel.getTranslation("自定义图集", "Custom Collections"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                if (collections.isEmpty()) {
                                    Text(
                                        text = viewModel.getTranslation("暂无自定义图集", "No Custom Collections"),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp)
                                    )
                                } else {
                                    collections.forEachIndexed { index, item ->
                                        val sourceKey = "collection_${item.id}"
                                        StyleRowItem(
                                            title = item.name,
                                            isSelected = selectedSource == sourceKey,
                                            onClick = {
                                                selectedSource = sourceKey
                                                selectedImageUrl = ""
                                                selectedImageId = ""
                                                savePreferences()
                                            }
                                        )
                                        if (index < collections.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // For "select" mode: show browse button to pick specific image
                        if (splashMode == "select") {
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = viewModel.getTranslation("选择具体图片", "Choose Specific Image"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                            )

                            // Show preview of selected image
                            if (selectedImageUrl.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = selectedImageUrl,
                                            contentDescription = "Selected splash",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(32.dp)
                                                .background(
                                                    Color(0xFF007AFF),
                                                    RoundedCornerShape(16.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Button(
                                onClick = {
                                    // Browse the selected source for images
                                    if (selectedSource == "favorites") {
                                        browseItems = favorites
                                        browseCollectionItems = emptyList()
                                    } else if (selectedSource.startsWith("collection_")) {
                                        val collId = selectedSource.removePrefix("collection_").toIntOrNull()
                                        if (collId != null) {
                                            viewModel.fetchActiveCollectionItems(collId)
                                            browseCollectionItems = viewModel.activeCollectionItems.value
                                            browseItems = emptyList()
                                        }
                                    }
                                    showBrowseDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Landscape,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedImageUrl.isEmpty()) {
                                        viewModel.getTranslation("浏览并选择图片", "Browse & Select Image")
                                    } else {
                                        viewModel.getTranslation("重新选择图片", "Reselect Image")
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ===== Upload mode preview =====
                    if (splashMode == "upload") {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uploadedImagePath.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = uploadedImagePath,
                                        contentDescription = "Uploaded splash",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { uploadLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uploadedImagePath.isEmpty()) {
                                    viewModel.getTranslation("上传开屏图", "Upload Splash Image")
                                } else {
                                    viewModel.getTranslation("重新上传", "Re-upload")
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ===== Current preview =====
                    if (splashMode == "app_icon") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = viewModel.getTranslation("预览", "Preview"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "TodayWallpaper",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        // Browse dialog for selecting specific images
        if (showBrowseDialog) {
            AlertDialog(
                onDismissRequest = { showBrowseDialog = false },
                title = {
                    Text(
                        text = viewModel.getTranslation("选择开屏图", "Select Splash Image"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (selectedSource == "favorites") {
                            if (favorites.isEmpty()) {
                                Text(
                                    text = viewModel.getTranslation("暂无喜欢的壁纸", "No favorites yet"),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                favorites.forEach { fav ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedImageUrl = fav.imageUrl
                                                selectedImageId = fav.id
                                                savePreferences()
                                                showBrowseDialog = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = fav.thumbnailUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fav.source,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (fav.authorName != null) {
                                                Text(
                                                    text = fav.authorName,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                        if (selectedImageId == fav.id) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (selectedSource.startsWith("collection_")) {
                            if (browseCollectionItems.isEmpty()) {
                                Text(
                                    text = viewModel.getTranslation("该图集为空", "This collection is empty"),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                browseCollectionItems.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedImageUrl = item.imageUrl
                                                selectedImageId = item.wallpaperId
                                                savePreferences()
                                                showBrowseDialog = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = item.thumbnailUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.source,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (item.authorName != null) {
                                                Text(
                                                    text = item.authorName,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                        if (selectedImageId == item.wallpaperId) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Category source - cannot browse specific images, inform user
                            Text(
                                text = viewModel.getTranslation(
                                    "分类来源仅支持随机模式，如需选择具体图片请从「喜欢」或「图集」中选择",
                                    "Category sources only support random mode. Select from 'Favorites' or 'Collections' to pick a specific image."
                                ),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBrowseDialog = false }) {
                        Text(viewModel.getTranslation("关闭", "Close"))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
private fun SplashModeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}