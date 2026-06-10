package btm.m.todaywallpaper.data.repository

import android.content.Context
import android.util.Log
import btm.m.todaywallpaper.data.api.NekosiaApiService
import btm.m.todaywallpaper.data.api.PexelsApiService
import btm.m.todaywallpaper.data.local.WallpaperDao
import btm.m.todaywallpaper.data.local.WallpaperDatabase
import btm.m.todaywallpaper.data.model.CollectionItem
import btm.m.todaywallpaper.data.model.FavoriteWallpaper
import btm.m.todaywallpaper.data.model.NekosiaResponse
import btm.m.todaywallpaper.data.model.PexelsResponse
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
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class WallpaperRepository(private val context: Context) {

    private val db = WallpaperDatabase.getDatabase(context)
    private val dao: WallpaperDao = db.wallpaperDao

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
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

    // ==========================================
    // 1. DATABASE OPS (FLOWS)
    // ==========================================

    val allFavorites: Flow<List<FavoriteWallpaper>> = dao.getAllFavorites()
    val allCollections: Flow<List<WallpaperCollection>> = dao.getAllCollections()

    fun getItemsForCollection(collectionId: Int): Flow<List<CollectionItem>> =
        dao.getItemsForCollection(collectionId)

    fun isFavorite(id: String): Flow<Boolean> = dao.isWallpaperFavorite(id)

    suspend fun insertFavorite(favorite: FavoriteWallpaper) = withContext(Dispatchers.IO) {
        dao.insertFavorite(favorite)
    }

    suspend fun deleteFavorite(id: String) = withContext(Dispatchers.IO) {
        dao.deleteFavoriteById(id)
    }

    suspend fun createCollection(name: String, description: String?): Int = withContext(Dispatchers.IO) {
        val collection = WallpaperCollection(name = name, description = description, coverUrl = null)
        dao.insertCollection(collection).toInt()
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

    private fun getApiKey(): String {
        val sp = context.getSharedPreferences("app_gallery_prefs", Context.MODE_PRIVATE)
        return sp.getString("pexels_api_key", "") ?: ""
    }

    /**
     * Fetch curated wallpapers from Pexels (highly realistic HD landscape & portrait scenery)
     */
    suspend fun getPexelsCurated(page: Int = 1, count: Int = 20): Result<PexelsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = pexelsApi.getCuratedWallpapers(apiKey = getApiKey(), perPage = count, page = page)
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
            val response = pexelsApi.searchWallpapers(apiKey = getApiKey(), query = query, perPage = count, page = page)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("WallpaperRepository", "Error searching Pexels for $query: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch random illustrated wallpapers from Nekosia (Anime themed) in parallel requests for instant speed and zero duplication logic
     */
    suspend fun getNekosiaRandom(category: String, count: Int = 15): Result<List<NekosiaResponse>> = withContext(Dispatchers.IO) {
        try {
            // We issues single requests in parallel for safety and conforming to the API
            coroutineScope {
                val deferreds = (1..count).map {
                    async {
                        try {
                            nekosiaApi.getRandomCategoryImages(
                                category = category,
                                count = 1,
                                rating = "safe"
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
