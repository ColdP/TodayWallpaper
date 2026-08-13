package btm.m.todaywallpaper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// 1. ROOM DATABASE ENTITIES
// ==========================================

@Entity(tableName = "favorite_wallpapers")
data class FavoriteWallpaper(
    @PrimaryKey val id: String, // Unique identifier generated based on source and individual ID
    val imageUrl: String,
    val thumbnailUrl: String,
    val authorName: String?,
    val authorUrl: String?,
    val source: String, // "Pexels", "Pixabay", "Wallhaven", or "Nekosia"
    val category: String?,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history_wallpapers")
data class HistoryWallpaper(
    @PrimaryKey val id: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val authorName: String?,
    val authorUrl: String?,
    val source: String,
    val category: String?,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "album_categories", indices = [Index(value = ["name"], unique = true)])
data class AlbumCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallpaper_collections", indices = [Index(value = ["categoryId"])])
data class WallpaperCollection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String?,
    val coverUrl: String?, // Image URL of the latest wallpaper in this collection or placeholder
    val categoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "collection_items")
data class CollectionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val collectionId: Int, // Refers to WallpaperCollection.id
    val wallpaperId: String, // Unique wallpaper id
    val imageUrl: String,
    val thumbnailUrl: String,
    val authorName: String?,
    val source: String, // "Pexels", "Pixabay", or "Nekosia"
    val addedAt: Long = System.currentTimeMillis()
)


// ==========================================
// 2. PEXELS API MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class PexelsResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "per_page") val perPage: Int,
    @Json(name = "photos") val photos: List<PexelsPhoto>,
    @Json(name = "total_results") val totalResults: Int,
    @Json(name = "next_page") val nextPage: String?
)

@JsonClass(generateAdapter = true)
data class PexelsPhoto(
    @Json(name = "id") val id: Long,
    @Json(name = "width") val width: Int,
    @Json(name = "height") val height: Int,
    @Json(name = "url") val url: String,
    @Json(name = "photographer") val photographer: String,
    @Json(name = "photographer_url") val photographerUrl: String,
    @Json(name = "photographer_id") val photographerId: Long,
    @Json(name = "avg_color") val avgColor: String?,
    @Json(name = "src") val src: PexelsPhotoSrc,
    @Json(name = "alt") val alt: String?
)

@JsonClass(generateAdapter = true)
data class PexelsPhotoSrc(
    @Json(name = "original") val original: String,
    @Json(name = "large2x") val large2x: String,
    @Json(name = "large") val large: String,
    @Json(name = "medium") val medium: String,
    @Json(name = "small") val small: String,
    @Json(name = "portrait") val portrait: String,
    @Json(name = "landscape") val landscape: String,
    @Json(name = "tiny") val tiny: String
)


// ==========================================
// 3. PIXABAY API MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class PixabayResponse(
    @Json(name = "total") val total: Int,
    @Json(name = "totalHits") val totalHits: Int,
    @Json(name = "hits") val hits: List<PixabayImage>
)

@JsonClass(generateAdapter = true)
data class PixabayImage(
    @Json(name = "id") val id: Long,
    @Json(name = "pageURL") val pageUrl: String,
    @Json(name = "tags") val tags: String?,
    @Json(name = "previewURL") val previewUrl: String,
    @Json(name = "webformatURL") val webformatUrl: String,
    @Json(name = "largeImageURL") val largeImageUrl: String,
    @Json(name = "fullHDURL") val fullHdUrl: String?,
    @Json(name = "imageURL") val imageUrl: String?,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "user") val user: String
)


// ==========================================
// 4. NEKOSIA API MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class WallhavenResponse(
    @Json(name = "data") val data: List<WallhavenWallpaper>,
    @Json(name = "meta") val meta: WallhavenMeta?
)

@JsonClass(generateAdapter = true)
data class WallhavenWallpaper(
    @Json(name = "id") val id: String,
    @Json(name = "url") val url: String?,
    @Json(name = "purity") val purity: String,
    @Json(name = "category") val category: String?,
    @Json(name = "path") val path: String,
    @Json(name = "thumbs") val thumbs: WallhavenThumbs
)

