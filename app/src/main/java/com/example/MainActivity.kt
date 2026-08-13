package btm.m.todaywallpaper

import android.content.Intent
import android.os.Bundle
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import btm.m.todaywallpaper.ui.screens.CategoriesScreen
import btm.m.todaywallpaper.ui.screens.CreateCollectionDialog
import btm.m.todaywallpaper.ui.screens.HomeScreen
import btm.m.todaywallpaper.ui.screens.MineLibraryHost
import btm.m.todaywallpaper.ui.screens.ProfileOverlay
import btm.m.todaywallpaper.ui.screens.WallpaperDetailViewer
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.viewmodel.Screen
import btm.m.todaywallpaper.ui.viewmodel.DetailWallpaperData
import btm.m.todaywallpaper.ui.viewmodel.CreateCollectionOverlayRequest

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.abs
import kotlinx.coroutines.delay

private const val SHOW_FULLSCREEN_CLOCK_ENTRY = false

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
            // LiquidGlassView handles its touch deformation in onTouchEvent. It remains
            // behind Compose, so making it clickable only lets mirrored events be handled
            // and does not take navigation gestures away from Compose.
            isClickable = true
            isFocusable = false
        }

        // Dedicated glass for the enlarged, finger-following selected capsule.
        // It is a root sibling rather than a child of the 72dp navigation surface,
        // so its 86.4dp pressed height can draw outside the bar without clipping.
        val pressedHighlightGlassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0)
            isClickable = false
            isFocusable = false
            visibility = View.INVISIBLE
            // This view is the glass fill itself; it must not draw the static
            // navigation foreground used by the normal bar.
            setNavigationHighlightVisible(false)
        }

        // Sibling B2: Native Liquid Glass Backdrop Blur View specifically for Detail View Action Card background
        val detailGlassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0)
            isClickable = false
            isFocusable = false
        }

        // Dedicated native glass layers for the detail header controls. They are
        // siblings below the Compose foreground so the icons stay crisp while the
        // glass itself can sample the wallpaper behind each control.
        val detailBackGlassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0)
            isClickable = false
            isFocusable = false
            // This sibling sits above the homepage Compose surface. Keep it out
            // of both rendering and hit testing until detail bounds are ready.
            visibility = View.INVISIBLE
        }
        val detailDownloadGlassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0)
            isClickable = false
            isFocusable = false
            visibility = View.INVISIBLE
        }

        // Native glass beneath the single Refresh / Previous / Next capsule on
        // the homepage header. Compose supplies controls and bounds; the native
        // sibling supplies refraction, touch feedback and its matching frame.
        val homeHeaderGlassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0)
            isClickable = false
            isFocusable = false
            visibility = View.INVISIBLE
        }

        // 3. Sibling C: Navigation Bar Compose View (drawn transparently on top of background glass)
        val navigationContentView = ComposeView(this)
        val navigationComposeView = object : FrameLayout(this@MainActivity) {
            private var forwardingTouchToGlass = false

            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val glassLeft = glassView.x
                    val glassTop = glassView.y
                    forwardingTouchToGlass = glassView.width > 0 &&
                        glassView.height > 0 &&
                        event.x >= glassLeft &&
                        event.x < glassLeft + glassView.width &&
                        event.y >= glassTop &&
                        event.y < glassTop + glassView.height

                }

                if (forwardingTouchToGlass) {
                    val glassEvent = MotionEvent.obtain(event)
                    glassEvent.offsetLocation(-glassView.x, -glassView.y)
                    try {
                        // The Compose navigation is above the native glass view, so mirror
                        // its gesture to the glass while keeping normal Compose interaction.
                        glassView.dispatchTouchEvent(glassEvent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        glassEvent.recycle()
                    }
                }

                val handled = super.dispatchTouchEvent(event)
                if (event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    forwardingTouchToGlass = false
                }
                return handled
            }
        }.apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Keep the normal navigation glass and its Compose foreground in one
            // sampling scene. The enlarged glass is a root sibling above this
            // container, so it can refract both the bar background and icons/text.
            addView(glassView)
            addView(
                navigationContentView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }

        // Highest application layer. Unlike MineScreen content, this sibling is
        // added after the native navigation/glass views, so it can never be
        // covered by the bottom navigation bar.
        val createCollectionOverlayView = ComposeView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Maintain the perfect layer order: content is bottom-most, then glass blurs, then top navigation and details foreground controls
        rootLayout.addView(contentComposeView)
        rootLayout.addView(detailGlassView)
        rootLayout.addView(detailBackGlassView)
        rootLayout.addView(detailDownloadGlassView)
        // Like the working detail header capsule, this native layer must be
        // above the ComposeView to remain visible. It is non-clickable and is
        // explicitly hidden outside Home, so Compose retains button clicks and
        // it never overlaps detail controls.
        rootLayout.addView(homeHeaderGlassView)
        rootLayout.addView(navigationComposeView)
        // The enlarged glass must be last: it samples navigationComposeView and
        // then renders above the complete navigation scene.
        rootLayout.addView(pressedHighlightGlassView)
        rootLayout.addView(createCollectionOverlayView)

        setContentView(rootLayout)

        // Set the compose contents respectively
        contentComposeView.setContent {
            MyApplicationTheme {
                MainContentContainer(
                    viewModel = viewModel,
                    detailGlassView = detailGlassView
                )
            }
        }

        navigationContentView.setContent {
            MyApplicationTheme {
                MainNavigationContainer(
                    viewModel = viewModel,
                    glassView = glassView,
                    pressedHighlightGlassView = pressedHighlightGlassView,
                    pressedGlassSource = navigationComposeView,
                    detailGlassView = detailGlassView,
                    detailBackGlassView = detailBackGlassView,
                    detailDownloadGlassView = detailDownloadGlassView,
                    homeHeaderGlassView = homeHeaderGlassView
                )
            }
        }


        createCollectionOverlayView.setContent {
            MyApplicationTheme {
                GlobalCreateCollectionOverlay(
                    viewModel = viewModel,
                    backgroundViews = remember {
                        listOf(
                            contentComposeView,
                            detailGlassView,
                            detailBackGlassView,
                            detailDownloadGlassView,
                            homeHeaderGlassView,
                            navigationComposeView,
                            pressedHighlightGlassView
                        )
                    }
                )
            }
        }

        // Bind the sampling source of LiquidGlassView correctly to contentComposeView which contains all screen assets and wallpapers
        glassView.post {
            try {
                // SafeLiquidGlassView begins with a no-op guard instance. Replace
                // it with the actual renderer once the root Compose source is
                // attached; bind() alone would leave the navigation transparent.
                glassView.initializeRealGlass(contentComposeView)
                glassView.requestLayout()
                glassView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Do not bind the pressed glass here: it is intentionally zero-sized and
        // invisible at rest. It is bound/configured only after a press gives it
        // real bounds, so the extra liquid-glass layer exists only while enlarged.
        detailGlassView.post {
            try {
                // Initialize a real glass instance once and keep its layout size
                // stable afterwards. Rebuilding on every detail-back frame races
                // the upstream library's deferred updateParameters callback.
                detailGlassView.initializeRealGlass(contentComposeView)
                detailGlassView.requestLayout()
                detailGlassView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        listOf(detailBackGlassView, detailDownloadGlassView).forEach { headerGlassView ->
            headerGlassView.post {
                try {
                    headerGlassView.initializeRealGlass(contentComposeView)
                    headerGlassView.requestLayout()
                    headerGlassView.invalidate()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
private fun GlobalCreateCollectionOverlay(
    viewModel: WallpaperViewModel,
    backgroundViews: List<View>
) {
    val request by viewModel.createCollectionOverlayRequest.collectAsState()
    val profileRequested by viewModel.profileOverlayVisible.collectAsState()
    val context = LocalContext.current
    var displayedRequest by remember { mutableStateOf<CreateCollectionOverlayRequest?>(null) }
    var displayedProfile by remember { mutableStateOf(false) }
    var collectionOverlayVisible by remember { mutableStateOf(false) }
    var profileOverlayVisible by remember { mutableStateOf(false) }

    // Keep the last request alive while the exit transition runs. This lets the
    // panel, scrim, and backdrop blur fade out together instead of disappearing
    // as soon as the ViewModel clears its request.
    LaunchedEffect(request) {
        if (request != null) {
            displayedRequest = request
            collectionOverlayVisible = true
        } else if (displayedRequest != null) {
            collectionOverlayVisible = false
            delay(340)
            if (!collectionOverlayVisible) displayedRequest = null
        }
    }

    LaunchedEffect(profileRequested) {
        if (profileRequested) {
            displayedProfile = true
            profileOverlayVisible = true
        } else if (displayedProfile) {
            profileOverlayVisible = false
            delay(340)
            if (!profileOverlayVisible) displayedProfile = false
        }
    }

    val overlayVisible = collectionOverlayVisible || profileOverlayVisible

    val blurProgress by animateFloatAsState(
        targetValue = if (overlayVisible) 1f else 0f,
        animationSpec = tween(280),
        label = "create_collection_backdrop_blur"
    )

    DisposableEffect(blurProgress, backgroundViews) {
        val blurEffect = if (blurProgress > 0.01f && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.graphics.RenderEffect.createBlurEffect(
                18f * blurProgress,
                18f * blurProgress,
                android.graphics.Shader.TileMode.CLAMP
            )
        } else {
            null
        }
        backgroundViews.forEach { view ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                view.setRenderEffect(blurEffect)
            }
        }
        onDispose {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                backgroundViews.forEach { it.setRenderEffect(null) }
            }
        }
    }

    AnimatedVisibility(
        visible = collectionOverlayVisible && displayedRequest != null,
        enter = fadeIn(tween(260)) + slideInVertically(
            animationSpec = tween(320),
            initialOffsetY = { it / 8 }
        ),
        exit = fadeOut(tween(220)) + slideOutVertically(
            animationSpec = tween(280),
            targetOffsetY = { it / 10 }
        ),
        label = "create_collection_overlay_transition"
    ) {
        displayedRequest?.let { activeRequest ->
            CreateCollectionDialog(
                viewModel = viewModel,
                requireImages = activeRequest.requireLocalImages,
                onDismiss = viewModel::dismissCreateCollectionOverlay,
                onConfirm = { name, description, categoryId, imageUris, onComplete ->
                    viewModel.createCollectionWithLocalImages(
                        name = name,
                        description = description,
                        categoryId = categoryId,
                        imageUris = imageUris,
                        requireImages = activeRequest.requireLocalImages
                    ) { result ->
                        result.onSuccess { creation ->
                            activeRequest.sourceWallpaper?.let { wallpaper ->
                                viewModel.addWallpaperToCollectionId(creation.collectionId, wallpaper)
                            }
                            viewModel.dismissCreateCollectionOverlay()
                            Toast.makeText(
                                context,
                                if (activeRequest.sourceWallpaper != null) {
                                    viewModel.getTranslation("专属图集已创建并存入图片！", "Album created and image stored!")
                                } else {
                                    viewModel.getTranslation(
                                        "图集创建成功！已添加 ${creation.importedImageCount} 张图片",
                                        "Album created! Added ${creation.importedImageCount} images"
                                    )
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                            onComplete(Result.success(Unit))
                        }.onFailure { error ->
                            onComplete(Result.failure(error))
                        }
                    }
                }
            )
        }
    }

    AnimatedVisibility(
        visible = profileOverlayVisible && displayedProfile,
        enter = fadeIn(tween(260)) + slideInVertically(
            animationSpec = tween(320),
            initialOffsetY = { it / 8 }
        ),
        exit = fadeOut(tween(220)) + slideOutVertically(
            animationSpec = tween(280),
            targetOffsetY = { it / 10 }
        ),
        label = "profile_overlay_transition"
    ) {
        ProfileOverlay(
            viewModel = viewModel,
            onDismiss = viewModel::dismissProfileOverlay
        )
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
                    MineLibraryHost(
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
    glassView: btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView,
    pressedHighlightGlassView: btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView,
    pressedGlassSource: ViewGroup,
    detailGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView,
    detailBackGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView,
    detailDownloadGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView,
    homeHeaderGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView
) {
    val activeScreen by viewModel.activeScreen.collectAsState()
    val detailWallpaper by viewModel.detailWallpaper.collectAsState()
    val isAboutPageVisible by viewModel.isAboutPageVisible.collectAsState()
    val mineDestination by viewModel.mineDestination.collectAsState()
    val selectedCategoryKey by viewModel.selectedCategoryKey.collectAsState()
    val language by viewModel.language.collectAsState()
    // Read the effective Material theme instead of the system setting so forced app
    // light/dark mode also receives the matching navigation palette.
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
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

    // Exact visual specification for the bottom navigation. The tint color is
    // navigation-specific, while its opacity must continue to follow the shared
    // Liquid Glass SetTintAlpha setting.
    val glassTint = if (isDark) Color(0xFF1D1C1C) else Color(0xFFF3F3F3)
    val glassStrokeColor = if (isDark) Color(0xFF989898).copy(alpha = 0.16f)
        else Color(0xFF9F9F9F).copy(alpha = 0.22f)
    // Higher-opacity material highlight, without an additional native glass layer.
    val highlightedCapsuleColor = if (isDark) Color(0xFF121212)
        else Color(0xFFEDEDED)
    val normalContentColor = if (isDark) Color.White else Color.Black
    val highlightedContentColor = Color(0xFF2873D7)
    val navigationBlendColor = Color.Black
    val navigationBlendAlpha = if (isDark) 0.16f else 0.08f
    val strokeWidthPx = with(density) { 1.dp.toPx().toInt().coerceAtLeast(1) }

    val tabs = remember {
        listOf(
            Triple(Screen.Home, Icons.Rounded.Home to Icons.Rounded.Home, "home"),
            Triple(Screen.Categories, Icons.Rounded.Category to Icons.Rounded.Category, "categories"),
            Triple(Screen.Mine, Icons.Rounded.Person to Icons.Rounded.Person, "mine")
        )
    }
    val selectedTabIndex = tabs.indexOfFirst { it.first == activeScreen }.coerceAtLeast(0)

    val applyGlassConfig = remember(isDark, cornerRadiusPx, blurRadiusPx, tintAlpha, refractionHeightPx, refractionOffsetPx, dispersion, draggable, elastic, touchEffect, glassTint, glassStrokeColor, strokeWidthPx, selectedTabIndex) {
        {
            if (glassView.width > 0 && glassView.height > 0) {
                try {
                    btm.m.todaywallpaper.MainActivity.safeConfigure(
                        view = glassView,
                        red = glassTint.red,
                        green = glassTint.green,
                        blue = glassTint.blue,
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
                    // Draw this in the native glass foreground so DARKEN blends
                    // against the actual liquid material, not a transparent
                    // Compose layer. It remains beneath the navigation controls.
                    glassView.configureNavigationBackgroundDarken(
                        color = navigationBlendColor.toArgb(),
                        alpha = (navigationBlendAlpha * 255).toInt()
                    )
                    glassView.configureNavigationHighlight(
                        selectedIndex = selectedTabIndex,
                        highlightColor = highlightedCapsuleColor.toArgb(),
                        borderColor = glassStrokeColor.toArgb(),
                        borderWidth = strokeWidthPx.toFloat()
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(isDark, cornerRadiusPx, blurRadiusPx, tintAlpha, refractionHeightPx, refractionOffsetPx, dispersion, draggable, elastic, touchEffect, selectedTabIndex) {
        applyGlassConfig()
    }

    DisposableEffect(glassView, blurRadiusPx, isDark, tintAlpha, refractionHeightPx, refractionOffsetPx, dispersion, draggable, elastic, touchEffect, selectedTabIndex) {
        val applyAndInvalidate = {
            if (glassView.width > 0 && glassView.height > 0) {
                try {
                    btm.m.todaywallpaper.MainActivity.safeConfigure(
                        view = glassView,
                        red = glassTint.red,
                        green = glassTint.green,
                        blue = glassTint.blue,
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
                    glassView.configureNavigationBackgroundDarken(
                        color = navigationBlendColor.toArgb(),
                        alpha = (navigationBlendAlpha * 255).toInt()
                    )
                    glassView.configureNavigationHighlight(
                        selectedIndex = selectedTabIndex,
                        highlightColor = highlightedCapsuleColor.toArgb(),
                        borderColor = glassStrokeColor.toArgb(),
                        borderWidth = strokeWidthPx.toFloat()
                    )
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

    fun getTabLabel(screen: Screen, lang: String): String {
        return when (screen) {
            Screen.Home -> if (lang == "zh") "首页" else "Home"
            Screen.Categories -> if (lang == "zh") "分类" else "Themes"
            Screen.Mine -> if (lang == "zh") "我的" else "Mine"
        }
    }

    val showBottomNav = detailWallpaper == null &&
        !isAboutPageVisible &&
        selectedCategoryKey == null &&
        (activeScreen != Screen.Mine || mineDestination is btm.m.todaywallpaper.ui.viewmodel.MineDestination.Dashboard)

    // Detail header glass is a root sibling above the homepage Compose surface.
    // Its Compose owner disappears before a posted disposal callback necessarily
    // runs, so hide it synchronously when returning home to avoid covering the
    // Refresh / Previous / Next controls or receiving their touch stream.
    LaunchedEffect(detailWallpaper) {
        if (detailWallpaper == null) {
            listOf(detailBackGlassView, detailDownloadGlassView).forEach { view ->
                view.visibility = View.INVISIBLE
                (view as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                    ?.clearDetailFrame()
                view.layoutParams = FrameLayout.LayoutParams(0, 0)
            }
        }
    }

    LaunchedEffect(activeScreen, detailWallpaper) {
        if (activeScreen != Screen.Home || detailWallpaper != null) {
            homeHeaderGlassView.visibility = View.INVISIBLE
            homeHeaderGlassView.layoutParams = FrameLayout.LayoutParams(0, 0)
            (homeHeaderGlassView as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView)
                ?.clearDetailFrame()
        }
    }

    var navWidthPx by remember { mutableFloatStateOf(0f) }
    var dragPositionPx by remember { mutableStateOf<Float?>(null) }
    var isNavigationPressed by remember { mutableStateOf(false) }
    var navContentPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var navRowHeightPx by remember { mutableFloatStateOf(0f) }

    // A press enlarges to 120% of the 72dp navigation height. Its width keeps
    // the normal destination capsule's aspect ratio. Moving the finger changes
    // only its center position; the dimensions remain fixed until release.
    val pressedHeightPx = with(density) { (72.dp * 1.2f).toPx() }
    val normalCapsuleHeightPx = with(density) { 62.dp.toPx() }
    val normalCapsuleWidthPx = if (navWidthPx > 0f) navWidthPx / tabs.size else 0f
    val pressedWidthPx = if (normalCapsuleWidthPx > 0f) {
        normalCapsuleWidthPx * (pressedHeightPx / normalCapsuleHeightPx)
    } else 0f
    // Dedicated values for the enlarged glass only. The library internally
    // clamps a requested 0dp blur to its minimum 0.01px, which is effectively
    // unblurred while preserving the refraction shader.
    val pressedBlurRadiusPx = 0f
    val pressedRefractionHeightPx = with(density) { 25.dp.toPx() }
    val pressedRefractionOffsetPx = with(density) { 29.dp.toPx() }
    val pressedDispersion = 1f

    LaunchedEffect(
        isNavigationPressed,
        dragPositionPx,
        navContentPosition,
        navRowHeightPx,
        pressedWidthPx,
        pressedHeightPx,
        isDark,
        blurRadiusPx,
        refractionHeightPx,
        refractionOffsetPx,
        dispersion,
        draggable,
        elastic,
        touchEffect
    ) {
        glassView.setNavigationHighlightVisible(!isNavigationPressed)
        if (isNavigationPressed && dragPositionPx != null && pressedWidthPx > 0f) {
            // Permit at most 20% of the enlarged pill outside either edge. Half
            // its width lies on either side of the center, so keeping the center
            // 30% of a pill-width inside each edge leaves exactly 20% outside.
            val minCenterX = pressedWidthPx * 0.30f
            val maxCenterX = navWidthPx - pressedWidthPx * 0.30f
            val constrainedPositionX = dragPositionPx!!.coerceIn(minCenterX, maxCenterX)
            val centerX = navContentPosition.x + constrainedPositionX
            val centerY = navContentPosition.y + navRowHeightPx / 2f
            val isFirstVisibleFrame = pressedHighlightGlassView.visibility != View.VISIBLE
            val lp = pressedHighlightGlassView.layoutParams as? FrameLayout.LayoutParams
                ?: FrameLayout.LayoutParams(0, 0)
            lp.width = pressedWidthPx.toInt()
            lp.height = pressedHeightPx.toInt()
            pressedHighlightGlassView.layoutParams = lp
            pressedHighlightGlassView.translationX = centerX - pressedWidthPx / 2f
            pressedHighlightGlassView.translationY = centerY - pressedHeightPx / 2f
            // Keep the pressed material in the foreground, after LiquidGlass has
            // rendered. The library may rebuild/clear its background while a long
            // press is held, which previously made the enlarged capsule disappear.
            // A foreground fill is independent from that lifecycle and therefore
            // remains visible for the complete press/drag gesture.
            val pressedMaterialForeground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = pressedHeightPx / 2f
                // The enlarged glass tint/material opacity is explicitly 0%;
                // only refraction, dispersion and the subtle stroke remain.
                setColor(highlightedCapsuleColor.copy(alpha = 0f).toArgb())
                setStroke(strokeWidthPx, glassStrokeColor.toArgb())
            }
            pressedHighlightGlassView.background = null
            pressedHighlightGlassView.foreground = pressedMaterialForeground
            pressedHighlightGlassView.visibility = View.VISIBLE

            // SafeLiquidGlassView starts with a crash-prevention dummy. Explicitly
            // replace it with the real LiquidGlass child after enlarged bounds are
            // committed; otherwise upstream ensureGlass() sees the dummy as a real
            // instance, never creates Config, and every visual setter is a no-op.
            if (isFirstVisibleFrame) {
                pressedHighlightGlassView.requestLayout()
                pressedHighlightGlassView.post {
                    try {
                        if (!pressedHighlightGlassView.initializeRealGlass(pressedGlassSource)) {
                            throw IllegalStateException("Real LiquidGlass initialization failed")
                        }
                        btm.m.todaywallpaper.MainActivity.safeConfigure(
                            view = pressedHighlightGlassView,
                            red = glassTint.red,
                            green = glassTint.green,
                            blue = glassTint.blue,
                            alpha = 0f,
                            cornerRadius = pressedHeightPx / 2f,
                            blurRadius = pressedBlurRadiusPx,
                            refractionHeight = pressedRefractionHeightPx
                        )
                        pressedHighlightGlassView.setDraggableEnabled(draggable)
                        pressedHighlightGlassView.setElasticEnabled(elastic)
                        pressedHighlightGlassView.setTouchEffectEnabled(touchEffect)
                        // These library methods call updateConfig() directly and
                        // used to crash when Config was not ready. Keep them inside
                        // the guarded, post-layout block.
                        try {
                            pressedHighlightGlassView.setRefractionOffset(pressedRefractionOffsetPx)
                            pressedHighlightGlassView.setDispersion(pressedDispersion)
                        } catch (t: Throwable) {
                            android.util.Log.w(
                                "PressedNavGlass",
                                "Optional glass parameters are not ready yet",
                                t
                            )
                        }
                        // bind/configure may rebuild the native glass rendering
                        // state. Restore the persistent material foreground after
                        // initialization so a held gesture can never turn blank.
                        pressedHighlightGlassView.foreground = pressedMaterialForeground
                        pressedHighlightGlassView.invalidate()
                    } catch (t: Throwable) {
                        // The pressed affordance must never make navigation crash;
                        // the static blended capsule remains as a safe fallback.
                        android.util.Log.e(
                            "PressedNavGlass",
                            "Failed to initialize pressed navigation glass",
                            t
                        )
                    }
                }
            }
        } else {
            pressedHighlightGlassView.visibility = View.INVISIBLE
            pressedHighlightGlassView.layoutParams = FrameLayout.LayoutParams(0, 0)
            pressedHighlightGlassView.foreground = null
        }
    }

    DisposableEffect(pressedHighlightGlassView) {
        onDispose {
            pressedHighlightGlassView.visibility = View.INVISIBLE
            pressedHighlightGlassView.layoutParams = FrameLayout.LayoutParams(0, 0)
            pressedHighlightGlassView.foreground = null
            glassView.setNavigationHighlightVisible(true)
        }
    }

    fun selectTabAt(x: Float) {
        if (navWidthPx <= 0f) return
        val index = ((x.coerceIn(0f, navWidthPx - 1f) / navWidthPx) * tabs.size)
            .toInt()
            .coerceIn(tabs.indices)
        val target = tabs[index].first
        if (target != activeScreen) viewModel.setActiveScreen(target)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // The native home capsule is a root sibling above contentComposeView,
        // exactly like the detail header glass. Render this Compose foreground
        // in navigationComposeView (which is above that sibling) so icons and
        // click targets remain visible while the glass stays behind them.
        if (activeScreen == Screen.Home && detailWallpaper == null) {
            HomeHeaderControlsOverlay(viewModel, homeHeaderGlassView)
        }

        if (showBottomNav) {
            Box(
                modifier = Modifier
                    // Give the three destinations more horizontal breathing room.
                    // The native glass is sized from the full Row below, so it
                    // expands together with this Compose container.
                    .width(300.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 20.dp, top = 8.dp)
                    .shadow(
                        elevation = if (isDark) 16.dp else 12.dp,
                        shape = RoundedCornerShape(299.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (isDark) 0.62f else 0.18f),
                        spotColor = Color.Black.copy(alpha = if (isDark) 0.54f else 0.22f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .height(72.dp)
                        .fillMaxWidth()
                        // Capture the complete 300 x 72dp navigation surface for
                        // the native glass before applying the content padding.
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInRoot()
                            val size = coordinates.size
                            glassView.post {
                                val lp = glassView.layoutParams as? FrameLayout.LayoutParams
                                if (lp != null) {
                                    lp.width = size.width
                                    lp.height = size.height
                                    glassView.layoutParams = lp
                                } else {
                                    glassView.layoutParams = FrameLayout.LayoutParams(
                                        size.width,
                                        size.height
                                    )
                                }
                                glassView.translationX = position.x
                                glassView.translationY = position.y
                                // The native view now has exactly the same bounds
                                // as this Row, so its local coordinate origin is
                                // stable regardless of system navigation insets.
                                glassView.setNavigationRowGeometry(
                                    left = 0f,
                                    top = 0f,
                                    width = size.width.toFloat(),
                                    height = size.height.toFloat()
                                )
                            }
                        }
                        .padding(horizontal = 5.dp)
                        // This callback sees the inner content area. Pointer x,
                        // item weights and drag selection therefore all use the
                        // same width, while the native drawable applies the same
                        // single 8dp inset to its full-width glass surface.
                        .onGloballyPositioned { coordinates ->
                            navWidthPx = coordinates.size.width.toFloat()
                            navContentPosition = coordinates.positionInRoot()
                            navRowHeightPx = coordinates.size.height.toFloat()
                        }
                        .pointerInput(tabs, navWidthPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                try {
                                    // Permit 20% of the enlarged capsule outside
                                    // either side of the navigation bar.
                                    val minDragX = pressedWidthPx * 0.30f
                                    val maxDragX = navWidthPx - pressedWidthPx * 0.30f
                                    val startX = down.position.x.coerceIn(minDragX, maxDragX)
                                    isNavigationPressed = true
                                    dragPositionPx = startX
                                    selectTabAt(startX)

                                    do {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                            ?: event.changes.firstOrNull()
                                        if (change != null && change.pressed) {
                                            val x = change.position.x.coerceIn(minDragX, maxDragX)
                                            dragPositionPx = x
                                            selectTabAt(x)
                                            change.consume()
                                        }
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    isNavigationPressed = false
                                    dragPositionPx = null
                                    // Restore synchronously on release instead of
                                    // waiting for the next composition/effect frame.
                                    glassView.setNavigationHighlightVisible(true)
                                    pressedHighlightGlassView.visibility = View.INVISIBLE
                                    pressedHighlightGlassView.foreground = null
                                }
                            }
                        }
                        .testTag("app_bottom_nav"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, (screen, iconPair, tagSuffix) ->
                        val itemWidth = if (navWidthPx > 0f) navWidthPx / tabs.size else 0f
                        val itemCenter = itemWidth * (index + 0.5f)
                        val dragInfluence = dragPositionPx?.let { x ->
                            if (itemWidth == 0f) 0f
                            else (1f - abs(x - itemCenter) / itemWidth).coerceIn(0f, 1f)
                        } ?: 0f
                        CapsuleNavItem(
                            selected = activeScreen == screen,
                            interactionFraction = maxOf(
                                dragInfluence,
                                if (activeScreen == screen) 1f else 0f
                            ),
                            touchFraction = dragInfluence,
                            pressed = isNavigationPressed && activeScreen == screen,
                            onClick = { viewModel.setActiveScreen(screen) },
                            activeIcon = iconPair.first,
                            inactiveIcon = iconPair.second,
                            label = getTabLabel(screen, language),
                            selectedContentColor = highlightedContentColor,
                            unselectedContentColor = normalContentColor,
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
                    detailBackGlassView = detailBackGlassView,
                    detailDownloadGlassView = detailDownloadGlassView,
                    renderForegroundOnly = true,
                    onBack = { viewModel.setDetailWallpaper(null) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeaderControlsOverlay(
    viewModel: WallpaperViewModel,
    glassView: com.qmdeve.liquidglass.widget.LiquidGlassView
) {
    val hasPrevious by viewModel.hasPreviousWallpaper.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val blurRadiusPx = with(density) { 5.6.dp.toPx() }
    val refractionHeightPx = with(density) { 22.dp.toPx() }
    val refractionOffsetPx = with(density) { 40.dp.toPx() }
    val strokeWidthPx = with(density) { 1.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 100.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .pointerInteropFilter { event ->
                    val glassEvent = MotionEvent.obtain(event)
                    try {
                        glassView.dispatchTouchEvent(glassEvent)
                    } catch (_: Throwable) {
                    } finally {
                        glassEvent.recycle()
                    }
                    false
                }
                .onGloballyPositioned { coordinates ->
                    val size = coordinates.size
                    val position = coordinates.positionInRoot()
                    glassView.post {
                        glassView.visibility = View.VISIBLE
                        val layoutParams = glassView.layoutParams as? FrameLayout.LayoutParams
                            ?: FrameLayout.LayoutParams(size.width, size.height)
                        layoutParams.width = size.width
                        layoutParams.height = size.height
                        glassView.layoutParams = layoutParams
                        glassView.translationX = position.x
                        glassView.translationY = position.y

                        val safeGlass = glassView as? btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView
                        val source = MainActivity.contentComposeViewRef
                        if (safeGlass != null && source != null) {
                            safeGlass.initializeRealGlass(source)
                        }
                        MainActivity.safeConfigure(
                            view = glassView,
                            red = 1.0f,
                            green = 1.0f,
                            blue = 1.0f,
                            alpha = 0.16f,
                            cornerRadius = size.height / 2f,
                            blurRadius = blurRadiusPx,
                            refractionHeight = refractionHeightPx
                        )
                        glassView.setRefractionOffset(refractionOffsetPx)
                        glassView.setDispersion(0.33f)
                        glassView.setDraggableEnabled(false)
                        glassView.setElasticEnabled(false)
                        glassView.setTouchEffectEnabled(true)
                        safeGlass?.configureDetailFrame(
                            cornerRadius = size.height / 2f,
                            strokeWidth = strokeWidthPx,
                            startColor = Color.White.copy(alpha = 0.22f).toArgb(),
                            endColor = Color.White.copy(alpha = 0.14f).toArgb()
                        )
                        glassView.invalidate()
                    }
                }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (SHOW_FULLSCREEN_CLOCK_ENTRY) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            context.startActivity(Intent(context, btm.m.todaywallpaper.ui.screens.FullscreenClockActivity::class.java))
                        }
                        .testTag("home_fullscreen_clock_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Fullscreen, "Fullscreen clock", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Box(
                    Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.2f))
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = hasPrevious) { viewModel.goToPreviousWallpaper() }
                    .testTag("home_prev_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    "Previous",
                    tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { viewModel.goToNextWallpaper() }
                    .testTag("home_next_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun RowScope.CapsuleNavItem(
    selected: Boolean,
    interactionFraction: Float,
    touchFraction: Float,
    pressed: Boolean,
    onClick: () -> Unit,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    tag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        // A selected item keeps its normal size. Enlargement is reserved for the
        // current touch/drag gesture and returns to 1x as soon as it ends.
        targetValue = if (pressed || isPressed) 1.2f else 1f,
        label = "TabScale"
    )
    val animatedEmphasis by animateFloatAsState(
        targetValue = interactionFraction,
        label = "TabEmphasis"
    )
    val contentColor = lerp(unselectedContentColor, selectedContentColor, animatedEmphasis)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(299.dp))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null // Let scale and color speak for themselves cleanly
            )
            .scale(scale)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(86.4f.dp)
                .height(62.dp)
                .clip(RoundedCornerShape(299.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selected) activeIcon else inactiveIcon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp) // Enlarged by ~110% of 24.dp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 13.sp, // Enlarged by ~110% of 12.sp
                    fontWeight = if (animatedEmphasis > 0.5f) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor
                )
            }
        }
    }
}
