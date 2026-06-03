package btm.m.todaywallpaper.data.api

import btm.m.todaywallpaper.data.model.NekosiaResponse
import btm.m.todaywallpaper.data.model.PexelsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// Base URL for Pexels API is: https://api.pexels.com/
interface PexelsApiService {
    
    @GET("v1/curated")
    suspend fun getCuratedWallpapers(
        @Header("Authorization") apiKey: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): PexelsResponse

    @GET("v1/search")
    suspend fun searchWallpapers(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): PexelsResponse

    companion object {
        const val BASE_URL = "https://api.pexels.com/"
    }
}

// Base URL for Nekosia API is: https://api.nekosia.cat/
interface NekosiaApiService {

    @GET("api/v1/images/{category}")
    suspend fun getRandomCategoryImages(
        @Path("category") category: String,
        @Query("count") count: Int = 1,
        @Query("rating") rating: String = "safe",
        @Query("additionalTags") additionalTags: String? = null,
        @Query("blacklistedTags") blacklistedTags: String? = null,
        @Query("session") session: String? = null,
        @Query("id") sessionId: String? = null
    ): NekosiaResponse

    companion object {
        const val BASE_URL = "https://api.nekosia.cat/"
    }
}
