package com.example.fishing

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.fishing.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * MAP-01: opening a v14 database with Room 15 must drop the disposable marker
 * cache, keep the user data tables and leave `report_details` readable.
 *
 * The v14 DDL below mirrors the Room-generated schema at that version, so
 * schema validation runs for real when the database is opened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class Migration_14_15_Test {

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun createLegacyV14Database() {
        context.deleteDatabase(DB_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(14) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(V14_MARKERS_DDL)
                    db.execSQL(V14_REPORT_DETAILS_DDL)
                    db.execSQL(V14_FAVORITES_DDL)
                    db.execSQL(V14_LIKES_DDL)
                    db.execSQL(V14_PENDING_REPORT_UPLOADS_DDL)
                    db.execSQL(V14_FISH_SPECIES_DDL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO report_details
                    (id, userId, publishedAt, type, name, waterName, waterLat, waterLng, waterPaid, spotLat, spotLng,
                     fishingStartAt, fishingEndAt, weight, fishingMethod, comment, shore, isPublic, imageUrls, fishJson,
                     baitsJson, authorName, authorAvatar, createdAt, likesCount)
                VALUES
                    (X'00000000000000000000000000000004', X'00000000000000000000000000000003',
                     '2026-07-15T06:00:00Z', 'type', 'kept report', 'water', 1.0, 2.0, 0, 3.0, 4.0,
                     '2026-07-15T06:26:53.197Z', NULL, 12.5, 'method', 'comment', 0, 0, '[]', '[]', '[]',
                     'author', 'avatar', '2026-07-15T06:00:00Z', 0)
                """
            )
            helper.writableDatabase.execSQL(
                """
                INSERT INTO markers
                    (id, userId, name, waterName, waterLat, waterLng, type, fishingMethod,
                     fishingStartAt, isPublic, isPaidWater, fishNames)
                VALUES
                    (X'00000000000000000000000000000005', X'00000000000000000000000000000003',
                     'old marker', 'water', 1.0, 2.0, 'FISHING_LOG', 'SPINNING', NULL, 1, 0, '[]')
                """
            )
        }
    }

    @Test
    fun migratingFromV14ToV15_dropsMarkers_andKeepsUserData() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_14_15)
            .allowMainThreadQueries()
            .build()

        db.openHelper.writableDatabase.use { sqlite ->
            check(sqlite.version == 15) { "expected user_version 15, was ${sqlite.version}" }
            assertFalse(tableExists(sqlite, "markers"))
            assertEquals(
                listOf("kept report"),
                sqlite.query("SELECT name FROM report_details").use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add(cursor.getString(0))
                    }
                }
            )
            assertTrue(tableExists(sqlite, "favorites"))
            assertTrue(tableExists(sqlite, "likes"))
            assertTrue(tableExists(sqlite, "pending_report_uploads"))
            assertTrue(tableExists(sqlite, "fish_species"))
        }

        runBlocking {
            assertEquals("kept report", db.reportDetailsDao().getAll().first().single().name)
        }

        db.close()
    }

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(name)
        ).use { cursor -> cursor.moveToFirst() }

    companion object {
        private const val DB_NAME = "migration_14_15_test.db"

        private const val V14_MARKERS_DDL =
            "CREATE TABLE `markers` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `name` TEXT NOT NULL, " +
                "`waterName` TEXT NOT NULL, `waterLat` REAL NOT NULL, `waterLng` REAL NOT NULL, " +
                "`type` TEXT NOT NULL, `fishingMethod` TEXT NOT NULL, `fishingStartAt` TEXT, " +
                "`isPublic` INTEGER NOT NULL, `isPaidWater` INTEGER NOT NULL, `fishNames` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))"

        private const val V14_REPORT_DETAILS_DDL =
            "CREATE TABLE `report_details` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `publishedAt` TEXT, " +
                "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT, `waterLat` REAL, `waterLng` REAL, " +
                "`waterPaid` INTEGER NOT NULL, `spotLat` REAL, `spotLng` REAL, `fishingStartAt` TEXT, " +
                "`fishingEndAt` TEXT, `weight` REAL NOT NULL, `fishingMethod` TEXT, `comment` TEXT, " +
                "`shore` INTEGER NOT NULL, `isPublic` INTEGER NOT NULL, `imageUrls` TEXT NOT NULL, " +
                "`fishJson` TEXT NOT NULL, `baitsJson` TEXT NOT NULL, `authorName` TEXT, " +
                "`authorAvatar` TEXT, `createdAt` TEXT, `likesCount` INTEGER NOT NULL, PRIMARY KEY(`id`))"

        private const val V14_FAVORITES_DDL =
            "CREATE TABLE `favorites` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"

        private const val V14_LIKES_DDL =
            "CREATE TABLE `likes` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"

        private const val V14_PENDING_REPORT_UPLOADS_DDL =
            "CREATE TABLE `pending_report_uploads` (`id` BLOB NOT NULL, `reportJson` TEXT NOT NULL, " +
                "`localPhotoPathsJson` TEXT NOT NULL, `temporaryPhotoPathsJson` TEXT NOT NULL, " +
                "`isPublic` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, " +
                "`state` TEXT NOT NULL, `uploadedPhotos` INTEGER NOT NULL, `totalPhotos` INTEGER NOT NULL, " +
                "`lastError` TEXT, PRIMARY KEY(`id`))"

        private const val V14_FISH_SPECIES_DDL =
            "CREATE TABLE `fish_species` (`id` TEXT NOT NULL, `canonicalName` TEXT NOT NULL, " +
                "`aliases` TEXT NOT NULL, `isPopular` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
    }
}
