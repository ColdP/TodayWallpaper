package btm.m.todaywallpaper.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.viewinterop.AndroidView
import btm.m.todaywallpaper.MainActivity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Wallpaper
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import btm.m.todaywallpaper.ui.viewmodel.UnifiedWallpaper
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDetailViewer(
    wallpaperId: String,
    imageUrl: String,
    authorName: String?,
    source: String,
    viewModel: WallpaperViewModel,
    detailGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView? = null,
    detailBackGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView? = null,
    detailDownloadGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView? = null,
    renderBackgroundOnly: Boolean = false,
    renderForegroundOnly: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val composeRootView = LocalView.current
    val settingState by viewModel.wallpaperSettingState.collectAsState()
    val isFav by viewModel.isWallpaperFavoriteFlow(wallpaperId).collectAsState()
    val collections by viewModel.collections.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val detailBackProgress by viewModel.detailBackProgress.collectAsState()
    val detailBackDirection by viewModel.detailBackDirection.collectAsState()
    val isDetailBackSwiping by viewModel.isDetailBackSwiping.collectAsState()
    val predictiveBackEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val predictiveBackMaxProgress by viewModel.predictiveBackMaxProgress.collectAsState()
    val deviceBackCorner = btm.m.todaywallpaper.ui.widget.rememberDeviceCornerRadius()
    val savedWallpaperScope by viewModel.wallpaperScope.collectAsState()

    // Scope dialog state
    var showScopeDialog by remember { mutableStateOf(false) }
    var pendingAlwaysScope by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.downloadWallpaper(context, imageUrl)
        } else {
            Toast.makeText(
                context,
                viewModel.getTranslation("未授予存储权限，无法保存图片", "Storage permission denied, cannot save image"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(downloadState) {
        when (downloadState) {
            is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.DownloadState.Success -> {
                Toast.makeText(
                    context,
                    viewModel.getTranslation("图片保存到相册成功！", "Image saved to gallery successfully!"),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetDownloadState()
            }
            is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.DownloadState.Error -> {
                Toast.makeText(
                    context,
                    viewModel.getTranslation(
                        "图片保存出错: ${(downloadState as btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.DownloadState.Error).message}",
                        "Download failed: ${(downloadState as btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.DownloadState.Error).message}"
                    ),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetDownloadState()
            }
            else -> {}
        }
    }

    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    val isFullscreen by viewModel.isDetailFullscreen.collectAsState()

    val unifiedWallpaper = remember(wallpaperId, imageUrl, authorName, source) {
        UnifiedWallpaper(
            id = wallpaperId,
            imageUrl = imageUrl,
            thumbnailUrl = imageUrl, // Detailed view uses original image as thumbnail preview as well
            author = authorName,
            authorUrl = null,
            source = source
        )
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val detailCornerRadiusPx = with(density) { 36.dp.toPx() }
    val liquidGlassBlurVal by viewModel.liquidGlassBlur.collectAsState()
    val detailBlurRadiusPx = with(density) { liquidGlassBlurVal.dp.toPx() }
    val lgRefractionHeight by viewModel.lgRefractionHeight.collectAsState()
    val lgRefractionOffset by viewModel.lgRefractionOffset.collectAsState()
    val lgTintAlpha by viewModel.lgTintAlpha.collectAsState()
    val lgDispersion by viewModel.lgDispersion.collectAsState()
    val lgDraggable by viewModel.lgDraggable.collectAsState()
    val lgElastic by viewModel.lgElastic.collectAsState()
    val lgTouchEffect by viewModel.lgTouchEffect.collectAsState()
    val lgRefractionHeightPx = with(density) { lgRefractionHeight.dp.toPx() }
    val lgRefractionOffsetPx = with(density) { lgRefractionOffset.dp.toPx() }
    val detailFrameStrokePx = with(density) { 1.2.dp.toPx() }
    val detailFrameStartColor = Color.White.copy(alpha = 0.22f).toArgb()
    val detailFrameEndColor = Color.White.copy(alpha = 0.05f).toArgb()
    // These header-control values intentionally do not read the shared Liquid
    // Glass settings. They define a consistent compact material over every
    // wallpaper, regardless of the user's action-deck/navigation tuning.
    val headerGlassBlurRadiusPx = with(density) { 5.6.dp.toPx() }
    val headerGlassRefractionHeightPx = with(density) { 22.dp.toPx() }
    val headerGlassRefractionOffsetPx = with(density) { 40.dp.toPx() }
    val headerGlassTintAlpha = 0.16f
    val headerGlassDispersion = 0.33f
    val headerGlassStrokePx = with(density) { 1.dp.toPx() }
    val headerGlassStrokeStartColor = Color.White.copy(alpha = 0.22f).toArgb()
    val headerGlassStrokeEndColor = Color.White.copy(alpha = 0.14f).toArgb()
    // LayoutCoordinates include graphics-layer transforms on this Compose version.
    // Keep the untransformed deck bounds captured before predictive-back starts;
    // otherwise each gesture frame would apply translation a second time.
    var actionDeckBaseBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    if (!renderBackgroundOnly) {
        BackHandler {
            viewModel.resetDetailBackGesture()
            onBack()
        }

        PredictiveBackHandler(enabled = predictiveBackEnabled) { progressFlow ->
            try {
                viewModel.setDetailBackGesture(0f, true)
                progressFlow.collect { backEvent ->
                    viewModel.setDetailBackGesture(
                        progress = kotlin.math.min(backEvent.progress, predictiveBackMaxProgress / 100f),
                        swiping = true,
                        direction = if (backEvent.swipeEdge == androidx.activity.BackEventCompat.EDGE_RIGHT) -1f else 1f
                    )
                }
                viewModel.setDetailBackGesture(1f, false)
                onBack()
            } catch (e: Exception) {
                viewModel.resetDetailBackGesture()
            }
        }
    }

    DisposableEffect(detailGlassView, detailBlurRadiusPx, lgRefractionHeightPx, lgRefractionOffsetPx, lgTintAlpha, lgDispersion, lgDraggable, lgElastic, lgTouchEffect) {
        if (detailGlassView == null || renderBackgroundOnly) return@DisposableEffect onDispose {}

        val applyDetailGlassConfig = {
            if (detailGlassView.width > 0 && detailGlassView.height > 0) {
                try {
                    btm.m.todaywallpaper.MainActivity.safeConfigure(
                        view = detailGlassView,
                        red = 0.0f,
                        green = 0.0f,
                        blue = 0.0f,
                        alpha = lgTintAlpha,
                        cornerRadius = detailCornerRadiusPx,
                        blurRadius = detailBlurRadiusPx,
                        refractionHeight = lgRefractionHeightPx
                    )
                    detailGlassView.setRefractionOffset(lgRefractionOffsetPx)
                    if (lgDispersion > 0f) detailGlassView.setDispersion(lgDispersion)
                    detailGlassView.setDraggableEnabled(lgDraggable)
                    detailGlassView.setElasticEnabled(lgElastic)
                    detailGlassView.setTouchEffectEnabled(lgTouchEffect)
                    (detailGlassView as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                        ?.configureDetailFrame(
                            cornerRadius = detailCornerRadiusPx,
                            strokeWidth = detailFrameStrokePx,
                            startColor = detailFrameStartColor,
                            endColor = detailFrameEndColor
                        )
                    detailGlassView.invalidate()
                } catch (_: Exception) {}
            }
        }

        val listener = android.view.View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = right - left
            val h = bottom - top
            if (w > 0 && h > 0) {
                applyDetailGlassConfig()
            }
        }

        detailGlassView.addOnLayoutChangeListener(listener)
        if (detailGlassView.width > 0 && detailGlassView.height > 0) {
            applyDetailGlassConfig()
        }
        detailGlassView.post { applyDetailGlassConfig() }

        onDispose {
            detailGlassView.removeOnLayoutChangeListener(listener)
            detailGlassView.post {
                try {
                    (detailGlassView as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                        ?.clearDetailFrame()
                    detailGlassView.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // The action deck is rendered in a Compose layer that shrinks and translates
    // during predictive back. Its native glass sibling is outside that layer, so
    // apply the same transform to its root coordinates explicitly.
    LaunchedEffect(actionDeckBaseBounds, detailBackProgress, detailBackDirection, isFullscreen) {
        val bounds = actionDeckBaseBounds ?: return@LaunchedEffect
        if (detailGlassView == null || renderBackgroundOnly || isFullscreen) return@LaunchedEffect
        val scale = 1f - detailBackProgress * 0.12f
        val composeWidth = composeRootView.width.toFloat()
        val composeHeight = composeRootView.height.toFloat()
        if (composeWidth <= 0f || composeHeight <= 0f) return@LaunchedEffect
        val parent = detailGlassView.parent as? android.view.View
            ?: return@LaunchedEffect
        val composeLocation = IntArray(2)
        val parentLocation = IntArray(2)
        composeRootView.getLocationOnScreen(composeLocation)
        parent.getLocationOnScreen(parentLocation)
        val composeOffsetX = (composeLocation[0] - parentLocation[0]).toFloat()
        val composeOffsetY = (composeLocation[1] - parentLocation[1]).toFloat()
        val rootWidth = composeWidth
        val rootHeight = composeHeight
        val baseLeft = composeOffsetX + bounds.left
        val baseTop = composeOffsetY + bounds.top
        val centerX = composeOffsetX + composeWidth / 2f
        val centerY = composeOffsetY + composeHeight / 2f
        val translationX = with(density) { (detailBackProgress * 48f * detailBackDirection).dp.toPx() }
        val translationY = with(density) { (detailBackProgress * 16f).dp.toPx() }
        val transformedLeft = centerX + (baseLeft - centerX) * scale + translationX
        val transformedTop = centerY + (baseTop - centerY) * scale + translationY
        detailGlassView.post {
            val lp = detailGlassView.layoutParams as? android.widget.FrameLayout.LayoutParams
                ?: android.widget.FrameLayout.LayoutParams(0, 0)
            // Keep native layout dimensions fixed. Updating them every gesture
            // frame triggers LiquidGlassView.rebuild(), which both doubles the
            // coordinate transform and races its asynchronous shader callback.
            val baseWidth = bounds.width.toInt().coerceAtLeast(1)
            val baseHeight = bounds.height.toInt().coerceAtLeast(1)
            if (lp.width != baseWidth || lp.height != baseHeight) {
                lp.width = baseWidth
                lp.height = baseHeight
                detailGlassView.layoutParams = lp
            }
            detailGlassView.pivotX = 0f
            detailGlassView.pivotY = 0f
            detailGlassView.translationX = transformedLeft
            detailGlassView.translationY = transformedTop
            detailGlassView.scaleX = scale
            detailGlassView.scaleY = scale
            detailGlassView.invalidate()
        }
    }

    DisposableEffect(detailBackGlassView) {
        if (detailBackGlassView == null || renderBackgroundOnly) return@DisposableEffect onDispose {}
        onDispose {
            detailBackGlassView.visibility = android.view.View.INVISIBLE
            detailBackGlassView.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
            (detailBackGlassView as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                ?.clearDetailFrame()
        }
    }

    DisposableEffect(detailDownloadGlassView) {
        if (detailDownloadGlassView == null || renderBackgroundOnly) return@DisposableEffect onDispose {}
        onDispose {
            detailDownloadGlassView.visibility = android.view.View.INVISIBLE
            detailDownloadGlassView.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
            (detailDownloadGlassView as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                ?.clearDetailFrame()
        }
    }

    // Hide/show all native LiquidGlassViews when entering/exiting fullscreen
    LaunchedEffect(isFullscreen) {
        val views = listOfNotNull(detailGlassView, detailBackGlassView, detailDownloadGlassView)
        views.forEach { view ->
            view.post {
                if (isFullscreen) {
                    view.layoutParams = android.widget.FrameLayout.LayoutParams(0, 0)
                }
                // When exiting fullscreen, the onGloballyPositioned callbacks will re-position them
            }
        }
    }

    LaunchedEffect(settingState) {
        when (settingState) {
            is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Success -> {
                Toast.makeText(
                    context,
                    viewModel.getTranslation("壁纸更换成功！", "Wallpaper matched successfully!"),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetWallpaperSettingState()
            }
            is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Error -> {
                Toast.makeText(
                    context,
                    viewModel.getTranslation(
                        "壁纸更换失败: ${(settingState as btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Error).message}",
                        "Apply failed: ${(settingState as btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Error).message}"
                    ),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetWallpaperSettingState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer(
                scaleX = 1f - detailBackProgress * 0.12f,
                scaleY = 1f - detailBackProgress * 0.12f,
                translationX = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (detailBackProgress * 48f * detailBackDirection).dp.toPx()
                },
                translationY = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (detailBackProgress * 16f).dp.toPx()
                },
                alpha = 1f,
                clip = detailBackProgress > 0f,
                shape = RoundedCornerShape(deviceBackCorner * detailBackProgress)
            )
    ) {
        if (renderBackgroundOnly || (!renderBackgroundOnly && !renderForegroundOnly)) {
            // Main HD Image
            AsyncImage(
                model = imageUrl,
                contentDescription = "HD Wallpaper Detail",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = isFullscreen) { viewModel.setDetailFullscreen(false) },
                contentScale = ContentScale.Crop
            )

            // Dark gradient shielding layers (lighter in fullscreen)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = if (isFullscreen) 0.15f else 0.5f),
                                Color.Transparent,
                                Color.Black.copy(alpha = if (isFullscreen) 0.05f else 0.3f),
                                Color.Black.copy(alpha = if (isFullscreen) 0.15f else 0.85f)
                            )
                        )
                    )
            )
        }

        if (!isFullscreen && (renderForegroundOnly || (!renderBackgroundOnly && !renderForegroundOnly))) {
            // Column content layer conforming to Safe status bar margins
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Elegant Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Circular glassmorphic Button for onBack
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .forwardTouchToGlass(detailBackGlassView)
                        .onGloballyPositioned { coordinates ->
                            val size = coordinates.size
                            val position = coordinates.positionInRoot()
                            detailBackGlassView?.let { dbg ->
                                dbg.post {
                                    dbg.visibility = android.view.View.VISIBLE
                                    val lp = dbg.layoutParams as? android.widget.FrameLayout.LayoutParams
                                    if (lp != null) {
                                        lp.width = size.width
                                        lp.height = size.height
                                        dbg.layoutParams = lp
                                    } else {
                                        dbg.layoutParams = android.widget.FrameLayout.LayoutParams(size.width, size.height)
                                    }
                                    dbg.translationX = position.x
                                    dbg.translationY = position.y
                                    
                                    // Fixed, detail-header-only glass configuration.
                                    // Its frame is native too, so it follows the
                                    // liquid view during touch deformation.
                                    btm.m.todaywallpaper.MainActivity.safeConfigure(
                                        view = dbg,
                                        red = 1.0f,
                                        green = 1.0f,
                                        blue = 1.0f,
                                        alpha = headerGlassTintAlpha,
                                        cornerRadius = size.height / 2f,
                                        blurRadius = headerGlassBlurRadiusPx,
                                        refractionHeight = headerGlassRefractionHeightPx
                                    )
                                    dbg.setRefractionOffset(headerGlassRefractionOffsetPx)
                                    dbg.setDispersion(headerGlassDispersion)
                                    dbg.setDraggableEnabled(false)
                                    dbg.setElasticEnabled(false)
                                    dbg.setTouchEffectEnabled(true)
                                    (dbg as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                                        ?.configureDetailFrame(
                                            cornerRadius = size.height / 2f,
                                            strokeWidth = headerGlassStrokePx,
                                            startColor = headerGlassStrokeStartColor,
                                            endColor = headerGlassStrokeEndColor
                                        )
                                }
                            }
                        }
                        .clickable { onBack() }
                        .testTag("detail_back_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Right side capsule: fullscreen + download buttons merged
                val isDownloading = downloadState is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.DownloadState.Downloading

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .forwardTouchToGlass(detailDownloadGlassView)
                        .onGloballyPositioned { coordinates ->
                            val size = coordinates.size
                            val position = coordinates.positionInRoot()
                            detailDownloadGlassView?.let { ddg ->
                                ddg.post {
                                    ddg.visibility = android.view.View.VISIBLE
                                    val lp = ddg.layoutParams as? android.widget.FrameLayout.LayoutParams
                                    if (lp != null) {
                                        lp.width = size.width
                                        lp.height = size.height
                                        ddg.layoutParams = lp
                                    } else {
                                        ddg.layoutParams = android.widget.FrameLayout.LayoutParams(size.width, size.height)
                                    }
                                    ddg.translationX = position.x
                                    ddg.translationY = position.y
                                    btm.m.todaywallpaper.MainActivity.safeConfigure(
                                        view = ddg,
                                        red = 1.0f,
                                        green = 1.0f,
                                        blue = 1.0f,
                                        alpha = headerGlassTintAlpha,
                                        cornerRadius = size.height / 2f,
                                        blurRadius = headerGlassBlurRadiusPx,
                                        refractionHeight = headerGlassRefractionHeightPx
                                    )
                                    ddg.setRefractionOffset(headerGlassRefractionOffsetPx)
                                    ddg.setDispersion(headerGlassDispersion)
                                    ddg.setDraggableEnabled(false)
                                    ddg.setElasticEnabled(false)
                                    ddg.setTouchEffectEnabled(true)
                                    (ddg as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                                        ?.configureDetailFrame(
                                            cornerRadius = size.height / 2f,
                                            strokeWidth = headerGlassStrokePx,
                                            startColor = headerGlassStrokeStartColor,
                                            endColor = headerGlassStrokeEndColor
                                        )
                                }
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fullscreen toggle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                        .clickable { viewModel.toggleDetailFullscreen() }
                        .testTag("detail_fullscreen_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Download button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                if (isDownloading) return@clickable
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    viewModel.downloadWallpaper(context, imageUrl)
                                } else {
                                    val isGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (isGranted) {
                                        viewModel.downloadWallpaper(context, imageUrl)
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                }
                            }
                            .testTag("detail_download_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.FileDownload,
                                contentDescription = "Download Wallpaper",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Action Deck Panel layered on top of the native detailGlassView sibling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        val size = coordinates.size
                        val currentBounds = androidx.compose.ui.geometry.Rect(
                            left = position.x,
                            top = position.y,
                            right = position.x + size.width,
                            bottom = position.y + size.height
                        )
                        if (detailBackProgress == 0f) {
                            actionDeckBaseBounds = currentBounds
                        }
                    }
                    .clip(RoundedCornerShape(36.dp))
            ) {
                // Sibling 2 (Top): Foreground layout with clear readable text and buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Info Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = authorName ?: viewModel.getTranslation("神秘创作者", "Aesthetic Creator"),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${viewModel.getTranslation("来源渠道", "Platform Channel")}: $source",
                                fontSize = 12.sp,
                                color = Color.LightGray.copy(alpha = 0.8f)
                            )
                        }

                        // Bottom Panel Fast bookmark toggle
                        FilledIconButton(
                            onClick = { viewModel.toggleFavorite(unifiedWallpaper) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = if (isFav) Color.Red else Color.White
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detail_fav_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Actions Column
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Add to custom album
                            Button(
                                onClick = { showAddToAlbumDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                                    .testTag("detail_add_to_album_btn")
                            ) {
                                Text(
                                    text = viewModel.getTranslation("加入图集", "Save to Album"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Set as custom profile Avatar
                            Button(
                                onClick = {
                                    viewModel.updateAvatar(imageUrl)
                                    Toast.makeText(
                                        context,
                                        viewModel.getTranslation("已将该壁纸快捷设置为您的头像！", "Profile avatar updated!"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                                    .testTag("detail_set_avatar_btn")
                            ) {
                                Text(
                                    text = viewModel.getTranslation("设为头像", "Set Avatar"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Apply Wallpaper with scope support
                        Button(
                            onClick = {
                                if (savedWallpaperScope == WallpaperViewModel.WallpaperScope.ASK_EVERY_TIME) {
                                    showScopeDialog = true
                                } else {
                                    viewModel.setSystemWallpaper(context, imageUrl, savedWallpaperScope)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("detail_apply_btn"),
                                    enabled = settingState !is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Setting
                        ) {
                                            if (settingState is btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel.SettingWallpaperState.Setting) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = viewModel.getTranslation("一键设为壁纸", "Apply Wallpaper"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen exit overlay (tap anywhere to exit, shows exit button in corner)
        if (isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.setDetailFullscreen(false) }
                    .testTag("detail_fullscreen_overlay"),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .statusBarsPadding()
                        .size(44.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { viewModel.setDetailFullscreen(false) }
                        .testTag("detail_exit_fullscreen_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FullscreenExit,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Dropdown Modal for selecting album
        if (showAddToAlbumDialog) {
            AlertDialog(
                onDismissRequest = { showAddToAlbumDialog = false },
                title = {
                    Text(
                        text = viewModel.getTranslation("保存至所属图集", "Add to Album Collection"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        if (collections.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = viewModel.getTranslation("你还没有创建过自定义图集", "You do not have any albums yet."),
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = {
                                        viewModel.showCreateCollectionOverlay(
                                            sourceWallpaper = unifiedWallpaper,
                                            requireLocalImages = false
                                        )
                                        showAddToAlbumDialog = false
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CreateNewFolder,
                                        contentDescription = "New Album",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = viewModel.getTranslation("立即创建新图集", "Create Album Now"),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(collections) { album ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .clickable {
                                                viewModel.addWallpaperToCollectionId(
                                                    album.id,
                                                    unifiedWallpaper
                                                )
                                                showAddToAlbumDialog = false
                                                Toast
                                                    .makeText(
                                                        context,
                                                        viewModel.getTranslation(
                                                            "已添加至图集: ${album.name}",
                                                            "Added to ${album.name}"
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FolderSpecial,
                                            contentDescription = "Folder",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = album.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!album.description.isNullOrEmpty()) {
                                                Text(
                                                    text = album.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.showCreateCollectionOverlay(
                                                    sourceWallpaper = unifiedWallpaper,
                                                    requireLocalImages = false
                                                )
                                                showAddToAlbumDialog = false
                                            }
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Create inline",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = viewModel.getTranslation("新建别的图集", "Create Another Album"),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddToAlbumDialog = false }) {
                        Text(text = viewModel.getTranslation("取消", "Close"))
                    }
                }
            )
        }

        // Wallpaper scope selection dialog
        if (showScopeDialog) {
            WallpaperScopeDialog(
                viewModel = viewModel,
                onDismiss = { showScopeDialog = false },
                onAlways = { scope ->
                    viewModel.setWallpaperScope(scope)
                    viewModel.setSystemWallpaper(context, imageUrl, scope)
                    showScopeDialog = false
                },
                onJustOnce = { scope ->
                    viewModel.setSystemWallpaper(context, imageUrl, scope)
                    showScopeDialog = false
                }
            )
        }

        } // closing if (renderForegroundOnly || ...)
    }
}

/**
 * Mirrors a Compose control's pointer stream to its native glass sibling.
 * Returning false deliberately preserves the original Compose click handler;
 * the native view only receives the gesture to render its touch feedback.
 */
private fun Modifier.forwardTouchToGlass(
    glassView: com.qmdeve.liquidglass.widget.LiquidGlassView?
): Modifier {
    if (glassView == null) return this
    return pointerInteropFilter { event ->
        val glassEvent = android.view.MotionEvent.obtain(event)
        try {
            // Event coordinates are already local to the Compose control and
            // its native glass sibling has precisely the same bounds.
            glassView.dispatchTouchEvent(glassEvent)
        } catch (_: Throwable) {
            // Header interaction must stay usable if a renderer is recreating.
        } finally {
            glassEvent.recycle()
        }
        false
    }
}
