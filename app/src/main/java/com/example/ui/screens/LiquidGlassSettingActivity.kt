package btm.m.todaywallpaper.ui.screens

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import btm.m.todaywallpaper.ui.theme.MyApplicationTheme
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import kotlin.math.roundToInt

class LiquidGlassSettingActivity : ComponentActivity() {

    private val viewModel: WallpaperViewModel by viewModels()

    private var onFilePicked: ((String) -> Unit)? = null
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    onFilePicked?.invoke(content)
                }
            } catch (_: Exception) {}
        }
    }

    fun launchFilePicker(callback: (String) -> Unit) {
        onFilePicked = callback
        try {
            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.loadPresets(this)

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val contentComposeView = ComposeView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val capsuleGlassView = btm.m.todaywallpaper.ui.widget.SafeLiquidGlassView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(0, 0)
            isClickable = false
            isFocusable = false
        }

        rootLayout.addView(contentComposeView)
        rootLayout.addView(capsuleGlassView)

        setContentView(rootLayout)

        capsuleGlassView.post {
            try {
                capsuleGlassView.bind(contentComposeView)
                capsuleGlassView.requestLayout()
                capsuleGlassView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        contentComposeView.setContent {
            MyApplicationTheme {
                LiquidGlassSettingScreen(
                    viewModel = viewModel,
                    capsuleGlassView = capsuleGlassView,
                    activity = this,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun LiquidGlassSettingScreen(
    viewModel: WallpaperViewModel,
    capsuleGlassView: com.qmdeve.liquidglass.widget.LiquidGlassView,
    activity: LiquidGlassSettingActivity,
    onBack: () -> Unit
) {
    val currentLang by viewModel.language.collectAsState()
    val liquidGlassBlur by viewModel.liquidGlassBlur.collectAsState()
    val advanced by viewModel.liquidGlassAdvanced.collectAsState()
    val refractionHeight by viewModel.lgRefractionHeight.collectAsState()
    val refractionOffset by viewModel.lgRefractionOffset.collectAsState()
    val tintAlpha by viewModel.lgTintAlpha.collectAsState()
    val dispersion by viewModel.lgDispersion.collectAsState()
    val draggable by viewModel.lgDraggable.collectAsState()
    val elastic by viewModel.lgElastic.collectAsState()
    val touchEffect by viewModel.lgTouchEffect.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val activePresetName by viewModel.activePresetName.collectAsState()
    val isDark = isSystemInDarkTheme()

    // Management mode state
    var isManageMode by remember { mutableStateOf(false) }
    val selectedForDelete = remember { mutableStateListOf<String>() }

    val natureSampleUrls = remember {
        listOf(
            "https://images.pexels.com/photos/3225517/pexels-photo-3225517.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/1287145/pexels-photo-1287145.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2559941/pexels-photo-2559941.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2662116/pexels-photo-2662116.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/147411/italy-mountains-dawn-daybreak-147411.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/417074/pexels-photo-417074.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/3408744/pexels-photo-3408744.jpeg?auto=compress&cs=tinysrgb&w=800",
            "https://images.pexels.com/photos/2325447/pexels-photo-2325447.jpeg?auto=compress&cs=tinysrgb&w=800"
        )
    }
    val selectedImageUrl = remember { natureSampleUrls.random() }

    val view = LocalView.current
    val context = view.context
    // Set default active preset to "通透" if none selected
    LaunchedEffect(Unit) {
        if (viewModel.activePresetName.value == null) {
            val defaultPreset = presets.firstOrNull { it.name == "通透" }
            if (defaultPreset != null) {
                viewModel.applyPreset(context, defaultPreset)
            }
        }
    }
    DisposableEffect(isDark) {
        val window = (context as? android.app.Activity)?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {}
    }

    var backProgress by remember { mutableStateOf(0f) }
    androidx.activity.compose.PredictiveBackHandler { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                backProgress = backEvent.progress
            }
            backProgress = 1f
            onBack()
        } catch (e: Exception) {
            backProgress = 0f
        }
    }

    val scale = 1f - (backProgress * 0.08f)
    val translationXDp = (backProgress * 120).dp
    val alphaVal = 1f - (backProgress * 0.2f)
    val cornerRadius = (backProgress * 24).dp
    val density = LocalDensity.current

    val cornerRadiusPx = with(density) { 299.dp.toPx() }
    val blurRadiusPx = with(density) { liquidGlassBlur.dp.toPx() }
    val refractionHeightPx = with(density) { refractionHeight.dp.toPx() }
    val refractionOffsetPx = with(density) { refractionOffset.dp.toPx() }

    DisposableEffect(blurRadiusPx, isDark, tintAlpha, refractionHeightPx, refractionOffsetPx, dispersion, draggable, elastic, touchEffect) {
        val applyConfig = {
            btm.m.todaywallpaper.MainActivity.safeConfigure(
                view = capsuleGlassView,
                red = if (isDark) 0.0f else 1.0f,
                green = if (isDark) 0.0f else 1.0f,
                blue = if (isDark) 0.0f else 1.0f,
                alpha = tintAlpha,
                cornerRadius = cornerRadiusPx,
                blurRadius = blurRadiusPx,
                refractionHeight = refractionHeightPx
            )
            try {
                capsuleGlassView.setRefractionOffset(refractionOffsetPx)
                if (dispersion > 0f) capsuleGlassView.setDispersion(dispersion)
                capsuleGlassView.setDraggableEnabled(draggable)
                capsuleGlassView.setElasticEnabled(elastic)
                capsuleGlassView.setTouchEffectEnabled(touchEffect)
                capsuleGlassView.invalidate()
            } catch (_: Exception) {}
        }

        val listener = View.OnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            if (right - left > 0 && bottom - top > 0) {
                applyConfig()
            }
        }
        capsuleGlassView.addOnLayoutChangeListener(listener)
        if (capsuleGlassView.width > 0 && capsuleGlassView.height > 0) {
            applyConfig()
        }
        capsuleGlassView.post { applyConfig() }

        onDispose {
            capsuleGlassView.removeOnLayoutChangeListener(listener)
        }
    }

    // Dialog states
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    if (showSaveDialog) {
        SavePresetDialog(
            currentLang = currentLang,
            existingNames = presets.map { it.name },
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCurrentAsPreset(context, name)
                showSaveDialog = false
            }
        )
    }

    if (showImportDialog) {
        ImportPresetDialog(
            currentLang = currentLang,
            onDismiss = { showImportDialog = false },
            onImport = { jsonStr ->
                val (count, error) = viewModel.importPresetsFromJson(context, jsonStr)
                if (count > 0) {
                    android.widget.Toast.makeText(
                        context,
                        if (currentLang == "zh") "成功导入 $count 个预设" else "Successfully imported $count preset(s)",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        error.ifEmpty { if (currentLang == "zh") "导入失败" else "Import failed" },
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                showImportDialog = false
            },
            onPickFile = { callback ->
                activity.launchFilePicker(callback)
            }
        )
    }

    val userPresets = remember(presets) { presets.filter { !it.isBuiltIn } }
    if (showExportDialog) {
        ExportPresetDialog(
            currentLang = currentLang,
            presets = userPresets,
            onDismiss = { showExportDialog = false },
            onExport = { selectedNames ->
                val json = viewModel.exportPresetsToJson(context, selectedNames.ifEmpty { null })
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("LiquidGlassPreset", json)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(
                    context,
                    if (currentLang == "zh") "已复制到剪贴板" else "Copied to clipboard",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showExportDialog = false
            }
        )
    }

    // Delete confirmation dialog
    var showDeleteConfirm by remember { mutableStateOf(false) }
    // Swipe delete confirmation
    var showSwipeDeleteConfirm by remember { mutableStateOf(false) }
    var pendingSwipeDeleteName by remember { mutableStateOf<String?>(null) }

    if (showSwipeDeleteConfirm && pendingSwipeDeleteName != null) {
        AlertDialog(
            onDismissRequest = { showSwipeDeleteConfirm = false; pendingSwipeDeleteName = null },
            title = { Text(if (currentLang == "zh") "删除预设" else "Delete Preset", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (currentLang == "zh") "确定要删除预设「${pendingSwipeDeleteName}」吗？此操作不可撤销。"
                    else "Are you sure you want to delete preset \"${pendingSwipeDeleteName}\"? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingSwipeDeleteName?.let { viewModel.deletePreset(context, it) }
                        showSwipeDeleteConfirm = false
                        pendingSwipeDeleteName = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(if (currentLang == "zh") "删除" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showSwipeDeleteConfirm = false; pendingSwipeDeleteName = null }) {
                    Text(if (currentLang == "zh") "取消" else "Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm && selectedForDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (currentLang == "zh") "删除预设" else "Delete Presets", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (currentLang == "zh") "确定要删除选中的 ${selectedForDelete.size} 个预设吗？此操作不可撤销。"
                    else "Are you sure you want to delete ${selectedForDelete.size} selected preset(s)? This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedForDelete.forEach { name ->
                            viewModel.deletePreset(context, name)
                        }
                        selectedForDelete.clear()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(if (currentLang == "zh") "删除" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (currentLang == "zh") "取消" else "Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = with(density) { translationXDp.toPx() },
                alpha = alphaVal,
                clip = cornerRadius > 0.dp,
                shape = RoundedCornerShape(cornerRadius)
            )
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                        tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = if (currentLang == "zh") "Liquid Glass 调整" else "Liquid Glass Adjustment",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Preview card - text layer ABOVE glass
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                AsyncImage(model = selectedImageUrl, contentDescription = "Nature",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(0.1f), Color.Black.copy(0.4f)))
                ))

                // Glass capsule positioning target
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.75f)
                        .height(52.dp)
                        .onGloballyPositioned { coordinates ->
                            val size = coordinates.size
                            val position = coordinates.positionInRoot()
                            capsuleGlassView.post {
                                val lp = capsuleGlassView.layoutParams as? FrameLayout.LayoutParams
                                if (lp != null) { lp.width = size.width; lp.height = size.height; capsuleGlassView.layoutParams = lp }
                                else { capsuleGlassView.layoutParams = FrameLayout.LayoutParams(size.width, size.height) }
                                capsuleGlassView.translationX = position.x
                                capsuleGlassView.translationY = position.y
                                btm.m.todaywallpaper.MainActivity.safeConfigure(
                                    view = capsuleGlassView,
                                    red = if (isDark) 0.0f else 1.0f,
                                    green = if (isDark) 0.0f else 1.0f,
                                    blue = if (isDark) 0.0f else 1.0f,
                                    alpha = tintAlpha,
                                    cornerRadius = cornerRadiusPx,
                                    blurRadius = blurRadiusPx,
                                    refractionHeight = refractionHeightPx
                                )
                                try {
                                    capsuleGlassView.setRefractionOffset(refractionOffsetPx)
                                    if (dispersion > 0f) capsuleGlassView.setDispersion(dispersion)
                                    capsuleGlassView.setDraggableEnabled(draggable)
                                    capsuleGlassView.setElasticEnabled(elastic)
                                    capsuleGlassView.setTouchEffectEnabled(touchEffect)
                                } catch (_: Exception) {}
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Border overlay above glass
                    Box(
                        Modifier.fillMaxSize().clip(RoundedCornerShape(299.dp))
                            .border(1.dp, Brush.linearGradient(
                                if (isDark) listOf(Color.White.copy(0.25f), Color.White.copy(0.08f))
                                else listOf(Color.White.copy(0.5f), Color.White.copy(0.15f))
                            ), RoundedCornerShape(299.dp))
                    )
                    // Text above glass (rendered on top via Box stacking)
                    Text("TodayWallpaper", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Scrollable controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Blur Radius - max 25dp, precision 0.01
                SliderCard(
                    title = if (currentLang == "zh") "模糊半径" else "Blur Radius",
                    value = liquidGlassBlur,
                    onValueChange = { viewModel.setLiquidGlassBlur(it) },
                    valueRange = 0f..25f,
                    label = { String.format("%.2fdp", it) },
                    description = if (currentLang == "zh") "调整模糊半径。左端最通透（0dp），右端最模糊（25dp）。"
                    else "Adjust blur radius. Left is clear (0dp), right is blurred (25dp)."
                )

                Spacer(Modifier.height(16.dp))

                // Preset Card with swipe + management
                PresetCard(
                    currentLang = currentLang,
                    presets = presets,
                    activePresetName = activePresetName,
                    isManageMode = isManageMode,
                    selectedForDelete = selectedForDelete,
                    onSelectPreset = { preset ->
                        if (!isManageMode) viewModel.applyPreset(context, preset)
                    },
                    onToggleManageMode = { isManageMode = it },
                    onToggleSelection = { name ->
                        if (name in selectedForDelete) selectedForDelete.remove(name)
                        else selectedForDelete.add(name)
                    },
                    onSwipeDelete = { name ->
                        if (!LiquidGlassPresetManager.BUILT_IN_PRESETS.any { it.name == name }) {
                            pendingSwipeDeleteName = name
                            showSwipeDeleteConfirm = true
                        }
                    },
                    onDeleteSelected = { showDeleteConfirm = true },
                    onSave = { showSaveDialog = true },
                    onImport = { showImportDialog = true },
                    onExport = { showExportDialog = true }
                )

                Spacer(Modifier.height(16.dp))

                // Advanced toggle
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (currentLang == "zh") "高级参数调整" else "Advanced Parameters",
                                fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentLang == "zh") "微调折射、着色、色散等效果" else "Fine-tune refraction, tint, dispersion effects",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        }
                        Switch(checked = advanced, onCheckedChange = { viewModel.setLiquidGlassAdvanced(it) })
                    }
                }

                AnimatedVisibility(
                    visible = advanced,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        SliderCard(
                            title = if (currentLang == "zh") "折射高度" else "Refraction Height",
                            value = refractionHeight,
                            onValueChange = { viewModel.setLgRefractionHeight(it) },
                            valueRange = 12f..50f,
                            label = { "${it.toInt()}dp" },
                            description = if (currentLang == "zh") "SetRefractionHeight (12dp-50dp)，默认 20dp"
                            else "SetRefractionHeight (12dp-50dp), default 20dp"
                        )
                        Spacer(Modifier.height(12.dp))
                        SliderCard(
                            title = if (currentLang == "zh") "折射偏移量" else "Refraction Offset",
                            value = refractionOffset,
                            onValueChange = { viewModel.setLgRefractionOffset(it) },
                            valueRange = 20f..120f,
                            label = { "${it.toInt()}dp" },
                            description = if (currentLang == "zh") "SetRefractionOffset (20dp-120dp)，默认 70dp"
                            else "SetRefractionOffset (20dp-120dp), default 70dp"
                        )
                        Spacer(Modifier.height(12.dp))
                        SliderCard(
                            title = if (currentLang == "zh") "着色不透明度" else "Tint Alpha",
                            value = tintAlpha,
                            onValueChange = { viewModel.setLgTintAlpha(it) },
                            valueRange = 0f..1f,
                            label = { "${(it * 100).toInt()}%" },
                            description = if (currentLang == "zh") "SetTintAlpha (0f-1f)，默认 0f（透明）"
                            else "SetTintAlpha (0f-1f), default 0f (transparent)"
                        )
                        Spacer(Modifier.height(12.dp))
                        SliderCard(
                            title = if (currentLang == "zh") "色散效果系数" else "Dispersion",
                            value = dispersion,
                            onValueChange = { viewModel.setLgDispersion(it) },
                            valueRange = 0f..1f,
                            label = { "${(it * 100).toInt()}%" },
                            description = if (currentLang == "zh") "SetDispersion (0f-1f)，默认 0f"
                            else "SetDispersion (0f-1f), default 0f"
                        )
                        Spacer(Modifier.height(12.dp))
                        ToggleRow(
                            title = if (currentLang == "zh") "启用拖拽" else "Draggable",
                            checked = draggable,
                            onCheckedChange = { viewModel.setLgDraggable(it) },
                            description = "SetDraggableEnabled, default false"
                        )
                        ToggleRow(
                            title = if (currentLang == "zh") "启用弹性效果" else "Elastic Effect",
                            checked = elastic,
                            onCheckedChange = { viewModel.setLgElastic(it) },
                            description = "SetElasticEnabled, default false"
                        )
                        ToggleRow(
                            title = if (currentLang == "zh") "启用触摸反馈" else "Touch Effect",
                            checked = touchEffect,
                            onCheckedChange = { viewModel.setLgTouchEffect(it) },
                            description = "SetTouchEffectEnabled, default false"
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }

        // Floating capsule menu for management mode (bottom-right, drawn last = on top)
        if (isManageMode) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 32.dp),
                shape = RoundedCornerShape(299.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = selectedForDelete.isNotEmpty()) {
                                if (selectedForDelete.isNotEmpty()) showDeleteConfirm = true
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (selectedForDelete.isNotEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (currentLang == "zh") "删除选中预设" else "Delete",
                            fontSize = 10.sp,
                            color = if (selectedForDelete.isNotEmpty()) MaterialTheme.colorScheme.onSurface.copy(0.7f)
                            else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                    // Cancel button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                isManageMode = false
                                selectedForDelete.clear()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (currentLang == "zh") "退出" else "Cancel",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// Preset Card with swipe-to-reveal + management mode
// ============================================================

@Composable
private fun PresetCard(
    currentLang: String,
    presets: List<LiquidGlassPreset>,
    activePresetName: String?,
    isManageMode: Boolean,
    selectedForDelete: List<String>,
    onSelectPreset: (LiquidGlassPreset) -> Unit,
    onToggleManageMode: (Boolean) -> Unit,
    onToggleSelection: (String) -> Unit,
    onSwipeDelete: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onSave: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isManageMode) {
                            if (currentLang == "zh") "管理模式 (${selectedForDelete.size})" else "Manage Mode (${selectedForDelete.size})"
                        } else {
                            if (currentLang == "zh") "预设管理" else "Preset Management"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = activePresetName ?: if (currentLang == "zh") "未选择预设" else "No preset selected",
                        fontSize = 12.sp,
                        color = if (activePresetName != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = Color.Black.copy(alpha = 0.06f)
            )

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PresetActionButton(
                    icon = Icons.Default.Save,
                    label = if (currentLang == "zh") "保存预设" else "Save Preset",
                    onClick = onSave
                )
                PresetActionButton(
                    icon = Icons.Default.FileUpload,
                    label = if (currentLang == "zh") "导入预设" else "Import",
                    onClick = onImport
                )
                PresetActionButton(
                    icon = Icons.Default.FileDownload,
                    label = if (currentLang == "zh") "导出预设" else "Export",
                    onClick = onExport
                )
                PresetActionButton(
                    icon = Icons.Default.AppRegistration,
                    label = if (currentLang == "zh") "管理预设" else "Manage",
                    onClick = {
                        if (isManageMode) {
                            onToggleManageMode(false)
                            selectedForDelete.let { /* clear via parent */ }
                        } else {
                            onToggleManageMode(true)
                            isExpanded = true
                        }
                    }
                )
            }

            // Expandable preset list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Color.Black.copy(alpha = 0.06f)
                    )

                    if (presets.isEmpty()) {
                        Text(
                            text = if (currentLang == "zh") "暂无预设，请先保存" else "No presets yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.35f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                        )
                    } else {
                        presets.forEachIndexed { index, preset ->
                            if (isManageMode) {
                                // Management mode: checkbox rows
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !preset.isBuiltIn) {
                                            onToggleSelection(preset.name)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = preset.name in selectedForDelete,
                                        onCheckedChange = {
                                            if (!preset.isBuiltIn) onToggleSelection(preset.name)
                                        },
                                        enabled = !preset.isBuiltIn
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = preset.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (preset.name == activePresetName) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (preset.name == activePresetName) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (preset.isBuiltIn) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = if (currentLang == "zh") "内置" else "Built-in",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.onSurface.copy(0.06f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Blur: ${String.format("%.2f", preset.blur)}dp · RH: ${preset.refractionHeight.toInt()} · RO: ${preset.refractionOffset.toInt()}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.35f)
                                        )
                                    }
                                }
                            } else {
                                // Normal mode: swipeable rows with delete
                                SwipeablePresetRow(
                                    preset = preset,
                                    isActive = preset.name == activePresetName,
                                    currentLang = currentLang,
                                    onClick = { onSelectPreset(preset) },
                                    onDelete = { onSwipeDelete(preset.name) }
                                )
                            }
                            if (index < presets.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = if (isManageMode) 16.dp else 20.dp),
                                    color = Color.Black.copy(alpha = 0.04f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

// ============================================================
// Swipeable preset row (iOS-style swipe to reveal)
// ============================================================

@Composable
private fun SwipeablePresetRow(
    preset: LiquidGlassPreset,
    isActive: Boolean,
    currentLang: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val swipeThreshold = with(LocalDensity.current) { 120.dp.toPx() }
    val maxSwipe = with(LocalDensity.current) { 200.dp.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "swipeOffset"
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    ) {
        // Background action buttons (apply + delete)
        Row(
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apply button
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF34C759))
                    .clickable { onClick(); offsetX = 0f },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, "Apply", tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(if (currentLang == "zh") "应用" else "Apply", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            // Delete button (only for non-built-in presets)
            if (!preset.isBuiltIn) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFFF3B30))
                        .clickable { onDelete(); offsetX = 0f },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(if (currentLang == "zh") "删除" else "Delete", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Foreground card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (offsetX < -swipeThreshold) -maxSwipe else 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-maxSwipe, 0f)
                        }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.name,
                            fontSize = 14.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (preset.isBuiltIn) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (currentLang == "zh") "内置" else "Built-in",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onSurface.copy(0.06f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Blur: ${String.format("%.2f", preset.blur)}dp · RH: ${preset.refractionHeight.toInt()} · RO: ${preset.refractionOffset.toInt()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.35f)
                    )
                }
                if (isActive) {
                    Icon(Icons.Default.Check, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PresetActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            textAlign = TextAlign.Center, maxLines = 1)
    }
}

// ============================================================
// Save Preset Dialog
// ============================================================

@Composable
private fun SavePresetDialog(
    currentLang: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentLang == "zh") "保存预设" else "Save Preset", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    if (currentLang == "zh") "输入预设名称以保存当前 Liquid Glass 配置。"
                    else "Enter a preset name to save the current Liquid Glass configuration.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text(if (currentLang == "zh") "预设名称" else "Preset Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                when {
                    trimmed.isBlank() -> {
                        nameError = if (currentLang == "zh") "名称不能为空" else "Name cannot be empty"
                    }
                    LiquidGlassPresetManager.BUILT_IN_PRESETS.any { it.name == trimmed } -> {
                        nameError = if (currentLang == "zh") "不能覆盖内置预设" else "Cannot overwrite built-in preset"
                    }
                    trimmed in existingNames -> {
                        nameError = if (currentLang == "zh") "该名称已存在，将覆盖原预设" else "Name already exists, will overwrite"
                        onSave(trimmed)
                    }
                    else -> onSave(trimmed)
                }
            }) { Text(if (currentLang == "zh") "保存" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (currentLang == "zh") "取消" else "Cancel") }
        }
    )
}

