package btm.m.todaywallpaper.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import btm.m.todaywallpaper.data.model.AlbumCategory
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel

private enum class CreateCollectionPage {
    Form,
    CategoryPicker
}

internal data class CreateCollectionColors(
    val scrim: Color,
    val panel: Color,
    val field: Color,
    val text: Color,
    val muted: Color,
    val button: Color,
    val controlIcon: Color,
    val primaryButton: Color,
    val primaryButtonText: Color,
    val secondaryButton: Color,
    val secondaryButtonText: Color,
    val panelBorder: Color,
    val fieldBorder: Color,
    val focusedFieldBorder: Color,
    val handle: Color,
    val error: Color,
    val previewPlaceholder: Color
)

internal object CreateCollectionTokens {
    val panelShape = RoundedCornerShape(40.dp)
    val fieldShape = RoundedCornerShape(19.dp)
    val previewShape = RoundedCornerShape(14.dp)
    val contentHorizontal = 22.dp
    val contentTop = 48.dp
    val contentBottom = 22.dp
    val contentGap = 10.dp

    @Composable
    fun colors(): CreateCollectionColors {
        val colorScheme = MaterialTheme.colorScheme
        val isDark = colorScheme.background.luminance() < 0.5f
        return CreateCollectionColors(
            scrim = colorScheme.scrim.copy(alpha = if (isDark) 0.62f else 0.40f),
            panel = (if (isDark) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer)
                .copy(alpha = if (isDark) 0.96f else 0.90f),
            field = colorScheme.surfaceBright.copy(alpha = if (isDark) 0.92f else 0.94f),
            text = colorScheme.onSurface,
            muted = colorScheme.onSurfaceVariant,
            button = (if (isDark) colorScheme.surfaceContainerHighest else colorScheme.surfaceContainerHigh)
                .copy(alpha = if (isDark) 0.94f else 0.90f),
            controlIcon = colorScheme.onSurfaceVariant,
            primaryButton = colorScheme.primary,
            primaryButtonText = colorScheme.onPrimary,
            secondaryButton = colorScheme.surfaceBright.copy(
                alpha = if (isDark) 0.88f else 0.78f
            ),
            secondaryButtonText = colorScheme.onSurface,
            panelBorder = colorScheme.outline.copy(alpha = if (isDark) 0.70f else 0.38f),
            fieldBorder = colorScheme.outline.copy(alpha = if (isDark) 0.70f else 0.26f),
            focusedFieldBorder = colorScheme.onSurface,
            handle = colorScheme.onSurface.copy(alpha = 0.58f),
            error = if (isDark) Color(0xFFFFB4AB) else Color(0xFFB3261E),
            previewPlaceholder = colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
fun CreateCollectionDialog(
    viewModel: WallpaperViewModel,
    onDismiss: () -> Unit,
    requireImages: Boolean = true,
    onConfirm: (
        name: String,
        description: String?,
        categoryId: Long,
        imageUris: List<Uri>,
        onComplete: (Result<Unit>) -> Unit
    ) -> Unit
) {
    val colors = CreateCollectionTokens.colors()
    val categories by viewModel.albumCategories.collectAsState()
    val selectedUris = remember { mutableStateListOf<Uri>() }
    var page by remember { mutableStateOf(CreateCollectionPage.Form) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var pendingCategoryId by remember { mutableStateOf<Long?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var fieldError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var creatingCategory by remember { mutableStateOf(false) }
    var showNewCategoryInput by remember { mutableStateOf(false) }
    var panelDragOffset by remember { mutableStateOf(0f) }
    val animatedPanelDragOffset by animateFloatAsState(
        targetValue = panelDragOffset,
        animationSpec = if (panelDragOffset == 0f) tween(260) else snap(),
        label = "create_collection_panel_drag"
    )
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val existing = selectedUris.toSet()
        uris.filter { it !in existing }.forEach(selectedUris::add)
        fieldError = null
    }

    LaunchedEffect(categories) {
        if (pendingCategoryId == null) {
            pendingCategoryId = selectedCategoryId ?: categories.firstOrNull()?.id
        }
    }

    fun closeOverlay() {
        if (!submitting) onDismiss()
    }

    BackHandler(enabled = true) {
        when {
            submitting -> Unit
            page == CreateCollectionPage.CategoryPicker -> page = CreateCollectionPage.Form
            else -> onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                enabled = !submitting,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = ::closeOverlay
            )
            .semantics { contentDescription = "创建新图集" }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.60f)
                .heightIn(min = 400.dp)
                .graphicsLayer { translationY = animatedPanelDragOffset }
                .shadow(22.dp, CreateCollectionTokens.panelShape)
                .clip(CreateCollectionTokens.panelShape)
                .background(colors.panel)
                .border(
                    BorderStroke(1.dp, colors.panelBorder),
                    CreateCollectionTokens.panelShape
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
                    .padding(top = 14.dp)
                    .width(72.dp)
                    .height(28.dp)
                    .pointerInput(submitting) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                panelDragOffset = (panelDragOffset + amount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (panelDragOffset > 110.dp.toPx() && !submitting) {
                                    onDismiss()
                                } else {
                                    panelDragOffset = 0f
                                }
                            },
                            onDragCancel = { panelDragOffset = 0f }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(colors.handle)
                )
            }

            AnimatedContent(
                targetState = page,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = CreateCollectionTokens.contentTop),
                transitionSpec = {
                    (fadeIn(tween(240)) + slideInHorizontally(tween(260)) { it / 5 })
                        .togetherWith(
                            fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 5 }
                        )
                },
                label = "create_collection_page_transition"
            ) { targetPage ->
                when (targetPage) {
                    CreateCollectionPage.Form -> CreateCollectionForm(
                        name = name,
                        description = description,
                        selectedCategory = categories.firstOrNull { it.id == selectedCategoryId },
                        selectedUris = selectedUris,
                        fieldError = fieldError,
                        submitting = submitting,
                        onNameChange = {
                            name = it.take(30)
                            fieldError = null
                        },
                        onDescriptionChange = {
                            description = it.take(80)
                            fieldError = null
                        },
                        onOpenCategory = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            pendingCategoryId = selectedCategoryId ?: categories.firstOrNull()?.id
                            categoryError = null
                            page = CreateCollectionPage.CategoryPicker
                        },
                        onPickImages = { imagePicker.launch("image/*") },
                        onClearImages = {
                            selectedUris.clear()
                            fieldError = null
                        },
                        onSubmit = {
                            val normalizedName = name.trim()
                            when {
                                normalizedName.isEmpty() -> fieldError = viewModel.getTranslation(
                                    "请输入图集名称",
                                    "Enter an album name"
                                )
                                selectedCategoryId == null -> fieldError = "category"
                                requireImages && selectedUris.isEmpty() -> fieldError = "images"
                                else -> {
                                    submitting = true
                                    onConfirm(
                                        normalizedName,
                                        description.trim().ifEmpty { null },
                                        selectedCategoryId!!,
                                        selectedUris.toList()
                                    ) { result ->
                                        submitting = false
                                        result.onFailure { error ->
                                            fieldError = error.localizedMessage ?: viewModel.getTranslation(
                                                "创建失败，请重试",
                                                "Creation failed. Try again."
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onCancel = ::closeOverlay,
                        viewModel = viewModel
                    )

                    CreateCollectionPage.CategoryPicker -> CategoryPickerPage(
                        categories = categories,
                        pendingCategoryId = pendingCategoryId,
                        showNewCategoryInput = showNewCategoryInput,
                        categoryName = categoryName,
                        categoryError = categoryError,
                        creatingCategory = creatingCategory,
                        onSelect = {
                            pendingCategoryId = it.id
                            categoryError = null
                        },
                        onConfirm = {
                            selectedCategoryId = pendingCategoryId
                            categoryError = null
                            page = CreateCollectionPage.Form
                        },
                        onCategoryNameChange = {
                            categoryName = it.take(24)
                            categoryError = null
                        },
                        onSaveCategory = {
                            creatingCategory = true
                            viewModel.createAlbumCategory(categoryName) { result ->
                                creatingCategory = false
                                result.onSuccess { category ->
                                    pendingCategoryId = category.id
                                    categoryName = ""
                                    showNewCategoryInput = false
                                    categoryError = null
                                }.onFailure { error ->
                                    categoryError = error.localizedMessage
                                        ?: viewModel.getTranslation(
                                            "分类创建失败",
                                            "Could not create category"
                                        )
                                }
                            }
                        },
                        onCancelCategory = {
                            categoryName = ""
                            categoryError = null
                            showNewCategoryInput = false
                        },
                        onShowNewCategory = { showNewCategoryInput = true },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateCollectionForm(
    name: String,
    description: String,
    selectedCategory: AlbumCategory?,
    selectedUris: List<Uri>,
    fieldError: String?,
    submitting: Boolean,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onOpenCategory: () -> Unit,
    onPickImages: () -> Unit,
    onClearImages: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WallpaperViewModel
) {
    val colors = CreateCollectionTokens.colors()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = CreateCollectionTokens.contentHorizontal,
                        end = CreateCollectionTokens.contentHorizontal,
                        bottom = 12.dp
                    ),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = viewModel.getTranslation("创建新图集", "Create New Album"),
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    AlbumBasicInput(
                        value = name,
                        onValueChange = onNameChange,
                        placeholder = viewModel.getTranslation("图集名称", "Album name"),
                        label = viewModel.getTranslation("图集名称", "Album name"),
                        imeAction = ImeAction.Next,
                        modifier = Modifier.testTag("dialog_album_name_input")
                    )
                    AlbumBasicInput(
                        value = description,
                        onValueChange = onDescriptionChange,
                        placeholder = viewModel.getTranslation("图集描述", "Album description"),
                        label = viewModel.getTranslation("图集描述", "Album description"),
                        imeAction = ImeAction.Done
                    )
                    CategoryTrigger(
                        selected = selectedCategory,
                        label = viewModel.getTranslation("分类", "Category"),
                        onClick = onOpenCategory,
                        error = fieldError == "category"
                    )
                    ImagePickerArea(
                        selectedUris = selectedUris,
                        onPick = onPickImages,
                        onClear = onClearImages,
                        viewModel = viewModel,
                        error = fieldError == "images"
                    )
                }
                if (fieldError != null && fieldError != "category" && fieldError != "images") {
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = fieldError,
                        color = colors.error,
                        fontSize = 12.sp,
                        modifier = Modifier.semantics { contentDescription = fieldError }
                    )
                }
            }
        }
        CreateActionRow(
            submitting = submitting,
            onSubmit = onSubmit,
            onCancel = onCancel,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = CreateCollectionTokens.contentHorizontal,
                    end = CreateCollectionTokens.contentHorizontal,
                    top = CreateCollectionTokens.contentGap,
                    bottom = CreateCollectionTokens.contentBottom
                )
        )
    }
}

@Composable
private fun CategoryPickerPage(
    categories: List<AlbumCategory>,
    pendingCategoryId: Long?,
    showNewCategoryInput: Boolean,
    categoryName: String,
    categoryError: String?,
    creatingCategory: Boolean,
    onSelect: (AlbumCategory) -> Unit,
    onConfirm: () -> Unit,
    onCategoryNameChange: (String) -> Unit,
    onSaveCategory: () -> Unit,
    onCancelCategory: () -> Unit,
    onShowNewCategory: () -> Unit,
    viewModel: WallpaperViewModel
) {
    val colors = CreateCollectionTokens.colors()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = CreateCollectionTokens.contentHorizontal,
                        end = CreateCollectionTokens.contentHorizontal,
                        bottom = 12.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = viewModel.getTranslation("选择分类", "Choose Category"),
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.text
                    )
                    OverlayIconButton(
                        contentDescription = viewModel.getTranslation("确认分类", "Confirm category"),
                        enabled = pendingCategoryId != null,
                        onClick = onConfirm,
                        icon = Icons.Rounded.Check
                    )
                }
                Spacer(modifier = Modifier.height(CreateCollectionTokens.contentGap))
                CategoryList(
                    categories = categories,
                    pendingCategoryId = pendingCategoryId,
                    onSelect = onSelect,
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
        CategoryActionFooter(
            showNewCategoryInput = showNewCategoryInput,
            categoryName = categoryName,
            categoryError = categoryError,
            creatingCategory = creatingCategory,
            onCategoryNameChange = onCategoryNameChange,
            onSaveCategory = onSaveCategory,
            onCancelCategory = onCancelCategory,
            onShowNewCategory = onShowNewCategory,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = CreateCollectionTokens.contentHorizontal,
                    end = CreateCollectionTokens.contentHorizontal,
                    top = CreateCollectionTokens.contentGap,
                    bottom = CreateCollectionTokens.contentBottom
                )
        )
    }
}

