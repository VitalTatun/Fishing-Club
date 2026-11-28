package com.example.fishing

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.entity.FishSpeciesEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * ACH-01: opening a v13 database with Room 14 must add the `fish_species`
 * cache table and leave the table usable through the DAO.
 *
 * The v13 DDL below is the Room-generated schema at that version, so schema
 * validation runs for real when the database is opened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class Migration_13_14_Test {

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun createLegacyV13Database() {
        context.deleteDatabase(DB_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(V13_MARKERS_DDL)
                    db.execSQL(V13_REPORT_DETAILS_DDL)
                    db.execSQL(V13_FAVORITES_DDL)
                    db.execSQL(V13_LIKES_DDL)
                    db.execSQL(V13_PENDING_REPORT_UPLOADS_DDL)
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
        }
    }

    @Test
    fun migratingFromV13ToV14_addsFishSpeciesCache_andKeepsReports() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            // The database has moved past 14; this test still verifies the
            // 13 -> 14 step and rides through the following migration.
            .addMigrations(AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15)
            .allowMainThreadQueries()
            .build()

        db.openHelper.writableDatabase.use { sqlite ->
            check(sqlite.version == 15) { "expected user_version 15, was ${sqlite.version}" }

            sqlite.query("PRAGMA table_info(fish_species)").use { cursor ->
                val columns = buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getString(1), cursor.getInt(3)) // name -> notNull
                    }
                }
                assertEquals(
                    setOf("id", "canonicalName", "aliases", "isPopular", "sortOrder"),
                    columns.keys
                )
                assertTrue(columns.values.all { it == 1 })
            }

            sqlite.query("SELECT name FROM report_details").use { cursor ->
                check(cursor.moveToFirst()) { "report row was lost" }
                assertEquals("kept report", cursor.getString(0))
            }
        }

        runBlocking {
            val entity = FishSpeciesEntity(
                id = "esox_lucius",
                canonicalName = "Щука",
                aliases = listOf("щука обыкновенная", "щуки"),
                isPopular = true,
                sortOrder = 3
            )
            db.fishSpeciesDao().upsertAll(listOf(entity))
            assertEquals(listOf(entity), db.fishSpeciesDao().observeAll().first())
        }

        db.close()
    }

    companion object {
        private const val DB_NAME = "migration_13_14_test.db"

        private const val V13_MARKERS_DDL =
            "CREATE TABLE `markers` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `name` TEXT NOT NULL, " +
                "`waterName` TEXT NOT NULL, `waterLat` REAL NOT NULL, `waterLng` REAL NOT NULL, " +
                "`type` TEXT NOT NULL, `fishingMethod` TEXT NOT NULL, `fishingStartAt` TEXT, " +
                "`isPublic` INTEGER NOT NULL, `isPaidWater` INTEGER NOT NULL, `fishNames` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))"

        private const val V13_REPORT_DETAILS_DDL =
            "CREATE TABLE `report_details` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `publishedAt` TEXT, " +
                "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT, `waterLat` REAL, `waterLng` REAL, " +
                "`waterPaid` INTEGER NOT NULL, `spotLat` REAL, `spotLng` REAL, `fishingStartAt` TEXT, " +
                "`fishingEndAt` TEXT, `weight` REAL NOT NULL, `fishingMethod` TEXT, `comment` TEXT, " +
                "`shore` INTEGER NOT NULL, `isPublic` INTEGER NOT NULL, `imageUrls` TEXT NOT NULL, " +
                "`fishJson` TEXT NOT NULL, `baitsJson` TEXT NOT NULL, `authorName` TEXT, " +
                "`authorAvatar` TEXT, `createdAt` TEXT, `likesCount` INTEGER NOT NULL, PRIMARY KEY(`id`))"

        private const val V13_FAVORITES_DDL =
            "CREATE TABLE `favorites` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"

        private const val V13_LIKES_DDL =
            "CREATE TABLE `likes` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"

        private const val V13_PENDING_REPORT_UPLOADS_DDL =
            "CREATE TABLE `pending_report_uploads` (`id` BLOB NOT NULL, `reportJson` TEXT NOT NULL, " +
                "`localPhotoPathsJson` TEXT NOT NULL, `temporaryPhotoPathsJson` TEXT NOT NULL, " +
                "`isPublic` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, " +
                "`state` TEXT NOT NULL, `uploadedPhotos` INTEGER NOT NULL, `totalPhotos` INTEGER NOT NULL, " +
                "`lastError` TEXT, PRIMARY KEY(`id`))"
    }
}
