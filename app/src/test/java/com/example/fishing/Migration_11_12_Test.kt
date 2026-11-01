package com.example.fishing

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.fishing.data.local.AppDatabase
import com.example.fishing.data.local.entity.PendingReportUploadEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Stage 3 upload queue: a v11 database has no `pending_report_uploads` table.
 * Opening it with Room 12 must run [AppDatabase.MIGRATION_11_12], keep the
 * existing reports and create a table the DAO can actually use.
 *
 * The legacy schema below is the v11 part of the Room-generated DDL (see
 * `AppDatabase_Impl.createAllTables`), so schema validation runs for real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class Migration_11_12_Test {

    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun createLegacyV11Database() {
        context.deleteDatabase(DB_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(V11_MARKERS_DDL)
                    db.execSQL(V11_REPORT_DETAILS_DDL)
                    db.execSQL(V11_FAVORITES_DDL)
                    db.execSQL(V11_LIKES_DDL)
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
                    (X'00000000000000000000000000000002', X'00000000000000000000000000000003', '2026-07-15T06:00:00Z',
                     'type', 'marker', 'water', 1.0, 2.0, 0, 3.0, 4.0,
                     '2026-07-15T06:26:53.197Z', NULL, 12.5, 'method', 'comment', 0, 0, '[]', '[]', '[]',
                     'author', 'avatar', '2026-07-15T06:00:00Z', 0)
                """
            )
        }
    }

    @Test
    fun migratingFromV11ToV12_createsPendingUploads_preservesReports() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()

        db.openHelper.writableDatabase.use { sqlite ->
            check(sqlite.version == 12) { "expected user_version 12, was ${sqlite.version}" }

            sqlite.query("PRAGMA table_info(pending_report_uploads)").use { cursor ->
                val columns = buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getString(1), cursor.getInt(5)) // name -> pk order
                    }
                }
                assertEquals(
                    setOf(
                        "id",
                        "reportJson",
                        "localPhotoPathsJson",
                        "temporaryPhotoPathsJson",
                        "isPublic",
                        "createdAt",
                        "attempts",
                        "state",
                        "uploadedPhotos",
                        "totalPhotos",
                        "lastError"
                    ),
                    columns.keys
                )
                assertEquals(1, columns["id"])
            }

            // Existing reports survive the migration.
            sqlite.query(
                "SELECT name, weight, likesCount FROM report_details WHERE id = X'00000000000000000000000000000002'"
            ).use { cursor ->
                check(cursor.moveToFirst()) { "report row was lost" }
                assertEquals("marker", cursor.getString(0))
                assertEquals(12.5, cursor.getDouble(1), 0.0)
                assertEquals(0, cursor.getInt(2))
            }
        }

        // The new table is usable through the DAO, not just present in the schema.
        val entity = PendingReportUploadEntity(
            id = UUID.fromString("00000000-0000-0000-0000-000000000010"),
            reportJson = "{}",
            localPhotoPathsJson = "[]",
            temporaryPhotoPathsJson = "[]",
            isPublic = true,
            createdAt = 1_763_000_000_000,
            attempts = 0,
            state = "QUEUED",
            uploadedPhotos = 0,
            totalPhotos = 0,
            lastError = null
        )
        runBlocking {
            db.pendingReportUploadDao().upsert(entity)
            val loaded = db.pendingReportUploadDao().getById(entity.id)
            assertNotNull(loaded)
            assertEquals(entity, loaded)
            db.pendingReportUploadDao().deleteById(entity.id)
        }

        db.close()
    }

    companion object {
        private const val DB_NAME = "migration_11_12_test.db"

        private const val V11_MARKERS_DDL =
            "CREATE TABLE `markers` (`id` BLOB NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT NOT NULL, " +
                "`waterLat` REAL NOT NULL, `waterLng` REAL NOT NULL, `type` TEXT NOT NULL, " +
                "`fishingMethod` TEXT NOT NULL, `fishingStartAt` TEXT, `isPublic` INTEGER NOT NULL, " +
                "`isPaidWater` INTEGER NOT NULL, `fishNames` TEXT NOT NULL, PRIMARY KEY(`id`))"

        private const val V11_REPORT_DETAILS_DDL =
            "CREATE TABLE `report_details` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `publishedAt` TEXT, " +
                "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT, `waterLat` REAL, `waterLng` REAL, " +
                "`waterPaid` INTEGER NOT NULL, `spotLat` REAL, `spotLng` REAL, `fishingStartAt` TEXT, " +
                "`fishingEndAt` TEXT, `weight` REAL NOT NULL, `fishingMethod` TEXT, `comment` TEXT, " +
                "`shore` INTEGER NOT NULL, `isPublic` INTEGER NOT NULL, `imageUrls` TEXT NOT NULL, " +
                "`fishJson` TEXT NOT NULL, `baitsJson` TEXT NOT NULL, `authorName` TEXT, " +
                "`authorAvatar` TEXT, `createdAt` TEXT, `likesCount` INTEGER NOT NULL, PRIMARY KEY(`id`))"

        private const val V11_FAVORITES_DDL =
            "CREATE TABLE `favorites` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"

        private const val V11_LIKES_DDL =
            "CREATE TABLE `likes` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, " +
                "PRIMARY KEY(`userId`, `reportId`))"
    }
}
