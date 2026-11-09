package com.example.fishing.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.fishing.data.local.converter.Converters
import com.example.fishing.data.local.dao.FavoriteReportDao
import com.example.fishing.data.local.dao.LikeDao
import com.example.fishing.data.local.dao.MarkerDao
import com.example.fishing.data.local.dao.PendingReportUploadDao
import com.example.fishing.data.local.dao.ReportDetailsDao
import com.example.fishing.data.local.entity.FavoriteReportEntity
import com.example.fishing.data.local.entity.LikeEntity
import com.example.fishing.data.local.entity.MarkerEntity
import com.example.fishing.data.local.entity.PendingReportUploadEntity
import com.example.fishing.data.local.entity.ReportDetailsEntity

@Database(
    entities = [
        MarkerEntity::class,
        ReportDetailsEntity::class,
        FavoriteReportEntity::class,
        LikeEntity::class,
        PendingReportUploadEntity::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
    abstract fun reportDetailsDao(): ReportDetailsDao
    abstract fun favoriteDao(): FavoriteReportDao
    abstract fun likeDao(): LikeDao
    abstract fun pendingReportUploadDao(): PendingReportUploadDao

    companion object {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE report_details ADD COLUMN fishingStartAt TEXT")
                db.execSQL("ALTER TABLE report_details ADD COLUMN fishingEndAt TEXT")
                db.execSQL("ALTER TABLE markers ADD COLUMN fishingStartAt TEXT")

                db.execSQL("""
                    UPDATE report_details 
                    SET fishingStartAt = fishingTime 
                    WHERE fishingStartAt IS NULL
                """)
                db.execSQL("""
                    UPDATE markers 
                    SET fishingStartAt = fishingTime 
                    WHERE fishingStartAt IS NULL
                """)

                db.execSQL("ALTER TABLE report_details DROP COLUMN fishingTime")
                db.execSQL("ALTER TABLE markers DROP COLUMN fishingTime")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `likes` " +
                        "(`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                        "PRIMARY KEY(`userId`, `reportId`))"
                )
                db.execSQL("ALTER TABLE report_details ADD COLUMN likesCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_report_uploads` (
                        `id` BLOB NOT NULL,
                        `reportJson` TEXT NOT NULL,
                        `localPhotoPathsJson` TEXT NOT NULL,
                        `temporaryPhotoPathsJson` TEXT NOT NULL,
                        `isPublic` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `state` TEXT NOT NULL,
                        `uploadedPhotos` INTEGER NOT NULL,
                        `totalPhotos` INTEGER NOT NULL,
                        `lastError` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Adds the marker author so the shared cache can filter by visibility.
         * Legacy rows have no owner recorded, and the correct author cannot be
         * recovered: guessing one would either hide public markers or leak
         * private ones. Markers are a disposable projection of the reports
         * cache, so the table is recreated empty and refilled from the server.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `markers`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `markers` (
                        `id` BLOB NOT NULL,
                        `userId` BLOB NOT NULL,
                        `name` TEXT NOT NULL,
                        `waterName` TEXT NOT NULL,
                        `waterLat` REAL NOT NULL,
                        `waterLng` REAL NOT NULL,
                        `type` TEXT NOT NULL,
                        `fishingMethod` TEXT NOT NULL,
                        `fishingStartAt` TEXT,
                        `isPublic` INTEGER NOT NULL,
                        `isPaidWater` INTEGER NOT NULL,
                        `fishNames` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
