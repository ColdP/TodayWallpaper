package btm.m.todaywallpaper.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.theme.isAppDarkTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.widget.enableMomentumTransparentWindow
import btm.m.todaywallpaper.ui.widget.momentumBackTransform

class StyleSettingActivity : ComponentActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableMomentumTransparentWindow()
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                StyleSettingScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun StyleSettingScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit
) {
    val homeType by viewModel.homeWallpaperType.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val currentLang by viewModel.language.collectAsState()

    val view = androidx.compose.ui.platform.LocalView.current
    val darkTheme = isAppDarkTheme()
    DisposableEffect(darkTheme) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {}
    }

    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val predictiveBackMaxProgress by viewModel.predictiveBackMaxProgress.collectAsState()
    var backProgress by remember { mutableStateOf(0f) }
    var backDirection by remember { mutableStateOf(1f) }
    var isBackSwiping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        backProgress = 0f
        isBackSwiping = false
    }

    androidx.activity.compose.PredictiveBackHandler(enabled = predictiveBackEnabled) { progressFlow ->
        try {
            isBackSwiping = true
            progressFlow.collect { backEvent ->
                backProgress = kotlin.math.min(backEvent.progress, predictiveBackMaxProgress / 100f)
                backDirection = if (backEvent.swipeEdge == androidx.activity.BackEventCompat.EDGE_RIGHT) -1f else 1f
            }
            isBackSwiping = false
            backProgress = 1f
            onBack()
        } catch (e: Exception) {
            isBackSwiping = false
            backProgress = 0f
        }
    }

    val basicOptions = remember {
        listOf(
            "PexelsCurated" to ("Pexels 山川每日精选" to "Pexels Curated Scenery"),
            "PexelsSpace" to ("Pexels 浩瀚太空星际" to "Pexels Galactic Space"),
            "PexelsMinimalist" to ("Pexels 优雅留白极简" to "Pexels Minimal Art"),
            "PexelsNature" to ("Pexels 壮丽山川自然" to "Pexels Natural Planet"),
            "Wallhaven" to ("Wallhaven 综合精选" to "Wallhaven Collection"),
            "Nekosia:cute" to ("Nekosia 萌系治愈二次元" to "Nekosia Kawaii Cute"),
            "Nekosia:girl" to ("Nekosia 唯美二次元少女" to "Nekosia Beauty Girl"),
            "Nekosia:maid" to ("Nekosia 黑白经典女仆" to "Nekosia Classic Maid"),
            "Nekosia:vtuber" to ("Nekosia 虚拟次元偶像" to "Nekosia VTubers")
        )
    }

    val scale = 1f - (backProgress * 0.12f)
    val translationXDp = (backProgress * 48f * backDirection).dp
    val alpha = 1f
    val cornerRadius = 28.dp * backProgress

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .momentumBackTransform(backProgress, backDirection),
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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = viewModel.getTranslation("首页沉浸式壁纸风格", "Immersive Style"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Scrollable Options List
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // Predefined styles card
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
                            basicOptions.forEach { (key, titlePair) ->
                                val title = if (currentLang == "zh") titlePair.first else titlePair.second
                                StyleRowItem(
                                    title = title,
                                    isSelected = homeType == key,
                                    onClick = {
                                        viewModel.setHomeWallpaperType(key)
                                            btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.mainViewModelInstance?.setHomeWallpaperType(key)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Custom Categories section
                    Text(
                        text = viewModel.getTranslation("自定义分类", "Custom Categories"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )

                    // Custom categories list card
                    val customItems = categories.filter { it.key.startsWith("custom_pexels_") }
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
                            if (customItems.isEmpty()) {
                                Text(
                                    text = viewModel.getTranslation("暂无自定义分类", "No Custom Categories"),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                )
                            } else {
                                customItems.forEach { item ->
                                    val name = if (currentLang == "zh") item.zhTitle else item.enTitle
                                    StyleRowItem(
                                        title = name,
                                        isSelected = homeType == item.key,
                                        onClick = {
                                            viewModel.setHomeWallpaperType(item.key)
                                            btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.mainViewModelInstance?.setHomeWallpaperType(item.key)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Custom Collections section (自定义图集)
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
                                collections.forEach { item ->
                                    StyleRowItem(
                                        title = item.name,
                                        isSelected = homeType == "collection_${item.id}",
                                        onClick = {
                                            viewModel.setHomeWallpaperType("collection_${item.id}")
                                            btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.mainViewModelInstance?.setHomeWallpaperType("collection_${item.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun StyleRowItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
