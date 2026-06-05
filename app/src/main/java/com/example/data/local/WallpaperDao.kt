package btm.m.todaywallpaper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import btm.m.todaywallpaper.data.model.CollectionItem
import btm.m.todaywallpaper.data.model.FavoriteWallpaper
import btm.m.todaywallpaper.data.model.WallpaperCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    // ==========================================
    // 1. FAVORITE WALLPAPERS
    // ==========================================

    @Query("SELECT * FROM favorite_wallpapers ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteWallpaper>>

    @Query("SELECT * FROM favorite_wallpapers ORDER BY savedAt DESC")
    suspend fun getAllFavoritesSync(): List<FavoriteWallpaper>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_wallpapers WHERE id = :id)")
    fun isWallpaperFavorite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteWallpaper)

    @Query("DELETE FROM favorite_wallpapers WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)


    // ==========================================
    // 2. WALLPAPER COLLECTIONS
    // ==========================================

    @Query("SELECT * FROM wallpaper_collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<WallpaperCollection>>

    @Query("SELECT * FROM wallpaper_collections WHERE id = :collectionId")
    suspend fun getCollectionById(collectionId: Int): WallpaperCollection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: WallpaperCollection): Long

    @Query("UPDATE wallpaper_collections SET coverUrl = :coverUrl WHERE id = :collectionId")
    suspend fun updateCollectionCover(collectionId: Int, coverUrl: String)

    @Query("DELETE FROM wallpaper_collections WHERE id = :collectionId")
    suspend fun deleteCollectionById(collectionId: Int)


    // ==========================================
    // 3. COLLECTION ITEMS
    // ==========================================

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    fun getItemsForCollection(collectionId: Int): Flow<List<CollectionItem>>

    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    suspend fun getItemsForCollectionSync(collectionId: Int): List<CollectionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionItem(item: CollectionItem)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND wallpaperId = :wallpaperId")
    suspend fun deleteCollectionItem(collectionId: Int, wallpaperId: String)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId")
    suspend fun deleteCollectionItemsByCollectionId(collectionId: Int)

    @Query("SELECT thumbnailUrl FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC LIMIT 1")
    suspend fun getLatestItemThumbnail(collectionId: Int): String?

    @Query("UPDATE collection_items SET authorName = :authorName, source = :source WHERE id = :itemId")
    suspend fun updateCollectionItemMeta(itemId: Int, authorName: String?, source: String)
}
