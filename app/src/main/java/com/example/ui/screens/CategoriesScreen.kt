package btm.m.todaywallpaper.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import btm.m.todaywallpaper.ui.viewmodel.WallpaperUiState
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.viewmodel.UnifiedWallpaper
import btm.m.todaywallpaper.ui.widget.momentumBackTransform
import btm.m.todaywallpaper.ui.widget.rememberMomentumPredictiveBack

private enum class CategoryLibraryFilter { ALL, PEXELS, DEVIANTART, PIXABAY, WALLHAVEN, NEKOSIA }

data class CategoryItem(
    val key: String,
    val zhTitle: String,
    val enTitle: String,
    val zhDesc: String,
    val enDesc: String,
    val sampleUrl: String, // Banner artwork URL
    val source: String // Search APIs or "Nekosia"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: WallpaperViewModel,
    onViewDetail: (String, String, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCategoryKey by viewModel.selectedCategoryKey.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    val categoriesScrollState = rememberScrollState()

    LaunchedEffect(categories) {
        viewModel.loadCategoryCovers(categories.map { it.key })
    }

    val selectedCategory = remember(selectedCategoryKey, categories) {
        categories.find { it.key == selectedCategoryKey }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Keep the category list mounted underneath the result page so the
        // predictive-back transform reveals the actual caller.
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(categoriesScrollState)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Header Area
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = viewModel.getTranslation("分类推荐", "Themes"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getTranslation("点击对应板块，发现极美灵感", "Find visual inspiration by theme"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Custom search themes creator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle(
                            icon = Icons.Rounded.Category,
                            text = viewModel.getTranslation("分类检索", "Category Search"),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.testTag("add_custom_category_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add custom category icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewModel.getTranslation("新增", "Add"),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val customList = categories.filter { it.key.startsWith("custom_pexels_") }
                    if (customList.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp)
                                .padding(vertical = 4.dp)
                                .clickable { showAddCategoryDialog = true }
                                .testTag("empty_custom_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add category outline icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = viewModel.getTranslation("点击新建你的专属摄影分类", "Create your personalized wallpaper theme"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                    )
                                }
                            }
                        }
                    } else {
                        customList.forEach { item ->
                            CategoryCard(item = item, viewModel = viewModel) {
                                viewModel.setSelectedCategoryKey(item.key)
                                viewModel.loadCategoryWallpapers(item.key)
                            }
                        }

                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Nekosia Illustration Header
                    SectionTitle(
                        icon = Icons.Rounded.Grid3x3,
                        text = viewModel.getTranslation("Nekosia API", "Nekosia API"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    categories.filter { it.source == "Nekosia" }.forEach { item ->
                        CategoryCard(item = item, viewModel = viewModel) {
                            viewModel.setSelectedCategoryKey(item.key)
                            viewModel.loadCategoryWallpapers(item.key)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(84.dp)) // Padding for bottom navbar safe bounds
                }
            }

            selectedCategory?.let { activeCategory ->
                CategoryLibraryLayer(
                    category = activeCategory,
                    viewModel = viewModel,
                    onBack = { viewModel.setSelectedCategoryKey(null) },
                    onViewDetail = onViewDetail
                )
            }
        }

        // Custom Category Creation Dialog
        if (showAddCategoryDialog) {
            var activeTabIndex by remember { mutableStateOf(0) } // 0 = Single, 1 = Batch & Presets
            
            var categoryTitle by remember { mutableStateOf("") }
            var searchQuery by remember { mutableStateOf("") }
            var categoryDesc by remember { mutableStateOf("") }
            
            val presets = remember {
                listOf(
                    Triple("海滩风光", "beach", "阳光沙滩和清澈的海浪"),
                    Triple("自然森林", "forest", "静谧祥和的绿色树林与阳光"),
                    Triple("速度跑车", "supercars", "酷炫具有力量感的赛车与跑车"),
                    Triple("可爱猫咪", "cute cats", "呆萌治愈的毛茸茸小猫咪"),
                    Triple("璀璨日落", "sunset", "唯美梦幻的夕阳晚霞与光影"),
                    Triple("纯净冰雪", "winter", "静寂圣洁的冰川雪原"),
                    Triple("夏日花卉", "flowers", "五彩斑斓盛开的野花"),
                    Triple("荒原沙漠", "desert", "广袤雄浑的长河落日圆"),
                    Triple("赛博朋克", "cyberpunk", "霓虹斑斓的高科技未来街道"),
                    Triple("咖啡美学", "coffee", "精致温暖的咖啡拉花与休闲角落")
                )
            }
            
            val selectedPresets = remember { mutableStateListOf<Int>() }
            var bulkInputText by remember { mutableStateOf("") }
            
            var isQueringApi by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { 
                    if (!isQueringApi) showAddCategoryDialog = false 
                },
                title = {
                    Text(
                        text = viewModel.getTranslation("创建自定义分类", "New Custom Theme"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Sliding Pill Select Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            listOf(
                                viewModel.getTranslation("单个创建", "Single Create"),
                                viewModel.getTranslation("批量与推荐", "Batch & Presets")
                            ).forEachIndexed { index, label ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (activeTabIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { activeTabIndex = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (activeTabIndex == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (activeTabIndex == 0) {
                            // Single Theme Creation View
                            Text(
                                text = viewModel.getTranslation(
                                    "输入分类名称与英文检索关键词。我们将通过已配置的图片 API 实时检索，并提取一张壁纸作为分类封面。",
                                    "Provide a title and search key. Configured image APIs will fetch live results and select a category cover."
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = categoryTitle,
                                onValueChange = { categoryTitle = it },
                                label = { Text(text = viewModel.getTranslation("分类名称 (例: 可爱猫咪)", "Category Title (e.g., Cats)")) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_title_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text(text = viewModel.getTranslation("检索英文词 (例: cute cats sleeping)", "Search query (e.g., cute cats sleeping)")) },
                                placeholder = { Text(text = viewModel.getTranslation("支持多词检索，词数不限", "Multi-word queries supported, no length limit"), fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_query_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = categoryDesc,
                                onValueChange = { categoryDesc = it },
                                label = { Text(text = viewModel.getTranslation("分类简介 (例: 各种姿势的可爱猫猫)", "Subtitle description (Optional)")) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_desc_input"),
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp)
                            )
                        } else {
                            // Batch & Curated Presets View
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = viewModel.getTranslation("精选热门推荐 (点击可多选或取消):", "Curated landscape tags (tap to multi-select):"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                presets.chunked(2).forEach { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPresets.forEach { preset ->
                                            val presetIndex = presets.indexOf(preset)
                                            val isSelected = selectedPresets.contains(presetIndex)
                                            val name = if (viewModel.getTranslation("zh", "en") == "zh") preset.first else preset.second.replaceFirstChar { it.uppercase() }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        if (isSelected) {
                                                            selectedPresets.remove(presetIndex)
                                                        } else {
                                                            selectedPresets.add(presetIndex)
                                                        }
                                                    }
                                                    .padding(8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = name,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = preset.second,
                                                            fontSize = 10.sp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Add,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (rowPresets.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = viewModel.getTranslation("自定义更多检索词 (由英文逗号分隔，例: neon, coffee):", "Or specify comma-separated search keys (e.g. neon, coffee):"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                OutlinedTextField(
                                    value = bulkInputText,
                                    onValueChange = { bulkInputText = it },
                                    placeholder = {
                                        Text(
                                            text = "e.g. space, rain, cars",
                                            fontSize = 11.sp
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("bulk_tag_input_field"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                val totalToCreateCount = selectedPresets.size + if (bulkInputText.isNotBlank()) bulkInputText.split(",").filter { it.trim().isNotEmpty() }.size else 0
                                if (totalToCreateCount > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = viewModel.getTranslation("当前准备并发获取并创建 $totalToCreateCount 个全新自定义板块！", "Going to simultaneously parse and fetch $totalToCreateCount custom sections!"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (errorMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp
                            )
                        }

                        if (isQueringApi) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.getTranslation("正在连接图片 API 并检索验证...", "Querying image APIs to validate..."),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (activeTabIndex == 0) {
                                if (categoryTitle.isBlank() || searchQuery.isBlank()) {
                                    errorMessage = viewModel.getTranslation("输入必填项不能为空！", "Required fields cannot be empty!")
                                    return@Button
                                }
                                isQueringApi = true
                                errorMessage = ""
                                viewModel.addCustomCategory(categoryTitle, searchQuery, categoryDesc) { success, errMsg ->
                                    isQueringApi = false
                                    if (success) {
                                        showAddCategoryDialog = false
                                    } else {
                                        errorMessage = errMsg
                                    }
                                }
                            } else {
                                val itemsToCreate = mutableListOf<Triple<String, String, String>>()
                                selectedPresets.forEach { idx ->
                                    val pre = presets[idx]
                                    val actualName = if (viewModel.getTranslation("zh", "en") == "zh") pre.first else pre.second.replaceFirstChar { it.uppercase() }
                                    itemsToCreate.add(Triple(actualName, pre.second, pre.third))
                                }
                                if (bulkInputText.isNotBlank()) {
                                    val tags = bulkInputText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    tags.forEach { tag ->
                                        val name = tag.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                        itemsToCreate.add(Triple(name, tag, ""))
                                    }
                                }

                                if (itemsToCreate.isEmpty()) {
                                    errorMessage = viewModel.getTranslation("请选择推荐标签或输入自定义板块词！", "Please pick tags or specify keys to create!")
                                    return@Button
                                }

                                isQueringApi = true
                                errorMessage = ""
                                viewModel.addCustomCategories(itemsToCreate) { success, failed, lastErr ->
                                    isQueringApi = false
                                    if (success > 0) {
                                        showAddCategoryDialog = false
                                    } else {
                                        errorMessage = if (lastErr.isNotEmpty()) lastErr else viewModel.getTranslation("批量检索均未获取到可用样片，请检查词汇!", "All queries failed to return photos, verify terms!")
                                    }
                                }
                            }
                        },
                        enabled = !isQueringApi,
                        modifier = Modifier.testTag("submit_custom_category")
                    ) {
                        Text(text = viewModel.getTranslation("确认创建", "Create"))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddCategoryDialog = false },
                        enabled = !isQueringApi
                    ) {
                        Text(text = viewModel.getTranslation("取消", "Cancel"))
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }

@Composable
fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Section Icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CategoryCard(
    item: CategoryItem,
    viewModel: WallpaperViewModel,
    onClick: () -> Unit
) {
    val title = if (viewModel.language.collectAsState().value == "zh") item.zhTitle else item.enTitle
    val desc = if (viewModel.language.collectAsState().value == "zh") item.zhDesc else item.enDesc
    val categoryCovers by viewModel.categoryCovers.collectAsState()
    val coverUrl = categoryCovers[item.key] ?: item.sampleUrl

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(vertical = 6.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("category_${item.key}_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Category Background Banner
            AsyncImage(
                model = coverUrl,
                contentDescription = item.enTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dynamic Dark overlay sheet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.62f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    color = Color.LightGray.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            // Delete handler / source badge on top right
            if (item.key.startsWith("custom_pexels_")) {
                var showDeleteConfirm by remember { mutableStateOf(false) }
                FilledIconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .testTag("delete_custom_${item.key}"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete custom category icon",
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text(text = viewModel.getTranslation("删除自定义分类", "Delete Custom Theme")) },
                        text = {
                            Text(
                                text = viewModel.getTranslation(
                                    "确认要删除该自定义分类：${item.zhTitle}？",
                                    "Delete the custom theme ${item.enTitle}?"
                                )
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirm = false
                                    viewModel.deleteCustomCategory(item.key)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text(text = viewModel.getTranslation("删除", "Delete"))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text(text = viewModel.getTranslation("取消", "Cancel"))
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.source,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryLibraryLayer(
    category: CategoryItem,
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    onViewDetail: (String, String, String?, String) -> Unit
) {
    val gridState by viewModel.categoryGridState.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val predictiveEnabled by viewModel.predictiveBackEnabled.collectAsState()
    val predictiveMaxProgress by viewModel.predictiveBackMaxProgress.collectAsState()
    val wallpaperDetail by viewModel.detailWallpaper.collectAsState()
    val blurNsfw by viewModel.blurNsfw.collectAsState()
    val title = if (viewModel.language.collectAsState().value == "zh") {
        category.zhTitle
    } else {
        category.enTitle
    }
    var filter by remember(category.key) { mutableStateOf(CategoryLibraryFilter.ALL) }
    val backState = rememberMomentumPredictiveBack(
        enabled = predictiveEnabled,
        maxProgressPercent = predictiveMaxProgress,
        onBack = onBack,
        handlerEnabled = wallpaperDetail == null
    )

    val wallpapers = (gridState as? WallpaperUiState.Success)?.data.orEmpty()
    val visibleWallpapers = remember(wallpapers, filter) {
        wallpapers.filter { wallpaper ->
            when (filter) {
                CategoryLibraryFilter.ALL -> true
                CategoryLibraryFilter.PEXELS -> wallpaper.source.equals("Pexels", true)
                CategoryLibraryFilter.DEVIANTART -> wallpaper.source.equals("DeviantArt", true)
                CategoryLibraryFilter.PIXABAY -> wallpaper.source.equals("Pixabay", true)
                CategoryLibraryFilter.WALLHAVEN -> wallpaper.source.equals("Wallhaven", true)
                CategoryLibraryFilter.NEKOSIA -> wallpaper.source.equals("Nekosia", true)
            }
        }
    }
    val libraryEntries = remember(visibleWallpapers) {
        visibleWallpapers.map { wallpaper ->
            LibraryWallpaper(
                id = wallpaper.id,
                imageUrl = wallpaper.imageUrl,
                thumbnailUrl = wallpaper.thumbnailUrl,
                author = wallpaper.author,
                source = wallpaper.source,
                category = wallpaper.category
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .momentumBackTransform(backState)
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("category_grid_back")
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = viewModel.getTranslation(
                        if (category.source == "Nekosia") {
                            "${visibleWallpapers.size} 张壁纸 · Nekosia"
                        } else {
                            "${visibleWallpapers.size} 张壁纸 · Pexels + DeviantArt + Pixabay + Wallhaven"
                        },
                        if (category.source == "Nekosia") {
                            "${visibleWallpapers.size} wallpapers · Nekosia"
                        } else {
                            "${visibleWallpapers.size} wallpapers · Pexels + DeviantArt + Pixabay + Wallhaven"
                        }
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = { viewModel.loadCategoryWallpapers(category.key) },
                modifier = Modifier.testTag("category_grid_retry")
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    "Refresh",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryFilterCapsule(
                    label = viewModel.getTranslation("全部", "All"),
                    selected = filter == CategoryLibraryFilter.ALL,
                    onClick = { filter = CategoryLibraryFilter.ALL }
                )
            }
            if (category.source == "Nekosia") {
                item {
                    CategoryFilterCapsule(
                        label = "Nekosia",
                        selected = filter == CategoryLibraryFilter.NEKOSIA,
                        onClick = { filter = CategoryLibraryFilter.NEKOSIA }
                    )
                }
            } else {
                item {
                    CategoryFilterCapsule(
                        label = "Pexels",
                        selected = filter == CategoryLibraryFilter.PEXELS,
                        onClick = { filter = CategoryLibraryFilter.PEXELS }
                    )
                }
                item {
                    CategoryFilterCapsule(
                        label = "DeviantArt",
                        selected = filter == CategoryLibraryFilter.DEVIANTART,
                        onClick = { filter = CategoryLibraryFilter.DEVIANTART }
                    )
                }
                item {
                    CategoryFilterCapsule(
                        label = "Pixabay",
                        selected = filter == CategoryLibraryFilter.PIXABAY,
                        onClick = { filter = CategoryLibraryFilter.PIXABAY }
                    )
                }
                item {
                    CategoryFilterCapsule(
                        label = "Wallhaven",
                        selected = filter == CategoryLibraryFilter.WALLHAVEN,
                        onClick = { filter = CategoryLibraryFilter.WALLHAVEN }
                    )
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (val state = gridState) {
                WallpaperUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is WallpaperUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.getTranslation("网络资源加载失败", "API Retrieval Failed"),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadCategoryWallpapers(category.key) }) {
                            Text(viewModel.getTranslation("重新加载", "Reload"))
                        }
                    }
                }

                is WallpaperUiState.Success -> {
                    if (visibleWallpapers.isEmpty()) {
                        EmptyStateView(viewModel.getTranslation("暂无壁纸", "No wallpapers found"))
                    } else {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 34.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            staggeredItems(visibleWallpapers, key = { it.id }) { wallpaper ->
                                CategoryWaterfallCard(
                                    wallpaper = wallpaper,
                                    blurPreview = blurNsfw && wallpaper.isNsfw,
                                    modifier = Modifier.clickable {
                                        onViewDetail(
                                            wallpaper.id,
                                            wallpaper.imageUrl,
                                            wallpaper.author,
                                            wallpaper.source
                                        )
                                    }
                                )
                            }
                            if (isLoadingMore) {
                                item(key = "category_loading_more") {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 18.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(26.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            } else {
                                item(key = "category_load_more_trigger") {
                                    LaunchedEffect(visibleWallpapers.size) {
                                        viewModel.loadMoreCategoryWallpapers()
                                    }
                                    Spacer(Modifier.height(1.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun CategoryFilterCapsule(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (selected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = .55f)
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun CategoryWaterfallCard(
    wallpaper: UnifiedWallpaper,
    blurPreview: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(categoryWaterfallRatio(wallpaper.id))
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.author,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurPreview) Modifier.blur(18.dp) else Modifier),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(.74f))
                        )
                    )
            )
            Column(Modifier.align(Alignment.BottomStart).padding(11.dp)) {
                Text(
                    text = wallpaper.author ?: "@Artist",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = wallpaper.source,
                    color = Color.White.copy(.72f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private fun categoryWaterfallRatio(key: String): Float = when ((key.hashCode() and Int.MAX_VALUE) % 4) {
    0 -> .62f
    1 -> .74f
    2 -> .86f
    else -> .68f
}

@Composable
fun CategoryGridView(
    category: CategoryItem,
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    onViewDetail: (String, String, String?, String) -> Unit
) {
    val gridState by viewModel.categoryGridState.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val title = if (viewModel.language.collectAsState().value == "zh") category.zhTitle else category.enTitle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Active Grid Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("category_grid_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${viewModel.getTranslation("检索自", "Retrieved via")} ${category.source}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { viewModel.loadCategoryWallpapers(category.key) },
                modifier = Modifier.testTag("category_grid_retry")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh Grid",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

        // Main Grid content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            when (val state = gridState) {
                is WallpaperUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is WallpaperUiState.Success -> {
                    val list = state.data
                    if (list.isEmpty()) {
                        EmptyStateView(
                            message = viewModel.getTranslation("未能获取壁纸数据", "No grid items found.")
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 0.dp, top = 12.dp, end = 0.dp, bottom = 84.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            gridItems(list, key = { it.id }) { wallpaper ->
                                GridWallpaperTile(wallpaper = wallpaper) {
                                    onViewDetail(
                                        wallpaper.id,
                                        wallpaper.imageUrl,
                                        wallpaper.author,
                                        wallpaper.source
                                    )
                                }
                            }
                            // Infinite scroll: trigger load more when near the end
                            if (isLoadingMore) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            } else {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    // Invisible trigger item: when this becomes visible, load more
                                    LaunchedEffect(list.size) {
                                        viewModel.loadMoreCategoryWallpapers()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                is WallpaperUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = viewModel.getTranslation("网络资源加载失败", "API Retrieval Failed"),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadCategoryWallpapers(category.key) }
                        ) {
                            Text(text = viewModel.getTranslation("重新加载", "Reload"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridWallpaperTile(
    wallpaper: UnifiedWallpaper,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("wallpaper_tile_${wallpaper.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = "Wallpaper preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // subtle name/author label on bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = wallpaper.author ?: "@Artist",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Category,
            contentDescription = "Empty",
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
