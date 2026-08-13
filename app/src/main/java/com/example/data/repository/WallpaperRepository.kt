package btm.m.todaywallpaper.data.repository

import android.content.Context
import android.util.Log
import btm.m.todaywallpaper.data.api.NekosiaApiService
import btm.m.todaywallpaper.data.api.PexelsApiService
import btm.m.todaywallpaper.data.api.PixabayApiService
import btm.m.todaywallpaper.data.api.WallhavenApiService
import btm.m.todaywallpaper.data.api.DeviantArtApiService
import btm.m.todaywallpaper.data.local.WallpaperDao
import btm.m.todaywallpaper.data.local.WallpaperDatabase
import btm.m.todaywallpaper.data.model.AlbumCategory
import btm.m.todaywallpaper.data.model.CollectionItem
import btm.m.todaywallpaper.data.model.FavoriteWallpaper
import btm.m.todaywallpaper.data.model.HistoryWallpaper
import btm.m.todaywallpaper.data.model.NekosiaResponse
import btm.m.todaywallpaper.data.model.PexelsResponse
import btm.m.todaywallpaper.data.model.PixabayResponse
import btm.m.todaywallpaper.data.model.WallhavenResponse
import btm.m.todaywallpaper.data.model.DeviantArtBrowseResponse
import btm.m.todaywallpaper.data.model.WallpaperCollection
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WallpaperRepository(private val context: Context) {

    private val defaultAlbumCategoryNames = listOf("默认")
    private val obsoleteBuiltInAlbumCategoryNames = listOf(
        "风景", "城市", "自然", "人物", "艺术", "极简",
        "动漫", "太空", "海洋", "旅行", "其他"
    )

    private val db = WallpaperDatabase.getDatabase(context)
    private val dao: WallpaperDao = db.wallpaperDao

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val pexelsApi: PexelsApiService = Retrofit.Builder()
        .baseUrl(PexelsApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PexelsApiService::class.java)

    private val nekosiaApi: NekosiaApiService = Retrofit.Builder()
        .baseUrl(NekosiaApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NekosiaApiService::class.java)

    private val pixabayApi: PixabayApiService = Retrofit.Builder()
        .baseUrl(PixabayApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PixabayApiService::class.java)

    private val wallhavenApi: WallhavenApiService = Retrofit.Builder()
        .baseUrl(WallhavenApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(WallhavenApiService::class.java)

    private val deviantArtApi: DeviantArtApiService = Retrofit.Builder()
        .baseUrl(DeviantArtApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DeviantArtApiService::class.java)

    private val deviantArtTokenMutex = Mutex()
    private var deviantArtAccessToken: String? = null
    private var deviantArtTokenExpiresAt = 0L
    private var deviantArtTokenClientId = ""
    private var deviantArtTokenClientSecret = ""

    // ==========================================
    // 1. DATABASE OPS (FLOWS)
    // ==========================================

    val allFavorites: Flow<List<FavoriteWallpaper>> = dao.getAllFavorites()
    val recentHistory: Flow<List<HistoryWallpaper>> = dao.getRecentHistory()
    val allCollections: Flow<List<WallpaperCollection>> = dao.getAllCollections()
    val allAlbumCategories: Flow<List<AlbumCategory>> = dao.getAllAlbumCategories()

    suspend fun ensureDefaultAlbumCategories() = withContext(Dispatchers.IO) {
        defaultAlbumCategoryNames.forEachIndexed { index, name ->
            dao.insertAlbumCategory(
                AlbumCategory(name = name, createdAt = index.toLong())
            )
        }
        val defaultCategory = dao.getAlbumCategoryByName("默认")
        if (defaultCategory != null) {
            dao.moveCollectionsToCategory(
                defaultCategoryId = defaultCategory.id,
                names = obsoleteBuiltInAlbumCategoryNames
            )
            dao.deleteAlbumCategoriesByNames(obsoleteBuiltInAlbumCategoryNames)
        }
    }

    fun getItemsForCollection(collectionId: Int): Flow<List<CollectionItem>> =
        dao.getItemsForCollection(collectionId)

    fun isFavorite(id: String): Flow<Boolean> = dao.isWallpaperFavorite(id)

    suspend fun insertFavorite(favorite: FavoriteWallpaper) = withContext(Dispatchers.IO) {
        dao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(id: String) = withContext(Dispatchers.IO) {
        dao.deleteFavoriteById(id)
    }

    suspend fun recordHistory(history: HistoryWallpaper) = withContext(Dispatchers.IO) {
        dao.insertHistory(history)
    }

    suspend fun deleteHistory(id: String) = withContext(Dispatchers.IO) {
        dao.deleteHistoryById(id)
    }

    suspend fun createCollection(name: String, description: String?, categoryId: Long? = null): Int = withContext(Dispatchers.IO) {
        val collection = WallpaperCollection(
            name = name,
            description = description,
            coverUrl = null,
            categoryId = categoryId
        )
        dao.insertCollection(collection).toInt()
    }

    suspend fun createAlbumCategory(name: String): AlbumCategory = withContext(Dispatchers.IO) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Category name cannot be empty" }
        dao.getAlbumCategoryByName(normalizedName)?.let { return@withContext it }

        val id = dao.insertAlbumCategory(AlbumCategory(name = normalizedName))
        if (id > 0) {
            AlbumCategory(id = id, name = normalizedName)
        } else {
            dao.getAlbumCategoryByName(normalizedName)
                ?: error("Failed to create album category")
        }
    }

    suspend fun deleteCollection(collectionId: Int) = withContext(Dispatchers.IO) {
        dao.deleteCollectionItemsByCollectionId(collectionId)
        dao.deleteCollectionById(collectionId)
    }

    suspend fun addWallpaperToCollection(
        collectionId: Int,
        itemId: String,
        imageUrl: String,
        thumbnailUrl: String,
        authorName: String?,
        source: String
    ) = withContext(Dispatchers.IO) {
        val item = CollectionItem(
            collectionId = collectionId,
            wallpaperId = itemId,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            authorName = authorName,
            source = source
        )
        dao.insertCollectionItem(item)
        // Also update the collection cover to this image
        dao.updateCollectionCover(collectionId, thumbnailUrl)
    }

    suspend fun removeWallpaperFromCollection(collectionId: Int, wallpaperId: String) = withContext(Dispatchers.IO) {
        dao.deleteCollectionItem(collectionId, wallpaperId)
        // Update collection cover to the latest remaining item's thumbnail, or null if empty
        val newCover = dao.getLatestItemThumbnail(collectionId)
        dao.updateCollectionCover(collectionId, newCover ?: "")
    }

    suspend fun updateCollectionItemMeta(itemId: Int, authorName: String?, source: String) = withContext(Dispatchers.IO) {
        dao.updateCollectionItemMeta(itemId, authorName, source)
    }

    suspend fun updateCollectionCover(collectionId: Int, coverUrl: String) = withContext(Dispatchers.IO) {
        dao.updateCollectionCover(collectionId, coverUrl)
    }


    // ==========================================
    // 2. REMOTE WALLPAPER API FETCHING
    // ==========================================

    private fun getPexelsApiKey(): String {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        return sp.getString("pexels_api_key", "") ?: ""
    }

    private fun getPixabayApiKey(): String {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        return sp.getString("pixabay_api_key", "") ?: ""
    }

    private fun getWallhavenApiKey(): String {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        return sp.getString("wallhaven_api_key", "") ?: ""
    }

    private fun getDeviantArtCredentials(): Pair<String, String> {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        return (sp.getString("deviantart_client_id", "") ?: "") to
            (sp.getString("deviantart_client_secret", "") ?: "")
    }

    /**
     * Fetch curated wallpapers from Pexels (highly realistic HD landscape & portrait scenery)
     */
    suspend fun getPexelsCurated(page: Int = 1, count: Int = 20): Result<PexelsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = pexelsApi.getCuratedWallpapers(apiKey = getPexelsApiKey(), perPage = count, page = page)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error fetching Pexels Curated: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Search wallpapers on Pexels under a given theme keyword
     */
    suspend fun searchPexels(query: String, page: Int = 1, count: Int = 20): Result<PexelsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = pexelsApi.searchWallpapers(apiKey = getPexelsApiKey(), query = query, perPage = count, page = page)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error searching Pexels for $query: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Search safe photo results on Pixabay under a given theme keyword. */
    suspend fun searchPixabay(query: String, page: Int = 1, count: Int = 20): Result<PixabayResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getPixabayApiKey()
            require(apiKey.isNotBlank()) { "Pixabay API Key is not configured." }
            Result.success(
                pixabayApi.searchImages(
                    apiKey = apiKey,
                    query = query,
                    page = page,
                    perPage = count.coerceIn(3, 200)
                )
            )
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error searching Pixabay for $query: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Search Wallhaven with SFW-only purity by default. NSFW requests require a key. */
    suspend fun searchWallhaven(
        query: String,
        page: Int = 1,
        count: Int = 24,
        includeNsfw: Boolean = false
    ): Result<WallhavenResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getWallhavenApiKey().takeIf { it.isNotBlank() }
            require(!includeNsfw || apiKey != null) { "Wallhaven API Key is required for NSFW browsing." }
            Result.success(
                wallhavenApi.searchWallpapers(
                    query = query.takeIf { it.isNotBlank() },
                    purity = if (includeNsfw) "111" else "100",
                    page = page,
                    apiKey = apiKey
                )
            )
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error searching Wallhaven for $query: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Search DeviantArt through the OAuth2 API with strict SFW/NSFW separation. */
    suspend fun searchDeviantArt(
        query: String,
        page: Int = 1,
        count: Int = 24,
        includeNsfw: Boolean = false,
        useTags: Boolean = true,
        offsetOverride: Int? = null
    ): Result<DeviantArtBrowseResponse> = withContext(Dispatchers.IO) {
        try {
            val (clientId, clientSecret) = getDeviantArtCredentials()
            require(clientId.isNotBlank() && clientSecret.isNotBlank()) {
                "DeviantArt Client ID and Client Secret are required."
            }
            val token = deviantArtTokenMutex.withLock {
                val now = System.currentTimeMillis()
                if (deviantArtAccessToken.isNullOrBlank() ||
                    now >= deviantArtTokenExpiresAt ||
                    clientId != deviantArtTokenClientId ||
                    clientSecret != deviantArtTokenClientSecret
                ) {
                    val response = deviantArtApi.getAccessToken(
                        clientId = clientId,
                        clientSecret = clientSecret
                    )
                    deviantArtAccessToken = response.accessToken
                    deviantArtTokenExpiresAt = now + response.expiresIn.coerceAtLeast(60) * 1000L - 30_000L
                    deviantArtTokenClientId = clientId
                    deviantArtTokenClientSecret = clientSecret
                }
                deviantArtAccessToken ?: error("DeviantArt access token was empty.")
            }
            val normalizedQuery = query.trim().replace(Regex("\\s+"), " ")
            val offset = offsetOverride ?: ((page - 1).coerceAtLeast(0)) * count
            val response = if (useTags) {
                deviantArtApi.searchByTags(
                    authorization = "Bearer $token",
                    tags = normalizedQuery.replace(" ", "+"),
                    matureContent = includeNsfw,
                    offset = offset,
                    limit = count
                )
            } else {
                deviantArtApi.search(
                    authorization = "Bearer $token",
                    query = normalizedQuery,
                    matureContent = includeNsfw,
                    offset = offset,
                    limit = count
                )
            }
            val filtered = response.results.filter { it.isMature == includeNsfw }
            Result.success(response.copy(results = filtered))
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error searching DeviantArt for $query: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch random illustrated wallpapers from Nekosia (Anime themed) in parallel requests for instant speed and zero duplication logic
     */
    suspend fun getNekosiaRandom(
        category: String,
        count: Int = 15,
        includeNsfw: Boolean = false
    ): Result<List<NekosiaResponse>> = withContext(Dispatchers.IO) {
        try {
            // We issues single requests in parallel for safety and conforming to the API
            coroutineScope {
                val deferreds = (1..count).map {
                    async {
                        try {
                            nekosiaApi.getRandomCategoryImages(
                                category = category,
                                count = 1,
                                rating = if (includeNsfw) "nsfw" else "safe"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val responses = deferreds.awaitAll().filterNotNull().filter { it.success && it.image != null }
                if (responses.isNotEmpty()) {
                    Result.success(responses)
                } else {
                    Result.failure(Exception("Nekosia API did not return any images."))
                }
            }
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error fetching Nekosia Category $category: ${e.message}", e)
            Result.failure(e)
        }
    }
}
