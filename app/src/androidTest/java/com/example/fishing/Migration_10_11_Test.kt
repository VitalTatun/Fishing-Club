package com.example.fishing

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fishing.data.local.AppDatabase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P0 Likes: an on-device DB at version 10 (without `likes` / `likesCount`)
 * must migrate to version 11, preserving existing rows and defaulting the
 * counter mirror to 0.
 */
@RunWith(AndroidJUnit4::class)
class Migration_10_11_Test {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun createLegacyV10Database() {
        context.deleteDatabase(DB_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(V10_MARKERS_DDL)
                    db.execSQL(V10_REPORT_DETAILS_DDL)
                    db.execSQL(V10_FAVORITES_DDL)
                    db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                    db.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)", arrayOf("legacy-v10"))
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO report_details
                    (id, userId, publishedAt, type, name, waterName, waterLat, waterLng, waterPaid, spotLat, spotLng,
                     fishingStartAt, fishingEndAt, weight, fishingMethod, comment, shore, isPublic, imageUrls, fishJson, baitsJson,
                     authorName, authorAvatar, createdAt)
                VALUES
                    (X'00000000000000000000000000000002', X'00000000000000000000000000000003', '2026-07-15T06:00:00Z',
                     'type', 'marker', 'water', 1.0, 2.0, 0, 3.0, 4.0,
                     '2026-07-15T06:26:53.197Z', NULL, 12.5, 'method', 'comment', 0, 0, '[]', '[]', '[]',
                     'author', 'avatar', '2026-07-15T06:00:00Z')
                """
            )
            helper.writableDatabase.execSQL(
                """
                INSERT INTO favorites (userId, reportId)
                VALUES (X'00000000000000000000000000000003', X'00000000000000000000000000000002')
                """
            )
        }
    }

    @Test
    fun migratingFromV10ToV11_createsLikes_addsLikesCount_preservesData() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_10_11)
            .build()

        db.openHelper.writableDatabase.use { sqlite ->
            check(sqlite.version == 11) { "expected user_version 11, was ${sqlite.version}" }

            sqlite.query("PRAGMA table_info(likes)").use { cursor ->
                val columns = buildMap {
                    while (cursor.moveToNext()) {
                        put(cursor.getString(1), cursor.getInt(5)) // name -> pk order
                    }
                }
                check(columns.keys == setOf("userId", "reportId")) {
                    "likes must contain exactly userId/reportId, found: ${columns.keys}"
                }
                check(columns.values.toSet() == setOf(1, 2)) {
                    "likes must have a composite primary key (userId, reportId), found: $columns"
                }
            }

            sqlite.query("PRAGMA table_info(report_details)").use { cursor ->
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == "likesCount") {
                        found = true
                        check(cursor.getInt(3) == 1) { "likesCount must be NOT NULL" }
                        check(cursor.getString(4) == "0") {
                            "likesCount default must be 0, was: ${cursor.getString(4)}"
                        }
                    }
                }
                check(found) { "report_details must contain likesCount" }
            }

            // Existing report data is preserved and the counter defaults to 0.
            sqlite.query(
                "SELECT name, weight, likesCount FROM report_details WHERE id = X'00000000000000000000000000000002'"
            ).use { cursor ->
                check(cursor.moveToFirst()) { "report row was lost" }
                check(cursor.getString(0) == "marker") { "report name was lost" }
                check(cursor.getDouble(1) == 12.5) { "report weight was lost" }
                check(cursor.getInt(2) == 0) { "likesCount must default to 0, was: ${cursor.getInt(2)}" }
            }

            // Existing favorites are preserved.
            sqlite.query("SELECT COUNT(*) FROM favorites").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) { "favorites row was lost" }
            }

            // Composite PK is enforced: the same (userId, reportId) twice must fail.
            sqlite.execSQL(
                "INSERT INTO likes (userId, reportId) VALUES " +
                    "(X'00000000000000000000000000000003', X'00000000000000000000000000000002')"
            )
            var duplicateRejected = false
            try {
                sqlite.execSQL(
                    "INSERT INTO likes (userId, reportId) VALUES " +
                        "(X'00000000000000000000000000000003', X'00000000000000000000000000000002')"
                )
            } catch (_: Exception) {
                duplicateRejected = true
            }
            check(duplicateRejected) { "likes must reject duplicate (userId, reportId)" }
        }
        db.close()
    }

    companion object {
        private const val DB_NAME = "migration_10_11_test.db"

        private const val V10_MARKERS_DDL =
            "CREATE TABLE `markers` (`id` BLOB NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT NOT NULL, " +
                "`waterLat` REAL NOT NULL, `waterLng` REAL NOT NULL, `type` TEXT NOT NULL, `fishingMethod` TEXT NOT NULL, " +
                "`fishingStartAt` TEXT, `isPublic` INTEGER NOT NULL, `isPaidWater` INTEGER NOT NULL, " +
                "`fishNames` TEXT NOT NULL, PRIMARY KEY(`id`))"

        private const val V10_REPORT_DETAILS_DDL =
            "CREATE TABLE `report_details` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `publishedAt` TEXT, " +
                "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT, `waterLat` REAL, `waterLng` REAL, " +
                "`waterPaid` INTEGER NOT NULL, `spotLat` REAL, `spotLng` REAL, `fishingStartAt` TEXT, `fishingEndAt` TEXT, " +
                "`weight` REAL NOT NULL, `fishingMethod` TEXT, `comment` TEXT, `shore` INTEGER NOT NULL, " +
                "`isPublic` INTEGER NOT NULL, `imageUrls` TEXT NOT NULL, `fishJson` TEXT NOT NULL, " +
                "`baitsJson` TEXT NOT NULL, `authorName` TEXT, `authorAvatar` TEXT, `createdAt` TEXT, PRIMARY KEY(`id`))"

        private const val V10_FAVORITES_DDL =
            "CREATE TABLE `favorites` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, PRIMARY KEY(`userId`, `reportId`))"
    }
}