@Composable
private fun CategoryActionFooter(
    showNewCategoryInput: Boolean,
    categoryName: String,
    categoryError: String?,
    creatingCategory: Boolean,
    onCategoryNameChange: (String) -> Unit,
    onSaveCategory: () -> Unit,
    onCancelCategory: () -> Unit,
    onShowNewCategory: () -> Unit,
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier
) {
    val colors = CreateCollectionTokens.colors()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showNewCategoryInput) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumBasicInput(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    placeholder = viewModel.getTranslation("新分类名称", "New category name"),
                    label = viewModel.getTranslation("新分类名称", "New category name"),
                    imeAction = ImeAction.Done,
                    enabled = !creatingCategory,
                    modifier = Modifier.weight(1f)
                )
                OverlayIconButton(
                    contentDescription = viewModel.getTranslation("保存分类", "Save category"),
                    enabled = !creatingCategory && categoryName.trim().isNotEmpty(),
                    onClick = onSaveCategory,
                    icon = Icons.Rounded.Check
                )
                OverlayIconButton(
                    contentDescription = viewModel.getTranslation("取消创建分类", "Cancel category creation"),
                    enabled = !creatingCategory,
                    onClick = onCancelCategory,
                    icon = Icons.Rounded.Close
                )
            }
            if (categoryError != null) {
                Text(
                    text = categoryError,
                    color = colors.error,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics { contentDescription = categoryError }
                )
            }
        } else {
            OverlayTextButton(
                text = viewModel.getTranslation("创建新分类", "Create New Category"),
                onClick = onShowNewCategory,
                primary = true,
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun AlbumBasicInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    imeAction: ImeAction,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = CreateCollectionTokens.colors()
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(
            color = colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(colors.text),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CreateCollectionTokens.fieldShape)
            .background(colors.field)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) colors.focusedFieldBorder else colors.fieldBorder,
                shape = CreateCollectionTokens.fieldShape
            )
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = colors.muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun CategoryTrigger(
    selected: AlbumCategory?,
    label: String,
    onClick: () -> Unit,
    error: Boolean
) {
    val colors = CreateCollectionTokens.colors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CreateCollectionTokens.fieldShape)
            .background(colors.field)
            .border(
                width = if (error) 2.dp else 1.dp,
                color = if (error) colors.error else colors.fieldBorder,
                shape = CreateCollectionTokens.fieldShape
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (selected == null) label else "$label ${selected.name}"
                role = Role.Button
            }
            .padding(horizontal = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = selected?.name ?: "",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.padding(start = 8.dp).size(22.dp)
        )
    }
}

