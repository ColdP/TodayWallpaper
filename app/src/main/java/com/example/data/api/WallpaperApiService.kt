package btm.m.todaywallpaper.data.api

import btm.m.todaywallpaper.data.model.NekosiaResponse
import btm.m.todaywallpaper.data.model.PexelsResponse
import btm.m.todaywallpaper.data.model.PixabayResponse
import btm.m.todaywallpaper.data.model.WallhavenResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import btm.m.todaywallpaper.data.model.DeviantArtAccessTokenResponse
import btm.m.todaywallpaper.data.model.DeviantArtBrowseResponse

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

// Base URL for Pixabay API is: https://pixabay.com/
interface PixabayApiService {

    @GET("api/")
    suspend fun searchImages(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("image_type") imageType: String = "photo",
        @Query("safesearch") safeSearch: Boolean = true,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): PixabayResponse

    companion object {
        const val BASE_URL = "https://pixabay.com/"
    }
}

// Base URL for Wallhaven API is: https://wallhaven.cc/
interface WallhavenApiService {

    @GET("api/v1/search")
    suspend fun searchWallpapers(
        @Query("q") query: String? = null,
        @Query("categories") categories: String = "111",
        @Query("purity") purity: String = "100",
        @Query("sorting") sorting: String = "date_added",
        @Query("order") order: String = "desc",
        @Query("page") page: Int = 1,
        @Query("apikey") apiKey: String? = null
    ): WallhavenResponse

    companion object {
        const val BASE_URL = "https://wallhaven.cc/"
    }
}

// Base URL for DeviantArt API is: https://www.deviantart.com/
interface DeviantArtApiService {

    @FormUrlEncoded
    @POST("oauth2/token")
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): DeviantArtAccessTokenResponse

    @GET("api/v1/oauth2/browse/tags")
    suspend fun searchByTags(
        @Header("Authorization") authorization: String,
        @Query(value = "tag", encoded = true) tags: String,
        @Query("mature_content") matureContent: Boolean,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): DeviantArtBrowseResponse

    @GET("api/v1/oauth2/browse/search")
    suspend fun search(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("mature_content") matureContent: Boolean,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): DeviantArtBrowseResponse

    companion object {
        const val BASE_URL = "https://www.deviantart.com/"
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
