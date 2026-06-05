package btm.m.todaywallpaper.ui.viewmodel

import android.content.Context
import android.app.Application
import android.app.WallpaperManager
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import btm.m.todaywallpaper.data.model.FavoriteWallpaper
import btm.m.todaywallpaper.data.model.WallpaperCollection
import btm.m.todaywallpaper.data.model.CollectionItem
import btm.m.todaywallpaper.data.repository.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import btm.m.todaywallpaper.ui.screens.CategoryItem

// Shared screen definition
sealed interface Screen {
    object Home : Screen
    object Categories : Screen
    object Mine : Screen
}

// Shared detail wallpaper wrapper class
data class DetailWallpaperData(
    val id: String,
    val imageUrl: String,
    val author: String?,
    val source: String
)

sealed interface WallpaperUiState<out T> {
    object Loading : WallpaperUiState<Nothing>
    data class Success<out T>(val data: T) : WallpaperUiState<T>
    data class Error(val message: String) : WallpaperUiState<Nothing>
}

data class UnifiedWallpaper(
    val id: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val author: String?,
    val authorUrl: String?,
    val source: String, // "Pexels" or "Nekosia"
    val category: String? = null
)

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        @Volatile
        var instance: WallpaperViewModel? = null

        @Volatile
        var mainViewModelInstance: WallpaperViewModel? = null
    }

    init {
        instance = this
    }

    private val repository = WallpaperRepository(application)

    // ==========================================
    // Navigation and detail state flows
    // ==========================================
    private val _activeScreen = MutableStateFlow<Screen>(Screen.Home)
    val activeScreen: StateFlow<Screen> = _activeScreen.asStateFlow()

    fun setActiveScreen(screen: Screen) {
        _activeScreen.value = screen
    }

    private val _detailWallpaper = MutableStateFlow<DetailWallpaperData?>(null)
    val detailWallpaper: StateFlow<DetailWallpaperData?> = _detailWallpaper.asStateFlow()

    fun setDetailWallpaper(detail: DetailWallpaperData?) {
        _detailWallpaper.value = detail
        // Reset fullscreen when closing detail
        if (detail == null) _isDetailFullscreen.value = false
    }

    private val _isAboutPageVisible = MutableStateFlow(false)
    val isAboutPageVisible: StateFlow<Boolean> = _isAboutPageVisible.asStateFlow()

    fun setAboutPageVisible(visible: Boolean) {
        _isAboutPageVisible.value = visible
    }

    private val _isDetailFullscreen = MutableStateFlow(false)
    val isDetailFullscreen: StateFlow<Boolean> = _isDetailFullscreen.asStateFlow()

    fun setDetailFullscreen(fullscreen: Boolean) {
        _isDetailFullscreen.value = fullscreen
    }

    fun toggleDetailFullscreen() {
        _isDetailFullscreen.value = !_isDetailFullscreen.value
    }

    private val _detailBackProgress = MutableStateFlow(0f)
    val detailBackProgress: StateFlow<Float> = _detailBackProgress.asStateFlow()

    private val _isDetailBackSwiping = MutableStateFlow(false)
    val isDetailBackSwiping: StateFlow<Boolean> = _isDetailBackSwiping.asStateFlow()

    fun setDetailBackGesture(progress: Float, swiping: Boolean) {
        _detailBackProgress.value = progress.coerceIn(0f, 1f)
        _isDetailBackSwiping.value = swiping
    }

    fun resetDetailBackGesture() {
        _detailBackProgress.value = 0f
        _isDetailBackSwiping.value = false
    }

    private val _selectedCategoryKey = MutableStateFlow<String?>(null)
    val selectedCategoryKey: StateFlow<String?> = _selectedCategoryKey.asStateFlow()

    fun setSelectedCategoryKey(key: String?) {
        _selectedCategoryKey.value = key
    }

    // ==========================================
    // 1. LOCAL PERSISTENCE STATEFLOWS
    // ==========================================
    val favorites: StateFlow<List<FavoriteWallpaper>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<WallpaperCollection>> = repository.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCollectionItems = MutableStateFlow<List<CollectionItem>>(emptyList())
    val activeCollectionItems = _activeCollectionItems.asStateFlow()


    // ==========================================
    // 2. CONFIGURATIONS & PREFERENCES
    // ==========================================
    private val _language = MutableStateFlow("zh") // "zh" for Chinese, "en" for English
    val language: StateFlow<String> = _language.asStateFlow()

    private val _homeWallpaperType = MutableStateFlow("PexelsCurated") // Type displayed on immersive homepage
    val homeWallpaperType: StateFlow<String> = _homeWallpaperType.asStateFlow()

    private val _username = MutableStateFlow("探索家用户")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _pexelsApiKey = MutableStateFlow("")
    val pexelsApiKey: StateFlow<String> = _pexelsApiKey.asStateFlow()

    private val _showApiKeyPrompt = MutableStateFlow(false)
    val showApiKeyPrompt: StateFlow<Boolean> = _showApiKeyPrompt.asStateFlow()

    private var pendingPexelsAction: (() -> Unit)? = null

    fun dismissApiKeyPrompt(proceedAnyway: Boolean) {
        _showApiKeyPrompt.value = false
        if (proceedAnyway) {
            val action = pendingPexelsAction
            pendingPexelsAction = null
            action?.invoke()
        } else {
            pendingPexelsAction = null
        }
    }

    fun savePexelsKeyAndProceed(newKey: String) {
        val trimmed = newKey.trim()
        updatePexelsApiKey(trimmed)
        _showApiKeyPrompt.value = false
        val action = pendingPexelsAction
        pendingPexelsAction = null
        action?.invoke()
    }

    fun updatePexelsApiKey(key: String) {
        _pexelsApiKey.value = key
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        sp.edit().putString("pexels_api_key", key).apply()
    }

    fun performWithApiKeyCheck(action: () -> Unit) {
        if (_pexelsApiKey.value.isBlank()) {
            pendingPexelsAction = action
            _showApiKeyPrompt.value = true
        } else {
            action()
        }
    }

    // Dynamic Category covers caching map
    private val _categoryCovers = MutableStateFlow<Map<String, String>>(emptyMap())
    val categoryCovers: StateFlow<Map<String, String>> = _categoryCovers.asStateFlow()

    // Predefined baseline categories
    private val predefinedCategories = listOf(
        CategoryItem(
            key = "nature",
            zhTitle = "山海自然",
            enTitle = "Mountain & Nature",
            zhDesc = "壮丽山河，晨曦晚霞的广阔自然风光",
            enDesc = "Breathtaking landscapes, forests, oceans & mountains.",
            sampleUrl = "https://images.pexels.com/photos/3225517/pexels-photo-3225517.jpeg?auto=compress&cs=tinysrgb&h=450",
            source = "Pexels"
        ),
        CategoryItem(
            key = "space",
            zhTitle = "浩瀚星空",
            enTitle = "Cosmic Space",
            zhDesc = "璀璨星系、遥远行星与深邃星空探索",
            enDesc = "Milky Way galaxies, deep space nebula & cosmic exploration.",
            sampleUrl = "https://images.pexels.com/photos/924824/pexels-photo-924824.jpeg?auto=compress&cs=tinysrgb&h=450",
            source = "Pexels"
        ),
        CategoryItem(
            key = "urban",
            zhTitle = "都市霓虹",
            enTitle = "Urban Streets",
            zhDesc = "现代城市赛步朋克与写实都市建筑光影",
            enDesc = "Modern skyscrapers, neon-lit alleys & cyberpunk vibes.",
            sampleUrl = "https://images.pexels.com/photos/3052361/pexels-photo-3052361.jpeg?auto=compress&cs=tinysrgb&h=450",
            source = "Pexels"
        ),
        CategoryItem(
            key = "minimalist",
            zhTitle = "极简主义",
            enTitle = "Minimal Space",
            zhDesc = "留白艺术与柔和色块的安静空间表达",
            enDesc = "Clean aesthetic negative space, textures & soft shades.",
            sampleUrl = "https://images.pexels.com/photos/2088205/pexels-photo-2088205.jpeg?auto=compress&cs=tinysrgb&h=450",
            source = "Pexels"
        ),
        CategoryItem(
            key = "nekosia_cute",
            zhTitle = "可爱插画",
            enTitle = "Kawaii Cutie",
            zhDesc = "萌系二次元萌娘、猫耳绘图与可爱瞬间",
            enDesc = "Adorable anime art, sweet moods & cute character drawings.",
            sampleUrl = "https://images.pexels.com/photos/10311598/pexels-photo-10311598.jpeg?auto=compress&cs=tinysrgb&h=450",
            source = "Nekosia"
        ),
        CategoryItem(
            key = "nekosia_girl",
            zhTitle = "唯美少女",
            enTitle = "Anime Girl",
            zhDesc = "清纯柔美、和风或现代画风少女人物立绘",
            enDesc = "Stunning school uniforms, modern daily outfits & anime portraits.",
            sampleUrl = "https://images.pexels.com/photos/34534/people-peoples-homeless-male.jpg?auto=compress&cs=tinysrgb&h=450",
            source = "Nekosia"
        ),
        CategoryItem(
            key = "nekosia_maid",
            zhTitle = "优雅女仆",
            enTitle = "Classic Maid",
            zhDesc = "经典黑白女仆装与古典优雅的动漫角色",
            enDesc = "Beautiful maid dresses, classic ribbons & anime artwork.",
            sampleUrl = "https://images.pexels.com/photos/1765005/pexels-photo-1765005.jpeg?auto=compress&cs=tinysrgb&h=450",
            source = "Nekosia"
        ),
        CategoryItem(
            key = "nekosia_vtuber",
            zhTitle = "虚拟直播",
            enTitle = "VTuber Universe",
            zhDesc = "绚丽的虚拟主播同人与生动活泼的插画",
            enDesc = "Colorful VTuber personalities, stream outfits & portraits.",
            sampleUrl = "https://images.pexels.com/photos/7858125/pexels-photo-7858125.jpeg?auto=compress&cs=tinysrgb&h=450",
            source = "Nekosia"
        )
    )

    private val _customCategories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val customCategories: StateFlow<List<CategoryItem>> = _customCategories.asStateFlow()

    // Combined category list exposed to UI
    private val _categories = MutableStateFlow<List<CategoryItem>>(predefinedCategories)
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    // Category query bindings
    private val _customQueries = mutableMapOf<String, String>()


    // ==========================================
    // 3. REMOTE CONTENT STATEFLOWS
    // ==========================================
    private val _todayWallpaper = MutableStateFlow<WallpaperUiState<UnifiedWallpaper>>(WallpaperUiState.Loading)
    val todayWallpaper: StateFlow<WallpaperUiState<UnifiedWallpaper>> = _todayWallpaper.asStateFlow()

    // Active category grid state (when inspecting a category)
    private val _categoryGridState = MutableStateFlow<WallpaperUiState<List<UnifiedWallpaper>>>(WallpaperUiState.Loading)
    val categoryGridState: StateFlow<WallpaperUiState<List<UnifiedWallpaper>>> = _categoryGridState.asStateFlow()

    // Pagination state for infinite scrolling in category grid
    private var currentCategoryPage = 1
    private var currentCategoryKey = ""
    private var isLoadingMoreCategory = false
    private var hasMoreCategoryPages = true
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Preload pool for home page wallpapers - instant refresh experience
    private val _preloadPool = mutableListOf<UnifiedWallpaper>()
    private var preloadPoolType = "" // Track which type the pool belongs to
    private var isPreloading = false

    // Wallpaper history navigation (previous/next)
    private val _wallpaperHistory = mutableListOf<UnifiedWallpaper>()
    private var _historyIndex = -1
    private val _hasPreviousWallpaper = MutableStateFlow(false)
    val hasPreviousWallpaper: StateFlow<Boolean> = _hasPreviousWallpaper.asStateFlow()
    private val _hasNextWallpaper = MutableStateFlow(false)
    val hasNextWallpaper: StateFlow<Boolean> = _hasNextWallpaper.asStateFlow()

    // Home gesture mode preference
    private val _homeGestureEnabled = MutableStateFlow(false)
    val homeGestureEnabled: StateFlow<Boolean> = _homeGestureEnabled.asStateFlow()

    fun setHomeGestureEnabled(enabled: Boolean) {
        _homeGestureEnabled.value = enabled
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        sp.edit().putBoolean("home_gesture_enabled", enabled).apply()
    }

    private val _wallpaperSettingState = MutableStateFlow<SettingWallpaperState>(SettingWallpaperState.Idle)
    val wallpaperSettingState = _wallpaperSettingState.asStateFlow()

    sealed interface SettingWallpaperState {
        object Idle : SettingWallpaperState
        object Setting : SettingWallpaperState
        object Success : SettingWallpaperState
        data class Error(val message: String) : SettingWallpaperState
    }

    init {
        // Loads SharedPreferences settings for language and homepage category preference
        loadPreferences()
        
        // Loads customized Pexels themes list
        loadCustomCategories()
        
        // Fetch initials for home screen with current setting
        fetchTodayWallpaper()
    }

    private fun loadPreferences() {
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        _language.value = sp.getString("language", "zh") ?: "zh"
        _homeWallpaperType.value = sp.getString("home_wallpaper_type", "PexelsCurated") ?: "PexelsCurated"
        _username.value = sp.getString("username", "探索家用户") ?: "探索家用户"
        _avatarUrl.value = sp.getString("avatar_url", null)
        _pexelsApiKey.value = sp.getString("pexels_api_key", "") ?: ""
        _homeGestureEnabled.value = sp.getBoolean("home_gesture_enabled", false)
    }

    fun updateUsername(newName: String) {
        _username.value = newName
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        sp.edit().putString("username", newName).apply()
    }

    fun updateAvatar(newUrl: String?) {
        _avatarUrl.value = newUrl
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        sp.edit().putString("avatar_url", newUrl).apply()
    }

    fun loadCategoryCovers(categoryKeys: List<String>) {
        if (_categoryCovers.value.size >= categoryKeys.size) return
        viewModelScope.launch {
            val currentMap = _categoryCovers.value.toMutableMap()
            categoryKeys.forEach { key ->
                if (!currentMap.containsKey(key)) {
                    if (key.startsWith("nekosia_")) {
                        val nekCode = key.removePrefix("nekosia_")
                        repository.getNekosiaRandom(nekCode, count = 1).onSuccess { list ->
                            list.firstOrNull()?.image?.compressed?.url?.let { url ->
                                currentMap[key] = url
                                _categoryCovers.value = currentMap.toMap()
                            }
                        }.onFailure {
                            Log.e("WallpaperViewModel", "Failed to fetch nekosia category cover for $nekCode: ${it.message}")
                        }
                    } else {
                        val queryMap = mapOf(
                            "nature" to "scenery landscape wallpaper",
                            "space" to "cosmic galaxy astronomy",
                            "urban" to "tokyo new york cyberpunk design",
                            "minimalist" to "clean minimalist backgrounds"
                        )
                        val query = queryMap[key] ?: key
                        repository.searchPexels(query, page = 1, count = 1).onSuccess { res ->
                            res.photos.firstOrNull()?.src?.large2x?.let { url ->
                                currentMap[key] = url
                                _categoryCovers.value = currentMap.toMap()
                            }
                        }.onFailure {
                            Log.e("WallpaperViewModel", "Failed to fetch pexels category cover for $key: ${it.message}")
                        }
                    }
                }
            }
        }
    }

    fun toggleLanguage() {
        val newLang = if (_language.value == "zh") "en" else "zh"
        _language.value = newLang
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        sp.edit().putString("language", newLang).apply()
    }

    fun setHomeWallpaperType(type: String) {
        _homeWallpaperType.value = type
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        sp.edit().putString("home_wallpaper_type", type).apply()
        // Instantly reload home wallpaper when type is adjusted!
        fetchTodayWallpaper()
    }


    // ==========================================
    // 4. NETWORKING API METHODS
    // ==========================================

    fun fetchTodayWallpaper() {
        val type = _homeWallpaperType.value
        if (!type.startsWith("Nekosia") && _pexelsApiKey.value.isEmpty()) {
            performWithApiKeyCheck {
                fetchTodayWallpaperInternal()
            }
        } else {
            fetchTodayWallpaperInternal()
        }
    }

    fun goToPreviousWallpaper() {
        if (_historyIndex > 0) {
            _historyIndex--
            val wp = _wallpaperHistory[_historyIndex]
            _todayWallpaper.value = WallpaperUiState.Success(wp)
            updateHistoryButtonStates()
        }
    }

    fun goToNextWallpaper() {
        if (_historyIndex < _wallpaperHistory.size - 1) {
            _historyIndex++
            val wp = _wallpaperHistory[_historyIndex]
            _todayWallpaper.value = WallpaperUiState.Success(wp)
            updateHistoryButtonStates()
        } else {
            // At the end of history, fetch a new one
            fetchTodayWallpaper()
        }
    }

    private fun recordWallpaperHistory(wallpaper: UnifiedWallpaper) {
        // If we're not at the end of history, truncate forward history
        if (_historyIndex < _wallpaperHistory.size - 1) {
            _wallpaperHistory.subList(_historyIndex + 1, _wallpaperHistory.size).clear()
        }
        // Don't add duplicate consecutive wallpapers
        if (_wallpaperHistory.isEmpty() || _wallpaperHistory.last().id != wallpaper.id) {
            _wallpaperHistory.add(wallpaper)
            // Keep history manageable (max 50)
            if (_wallpaperHistory.size > 50) {
                _wallpaperHistory.removeFirstOrNull()
            }
            _historyIndex = _wallpaperHistory.size - 1
        }
        updateHistoryButtonStates()
    }

    private fun updateHistoryButtonStates() {
        _hasPreviousWallpaper.value = _historyIndex > 0
        _hasNextWallpaper.value = _historyIndex < _wallpaperHistory.size - 1
    }

    private fun fetchTodayWallpaperInternal() {
        viewModelScope.launch {
            val currentId = (_todayWallpaper.value as? WallpaperUiState.Success)?.data?.id
            val type = _homeWallpaperType.value

            // ===== PRELOAD POOL: Try to instantly serve from preloaded cache =====
            synchronized(_preloadPool) {
                if (preloadPoolType == type && _preloadPool.isNotEmpty()) {
                    val preloaded = _preloadPool.removeFirstOrNull()
                    if (preloaded != null && preloaded.id != currentId) {
                        _todayWallpaper.value = WallpaperUiState.Success(preloaded)
                        recordWallpaperHistory(preloaded)
                        // Preload more in background for next refresh
                        preloadNextHomeWallpapers()
                        return@launch
                    } else if (preloaded != null && preloaded.id == currentId && _preloadPool.isNotEmpty()) {
                        // Same ID, try next in pool
                        val alt = _preloadPool.removeFirstOrNull()
                        if (alt != null) {
                            _todayWallpaper.value = WallpaperUiState.Success(alt)
                            recordWallpaperHistory(alt)
                            preloadNextHomeWallpapers()
                            return@launch
                        }
                    }
                    // Pool empty or only duplicates, fall through to network fetch
                }
            }

            // ===== FALLBACK: Normal network fetch =====
            _todayWallpaper.value = WallpaperUiState.Loading
            
            if (type.startsWith("collection_")) {
                val collectionId = type.removePrefix("collection_").toIntOrNull()
                if (collectionId != null) {
                    try {
                        val items = repository.getItemsForCollection(collectionId).first()
                        if (items.isNotEmpty()) {
                            val context = getApplication<Application>()
                            val itemsNeedCaching = items.filter { !it.imageUrl.startsWith("file://") }
                            if (itemsNeedCaching.isNotEmpty()) {
                                launch {
                                    itemsNeedCaching.forEach { item ->
                                        val localImg = saveImageToInternalStorage(context, item.imageUrl, item.wallpaperId)
                                        val localThumb = if (item.thumbnailUrl != item.imageUrl) {
                                            saveImageToInternalStorage(context, item.thumbnailUrl, "${item.wallpaperId}_thumb")
                                        } else {
                                            localImg
                                        }
                                        repository.addWallpaperToCollection(
                                            collectionId = item.collectionId,
                                            itemId = item.wallpaperId,
                                            imageUrl = localImg,
                                            thumbnailUrl = localThumb,
                                            authorName = item.authorName,
                                            source = item.source
                                        )
                                    }
                                }
                            }

                            val randomItem = if (items.size == 1) {
                                val single = items.first()
                                if (single.wallpaperId == currentId) {
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            getTranslation("该图集只有一张图片 / This collection only has one image", "Only one image in this collection"),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                single
                            } else {
                                val choices = items.filter { it.wallpaperId != currentId }
                                choices.randomOrNull() ?: items.random()
                            }

                            val item = UnifiedWallpaper(
                                id = randomItem.wallpaperId,
                                imageUrl = randomItem.imageUrl,
                                thumbnailUrl = randomItem.thumbnailUrl,
                                author = randomItem.authorName ?: "Unknown Creator",
                                authorUrl = "",
                                source = randomItem.source,
                                category = null
                            )
                            _todayWallpaper.value = WallpaperUiState.Success(item)
                            recordWallpaperHistory(item)
                        } else {
                            _todayWallpaper.value = WallpaperUiState.Error("该图集暂无图片 / This collection is empty")
                        }
                    } catch (e: Exception) {
                        _todayWallpaper.value = WallpaperUiState.Error(e.localizedMessage ?: "Failed to load collection")
                    }
                } else {
                    _todayWallpaper.value = WallpaperUiState.Error("Invalid collection ID")
                }
            } else if (type.startsWith("Nekosia")) {
                val categoryName = type.removePrefix("Nekosia:")
                val result = repository.getNekosiaRandom(category = categoryName, count = 5)
                result.onSuccess { list ->
                    if (list.isNotEmpty()) {
                        val target = if (list.size == 1) {
                            val single = list.first()
                            val tId = "nekosia_${single.id ?: System.currentTimeMillis()}"
                            if (tId == currentId) {
                                val context = getApplication<Application>()
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast.makeText(
                                        context,
                                        getTranslation("该分类只有一张图片 / This category only has one image", "Only one image in this category"),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            single
                        } else {
                            val choices = list.filter { "nekosia_${it.id}" != currentId }
                            choices.randomOrNull() ?: list.random()
                        }

                        val item = UnifiedWallpaper(
                            id = "nekosia_${target.id ?: System.currentTimeMillis()}",
                            imageUrl = target.image?.original?.url ?: "",
                            thumbnailUrl = target.image?.compressed?.url ?: "",
                            author = target.attribution?.artist?.username ?: "Nekosia Artist",
                            authorUrl = target.attribution?.artist?.profile,
                            source = "Nekosia",
                            category = categoryName
                        )
                        _todayWallpaper.value = WallpaperUiState.Success(item)
                        recordWallpaperHistory(item)
                        // Background preload more for next refresh
                        preloadNextHomeWallpapers()
                    } else {
                        _todayWallpaper.value = WallpaperUiState.Error("No images available")
                    }
                }.onFailure {
                    _todayWallpaper.value = WallpaperUiState.Error(it.localizedMessage ?: "Network error")
                }
            } else {
                val query = when (type) {
                    "PexelsSpace" -> "cosmic space"
                    "PexelsMinimalist" -> "minimalist wallpaper"
                    "PexelsNature" -> "scenery wallpaper"
                    else -> _customQueries[type] ?: "curated"
                }

                if (query == "curated") {
                    val result = repository.getPexelsCurated(page = (1..50).random(), count = 80)
                    result.onSuccess { response ->
                        if (response.photos.isNotEmpty()) {
                            val targetPhoto = if (response.photos.size == 1) {
                                val single = response.photos.first()
                                if ("pexels_${single.id}" == currentId) {
                                    val context = getApplication<Application>()
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            getTranslation("该分类只有一张图片 / This category only has one image", "Only one image in this category"),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                single
                            } else {
                                val choices = response.photos.filter { "pexels_${it.id}" != currentId }
                                choices.randomOrNull() ?: response.photos.random()
                            }

                            val item = UnifiedWallpaper(
                                id = "pexels_${targetPhoto.id}",
                                imageUrl = targetPhoto.src.original,
                                thumbnailUrl = targetPhoto.src.large2x,
                                author = targetPhoto.photographer,
                                authorUrl = targetPhoto.photographerUrl,
                                source = "Pexels"
                            )
                            _todayWallpaper.value = WallpaperUiState.Success(item)
                            recordWallpaperHistory(item)
                            // Cache remaining photos into preload pool for instant next refresh
                            synchronized(_preloadPool) {
                                _preloadPool.clear()
                                val remaining = response.photos.filter { "pexels_${it.id}" != item.id && "pexels_${it.id}" != currentId }
                                remaining.shuffled().take(5).forEach { photo ->
                                    _preloadPool.add(
                                        UnifiedWallpaper(
                                            id = "pexels_${photo.id}",
                                            imageUrl = photo.src.original,
                                            thumbnailUrl = photo.src.large2x,
                                            author = photo.photographer,
                                            authorUrl = photo.photographerUrl,
                                            source = "Pexels"
                                        )
                                    )
                                }
                                preloadPoolType = type
                            }
                        } else {
                            _todayWallpaper.value = WallpaperUiState.Error("No photos found")
                        }
                    }.onFailure {
                        _todayWallpaper.value = WallpaperUiState.Error(it.localizedMessage ?: "Api error")
                    }
                } else {
                    val isCustom = type.startsWith("custom_pexels_")
                    val pageNum = if (isCustom) 1 else (1..10).random()
                    val count = if (isCustom) 40 else 20
                    val result = repository.searchPexels(query = query, page = pageNum, count = count)
                    result.onSuccess { response ->
                        if (response.photos.isNotEmpty()) {
                            val targetPhoto = if (response.photos.size == 1) {
                                val single = response.photos.first()
                                if ("pexels_${single.id}" == currentId) {
                                    val context = getApplication<Application>()
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            getTranslation("该分类只有一张图片 / This category only has one image", "Only one image in this category"),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                single
                            } else {
                                val choices = response.photos.filter { "pexels_${it.id}" != currentId }
                                choices.randomOrNull() ?: response.photos.random()
                            }

                            val item = UnifiedWallpaper(
                                id = "pexels_${targetPhoto.id}",
                                imageUrl = targetPhoto.src.original,
                                thumbnailUrl = targetPhoto.src.large2x,
                                author = targetPhoto.photographer,
                                authorUrl = targetPhoto.photographerUrl,
                                source = "Pexels"
                            )
                            _todayWallpaper.value = WallpaperUiState.Success(item)
                            recordWallpaperHistory(item)
                            // Cache remaining photos into preload pool
                            synchronized(_preloadPool) {
                                _preloadPool.clear()
                                val remaining = response.photos.filter { "pexels_${it.id}" != item.id && "pexels_${it.id}" != currentId }
                                remaining.shuffled().take(5).forEach { photo ->
                                    _preloadPool.add(
                                        UnifiedWallpaper(
                                            id = "pexels_${photo.id}",
                                            imageUrl = photo.src.original,
                                            thumbnailUrl = photo.src.large2x,
                                            author = photo.photographer,
                                            authorUrl = photo.photographerUrl,
                                            source = "Pexels"
                                        )
                                    )
                                }
                                preloadPoolType = type
                            }
                        } else {
                            _todayWallpaper.value = WallpaperUiState.Error("No photos found for keyword $query")
                        }
                    }.onFailure {
                        _todayWallpaper.value = WallpaperUiState.Error(it.localizedMessage ?: "Api error")
                    }
                }
            }
        }
    }

    /**
     * Background preload: fetch wallpapers in the background and cache them in the preload pool.
     * Called after each successful fetch to keep the pool filled for instant next refresh.
     */
    private fun preloadNextHomeWallpapers() {
        val type = _homeWallpaperType.value
        // Only preload if pool is low and not already preloading
        synchronized(_preloadPool) {
            if (isPreloading || _preloadPool.size >= 3 || preloadPoolType != type) return
            isPreloading = true
        }

        viewModelScope.launch {
            try {
                if (type.startsWith("Nekosia")) {
                    val categoryName = type.removePrefix("Nekosia:")
                    val result = repository.getNekosiaRandom(category = categoryName, count = 5)
                    result.onSuccess { list ->
                        val currentId = (_todayWallpaper.value as? WallpaperUiState.Success)?.data?.id
                        synchronized(_preloadPool) {
                            val existingIds = _preloadPool.map { it.id }.toSet()
                            list.filter { "nekosia_${it.id}" != currentId && "nekosia_${it.id}" !in existingIds }
                                .take(3)
                                .forEach { res ->
                                    _preloadPool.add(
                                        UnifiedWallpaper(
                                            id = "nekosia_${res.id ?: System.currentTimeMillis()}",
                                            imageUrl = res.image?.original?.url ?: "",
                                            thumbnailUrl = res.image?.compressed?.url ?: "",
                                            author = res.attribution?.artist?.username ?: "Nekosia Artist",
                                            authorUrl = res.attribution?.artist?.profile,
                                            source = "Nekosia",
                                            category = categoryName
                                        )
                                    )
                                }
                            preloadPoolType = type
                        }
                    }
                } else if (!type.startsWith("collection_")) {
                    val query = when (type) {
                        "PexelsSpace" -> "cosmic space"
                        "PexelsMinimalist" -> "minimalist wallpaper"
                        "PexelsNature" -> "scenery wallpaper"
                        else -> _customQueries[type] ?: "curated"
                    }

                    if (query == "curated") {
                        val result = repository.getPexelsCurated(page = (1..50).random(), count = 80)
                        result.onSuccess { response ->
                            val currentId = (_todayWallpaper.value as? WallpaperUiState.Success)?.data?.id
                            synchronized(_preloadPool) {
                                val existingIds = _preloadPool.map { it.id }.toSet()
                                response.photos.filter { "pexels_${it.id}" != currentId && "pexels_${it.id}" !in existingIds }
                                    .shuffled().take(5)
                                    .forEach { photo ->
                                        _preloadPool.add(
                                            UnifiedWallpaper(
                                                id = "pexels_${photo.id}",
                                                imageUrl = photo.src.original,
                                                thumbnailUrl = photo.src.large2x,
                                                author = photo.photographer,
                                                authorUrl = photo.photographerUrl,
                                                source = "Pexels"
                                            )
                                        )
                                    }
                                preloadPoolType = type
                            }
                        }
                    } else {
                        val isCustom = type.startsWith("custom_pexels_")
                        val pageNum = if (isCustom) 1 else (1..10).random()
                        val count = if (isCustom) 40 else 20
                        val result = repository.searchPexels(query = query, page = pageNum, count = count)
                        result.onSuccess { response ->
                            val currentId = (_todayWallpaper.value as? WallpaperUiState.Success)?.data?.id
                            synchronized(_preloadPool) {
                                val existingIds = _preloadPool.map { it.id }.toSet()
                                response.photos.filter { "pexels_${it.id}" != currentId && "pexels_${it.id}" !in existingIds }
                                    .shuffled().take(5)
                                    .forEach { photo ->
                                        _preloadPool.add(
                                            UnifiedWallpaper(
                                                id = "pexels_${photo.id}",
                                                imageUrl = photo.src.original,
                                                thumbnailUrl = photo.src.large2x,
                                                author = photo.photographer,
                                                authorUrl = photo.photographerUrl,
                                                source = "Pexels"
                                            )
                                        )
                                    }
                                preloadPoolType = type
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Preload failed: ${e.message}")
            } finally {
                isPreloading = false
            }
        }
    }

    fun loadCategoryWallpapers(categoryKey: String) {
        if (!categoryKey.startsWith("nekosia_") && _pexelsApiKey.value.isEmpty()) {
            performWithApiKeyCheck {
                loadCategoryWallpapersInternal(categoryKey)
            }
        } else {
            loadCategoryWallpapersInternal(categoryKey)
        }
    }

    private fun loadCategoryWallpapersInternal(categoryKey: String) {
        viewModelScope.launch {
            _categoryGridState.value = WallpaperUiState.Loading
            // Reset pagination state
            currentCategoryKey = categoryKey
            currentCategoryPage = 1
            hasMoreCategoryPages = true
            
            if (categoryKey.startsWith("nekosia_")) {
                val nekCode = categoryKey.removePrefix("nekosia_")
                hasMoreCategoryPages = false // Nekosia doesn't support pagination
                val result = repository.getNekosiaRandom(nekCode, count = 15)
                result.onSuccess { list ->
                    val wallpapers = list.map { res ->
                        UnifiedWallpaper(
                            id = "nekosia_${res.id ?: (System.currentTimeMillis() + (1..1000).random())}",
                            imageUrl = res.image?.original?.url ?: "",
                            thumbnailUrl = res.image?.compressed?.url ?: "",
                            author = res.attribution?.artist?.username ?: "Artist",
                            authorUrl = res.attribution?.artist?.profile,
                            source = "Nekosia",
                            category = nekCode
                        )
                    }.distinctBy { it.id }
                    _categoryGridState.value = WallpaperUiState.Success(wallpapers)
                }.onFailure {
                    _categoryGridState.value = WallpaperUiState.Error(it.localizedMessage ?: "Network error")
                }
            } else {
                // Pexels Category with pagination support (80 images per page for infinite experience)
                val queryMap = mapOf(
                    "nature" to "scenery landscape wallpaper",
                    "space" to "cosmic galaxy astronomy",
                    "urban" to "tokyo new york cyberpunk design",
                    "ocean" to "deep ocean waves blue",
                    "minimalist" to "clean minimalist backgrounds"
                )
                val query = queryMap[categoryKey] ?: _customQueries[categoryKey] ?: categoryKey
                val pageNum = if (categoryKey.startsWith("custom_pexels_")) 1 else (1..5).random()
                currentCategoryPage = pageNum
                // Fetch 80 wallpapers per page (Pexels max per_page is 80)
                val result = repository.searchPexels(query, page = pageNum, count = 80)
                result.onSuccess { res ->
                    val wallpapers = res.photos.map { photo ->
                        UnifiedWallpaper(
                            id = "pexels_${photo.id}",
                            imageUrl = photo.src.original,
                            thumbnailUrl = photo.src.large,
                            author = photo.photographer,
                            authorUrl = photo.photographerUrl,
                            source = "Pexels"
                        )
                    }.distinctBy { it.id }
                    // If we got fewer than expected or page exceeded max, no more pages
                    hasMoreCategoryPages = res.photos.size >= 80
                    _categoryGridState.value = WallpaperUiState.Success(wallpapers)
                }.onFailure {
                    _categoryGridState.value = WallpaperUiState.Error(it.localizedMessage ?: "Api error")
                }
            }
        }
    }

    /**
     * Load more wallpapers for the current category (infinite scroll pagination)
     */
    fun loadMoreCategoryWallpapers() {
        if (isLoadingMoreCategory || !hasMoreCategoryPages) return
        val categoryKey = currentCategoryKey
        if (categoryKey.isEmpty()) return
        
        isLoadingMoreCategory = true
        _isLoadingMore.value = true
        
        viewModelScope.launch {
            if (categoryKey.startsWith("nekosia_")) {
                val nekCode = categoryKey.removePrefix("nekosia_")
                val result = repository.getNekosiaRandom(nekCode, count = 15)
                result.onSuccess { list ->
                    val newWallpapers = list.map { res ->
                        UnifiedWallpaper(
                            id = "nekosia_${res.id ?: (System.currentTimeMillis() + (1..1000).random())}",
                            imageUrl = res.image?.original?.url ?: "",
                            thumbnailUrl = res.image?.compressed?.url ?: "",
                            author = res.attribution?.artist?.username ?: "Artist",
                            authorUrl = res.attribution?.artist?.profile,
                            source = "Nekosia",
                            category = nekCode
                        )
                    }.distinctBy { it.id }
                    
                    val current = (_categoryGridState.value as? WallpaperUiState.Success)?.data ?: emptyList()
                    val existingIds = current.map { it.id }.toSet()
                    val filteredNew = newWallpapers.filter { it.id !in existingIds }
                    if (filteredNew.isNotEmpty()) {
                        _categoryGridState.value = WallpaperUiState.Success(current + filteredNew)
                    }
                }
            } else {
                val queryMap = mapOf(
                    "nature" to "scenery landscape wallpaper",
                    "space" to "cosmic galaxy astronomy",
                    "urban" to "tokyo new york cyberpunk design",
                    "ocean" to "deep ocean waves blue",
                    "minimalist" to "clean minimalist backgrounds"
                )
                val query = queryMap[categoryKey] ?: _customQueries[categoryKey] ?: categoryKey
                currentCategoryPage++
                val result = repository.searchPexels(query, page = currentCategoryPage, count = 80)
                result.onSuccess { res ->
                    val newWallpapers = res.photos.map { photo ->
                        UnifiedWallpaper(
                            id = "pexels_${photo.id}",
                            imageUrl = photo.src.original,
                            thumbnailUrl = photo.src.large,
                            author = photo.photographer,
                            authorUrl = photo.photographerUrl,
                            source = "Pexels"
                        )
                    }.distinctBy { it.id }
                    
                    val current = (_categoryGridState.value as? WallpaperUiState.Success)?.data ?: emptyList()
                    val existingIds = current.map { it.id }.toSet()
                    val filteredNew = newWallpapers.filter { it.id !in existingIds }
                    if (filteredNew.isNotEmpty()) {
                        _categoryGridState.value = WallpaperUiState.Success(current + filteredNew)
                    }
                    hasMoreCategoryPages = res.photos.size >= 80
                }.onFailure {
                    // Silently fail on load more - don't break the grid
                    Log.e("WallpaperViewModel", "Failed to load more wallpapers: ${it.message}")
                }
            }
            isLoadingMoreCategory = false
            _isLoadingMore.value = false
        }
    }


    // ==========================================
    // 5. FAVORITING & ALBUM OPERATIONS
    // ==========================================

    fun isWallpaperFavoriteFlow(id: String): StateFlow<Boolean> {
        return repository.isFavorite(id).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )
    }

    fun toggleFavorite(wallpaper: UnifiedWallpaper) {
        viewModelScope.launch {
            val isFav = repository.isFavorite(wallpaper.id).first()
            if (isFav) {
                repository.deleteFavorite(wallpaper.id)
            } else {
                repository.insertFavorite(
                    FavoriteWallpaper(
                        id = wallpaper.id,
                        imageUrl = wallpaper.imageUrl,
                        thumbnailUrl = wallpaper.thumbnailUrl,
                        authorName = wallpaper.author,
                        authorUrl = wallpaper.authorUrl,
                        source = wallpaper.source,
                        category = wallpaper.category
                    )
                )
            }
        }
    }

    fun createNewCollection(name: String, description: String?, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val id = repository.createCollection(name, description)
            onComplete(id)
        }
    }

    private suspend fun saveImageToInternalStorage(context: Context, url: String, id: String): String {
        if (url.startsWith("file://")) return url
        return withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            try {
                val result = loader.execute(request)
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val folder = java.io.File(context.filesDir, "local_collections")
                    if (!folder.exists()) {
                        folder.mkdirs()
                    }
                    val sanitizedId = id.replace("[^a-zA-Z0-9]".toRegex(), "_")
                    val file = java.io.File(folder, "img_${sanitizedId}_${System.currentTimeMillis()}.jpg")
                    java.io.FileOutputStream(file).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    "file://${file.absolutePath}"
                } else {
                    url
                }
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Failed to save image locally: $url", e)
                url
            }
        }
    }

    fun addWallpaperToCollectionId(collectionId: Int, wallpaper: UnifiedWallpaper) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            // Automatically download and cache image & thumbnail locally, keeping collections fully offline-persistent!
            val localImageUrl = saveImageToInternalStorage(context, wallpaper.imageUrl, wallpaper.id)
            val localThumbnailUrl = if (wallpaper.thumbnailUrl != wallpaper.imageUrl) {
                saveImageToInternalStorage(context, wallpaper.thumbnailUrl, "${wallpaper.id}_thumb")
            } else {
                localImageUrl
            }

            repository.addWallpaperToCollection(
                collectionId = collectionId,
                itemId = wallpaper.id,
                imageUrl = localImageUrl,
                thumbnailUrl = localThumbnailUrl,
                authorName = wallpaper.author,
                source = wallpaper.source
            )
        }
    }

    fun deleteCollectionId(id: Int) {
        viewModelScope.launch {
            repository.deleteCollection(id)
        }
    }

    fun removeWallpaperFromCollection(collectionId: Int, wallpaperId: String) {
        viewModelScope.launch {
            repository.removeWallpaperFromCollection(collectionId, wallpaperId)
        }
    }

    fun updateCollectionItemMeta(itemId: Int, authorName: String?, source: String) {
        viewModelScope.launch {
            repository.updateCollectionItemMeta(itemId, authorName, source)
        }
    }

    fun fetchActiveCollectionItems(collectionId: Int) {
        viewModelScope.launch {
            repository.getItemsForCollection(collectionId).collect {
                _activeCollectionItems.value = it
            }
        }
    }


    // ==========================================
    // 6. REAL ANDROID SDK WALLPAPER MANAGER
    // ==========================================

    fun setSystemWallpaper(context: Context, imageUrl: String) {
        viewModelScope.launch {
            _wallpaperSettingState.value = SettingWallpaperState.Setting
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Required for getting bitmap out of drawable
                .build()

            try {
                val result = loader.execute(request)
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    // Set wallpaper on background Dispatcher as setBitmap is a blocking I/O operation
                    withContext(Dispatchers.IO) {
                        wallpaperManager.setBitmap(bitmap)
                    }
                    _wallpaperSettingState.value = SettingWallpaperState.Success
                } else {
                    _wallpaperSettingState.value = SettingWallpaperState.Error("Image decoding failed")
                }
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error setting wallpaper: ${e.message}", e)
                _wallpaperSettingState.value = SettingWallpaperState.Error(e.localizedMessage ?: "Failed to set wallpaper")
            }
        }
    }

    fun resetWallpaperSettingState() {
        _wallpaperSettingState.value = SettingWallpaperState.Idle
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    sealed interface DownloadState {
        object Idle : DownloadState
        object Downloading : DownloadState
        object Success : DownloadState
        data class Error(val message: String) : DownloadState
    }

    fun downloadWallpaper(context: Context, imageUrl: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            try {
                val result = loader.execute(request)
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val filename = "wallpaper_${System.currentTimeMillis()}"

                    val success = withContext(Dispatchers.IO) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val contentValues = android.content.ContentValues().apply {
                                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$filename.jpg")
                                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/TodayWallpaper")
                                }
                                val resolver = context.contentResolver
                                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                                if (uri != null) {
                                    resolver.openOutputStream(uri)?.use { out ->
                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                                    }
                                    true
                                } else {
                                    false
                                }
                            } else {
                                val dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                                val albumDir = java.io.File(dir, "TodayWallpaper")
                                if (!albumDir.exists()) {
                                    albumDir.mkdirs()
                                }
                                val file = java.io.File(albumDir, "$filename.jpg")
                                java.io.FileOutputStream(file).use { out ->
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                                }
                                android.media.MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(file.absolutePath),
                                    arrayOf("image/jpeg"),
                                    null
                                )
                                true
                            }
                        } catch (e: Exception) {
                            Log.e("WallpaperViewModel", "Error saving image: ${e.message}", e)
                            false
                        }
                    }

                    if (success) {
                        _downloadState.value = DownloadState.Success
                    } else {
                        _downloadState.value = DownloadState.Error("Failed to save image to gallery")
                    }
                } else {
                    _downloadState.value = DownloadState.Error("Failed to decode image drawable")
                }
            } catch (e: Exception) {
                Log.e("WallpaperViewModel", "Error downloading wallpaper: ${e.message}", e)
                _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Failed to download image")
            }
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    // Locale Bilingual text helper (returns String depending on selected state)
    fun getTranslation(zh: String, en: String): String {
        return if (_language.value == "zh") zh else en
    }

    // ==========================================
    // 7. CUSTOM PEXELS CATEGORIES PERSISTENCE
    // ==========================================

    private fun loadCustomCategories() {
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        val jsonStr = sp.getString("custom_categories_json", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<CategoryItem>()
            _customQueries.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val key = obj.getString("key")
                val title = obj.getString("title")
                val query = obj.getString("query")
                val desc = obj.optString("desc", "")
                val sampleUrl = obj.optString("sampleUrl", "https://images.pexels.com/photos/3225517/pexels-photo-3225517.jpeg")
                
                val item = CategoryItem(
                    key = key,
                    zhTitle = title,
                    enTitle = title,
                    zhDesc = desc,
                    enDesc = desc,
                    sampleUrl = sampleUrl,
                    source = "Pexels"
                )
                list.add(item)
                _customQueries[key] = query
            }
            _customCategories.value = list
            updateUnifiedCategories()
        } catch (e: Exception) {
            Log.e("WallpaperViewModel", "Error parsing custom categories json: ${e.message}")
        }
    }

    private fun saveCustomCategories() {
        val sp = getApplication<Application>().getSharedPreferences("app_gallery_prefs", Application.MODE_PRIVATE)
        try {
            val arr = JSONArray()
            _customCategories.value.forEach { item ->
                val obj = JSONObject()
                obj.put("key", item.key)
                obj.put("title", item.zhTitle)
                val query = _customQueries[item.key] ?: item.key
                obj.put("query", query)
                obj.put("desc", item.zhDesc)
                obj.put("sampleUrl", item.sampleUrl)
                arr.put(obj)
            }
            sp.edit().putString("custom_categories_json", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("WallpaperViewModel", "Error saving custom categories: ${e.message}")
        }
    }

    private fun updateUnifiedCategories() {
        _categories.value = predefinedCategories + _customCategories.value
    }

    fun addCustomCategory(title: String, query: String, desc: String, onResult: (Boolean, String) -> Unit) {
        if (_pexelsApiKey.value.isEmpty()) {
            performWithApiKeyCheck {
                addCustomCategoryInternal(title, query, desc, onResult)
            }
        } else {
            addCustomCategoryInternal(title, query, desc, onResult)
        }
    }

    private fun addCustomCategoryInternal(title: String, query: String, desc: String, onResult: (Boolean, String) -> Unit) {
        if (title.isBlank() || query.isBlank()) {
            onResult(false, getTranslation("分类名称与搜索关键词不能为空", "Title and search query cannot be empty"))
            return
        }
        
        viewModelScope.launch {
            _categoryGridState.value = WallpaperUiState.Loading
            repository.searchPexels(query, page = 1, count = 1).onSuccess { res ->
                if (res.photos.isEmpty()) {
                    onResult(false, getTranslation("Pexels 平台上找不到与该关键词匹配的高清美图，请重新输入更常见的英文词汇", "No direct landscape wallpapers found matching this query. Please check your spelling or use a more descriptive term."))
                    return@onSuccess
                }
                
                val firstPhoto = res.photos.first()
                val coverUrl = firstPhoto.src.large2x
                val key = "custom_pexels_${System.currentTimeMillis()}"
                
                val newItem = CategoryItem(
                    key = key,
                    zhTitle = title,
                    enTitle = title,
                    zhDesc = if (desc.isBlank()) getTranslation("自定义摄影系列：「$query」", "Custom landscape collection: $query") else desc,
                    enDesc = if (desc.isBlank()) getTranslation("自定义摄影系列：「$query」", "Custom landscape collection: $query") else desc,
                    sampleUrl = coverUrl,
                    source = "Pexels"
                )
                
                val currentList = _customCategories.value.toMutableList()
                currentList.add(newItem)
                _customCategories.value = currentList
                _customQueries[key] = query
                
                // Add cover cache
                val coverMap = _categoryCovers.value.toMutableMap()
                coverMap[key] = coverUrl
                _categoryCovers.value = coverMap
                
                saveCustomCategories()
                updateUnifiedCategories()
                
                onResult(true, "")
            }.onFailure { error ->
                Log.e("WallpaperViewModel", "API check failed for keyword: $query: ${error.message}")
                onResult(false, getTranslation("检索 Pexels API 发生网络异常，请确认网络环境或更换关键词! (${error.localizedMessage})", "Error querying search API: ${error.localizedMessage}"))
            }
        }
    }

    fun addCustomCategories(items: List<Triple<String, String, String>>, onResult: (Int, Int, String) -> Unit) {
        if (_pexelsApiKey.value.isEmpty()) {
            performWithApiKeyCheck {
                addCustomCategoriesInternal(items, onResult)
            }
        } else {
            addCustomCategoriesInternal(items, onResult)
        }
    }

    private fun addCustomCategoriesInternal(items: List<Triple<String, String, String>>, onResult: (Int, Int, String) -> Unit) {
        if (items.isEmpty()) {
            onResult(0, 0, getTranslation("未选择或输入任何分类", "No categories specified"))
            return
        }

        viewModelScope.launch {
            _categoryGridState.value = WallpaperUiState.Loading
            var successCount = 0
            var failedCount = 0
            val successItems = mutableListOf<CategoryItem>()
            val newQueries = mutableMapOf<String, String>()
            val newCovers = mutableMapOf<String, String>()
            var lastError = ""

            items.forEach { (title, query, desc) ->
                if (title.isBlank() || query.isBlank()) {
                    failedCount++
                    return@forEach
                }

                repository.searchPexels(query, page = 1, count = 1).onSuccess { res ->
                    if (res.photos.isEmpty()) {
                        failedCount++
                        lastError = getTranslation("Pexels 上找不到关键词: $query", "No photos found on Pexels for keyword: $query")
                    } else {
                        val firstPhoto = res.photos.first()
                        val coverUrl = firstPhoto.src.large2x
                        val key = "custom_pexels_${System.currentTimeMillis()}_${(1000..9999).random()}"

                        val newItem = CategoryItem(
                            key = key,
                            zhTitle = title,
                            enTitle = title,
                            zhDesc = if (desc.isBlank()) getTranslation("自定义摄影系列：「$query」", "Custom landscape collection: $query") else desc,
                            enDesc = if (desc.isBlank()) getTranslation("自定义摄影系列：「$query」", "Custom landscape collection: $query") else desc,
                            sampleUrl = coverUrl,
                            source = "Pexels"
                        )
                        successItems.add(newItem)
                        newQueries[key] = query
                        newCovers[key] = coverUrl
                        successCount++
                    }
                }.onFailure { error ->
                    failedCount++
                    lastError = error.localizedMessage ?: "Error querying server"
                }
            }

            if (successItems.isNotEmpty()) {
                val currentList = _customCategories.value.toMutableList()
                currentList.addAll(successItems)
                _customCategories.value = currentList
                _customQueries.putAll(newQueries)

                // Add cover cache
                val coverMap = _categoryCovers.value.toMutableMap()
                coverMap.putAll(newCovers)
                _categoryCovers.value = coverMap

                saveCustomCategories()
                updateUnifiedCategories()
            }

            _categoryGridState.value = WallpaperUiState.Success(emptyList())
            onResult(successCount, failedCount, lastError)
        }
    }

    fun deleteCustomCategory(key: String) {
        val currentList = _customCategories.value.toMutableList()
        currentList.removeAll { it.key == key }
        _customCategories.value = currentList
        _customQueries.remove(key)
        
        saveCustomCategories()
        updateUnifiedCategories()
    }

    // ==========================================
    // 8. WIDGET DATA PERSISTENCE
    // ==========================================

    init {
        // Observe wallpaper changes and update widget SharedPreferences
        viewModelScope.launch {
            todayWallpaper.collect { state ->
                if (state is WallpaperUiState.Success) {
                    val wallpaper = state.data
                    val sp = getApplication<Application>().getSharedPreferences(
                        "app_gallery_prefs", Application.MODE_PRIVATE
                    )
                    sp.edit()
                        .putString("widget_wallpaper_url", wallpaper.imageUrl)
                        .putString("widget_wallpaper_author", wallpaper.author)
                        .putString("widget_wallpaper_source", wallpaper.source)
                        .apply()
                    // Refresh all widget instances
                    try {
                        btm.m.todaywallpaper.ui.widget.TodayWallpaperWidgetUpdater.updateAll(
                            getApplication<Application>()
                        )
                    } catch (e: Exception) {
                        Log.e("WallpaperViewModel", "Failed to update widgets: ${e.message}")
                    }
                }
            }
        }
    }
}
