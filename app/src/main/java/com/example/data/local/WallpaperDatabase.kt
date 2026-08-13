package btm.m.todaywallpaper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import btm.m.todaywallpaper.data.model.AlbumCategory
import btm.m.todaywallpaper.data.model.CollectionItem
import btm.m.todaywallpaper.data.model.FavoriteWallpaper
import btm.m.todaywallpaper.data.model.HistoryWallpaper
import btm.m.todaywallpaper.data.model.WallpaperCollection

@Database(
    entities = [FavoriteWallpaper::class, HistoryWallpaper::class, WallpaperCollection::class, CollectionItem::class, AlbumCategory::class],
    version = 3,
    exportSchema = false
)
abstract class WallpaperDatabase : RoomDatabase() {

    abstract val wallpaperDao: WallpaperDao

    companion object {
        @Volatile
        private var INSTANCE: WallpaperDatabase? = null

        fun getDatabase(context: Context): WallpaperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WallpaperDatabase::class.java,
                    "app_gallery_wallpaper_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `history_wallpapers` (
                        `id` TEXT NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `thumbnailUrl` TEXT NOT NULL,
                        `authorName` TEXT,
                        `authorUrl` TEXT,
                        `source` TEXT NOT NULL,
                        `category` TEXT,
                        `viewedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `album_categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )""".trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_album_categories_name` ON `album_categories` (`name`)"
                )
                database.execSQL("ALTER TABLE `wallpaper_collections` ADD COLUMN `categoryId` INTEGER")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_wallpaper_collections_categoryId` ON `wallpaper_collections` (`categoryId`)"
                )

                val defaults = listOf("默认")
                defaults.forEachIndexed { index, name ->
                    database.execSQL(
                        "INSERT OR IGNORE INTO `album_categories` (`name`, `createdAt`) VALUES (?, ?)",
                        arrayOf(name, index.toLong())
                    )
                }
            }
        }
    }
}
