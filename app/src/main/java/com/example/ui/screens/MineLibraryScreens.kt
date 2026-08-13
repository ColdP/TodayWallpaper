package btm.m.todaywallpaper.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import btm.m.todaywallpaper.data.model.CollectionItem
import btm.m.todaywallpaper.data.model.WallpaperCollection
import btm.m.todaywallpaper.ui.viewmodel.MineDestination
import btm.m.todaywallpaper.ui.viewmodel.UnifiedWallpaper
import btm.m.todaywallpaper.ui.viewmodel.WallpaperViewModel
import btm.m.todaywallpaper.ui.widget.momentumBackTransform
import btm.m.todaywallpaper.ui.widget.rememberMomentumPredictiveBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class WallpaperLibraryFilter {
    ALL, PEXELS, DEVIANTART, PIXABAY, WALLHAVEN, NEKOSIA, LOCAL
}

/**
 * Mine navigation host. Secondary pages are intentionally layered over their caller so the
 * Momentum predictive-back transform reveals the real previous destination underneath.
 */
@Composable
fun MineLibraryHost(
    viewModel: WallpaperViewModel,
    onViewDetail: (String, String, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val destination by viewModel.mineDestination.collectAsState()
    val collections by viewModel.collections.collectAsState()

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MineDashboard(
            viewModel = viewModel,
            onOpen = viewModel::setMineDestination,
            onViewDetail = onViewDetail
        )

        when (val current = destination) {
            MineDestination.Dashboard -> Unit
            MineDestination.Collections -> CollectionsLayer(
                viewModel = viewModel,
                detailVisible = false,
                onBack = { viewModel.setMineDestination(MineDestination.Dashboard) },
                onOpenCollection = { collection ->
                    viewModel.fetchActiveCollectionItems(collection.id)
                    viewModel.setMineDestination(MineDestination.CollectionDetail(collection.id))
                }
            )
            MineDestination.History -> HistoryLayer(
                viewModel = viewModel,
                onBack = { viewModel.setMineDestination(MineDestination.Dashboard) },
                onViewDetail = onViewDetail
            )
            MineDestination.Favorites -> FavoritesLayer(
                viewModel = viewModel,
                onBack = { viewModel.setMineDestination(MineDestination.Dashboard) },
                onViewDetail = onViewDetail
            )
            MineDestination.About -> AboutScreen(
                viewModel = viewModel,
                onBack = { viewModel.setMineDestination(MineDestination.Dashboard) },
                modifier = Modifier.fillMaxSize()
            )
            is MineDestination.CollectionDetail -> {
                CollectionsLayer(
                    viewModel = viewModel,
                    detailVisible = true,
                    onBack = { viewModel.setMineDestination(MineDestination.Dashboard) },
                    onOpenCollection = { }
                )
                val collection = collections.firstOrNull { it.id == current.collectionId }
                if (collection != null) {
                    CollectionDetailLayer(
                        collection = collection,
                        viewModel = viewModel,
                        onBack = { viewModel.setMineDestination(MineDestination.Collections) },
                        onViewDetail = onViewDetail
                    )
                } else {
                    LaunchedEffect(current.collectionId) {
                        viewModel.setMineDestination(MineDestination.Collections)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MineDashboard(
    viewModel: WallpaperViewModel,
    onOpen: (MineDestination) -> Unit,
    onViewDetail: (String, String, String?, String) -> Unit
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val categories by viewModel.albumCategories.collectAsState()
    val username by viewModel.username.collectAsState()
    val avatar by viewModel.avatarUrl.collectAsState()
    val subtitle by viewModel.profileSubtitle.collectAsState()
    var historyToDelete by remember { mutableStateOf<btm.m.todaywallpaper.data.model.HistoryWallpaper?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .clickable(onClick = { viewModel.showProfileOverlay() }).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(64.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .1f)) {
                if (avatar != null) {
                    AsyncImage(avatar, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    subtitle.ifEmpty { viewModel.getTranslation("每一次收藏，皆是对美学的赞美", "Each bookmark praises outstanding aesthetics") },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(26.dp))
        DashboardSectionHeader(Icons.Rounded.Collections, viewModel.getTranslation("我的图集", "My Collections")) {
            onOpen(MineDestination.Collections)
        }
        Spacer(Modifier.height(10.dp))
        if (collections.isEmpty()) {
            DashboardEmptyCard(viewModel.getTranslation("还没有图集，进入页面创建第一个图集", "No collections yet. Open the page to create one")) {
                onOpen(MineDestination.Collections)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(collections, key = { it.id }) { collection ->
                    DashboardCollectionCard(collection, categories.firstOrNull { it.id == collection.categoryId }?.name) {
                        viewModel.fetchActiveCollectionItems(collection.id)
                        onOpen(MineDestination.CollectionDetail(collection.id))
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        DashboardSectionHeader(Icons.Rounded.History, viewModel.getTranslation("历史壁纸", "Wallpaper History")) {
            onOpen(MineDestination.History)
        }
        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) {
            DashboardEmptyCard(viewModel.getTranslation("打开壁纸详情后，浏览记录会显示在这里", "Viewed wallpapers will appear here")) {
                onOpen(MineDestination.History)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history.take(10), key = { it.id }) { wallpaper ->
                    DashboardWallpaperCard(
                        url = wallpaper.thumbnailUrl,
                        source = wallpaper.source,
                        tag = "history_item_${wallpaper.id}",
                        onClick = { onViewDetail(wallpaper.id, wallpaper.imageUrl, wallpaper.authorName, wallpaper.source) },
                        onLongClick = { historyToDelete = wallpaper }
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        DashboardSectionHeader(Icons.Rounded.Favorite, viewModel.getTranslation("我喜欢的壁纸", "Favorite Wallpapers")) {
            onOpen(MineDestination.Favorites)
        }
        Spacer(Modifier.height(10.dp))
        if (favorites.isEmpty()) {
            DashboardEmptyCard(viewModel.getTranslation("喜欢的壁纸会集中显示在这里", "Your favorite wallpapers will appear here")) {
                onOpen(MineDestination.Favorites)
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favorites.take(10), key = { it.id }) { wallpaper ->
                    DashboardWallpaperCard(
                        url = wallpaper.thumbnailUrl,
                        source = wallpaper.source,
                        tag = "fav_item_${wallpaper.id}",
                        onClick = { onViewDetail(wallpaper.id, wallpaper.imageUrl, wallpaper.authorName, wallpaper.source) }
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)) {
            DashboardSettingsRow(
                icon = Icons.Rounded.Settings,
                title = viewModel.getTranslation("设置", "Settings"),
                subtitle = viewModel.getTranslation("语言、壁纸与系统偏好", "Language, wallpaper and system preferences")
            ) { context.startActivity(android.content.Intent(context, SettingsActivity::class.java)) }
            DashboardSettingsRow(
                icon = Icons.Rounded.Info,
                title = viewModel.getTranslation("关于", "About"),
                subtitle = viewModel.getTranslation("项目介绍、链接与设备信息", "Project info, links & device details")
            ) { onOpen(MineDestination.About) }
        }
        Spacer(Modifier.height(110.dp))
    }

    historyToDelete?.let { wallpaper ->
        DeleteHistoryDialog(
            viewModel = viewModel,
            onDismiss = { historyToDelete = null },
            onConfirm = {
                viewModel.deleteHistoryWallpaper(wallpaper.id)
                historyToDelete = null
            }
        )
    }
}

@Composable
private fun DashboardSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(9.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DashboardEmptyCard(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(96.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardCollectionCard(collection: WallpaperCollection, category: String?, onClick: () -> Unit) {
    Card(
        Modifier.width(160.dp).height(130.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            CollectionCover(collection.coverUrl, collection.name)
            CollectionCardScrim()
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(collection.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                category?.let { Text(it, color = Color.White.copy(.75f), fontSize = 10.sp, maxLines = 1) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardWallpaperCard(
    url: String,
    source: String,
    tag: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        Modifier.width(100.dp).height(150.dp).clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).testTag(tag),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Text(
                source,
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(.48f)).padding(5.dp),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DashboardSettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CollectionsLayer(
    viewModel: WallpaperViewModel,
    detailVisible: Boolean,
    onBack: () -> Unit,
    onOpenCollection: (WallpaperCollection) -> Unit
) {
    val collections by viewModel.collections.collectAsState()
    val categories by viewModel.albumCategories.collectAsState()
    val predictive by viewModel.predictiveBackEnabled.collectAsState()
    val maxProgress by viewModel.predictiveBackMaxProgress.collectAsState()
    val wallpaperDetail by viewModel.detailWallpaper.collectAsState()
    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    val backState = rememberMomentumPredictiveBack(
        predictive,
        maxProgress,
        onBack,
        handlerEnabled = !detailVisible && wallpaperDetail == null
    )
    val visible = remember(collections, selectedCategory) {
        selectedCategory?.let { id -> collections.filter { it.categoryId == id } } ?: collections
    }

    LibraryPageFrame(
        title = viewModel.getTranslation("我的图集", "My Collections"),
        subtitle = viewModel.getTranslation("${visible.size} 个图集", "${visible.size} collections"),
        onBack = onBack,
        backModifier = Modifier.momentumBackTransform(backState),
        action = {
            IconButton(onClick = viewModel::showCreateCollectionOverlay, modifier = Modifier.testTag("collections_create")) {
                Icon(Icons.Rounded.Add, viewModel.getTranslation("创建图集", "Create collection"))
            }
        },
        filters = {
            CapsuleFilterRow(
                options = listOf(null to viewModel.getTranslation("全部", "All")) + categories.map { it.id to it.name },
                selected = selectedCategory,
                onSelected = { selectedCategory = it }
            )
        }
    ) {
        if (visible.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Rounded.Collections,
                title = viewModel.getTranslation("这里还没有图集", "No collections here yet"),
                message = viewModel.getTranslation("点击右上角 + 创建你的第一个图集", "Tap + to create your first collection")
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                items(visible, key = { it.id }) { collection ->
                    CollectionWaterfallCard(
                        collection = collection,
                        category = categories.firstOrNull { it.id == collection.categoryId }?.name,
                        onClick = { onOpenCollection(collection) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryLayer(viewModel: WallpaperViewModel, onBack: () -> Unit, onViewDetail: (String, String, String?, String) -> Unit) {
    val history by viewModel.history.collectAsState()
    var historyToDelete by remember { mutableStateOf<LibraryWallpaper?>(null) }
    WallpaperLibraryLayer(
        title = viewModel.getTranslation("历史壁纸", "Wallpaper History"),
        emptyTitle = viewModel.getTranslation("暂无浏览记录", "No viewing history"),
        emptyMessage = viewModel.getTranslation("打开任意壁纸详情后会自动保存", "Open any wallpaper detail to save it here"),
        entries = history.map { LibraryWallpaper(it.id, it.imageUrl, it.thumbnailUrl, it.authorName, it.source, it.category) },
        viewModel = viewModel,
        onBack = onBack,
        onViewDetail = onViewDetail,
        onLongClick = { historyToDelete = it }
    )
    historyToDelete?.let { wallpaper ->
        DeleteHistoryDialog(
            viewModel = viewModel,
            onDismiss = { historyToDelete = null },
            onConfirm = {
                viewModel.deleteHistoryWallpaper(wallpaper.id)
                historyToDelete = null
            }
        )
    }
}

@Composable
private fun FavoritesLayer(viewModel: WallpaperViewModel, onBack: () -> Unit, onViewDetail: (String, String, String?, String) -> Unit) {
    val favorites by viewModel.favorites.collectAsState()
    WallpaperLibraryLayer(
        title = viewModel.getTranslation("我喜欢的壁纸", "Favorite Wallpapers"),
        emptyTitle = viewModel.getTranslation("暂无喜欢的壁纸", "No favorite wallpapers"),
        emptyMessage = viewModel.getTranslation("在首页、分类或详情中收藏壁纸", "Favorite wallpapers from Home, Themes or detail"),
        entries = favorites.map { LibraryWallpaper(it.id, it.imageUrl, it.thumbnailUrl, it.authorName, it.source, it.category) },
        viewModel = viewModel,
        onBack = onBack,
        onViewDetail = onViewDetail
    )
}

internal data class LibraryWallpaper(
    val id: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val author: String?,
    val source: String,
    val category: String?
)

@Composable
private fun WallpaperLibraryLayer(
    title: String,
    emptyTitle: String,
    emptyMessage: String,
    entries: List<LibraryWallpaper>,
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    onViewDetail: (String, String, String?, String) -> Unit,
    onLongClick: ((LibraryWallpaper) -> Unit)? = null
) {
    val predictive by viewModel.predictiveBackEnabled.collectAsState()
    val maxProgress by viewModel.predictiveBackMaxProgress.collectAsState()
    val wallpaperDetail by viewModel.detailWallpaper.collectAsState()
    var filter by remember { mutableStateOf(WallpaperLibraryFilter.ALL) }
    val backState = rememberMomentumPredictiveBack(predictive, maxProgress, onBack, handlerEnabled = wallpaperDetail == null)
    val visible = remember(entries, filter) {
        entries.filter {
            when (filter) {
                WallpaperLibraryFilter.ALL -> true
                WallpaperLibraryFilter.PEXELS -> it.source.equals("Pexels", true)
                WallpaperLibraryFilter.DEVIANTART -> it.source.equals("DeviantArt", true)
                WallpaperLibraryFilter.PIXABAY -> it.source.equals("Pixabay", true)
                WallpaperLibraryFilter.WALLHAVEN -> it.source.equals("Wallhaven", true)
                WallpaperLibraryFilter.NEKOSIA -> it.source.equals("Nekosia", true)
                WallpaperLibraryFilter.LOCAL -> it.source.contains("local", true) || it.imageUrl.startsWith("file:")
            }
        }
    }
    LibraryPageFrame(
        title = title,
        subtitle = viewModel.getTranslation("${visible.size} 张壁纸", "${visible.size} wallpapers"),
        onBack = onBack,
        backModifier = Modifier.momentumBackTransform(backState),
        filters = {
            CapsuleFilterRow(
                options = listOf(
                    WallpaperLibraryFilter.ALL to viewModel.getTranslation("全部", "All"),
                    WallpaperLibraryFilter.PEXELS to "Pexels",
                    WallpaperLibraryFilter.DEVIANTART to "DeviantArt",
                    WallpaperLibraryFilter.PIXABAY to "Pixabay",
                    WallpaperLibraryFilter.WALLHAVEN to "Wallhaven",
                    WallpaperLibraryFilter.NEKOSIA to "Nekosia",
                    WallpaperLibraryFilter.LOCAL to viewModel.getTranslation("本地", "Local")
                ),
                selected = filter,
                onSelected = { filter = it }
            )
        }
    ) {
        if (visible.isEmpty()) {
            LibraryEmptyState(Icons.Rounded.ImageSearch, emptyTitle, emptyMessage)
        } else {
            WallpaperWaterfall(
                entries = visible,
                onClick = { onViewDetail(it.id, it.imageUrl, it.author, it.source) },
                onLongClick = onLongClick
            )
        }
    }
}

@Composable
private fun DeleteHistoryDialog(
    viewModel: WallpaperViewModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(viewModel.getTranslation("删除历史壁纸？", "Delete history wallpaper?")) },
        text = { Text(viewModel.getTranslation("这条浏览记录将被永久删除。", "This viewing history entry will be permanently deleted.")) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(viewModel.getTranslation("删除", "Delete"), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(viewModel.getTranslation("取消", "Cancel")) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionDetailLayer(
    collection: WallpaperCollection,
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    onViewDetail: (String, String, String?, String) -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.activeCollectionItems.collectAsState()
    val predictive by viewModel.predictiveBackEnabled.collectAsState()
    val maxProgress by viewModel.predictiveBackMaxProgress.collectAsState()
    val wallpaperDetail by viewModel.detailWallpaper.collectAsState()
    val scope = rememberCoroutineScope()
    var actionItem by remember { mutableStateOf<CollectionItem?>(null) }
    var displayedActionItem by remember { mutableStateOf<CollectionItem?>(null) }
    var actionOverlayVisible by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val backState = rememberMomentumPredictiveBack(
        predictive,
        maxProgress,
        onBack,
        handlerEnabled = wallpaperDetail == null && displayedActionItem == null && !confirmDelete
    )

    LaunchedEffect(actionItem) {
        if (actionItem != null) {
            displayedActionItem = actionItem
            actionOverlayVisible = true
        } else if (displayedActionItem != null) {
            actionOverlayVisible = false
            val closingItem = displayedActionItem
            delay(340)
            if (actionItem == null && displayedActionItem == closingItem) {
                displayedActionItem = null
            }
        }
    }

    BackHandler(enabled = confirmDelete) {
        confirmDelete = false
    }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val folder = java.io.File(context.filesDir, "local_collections").apply { mkdirs() }
                    val id = "local_${System.currentTimeMillis()}"
                    val file = java.io.File(folder, "img_$id.jpg")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(file).use(input::copyTo)
                        } ?: error("Unable to read image")
                    }
                    viewModel.addWallpaperToCollectionId(
                        collection.id,
                        UnifiedWallpaper(id, "file://${file.absolutePath}", "file://${file.absolutePath}", null, null, "Local Gallery")
                    )
                }
                Toast.makeText(
                    context,
                    if (result.isSuccess) viewModel.getTranslation("图片已添加", "Image added") else viewModel.getTranslation("图片添加失败", "Failed to add image"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val folder = java.io.File(context.filesDir, "local_collections").apply { mkdirs() }
                    val file = java.io.File(folder, "cover_${collection.id}_${System.currentTimeMillis()}.jpg")
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input -> java.io.FileOutputStream(file).use(input::copyTo) }
                            ?: error("Unable to read cover")
                    }
                    viewModel.updateCollectionCover(collection.id, "file://${file.absolutePath}")
                }
            }
        }
    }

    LibraryPageFrame(
        title = collection.name,
        subtitle = viewModel.getTranslation("${items.size} 张壁纸", "${items.size} wallpapers"),
        onBack = onBack,
        backModifier = Modifier.momentumBackTransform(backState),
        action = {
            IconButton(onClick = { coverLauncher.launch("image/*") }) {
                Icon(Icons.Rounded.PhotoCamera, viewModel.getTranslation("更换封面", "Change cover"))
            }
            IconButton(onClick = { uploadLauncher.launch("image/*") }) {
                Icon(Icons.Rounded.Add, viewModel.getTranslation("添加图片", "Add image"))
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Rounded.DeleteOutline, viewModel.getTranslation("删除图集", "Delete collection"), tint = MaterialTheme.colorScheme.error)
            }
        },
        filters = {
            LazyRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { StaticInfoCapsule(viewModel.getTranslation("全部", "All"), selected = true) }
                item { StaticInfoCapsule(viewModel.getTranslation("长按图片可管理", "Long press to manage"), selected = false) }
            }
        }
    ) {
        if (items.isEmpty()) {
            LibraryEmptyState(
                Icons.Rounded.Collections,
                viewModel.getTranslation("图集还是空的", "This collection is empty"),
                viewModel.getTranslation("点击右上角 + 添加本地图片", "Tap + to add a local image")
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    WallpaperWaterfallCard(
                        imageUrl = item.thumbnailUrl,
                        title = item.authorName,
                        source = item.source,
                        ratio = waterfallRatio(item.wallpaperId),
                        modifier = Modifier.combinedClickable(
                            onClick = { onViewDetail(item.wallpaperId, item.imageUrl, item.authorName, item.source) },
                            onLongClick = { actionItem = item }
                        )
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = actionOverlayVisible && displayedActionItem != null,
        enter = fadeIn(tween(260)) + slideInVertically(
            animationSpec = tween(320),
            initialOffsetY = { it / 8 }
        ),
        exit = fadeOut(tween(220)) + slideOutVertically(
            animationSpec = tween(280),
            targetOffsetY = { it / 10 }
        ),
        label = "image_actions_overlay_transition"
    ) {
        displayedActionItem?.let { item ->
            ImageActionsOverlay(
                item = item,
                viewModel = viewModel,
                onDismiss = { actionItem = null },
                onSetCover = {
                    viewModel.updateCollectionCover(collection.id, item.imageUrl)
                    actionItem = null
                },
                onRemove = {
                    viewModel.removeWallpaperFromCollection(collection.id, item.wallpaperId)
                    actionItem = null
                }
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(viewModel.getTranslation("删除此图集？", "Delete this collection?")) },
            text = { Text(viewModel.getTranslation("图集关系将被永久删除。", "The collection will be permanently deleted.")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCollectionId(collection.id)
                    confirmDelete = false
                    onBack()
                }) { Text(viewModel.getTranslation("删除", "Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(viewModel.getTranslation("取消", "Cancel")) } }
        )
    }
}

@Composable
private fun ImageActionsOverlay(
    item: CollectionItem,
    viewModel: WallpaperViewModel,
    onDismiss: () -> Unit,
    onSetCover: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = CreateCollectionTokens.colors()
    var panelDragOffset by remember { mutableStateOf(0f) }
    val animatedPanelDragOffset by animateFloatAsState(
        targetValue = panelDragOffset,
        animationSpec = if (panelDragOffset == 0f) tween(260) else snap(),
        label = "image_actions_panel_drag"
    )

    LaunchedEffect(item.id) {
        panelDragOffset = 0f
    }

    BackHandler(enabled = true, onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onDismiss
            )
            .semantics { contentDescription = viewModel.getTranslation("图片操作", "Image actions") }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                // The two-action sheet is intentionally shorter than the profile panel.
                .fillMaxHeight(0.38f)
                .heightIn(min = 300.dp)
                .graphicsLayer { translationY = animatedPanelDragOffset }
                .shadow(22.dp, CreateCollectionTokens.panelShape)
                .clip(CreateCollectionTokens.panelShape)
                .background(colors.panel)
                .border(BorderStroke(1.dp, colors.panelBorder), CreateCollectionTokens.panelShape)
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
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                panelDragOffset = (panelDragOffset + amount).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (panelDragOffset > 110.dp.toPx()) {
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = CreateCollectionTokens.contentHorizontal,
                        end = CreateCollectionTokens.contentHorizontal,
                        top = CreateCollectionTokens.contentTop,
                        bottom = CreateCollectionTokens.contentBottom
                    )
            ) {
                Text(
                    text = viewModel.getTranslation("图片操作", "Image actions"),
                    fontSize = 30.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.text
                )
                Spacer(Modifier.height(12.dp))
                ImageActionRow(
                    text = viewModel.getTranslation("设为图集封面", "Set as collection cover"),
                    colors = colors,
                    onClick = onSetCover
                )
                Spacer(Modifier.height(8.dp))
                ImageActionRow(
                    text = viewModel.getTranslation("从图集中移除", "Remove from collection"),
                    colors = colors,
                    error = true,
                    onClick = onRemove
                )
                Spacer(Modifier.weight(1f))
                OverlayTextButton(
                    text = viewModel.getTranslation("取消", "Cancel"),
                    onClick = onDismiss,
                    primary = false,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ImageActionRow(
    text: String,
    colors: CreateCollectionColors,
    error: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (error) colors.error else colors.text
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CreateCollectionTokens.fieldShape)
            .background(colors.button)
            .border(BorderStroke(1.dp, colors.fieldBorder), CreateCollectionTokens.fieldShape)
            .clickable(
                onClick = onClick
            )
            .semantics {
                contentDescription = text
                role = Role.Button
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun LibraryPageFrame(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backModifier: Modifier,
    action: @Composable RowScope.() -> Unit = {},
    filters: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Column(
        backModifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("library_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action()
        }
        filters()
        Box(Modifier.fillMaxSize(), content = content)
    }
}

@Composable
internal fun <T> CapsuleFilterRow(options: List<Pair<T, String>>, selected: T, onSelected: (T) -> Unit) {
    val selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
    val selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options, key = { it.second }) { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelected(value) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) selectedContainerColor else Color.Transparent,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(.55f))
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSelected) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StaticInfoCapsule(text: String, selected: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(.55f))
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CollectionWaterfallCard(collection: WallpaperCollection, category: String?, onClick: () -> Unit) {
    val ratio = if (collection.id % 3 == 0) .86f else if (collection.id % 3 == 1) 1.04f else .74f
    Card(
        Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            CollectionCover(collection.coverUrl, collection.name)
            CollectionCardScrim()
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(collection.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                collection.description?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = Color.White.copy(.74f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                category?.let { Text(it, color = Color.White.copy(.7f), fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
            }
        }
    }
}

@Composable
private fun CollectionCover(url: String?, description: String) {
    if (url != null) {
        AsyncImage(url, description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(.42f), MaterialTheme.colorScheme.surfaceVariant))
            )
        )
    }
}

@Composable
private fun CollectionCardScrim() {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.82f)))))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WallpaperWaterfall(
    entries: List<LibraryWallpaper>,
    onClick: (LibraryWallpaper) -> Unit,
    onLongClick: ((LibraryWallpaper) -> Unit)? = null,
    isLoadingMore: Boolean = false,
    onLoadMore: (() -> Unit)? = null
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 34.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        items(entries, key = { it.id }) { item ->
            WallpaperWaterfallCard(
                imageUrl = item.thumbnailUrl,
                title = item.author,
                source = item.source,
                ratio = waterfallRatio(item.id),
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(item) },
                    onLongClick = onLongClick?.let { callback -> { callback(item) } }
                )
            )
        }
        if (onLoadMore != null) {
            if (isLoadingMore) {
                item(key = "library_loading_more") {
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
                item(key = "library_load_more_trigger") {
                    LaunchedEffect(entries.size) { onLoadMore() }
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
    }
}

@Composable
internal fun WallpaperWaterfallCard(
    imageUrl: String,
    title: String?,
    source: String,
    ratio: Float,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp)) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(.72f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(11.dp)) {
                title?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(source, color = Color.White.copy(.72f), fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

internal fun waterfallRatio(key: String): Float = when ((key.hashCode() and Int.MAX_VALUE) % 4) {
    0 -> .62f
    1 -> .74f
    2 -> .86f
    else -> .68f
}

@Composable
internal fun LibraryEmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String) {
    Column(
        Modifier.fillMaxSize().padding(36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Icon(icon, null, Modifier.padding(20.dp).size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(.55f))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}