// ============================================================
// Import Preset Dialog
// ============================================================

@Composable
private fun ImportPresetDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    onPickFile: ((String) -> Unit) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentLang == "zh") "导入预设" else "Import Presets", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    if (currentLang == "zh") "粘贴 JSON 格式的预设数据，或从文件导入。"
                    else "Paste JSON preset data, or import from a file.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it; isError = false },
                    label = { Text("JSON") },
                    placeholder = { Text("""[{"name":"My Preset","blur":8,...}]""", fontSize = 12.sp) },
                    minLines = 4, maxLines = 8,
                    isError = isError,
                    supportingText = if (isError) {{ Text(if (currentLang == "zh") "无效的 JSON 格式" else "Invalid JSON format") }} else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onPickFile { content -> onImport(content) } },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (currentLang == "zh") "从文件导入" else "Import from file")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = jsonText.trim()
                if (trimmed.isBlank() || (!trimmed.startsWith("{") && !trimmed.startsWith("["))) {
                    isError = true
                } else {
                    onImport(trimmed)
                }
            }) { Text(if (currentLang == "zh") "导入" else "Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (currentLang == "zh") "取消" else "Cancel") }
        }
    )
}

// ============================================================
// Export Preset Dialog
// ============================================================

@Composable
private fun ExportPresetDialog(
    currentLang: String,
    presets: List<LiquidGlassPreset>,
    onDismiss: () -> Unit,
    onExport: (List<String>) -> Unit
) {
    val selectedNames = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentLang == "zh") "导出预设" else "Export Presets", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    if (currentLang == "zh") "选择要导出的预设（可多选）：" else "Select presets to export:",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                if (presets.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (currentLang == "zh") "暂无用户预设可导出" else "No user presets to export",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.35f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (selectedNames.size == presets.size) selectedNames.clear()
                                else { selectedNames.clear(); selectedNames.addAll(presets.map { it.name }) }
                            }.padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedNames.size == presets.size && presets.isNotEmpty(),
                            onCheckedChange = { checked ->
                                if (checked) { selectedNames.clear(); selectedNames.addAll(presets.map { it.name }) }
                                else selectedNames.clear()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (currentLang == "zh") "全选" else "Select All",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Color.Black.copy(0.06f))
                    presets.forEach { preset ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (preset.name in selectedNames) selectedNames.remove(preset.name)
                                    else selectedNames.add(preset.name)
                                }.padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = preset.name in selectedNames,
                                onCheckedChange = { checked ->
                                    if (checked) selectedNames.add(preset.name)
                                    else selectedNames.remove(preset.name)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(preset.name, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(selectedNames.toList()) }, enabled = selectedNames.isNotEmpty()) {
                Text(if (currentLang == "zh") "导出 (${selectedNames.size})" else "Export (${selectedNames.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (currentLang == "zh") "取消" else "Cancel") }
        }
    )
}

// ============================================================
// Reusable Slider Card (precision 0.01)
// ============================================================

@Composable
private fun SliderCard(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: (Float) -> String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        val rangeMin: Float = valueRange.start
        val rangeMax: Float = valueRange.endInclusive
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(label(rangeMin), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                Text(label(value), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
                Text(label(rangeMax), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Slider(
                value = value,
                onValueChange = { onValueChange((it * 100).roundToInt() / 100f) },
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(0.15f)
                )
            )
            Text(description, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.4f), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}