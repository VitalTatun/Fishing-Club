package com.example.fishing

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.entity.MarkerEntity
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
import java.util.UUID

/**
 * AUD-02: a v12 database has markers without an author. Opening it with Room 13
 * must run [AppDatabase.MIGRATION_12_13], recreate the markers table with a
 * NOT NULL `userId`, keep the untouched report cache, and leave a table the DAO
 * can use. Legacy markers are dropped: they are a disposable cache and their
 * author is unknown.
 *
 * The legacy DDL below is the v12 part of the Room-generated schema, so schema
 * validation runs for real when the database is opened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class Migration_12_13_Test {

    private val context get() = RuntimeEnvironment.getApplication()

    private val viewerId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    @Before
    fun createLegacyV12Database() {
        context.deleteDatabase(DB_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(V12_MARKERS_DDL)
                    db.execSQL(V12_REPORT_DETAILS_DDL)
                    db.execSQL(V12_FAVORITES_DDL)
                    db.execSQL(V12_LIKES_DDL)
                    db.execSQL(V12_PENDING_REPORT_UPLOADS_DDL)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            // Legacy cached marker without an author: must not survive.
            helper.writableDatabase.execSQL(
                """
                INSERT INTO markers
                    (id, name, waterName, waterLat, waterLng, type, fishingMethod, fishingStartAt,
                     isPublic, isPaidWater, fishNames)
                VALUES
                    (X'00000000000000000000000000000002', 'legacy marker', 'water', 1.0, 2.0,
                     'FISHING_LOG', 'SPINNING', NULL, 0, 0, '[]')
                """
            )
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
    fun migratingFromV12ToV13_addsMarkerAuthor_andDropsLegacyCache() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_12_13)
            .allowMainThreadQueries()
            .build()

        db.openHelper.writableDatabase.use { sqlite ->
            check(sqlite.version == 13) { "expected user_version 13, was ${sqlite.version}" }

            sqlite.query("PRAGMA table_info(markers)").use { cursor ->
                val columns = buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getString(1), cursor.getInt(3)) // name -> notNull
                    }
                }
                assertTrue("markers must contain userId, found: ${columns.keys}", "userId" in columns)
                assertEquals("userId must be NOT NULL", 1, columns["userId"])
            }

            // Legacy cache rows cannot be attributed to a user and are dropped.
            sqlite.query("SELECT COUNT(*) FROM markers").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            // The report cache is untouched by the marker migration.
            sqlite.query(
                "SELECT name FROM report_details WHERE id = X'00000000000000000000000000000004'"
            ).use { cursor ->
                check(cursor.moveToFirst()) { "report row was lost" }
                assertEquals("kept report", cursor.getString(0))
            }
        }

        // The recreated table is usable through the DAO, not just present.
        runBlocking {
            val entity = MarkerEntity(
                id = UUID.fromString("00000000-0000-0000-0000-000000000020"),
                userId = viewerId,
                name = "new marker",
                waterName = "water",
                waterLat = 1.0,
                waterLng = 2.0,
                type = "FISHING_LOG",
                fishingMethod = "SPINNING",
                fishingStartAt = null,
                isPublic = false
            )
            db.markerDao().insertAll(listOf(entity))
            val visible = db.markerDao().getVisibleForUser(viewerId).first()
            assertEquals(1, visible.size)
            assertEquals(entity, visible.single())
        }

        db.close()
    }

    companion object {
        private const val DB_NAME = "migration_12_13_test.db"

        private const val V12_MARKERS_DDL =
            "CREATE TABLE `markers` (`id` BLOB NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT NOT NULL, " +
                "`waterLat` REAL NOT NULL, `waterLng` REAL NOT NULL, `type` TEXT NOT NULL, " +
                "`fishingMethod` TEXT NOT NULL, `fishingStartAt` TEXT, `isPublic` INTEGER NOT NULL, " +
                "`isPaidWater` INTEGER NOT NULL, `fishNames` TEXT NOT NULL, PRIMARY KEY(`id`))"

        private const val V12_REPORT_DETAILS_DDL =
            "CREATE TABLE `report_details` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `publishedAt` TEXT, " +
                "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT, `waterLat` REAL, `waterLng` REAL, " +
                "`waterPaid` INTEGER NOT NULL, `spotLat` REAL, `spotLng` REAL, `fishingStartAt` TEXT, " +
                "`fishingEndAt` TEXT, `weight` REAL NOT NULL, `fishingMethod` TEXT, `comment` TEXT, " +
                "`shore` INTEGER NOT NULL, `isPublic` INTEGER NOT NULL, `imageUrls` TEXT NOT NULL, " +
                "`fishJson` TEXT NOT NULL, `baitsJson` TEXT NOT NULL, `authorName` TEXT, " +
                "`authorAvatar` TEXT, `createdAt` TEXT, `likesCount` INTEGER NOT NULL, PRIMARY KEY(`id`))"

        private const val V12_FAVORITES_DDL =
            "CREATE TABLE `favorites` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"

        private const val V12_LIKES_DDL =
            "CREATE TABLE `likes` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"

        private const val V12_PENDING_REPORT_UPLOADS_DDL =
            "CREATE TABLE `pending_report_uploads` (`id` BLOB NOT NULL, `reportJson` TEXT NOT NULL, " +
                "`localPhotoPathsJson` TEXT NOT NULL, `temporaryPhotoPathsJson` TEXT NOT NULL, " +
                "`isPublic` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, " +
                "`state` TEXT NOT NULL, `uploadedPhotos` INTEGER NOT NULL, `totalPhotos` INTEGER NOT NULL, " +
                "`lastError` TEXT, PRIMARY KEY(`id`))"
    }
}
