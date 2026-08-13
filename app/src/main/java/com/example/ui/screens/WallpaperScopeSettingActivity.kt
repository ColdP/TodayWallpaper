package btm.m.todaywallpaper.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

class WallpaperScopeSettingActivity : ComponentActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        enableMomentumTransparentWindow()

        setContent {
            MyApplicationTheme {
                WallpaperScopeSettingScreen(
                    viewModel = viewModel,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
private fun WallpaperScopeSettingScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wallpaperScope by viewModel.wallpaperScope.collectAsState()

    val view = androidx.compose.ui.platform.LocalView.current
    val darkTheme = isAppDarkTheme()
    val accentColor = MaterialTheme.colorScheme.primary

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

    PredictiveBackHandler(enabled = predictiveBackEnabled) { progressFlow ->
        try {
            isBackSwiping = true
            progressFlow.collect { backEvent ->
                backProgress = kotlin.math.min(backEvent.progress, predictiveBackMaxProgress / 100f)
                backDirection = if (backEvent.swipeEdge == androidx.activity.BackEventCompat.EDGE_RIGHT) -1f else 1f
            }
            isBackSwiping = false
            backProgress = 1f
            onBack()
        } catch (_: Exception) {
            isBackSwiping = false
            backProgress = 0f
        }
    }

    val scale = 1f - (backProgress * 0.12f)
    val translationXDp = (backProgress * 48f * backDirection).dp
    val alpha = 1f
    val cornerRadius = 28.dp * backProgress

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = with(androidx.compose.ui.platform.LocalDensity.current) { translationXDp.toPx() },
                            translationY = with(androidx.compose.ui.platform.LocalDensity.current) { (backProgress * 16f).dp.toPx() },
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
                    .verticalScroll(rememberScrollState())
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
                            contentDescription = viewModel.getTranslation("返回", "Back"),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = viewModel.getTranslation("壁纸设置范围", "Wallpaper Scope"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Options list
                val options = listOf(
                    WallpaperViewModel.WallpaperScope.ASK_EVERY_TIME,
                    WallpaperViewModel.WallpaperScope.HOME_SCREEN,
                    WallpaperViewModel.WallpaperScope.LOCK_SCREEN,
                    WallpaperViewModel.WallpaperScope.BOTH
                )

                options.forEachIndexed { index, scope ->
                    val isSelected = wallpaperScope == scope
                    val bgAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 0.08f else 0.0f,
                        animationSpec = tween(300),
                        label = "optionBg$index"
                    )

                    val scopeLabel = viewModel.getTranslation(scope.zhLabel, scope.enLabel)
                    val scopeHint = when (scope) {
                        WallpaperViewModel.WallpaperScope.ASK_EVERY_TIME -> viewModel.getTranslation(
                            "每次设置壁纸时都会询问应用范围",
                            "Ask every time before applying wallpaper"
                        )
                        WallpaperViewModel.WallpaperScope.HOME_SCREEN -> viewModel.getTranslation(
                            "仅应用到主屏幕桌面",
                            "Home screen only"
                        )
                        WallpaperViewModel.WallpaperScope.LOCK_SCREEN -> viewModel.getTranslation(
                            "仅应用到锁屏界面",
                            "Lock screen only"
                        )
                        WallpaperViewModel.WallpaperScope.BOTH -> viewModel.getTranslation(
                            "同时应用到桌面和锁屏",
                            "Both home & lock screen"
                        )
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clickable { viewModel.setWallpaperScope(scope) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                accentColor.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Check circle indicator
                            val checkBgColor by animateColorAsState(
                                targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                animationSpec = tween(300),
                                label = "checkBg$index"
                            )
                            val checkIconColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
                                animationSpec = tween(300),
                                label = "checkIcon$index"
                            )

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(checkBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = checkIconColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scopeLabel,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = scopeHint,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Right indicator dot
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