@JsonClass(generateAdapter = true)
data class WallhavenThumbs(
    @Json(name = "large") val large: String,
    @Json(name = "original") val original: String?,
    @Json(name = "small") val small: String?
)

@JsonClass(generateAdapter = true)
data class WallhavenMeta(
    @Json(name = "current_page") val currentPage: Int?,
    @Json(name = "last_page") val lastPage: Int?,
    @Json(name = "per_page") val perPage: Int?,
    @Json(name = "total") val total: Int?
)

// ==========================================
// 5. DEVIANTART API MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class DeviantArtAccessTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Long = 3600
)

@JsonClass(generateAdapter = true)
data class DeviantArtBrowseResponse(
    @Json(name = "results") val results: List<DeviantArtDeviation> = emptyList(),
    @Json(name = "has_more") val hasMore: Boolean = false,
    @Json(name = "next_offset") val nextOffset: Int? = null
)

@JsonClass(generateAdapter = true)
data class DeviantArtDeviation(
    @Json(name = "deviationid") val deviationId: String,
    @Json(name = "title") val title: String = "",
    @Json(name = "is_mature") val isMature: Boolean = false,
    @Json(name = "author") val author: DeviantArtAuthor? = null,
    @Json(name = "preview") val preview: DeviantArtImage? = null,
    @Json(name = "content") val content: DeviantArtImage? = null,
    @Json(name = "thumbs") val thumbs: List<DeviantArtImage> = emptyList(),
    @Json(name = "tags") val tags: List<DeviantArtTag> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeviantArtAuthor(
    @Json(name = "username") val username: String = "",
    @Json(name = "usericon") val userIcon: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviantArtImage(
    @Json(name = "src") val src: String = "",
    @Json(name = "width") val width: Int = 0,
    @Json(name = "height") val height: Int = 0
)

@JsonClass(generateAdapter = true)
data class DeviantArtTag(
    @Json(name = "name") val tagName: String = ""
)

@JsonClass(generateAdapter = true)
data class NekosiaResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "status") val status: Int,
    @Json(name = "count") val count: Int?,
    @Json(name = "id") val id: String?,
    @Json(name = "colors") val colors: NekosiaColors?,
    @Json(name = "image") val image: NekosiaImage?,
    @Json(name = "category") val category: String?,
    @Json(name = "tags") val tags: List<String>?,
    @Json(name = "rating") val rating: String?,
    @Json(name = "anime") val anime: NekosiaAnime?,
    @Json(name = "source") val source: NekosiaSource?,
    @Json(name = "attribution") val attribution: NekosiaAttribution?
)

@JsonClass(generateAdapter = true)
data class NekosiaColors(
    @Json(name = "main") val main: String?,
    @Json(name = "palette") val palette: List<String>?
)

@JsonClass(generateAdapter = true)
data class NekosiaImage(
    @Json(name = "original") val original: NekosiaUrlDetails?,
    @Json(name = "compressed") val compressed: NekosiaUrlDetails?
)

@JsonClass(generateAdapter = true)
data class NekosiaUrlDetails(
    @Json(name = "url") val url: String,
    @Json(name = "extension") val extension: String?
)

@JsonClass(generateAdapter = true)
data class NekosiaAnime(
    @Json(name = "title") val title: String?,
    @Json(name = "character") val character: String?
)

@JsonClass(generateAdapter = true)
data class NekosiaSource(
    @Json(name = "url") val url: String?,
    @Json(name = "direct") val direct: String?
)

@JsonClass(generateAdapter = true)
data class NekosiaAttribution(
    @Json(name = "artist") val artist: NekosiaArtist?,
    @Json(name = "copyright") val copyright: String?
)

@JsonClass(generateAdapter = true)
data class NekosiaArtist(
    @Json(name = "username") val username: String?,
    @Json(name = "profile") val profile: String?
)
