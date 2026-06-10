package btm.m.todaywallpaper

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.screens.CategoriesScreen
import btm.m.todaywallpaper.ui.screens.HomeScreen
import btm.m.todaywallpaper.ui.screens.MineScreen
import btm.m.todaywallpaper.ui.screens.WallpaperDetailViewer
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.viewmodel.Screen
import btm.m.todaywallpaper.ui.viewmodel.DetailWallpaperData

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

class MainActivity : ComponentActivity() {
    
    companion object {
        var contentComposeViewRef: android.view.ViewGroup? = null

        fun isGlassInitialized(view: com.qmdeve.liquidglass.widget.LiquidGlassView): Boolean {
            return true
        }

        @JvmStatic
        fun safeConfigure(
            view: com.qmdeve.liquidglass.widget.LiquidGlassView,
            red: Float,
            green: Float,
            blue: Float,
            alpha: Float,
            cornerRadius: Float,
            blurRadius: Float,
            refractionHeight: Float = 12f
        ) {
            try {
                view.setTintColorRed(red)
                view.setTintColorGreen(green)
                view.setTintColorBlue(blue)
                view.setTintAlpha(alpha)
                view.setCornerRadius(cornerRadius)
                view.setBlurRadius(blurRadius)
                view.setRefractionHeight(refractionHeight)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.mainViewModelInstance = viewModel
        
        // Command edge-to-edge system draw behavior
        enableEdgeToEdge()
        
        // Root container FrameLayout that hosts all full-screen elements
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Sibling A: Main Scrollable Content Compose View (rendered at the bottom)
        val contentComposeView = ComposeView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        contentComposeViewRef = contentComposeView

        // 2. Sibling B: Native Liquid Glass Backdrop Blur View overlay behind Navigation
        val glassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0) // Will be resized dynamically by Navigation container globally positioned callback
            isClickable = false
            isFocusable = false
        }

        // Sibling B2: Native Liquid Glass Backdrop Blur View specifically for Detail View Action Card background
        val detailGlassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0)
            isClickable = false
            isFocusable = false
        }

        // 3. Sibling C: Navigation Bar Compose View (drawn transparently on top of background glass)
        val navigationComposeView = ComposeView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Maintain the perfect layer order: content is bottom-most, then glass blurs, then top navigation and details foreground controls
        rootLayout.addView(contentComposeView)
        rootLayout.addView(detailGlassView)
        rootLayout.addView(glassView)
        rootLayout.addView(navigationComposeView)

        setContentView(rootLayout)

        // Set the compose contents respectively
        contentComposeView.setContent {
            MyApplicationTheme {
                MainContentContainer(viewModel = viewModel, detailGlassView = detailGlassView)
            }
        }

        navigationComposeView.setContent {
            MyApplicationTheme {
                MainNavigationContainer(
                    viewModel = viewModel,
                    glassView = glassView,
                    detailGlassView = detailGlassView
                )
            }
        }

        // Bind the sampling source of LiquidGlassView correctly to contentComposeView which contains all screen assets and wallpapers
        glassView.post {
            try {
                glassView.bind(contentComposeView)
                glassView.requestLayout()
                glassView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        detailGlassView.post {
            try {
                detailGlassView.bind(contentComposeView)
                detailGlassView.requestLayout()
                detailGlassView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.mainViewModelInstance == viewModel) {
            btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.mainViewModelInstance = null
        }
    }
}

@Composable
fun MainContentContainer(
    viewModel: WallpaperViewModel,
    detailGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView
) {
    val activeScreen by viewModel.activeScreen.collectAsState()
    val detailWallpaper by viewModel.detailWallpaper.collectAsState()

    LaunchedEffect(detailWallpaper) {
        if (detailWallpaper == null) {
            viewModel.resetDetailBackGesture()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Crossfade transition between screens
        Crossfade(
            targetState = activeScreen,
            label = "ParentScreenCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                Screen.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onViewDetail = { id, url, author, api ->
                            viewModel.setDetailWallpaper(DetailWallpaperData(id, url, author, api))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Screen.Categories -> {
                    CategoriesScreen(
                        viewModel = viewModel,
                        onViewDetail = { id, url, author, api ->
                            viewModel.setDetailWallpaper(DetailWallpaperData(id, url, author, api))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Screen.Mine -> {
                    MineScreen(
                        viewModel = viewModel,
                        onViewDetail = { id, url, author, api ->
                            viewModel.setDetailWallpaper(DetailWallpaperData(id, url, author, api))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Animated full screen overlay detail view container (Background aspect only)
        AnimatedVisibility(
            visible = detailWallpaper != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val wallpaper = detailWallpaper
            if (wallpaper != null) {
                WallpaperDetailViewer(
                    wallpaperId = wallpaper.id,
                    imageUrl = wallpaper.imageUrl,
                    authorName = wallpaper.author,
                    source = wallpaper.source,
                    viewModel = viewModel,
                    detailGlassView = null,
                    renderBackgroundOnly = true,
                    onBack = { viewModel.setDetailWallpaper(null) }
                )
            }
        }

        // Global Pexels API Key missing warning prompt
        val showPexelsApiKeyPrompt by viewModel.showApiKeyPrompt.collectAsState()
        if (showPexelsApiKeyPrompt) {
            var inputKey by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.dismissApiKeyPrompt(proceedAnyway = false) },
                title = {
                    Text(
                        text = viewModel.getTranslation("Pexels API Key 设置", "Pexels API Key Setup"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = viewModel.getTranslation(
                                "本应用使用 Pexels 获取高清自然与星空摄影壁纸。当前系统内未配置 API Key，虽然可以直接尝试加载，但在无 Key 状态下极易访问受限、加载失败。\n\n建议前往 Pexels 官方免费申请一个 Key 并配置在此处：",
                                "This app queries live realistic landscapes on Pexels. No API key is loaded. Although you can attempt loading without one, empty-key queries may fail due to rate limits.\n\nWe recommend getting a free key from Pexels and pasting it below:"
                            ),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { inputKey = it },
                            modifier = Modifier.fillMaxWidth().testTag("config_pexels_api_key_field"),
                            placeholder = {
                                Text(
                                    text = "Pexels API Key...",
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.savePexelsKeyAndProceed(inputKey)
                        },
                        modifier = Modifier.testTag("save_and_proceed_key_button")
                    ) {
                        Text(text = viewModel.getTranslation("保存并加载", "Save & Load"))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = { viewModel.dismissApiKeyPrompt(proceedAnyway = true) },
                            modifier = Modifier.testTag("proceed_without_key_button")
                        ) {
                            Text(text = viewModel.getTranslation("暂不设置 (继续获取)", "Try Anyway"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { viewModel.dismissApiKeyPrompt(proceedAnyway = false) }
                        ) {
                            Text(text = viewModel.getTranslation("取消", "Cancel"))
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun MainNavigationContainer(
    viewModel: WallpaperViewModel,
    glassView: com.qmdeve.liquidglass.widget.LiquidGlassView,
    detailGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView
) {
    val activeScreen by viewModel.activeScreen.collectAsState()
    val detailWallpaper by viewModel.detailWallpaper.collectAsState()
    val isAboutPageVisible by viewModel.isAboutPageVisible.collectAsState()
    val language by viewModel.language.collectAsState()
    val isDark = isSystemInDarkTheme()
    val liquidGlassBlur by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.lgRefractionHeight.collectAsState()
    val refractionOffset by viewModel.lgRefractionOffset.collectAsState()
    val tintAlpha by viewModel.lgTintAlpha.collectAsState()
    val dispersion by viewModel.lgDispersion.collectAsState()
    val draggable by viewModel.lgDraggable.collectAsState()
    val elastic by viewModel.lgElastic.collectAsState()
    val touchEffect by viewModel.lgTouchEffect.collectAsState()

    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 299.dp.toPx() }
    val blurRadiusPx = with(density) { liquidGlassBlur.dp.toPx() }
    val refractionHeightPx = with(density) { refractionHeight.dp.toPx() }
    val refractionOffsetPx = with(density) { refractionOffset.dp.toPx() }

    val applyGlassConfig = remember(isDark, cornerRadiusPx, blurRadiusPx, tintAlpha, refractionHeightPx, refractionOffsetPx, dispersion, draggable, elastic, touchEffect) {
        {
            if (glassView.width > 0 && glassView.height > 0) {
                try {
                    btm.m.todaywallpaper.MainActivity.safeConfigure(
                        view = glassView,
                        red = if (isDark) 0.0f else 1.0f,
                        green = if (isDark) 0.0f else 1.0f,
                        blue = if (isDark) 0.0f else 1.0f,
                        alpha = tintAlpha,
                        cornerRadius = cornerRadiusPx,
                        blurRadius = blurRadiusPx,
                        refractionHeight = refractionHeightPx
                    )
                    glassView.setRefractionOffset(refractionOffsetPx)
                    if (dispersion > 0f) glassView.setDispersion(dispersion)
                    glassView.setDraggableEnabled(draggable)
                    glassView.setElasticEnabled(elastic)
                    glassView.setTouchEffectEnabled(touchEffect)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(isDark, cornerRadiusPx, blurRadiusPx, tintAlpha, refractionHeightPx, refractionOffsetPx, dispersion, draggable, elastic, touchEffect) {
        applyGlassConfig()
    }

    DisposableEffect(glassView, blurRadiusPx, isDark, tintAlpha, refractionHeightPx, refractionOffsetPx, dispersion, draggable, elastic, touchEffect) {
        val applyAndInvalidate = {
            if (glassView.width > 0 && glassView.height > 0) {
                try {
                    btm.m.todaywallpaper.MainActivity.safeConfigure(
                        view = glassView,
                        red = if (isDark) 0.0f else 1.0f,
                        green = if (isDark) 0.0f else 1.0f,
                        blue = if (isDark) 0.0f else 1.0f,
                        alpha = tintAlpha,
                        cornerRadius = cornerRadiusPx,
                        blurRadius = blurRadiusPx,
                        refractionHeight = refractionHeightPx
                    )
                    glassView.setRefractionOffset(refractionOffsetPx)
                    if (dispersion > 0f) glassView.setDispersion(dispersion)
                    glassView.setDraggableEnabled(draggable)
                    glassView.setElasticEnabled(elastic)
                    glassView.setTouchEffectEnabled(touchEffect)
                    glassView.invalidate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val listener = View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = right - left
            val h = bottom - top
            if (w > 0 && h > 0) {
                applyAndInvalidate()
            }
        }
        glassView.addOnLayoutChangeListener(listener)
        if (glassView.width > 0 && glassView.height > 0) {
            applyAndInvalidate()
        }
        glassView.post { applyAndInvalidate() }
        onDispose {
            glassView.removeOnLayoutChangeListener(listener)
        }
    }

    val tabs = remember {
        listOf(
            Triple(Screen.Home, Icons.Filled.Home to Icons.Outlined.Home, "home"),
            Triple(Screen.Categories, Icons.Filled.Category to Icons.Outlined.Category, "categories"),
            Triple(Screen.Mine, Icons.Filled.Person to Icons.Outlined.Person, "mine")
        )
    }

    fun getTabLabel(screen: Screen, lang: String): String {
        return when (screen) {
            Screen.Home -> if (lang == "zh") "首页" else "Home"
            Screen.Categories -> if (lang == "zh") "分类" else "Themes"
            Screen.Mine -> if (lang == "zh") "我的" else "Mine"
        }
    }

    val showBottomNav = detailWallpaper == null && !isAboutPageVisible

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (showBottomNav) {
            Box(
                modifier = Modifier
                    .width(278.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 20.dp, top = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        val size = coordinates.size
                        val position = coordinates.positionInRoot()

                        glassView.post {
                            val lp = glassView.layoutParams as? FrameLayout.LayoutParams
                            if (lp != null) {
                                lp.width = size.width
                                lp.height = size.height
                                glassView.layoutParams = lp
                            } else {
                                glassView.layoutParams = FrameLayout.LayoutParams(size.width, size.height)
                            }
                            glassView.translationX = position.x
                            glassView.translationY = position.y
                        }
                    }
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(299.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .border(
                        width = 1.2.dp,
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f))
                            } else {
                                listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.12f))
                            }
                        ),
                        shape = RoundedCornerShape(299.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .height(72.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag("app_bottom_nav"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEach { (screen, iconPair, tagSuffix) ->
                        CapsuleNavItem(
                            selected = activeScreen == screen,
                            onClick = { viewModel.setActiveScreen(screen) },
                            activeIcon = iconPair.first,
                            inactiveIcon = iconPair.second,
                            label = getTabLabel(screen, language),
                            selectedColor = Color(0xFF1D82EC),
                            unselectedColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF555555),
                            tag = "tab_$tagSuffix"
                        )
                    }
                }
            }
        } else {
            LaunchedEffect(showBottomNav) {
                glassView.post {
                    glassView.layoutParams = FrameLayout.LayoutParams(0, 0)
                }
            }
        }

        if (detailWallpaper != null) {
            val wallpaper = detailWallpaper
            if (wallpaper != null) {
                LaunchedEffect(Unit) {
                    glassView.post {
                        glassView.layoutParams = FrameLayout.LayoutParams(0, 0)
                    }
                }
                WallpaperDetailViewer(
                    wallpaperId = wallpaper.id,
                    imageUrl = wallpaper.imageUrl,
                    authorName = wallpaper.author,
                    source = wallpaper.source,
                    viewModel = viewModel,
                    detailGlassView = detailGlassView,
                    renderForegroundOnly = true,
                    onBack = { viewModel.setDetailWallpaper(null) }
                )
            }
        }
    }
}

@Composable
fun RowScope.CapsuleNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selectedColor: Color,
    unselectedColor: Color,
    tag: String
) {
    val scale by animateFloatAsState(if (selected) 1.05f else 1.0f, label = "TabScale")

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(299.dp))
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Let scale and color speak for themselves cleanly
            )
            .scale(scale)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        val isDark = isSystemInDarkTheme()
        Column(
            modifier = Modifier
                .width(80.dp) // Tuned down to ~96% of 84.dp
                .height(58.dp) // Tuned down to ~96% of 61.dp
                .clip(RoundedCornerShape(299.dp))
                .background(
                    if (selected) {
                        if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.Transparent
                    }
                )
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(299.dp)
                        )
                    } else {
                        Modifier
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) activeIcon else inactiveIcon,
                contentDescription = label,
                tint = if (selected) selectedColor else unselectedColor,
                modifier = Modifier.size(26.dp) // Enlarged by ~110% of 24.dp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp, // Enlarged by ~110% of 12.sp
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) selectedColor else unselectedColor
            )
        }
    }
}