@Composable
private fun ImagePickerArea(
    selectedUris: List<Uri>,
    onPick: () -> Unit,
    onClear: () -> Unit,
    viewModel: WallpaperViewModel,
    error: Boolean
) {
    val colors = CreateCollectionTokens.colors()
    val baseModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 102.dp)
        .clip(CreateCollectionTokens.fieldShape)
        .background(colors.field)
        .border(
            width = if (error) 2.dp else 1.dp,
            color = if (error) colors.error else colors.fieldBorder,
            shape = CreateCollectionTokens.fieldShape
        )

    if (selectedUris.isEmpty()) {
        Box(
            modifier = baseModifier
                .clickable(onClick = onPick)
                .semantics {
                    contentDescription = viewModel.getTranslation(
                        "选择本地图片",
                        "Choose local images"
                    )
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = viewModel.getTranslation("选择本地图片", "Choose local images"),
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        Row(
            modifier = baseModifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedUris.take(4).forEachIndexed { index, uri ->
                    Box(
                        modifier = Modifier
                            .width(54.dp)
                            .height(76.dp)
                            .clip(CreateCollectionTokens.previewShape)
                            .background(colors.previewPlaceholder)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = viewModel.getTranslation(
                                "已选择图片 ${index + 1}",
                                "Selected image ${index + 1}"
                            ),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        if (index == 3 && selectedUris.size >= 4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.48f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = viewModel.getTranslation(
                                        "共${selectedUris.size}张",
                                        "${selectedUris.size} total"
                                    ),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                OverlayIconButton(
                    contentDescription = viewModel.getTranslation("继续添加图片", "Add more images"),
                    onClick = onPick,
                    icon = Icons.Rounded.Add
                )
                OverlayIconButton(
                    contentDescription = viewModel.getTranslation("清空已选图片", "Clear selected images"),
                    onClick = onClear,
                    icon = Icons.Rounded.DeleteOutline
                )
            }
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<AlbumCategory>,
    pendingCategoryId: Long?,
    onSelect: (AlbumCategory) -> Unit,
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier
) {
    val colors = CreateCollectionTokens.colors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .clip(CreateCollectionTokens.fieldShape)
            .background(colors.field)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (categories.isEmpty()) {
            Text(
                text = viewModel.getTranslation("正在加载分类...", "Loading categories..."),
                color = colors.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            categories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                        .clickable { onSelect(category) }
                        .semantics {
                            contentDescription = category.name
                            role = Role.RadioButton
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.name,
                        color = colors.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (category.id == pendingCategoryId) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = viewModel.getTranslation("已选择", "Selected"),
                            tint = colors.text,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateActionRow(
    submitting: Boolean,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverlayTextButton(
            text = if (submitting) viewModel.getTranslation("创建中...", "Creating...")
            else viewModel.getTranslation("创建", "Create"),
            onClick = onSubmit,
            primary = true,
            viewModel = viewModel,
            modifier = Modifier.weight(1f),
            enabled = !submitting,
            testTag = "dialog_album_confirm_btn"
        )
        OverlayTextButton(
            text = viewModel.getTranslation("取消", "Cancel"),
            onClick = onCancel,
            primary = false,
            viewModel = viewModel,
            modifier = Modifier.weight(1f),
            enabled = !submitting
        )
    }
}

@Composable
internal fun OverlayTextButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    viewModel: WallpaperViewModel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val colors = CreateCollectionTokens.colors()
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (primary) colors.primaryButton else colors.secondaryButton)
            .clickable(enabled = enabled, onClick = onClick)
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .semantics {
                contentDescription = text
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) colors.primaryButtonText else colors.secondaryButtonText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverlayIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    val colors = CreateCollectionTokens.colors()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.button.copy(alpha = if (enabled) 1f else 0.45f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.controlIcon,
            modifier = Modifier.size(22.dp)
        )
    }
}