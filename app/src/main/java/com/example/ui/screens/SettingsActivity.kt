package btm.m.todaywallpaper.ui.screens

import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.saveable.rememberSaveable
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.theme.AppThemeMode
import btm.m.todaywallpaper.ui.theme.AppThemePreference
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView
import btm.m.todaywallpaper.ui.widget.enableMomentumTransparentWindow
import btm.m.todaywallpaper.ui.widget.momentumBackTransform
import btm.m.todaywallpaper.ui.widget.rememberMomentumPredictiveBack

private const val SHOW_FULLSCREEN_CLOCK_CUSTOM_IMAGE_ENTRY = false

class SettingsActivity : ComponentActivity() {
    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableMomentumTransparentWindow()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SettingsScreen(viewModel = viewModel, onBack = ::finish)
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: WallpaperViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val language by viewModel.language.collectAsState()
    val predictiveEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val predictiveMax by viewModel.predictiveBackMaxProgress.collectAsState()
    val themeMode by AppThemePreference.mode.collectAsState()
    val backState = rememberMomentumPredictiveBack(predictiveEnabled, predictiveMax, onBack)

    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .momentumBackTransform(backState)
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    viewModel.getTranslation("设置", "Settings"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsGroup {
                    SettingsRow(Icons.Rounded.Language, viewModel.getTranslation("语言", "Language"), if (language == "zh") "简体中文" else "English") {
                        viewModel.toggleLanguage()
                    }
                    Row(
                        Modifier.fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.DarkMode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            viewModel.getTranslation("外观", "Appearance"),
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AppearanceModePicker(
                            selected = themeMode,
                            labels = mapOf(
                                AppThemeMode.SYSTEM to viewModel.getTranslation("跟随系统", "Follow system"),
                                AppThemeMode.LIGHT to viewModel.getTranslation("浅色模式", "Light mode"),
                                AppThemeMode.DARK to viewModel.getTranslation("深色模式", "Dark mode")
                            ),
                            expandDescription = viewModel.getTranslation("展开外观模式", "Expand appearance mode"),
                            collapseDescription = viewModel.getTranslation("收起外观模式", "Collapse appearance mode"),
                            onSelected = { AppThemePreference.setMode(context, it) }
                        )
                    }
                    SettingsRow(Icons.Rounded.Palette, viewModel.getTranslation("首页沉浸壁纸风格", "Homepage Immersive Style")) {
                        context.startActivity(Intent(context, StyleSettingActivity::class.java))
                    }
                }

                SettingsGroup {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                viewModel.getTranslation("预测性返回", "Predictive Back"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = predictiveEnabled,
                            onCheckedChange = viewModel::setPredictiveBackEnabled,
                            colors = monochromeSwitchColors()
                        )
                    }
                    AnimatedVisibility(predictiveEnabled) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                viewModel.getTranslation("最大动画进度：$predictiveMax%", "Maximum animation progress: $predictiveMax%"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = predictiveMax.toFloat(),
                                onValueChange = { viewModel.setPredictiveBackMaxProgress(it.toInt()) },
                                valueRange = 10f..100f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                                    activeTickColor = MaterialTheme.colorScheme.onPrimary,
                                    inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            )
                        }
                    }
                }

                SettingsGroup {
                    SettingsRow(Icons.Rounded.BlurOn, viewModel.getTranslation("Liquid Glass 调整", "Liquid Glass Adjustment")) {
                        context.startActivity(Intent(context, LiquidGlassSettingActivity::class.java))
                    }
                    SettingsRow(Icons.Rounded.Dashboard, viewModel.getTranslation("壁纸设置范围", "Wallpaper Scope")) {
                        context.startActivity(Intent(context, WallpaperScopeSettingActivity::class.java))
                    }
                    SettingsRow(Icons.Rounded.AutoAwesome, viewModel.getTranslation("自定义开屏界面", "Custom Splash Screen")) {
                        context.startActivity(Intent(context, SplashSettingActivity::class.java))
                    }
                    if (SHOW_FULLSCREEN_CLOCK_CUSTOM_IMAGE_ENTRY) {
                        SettingsRow(Icons.Rounded.Fullscreen, viewModel.getTranslation("全屏时钟自定义图片", "Fullscreen Clock Image")) {
                            context.startActivity(Intent(context, FullscreenClockSettingActivity::class.java))
                        }
                    }
                    SettingsRow(Icons.Rounded.Autorenew, viewModel.getTranslation("自动切换桌面壁纸", "Auto Switch Wallpaper")) {
                        context.startActivity(Intent(context, AutoSwitchWallpaperActivity::class.java))
                    }
                    SettingsRow(Icons.Rounded.VpnKey, viewModel.getTranslation("API 设置", "API Settings")) {
                        context.startActivity(Intent(context, ApiKeysActivity::class.java))
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * App-specific appearance popup defined by ANDROID_DROPDOWN_SPEC.
 *
 * This deliberately uses Popup + a fully styled Surface instead of Material's
 * DropdownMenu, whose built-in shape, padding and elevation vary by version.
 */
@Composable
private fun AppearanceModePicker(
    selected: AppThemeMode,
    labels: Map<AppThemeMode, String>,
    expandDescription: String,
    collapseDescription: String,
    onSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val glassSource = LocalView.current as? ViewGroup
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val triggerFocusRequester = remember { FocusRequester() }
    val optionFocusRequesters = remember { AppThemeMode.entries.map { FocusRequester() } }
    val popupVisibility = remember { MutableTransitionState(false) }
    popupVisibility.targetState = expanded

    val menuBackground = if (isDark) Color(0xFF242426).copy(alpha = 0.96f)
        else Color(0xFFFAFAFA).copy(alpha = 0.96f)
    val menuBorder = if (isDark) Color(0xFF4A4A4E) else Color(0xFFC4C4C7)
    val menuContent = if (isDark) Color(0xFFF5F5F5) else Color(0xFF050505)
    val triggerContent = MaterialTheme.colorScheme.onSurfaceVariant
    val menuWidth = if (
        density.fontScale >= 1.3f || labels.values.any { it.length > 5 }
    ) 148.dp else 132.dp
    val popupGapPx = with(density) { 4.dp.roundToPx() }
    val animatedOffsetPx = with(density) { 4.dp.roundToPx() }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(150),
        label = "AppearanceMenuArrow"
    )

    fun dismissAndRestoreFocus() {
        expanded = false
        triggerFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .onSizeChanged { anchorHeightPx = it.height }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .focusRequester(triggerFocusRequester)
                .semantics {
                    contentDescription = labels[selected].orEmpty() + ", " +
                        if (expanded) collapseDescription else expandDescription
                }
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { expanded = !expanded }
                .focusable()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labels[selected].orEmpty(),
                color = triggerContent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp).rotate(arrowRotation),
                tint = triggerContent
            )
        }

        if (popupVisibility.currentState || popupVisibility.targetState) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, anchorHeightPx + popupGapPx),
                onDismissRequest = ::dismissAndRestoreFocus,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = true
                )
            ) {
                AnimatedVisibility(
                    visibleState = popupVisibility,
                    enter = fadeIn(tween(150)) + slideInVertically(
                        animationSpec = tween(150),
                        initialOffsetY = { -animatedOffsetPx }
                    ),
                    exit = fadeOut(tween(120)) + slideOutVertically(
                        animationSpec = tween(120),
                        targetOffsetY = { -animatedOffsetPx }
                    )
                ) {
                    Surface(
                        modifier = Modifier.width(menuWidth),
                        shape = RoundedCornerShape(26.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, menuBorder),
                        shadowElevation = 6.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(26.dp))
                                .background(menuBackground)
                        ) {
                            if (glassSource != null) {
                                AppearanceMenuLiquidGlass(
                                    source = glassSource,
                                    isDark = isDark,
                                    modifier = Modifier.matchParentSize()
                                )
                            } else {
                                // Preview/fallback only. At runtime the popup always
                                // receives the Settings ComposeView as its source.
                                Box(Modifier.matchParentSize().background(menuBackground))
                            }
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                                AppThemeMode.entries.forEachIndexed { index, mode ->
                                    val isSelected = selected == mode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .focusRequester(optionFocusRequesters[index])
                                            .clip(RoundedCornerShape(14.dp))
                                            .selectable(
                                                selected = isSelected,
                                                role = Role.RadioButton,
                                                onClick = {
                                                    onSelected(mode)
                                                    dismissAndRestoreFocus()
                                                }
                                            )
                                            .padding(horizontal = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = labels[mode].orEmpty(),
                                            modifier = Modifier.weight(1f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = menuContent,
                                            maxLines = 1
                                        )
                                        Box(
                                            modifier = Modifier.size(18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = menuContent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            // Let the focusable popup attach before focusing the selected radio item.
            withFrameNanos { }
            optionFocusRequesters[selected.ordinal].requestFocus()
        }
    }
}

/** Liquid Glass values for this popup are intentionally independent from app settings. */
@Composable
private fun AppearanceMenuLiquidGlass(
    source: ViewGroup,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { 26.dp.toPx() }
    val blurRadiusPx = with(density) { 4.dp.toPx() }
    val refractionHeightPx = with(density) { 18.dp.toPx() }
    val refractionOffsetPx = with(density) { 35.dp.toPx() }
    val samplingProxy = remember(source) {
        LiquidGlassSamplingProxy(source.context, source)
    }

    fun configure(view: SafeLiquidGlassView) {
        btm.m.todaywallpaper.MainActivity.safeConfigure(
            view = view,
            red = if (isDark) 0f else 1f,
            green = if (isDark) 0f else 1f,
            blue = if (isDark) 0f else 1f,
            alpha = 0.75f,
            cornerRadius = cornerRadiusPx,
            blurRadius = blurRadiusPx,
            refractionHeight = refractionHeightPx
        )
        try {
            view.setRefractionOffset(refractionOffsetPx)
            view.setDispersion(0.38f)
            view.setDraggableEnabled(false)
            view.setElasticEnabled(false)
            view.setTouchEffectEnabled(true)
            view.invalidate()
        } catch (t: Throwable) {
            android.util.Log.w("AppearanceMenuGlass", "Glass parameters are not ready", t)
        }
    }

    key(isDark, source, samplingProxy) {
        AndroidView(
            factory = { context ->
                SafeLiquidGlassView(context).apply {
                    var realGlassInitialized = false
                    val glassLocationOnScreen = IntArray(2)
                    val sourceLocationOnScreen = IntArray(2)
                    isClickable = false
                    isFocusable = false
                    setNavigationHighlightVisible(false)
                    addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                        if (right - left > 0 && bottom - top > 0) {
                            post {
                                // Popup owns a separate Android window. Screen
                                // coordinates are therefore required; window-local
                                // coordinates would incorrectly resolve to (0, 0).
                                getLocationOnScreen(glassLocationOnScreen)
                                source.getLocationOnScreen(sourceLocationOnScreen)
                                samplingProxy.setSourceOffset(
                                    x = (glassLocationOnScreen[0] - sourceLocationOnScreen[0]).toFloat(),
                                    y = (glassLocationOnScreen[1] - sourceLocationOnScreen[1]).toFloat()
                                )
                                if (!realGlassInitialized) {
                                    realGlassInitialized = initializeRealGlass(samplingProxy)
                                }
                                if (realGlassInitialized) configure(this)
                            }
                        }
                    }
                }
            },
            update = { view ->
                view.post {
                    if (view.width > 0 && view.height > 0) {
                        configure(view)
                    }
                }
            },
            modifier = modifier
        )
    }
}

/**
 * LiquidGlassView draws its source at the source's origin. Popup content lives
 * in another window, so binding the screen ComposeView directly would always
 * sample its top-left corner. This proxy draws only the screen rectangle under
 * the popup into the source origin, keeping refraction aligned with the menu.
 */
private class LiquidGlassSamplingProxy(
    context: android.content.Context,
    private val source: View
) : ViewGroup(context) {
    private var sourceOffsetX = 0f
    private var sourceOffsetY = 0f

    init {
        setWillNotDraw(false)
        layout(0, 0, source.width, source.height)
    }

    fun setSourceOffset(x: Float, y: Float) {
        sourceOffsetX = x
        sourceOffsetY = y
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(source.width, source.height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) = Unit

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(-sourceOffsetX, -sourceOffsetY)
        source.draw(canvas)
        canvas.restore()
    }
}

@Composable
private fun monochromeSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface),
        content = content
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(title, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        if (value != null) Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}