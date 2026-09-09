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
 * Regression: on-device DB created at version 9 (with legacy `fishingTime` column) must
 * migrate to version 10 without Room rejecting the schema. Before the fix Room threw
 * `IllegalStateException: Migration didn't properly handle: markers` because the
 * migration added `fishingStartAt`/`fishingEndAt` but never dropped `fishingTime`,
 * so the migrated table had an unexpected extra column.
 */
@RunWith(AndroidJUnit4::class)
class Migration_9_10_Test {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun createLegacyV9Database() {
        context.deleteDatabase(DB_NAME)
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(V9_MARKERS_DDL)
                    db.execSQL(V9_REPORT_DETAILS_DDL)
                    db.execSQL(V9_FAVORITES_DDL)
                    db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                    db.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)", arrayOf(MARKER_ID_HEX))
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO markers
                    (id, name, waterName, waterLat, waterLng, type, fishingMethod, fishingTime, isPublic, isPaidWater, fishNames)
                VALUES
                    (X'00000000000000000000000000000001', 'marker', 'water', 1.0, 2.0, 'type', 'method',
                     '2026-07-15T06:26:53.197Z', 0, 0, '[]')
                """
            )
            helper.writableDatabase.execSQL(
                """
                INSERT INTO report_details
                    (id, userId, publishedAt, type, name, waterName, waterLat, waterLng, waterPaid, spotLat, spotLng,
                     fishingTime, weight, fishingMethod, comment, shore, isPublic, imageUrls, fishJson, baitsJson,
                     authorName, authorAvatar, createdAt)
                VALUES
                    (X'00000000000000000000000000000002', X'00000000000000000000000000000003', '2026-07-15T06:00:00Z',
                     'type', 'marker', 'water', 1.0, 2.0, 0, 3.0, 4.0,
                     '2026-07-15T06:26:53.197Z', 12.5, 'method', 'comment', 0, 0, '[]', '[]', '[]',
                     'author', 'avatar', '2026-07-15T06:00:00Z')
                """
            )
        }
    }

    @Test
    fun migratingFromV9ToV10_succeeds_preservesData_andDropsLegacyColumn() {
        val db = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_9_10)
            .build()

        db.openHelper.writableDatabase.use { sqlite ->
            check(sqlite.version == 10) { "expected user_version 10, was ${sqlite.version}" }

            sqlite.query("PRAGMA table_info(markers)").use { cursor ->
                val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
                check("fishingStartAt" in columns) { "markers must contain fishingStartAt, found: $columns" }
                check("fishingTime" !in columns) { "markers must not contain legacy fishingTime, found: $columns" }
            }

            sqlite.query("PRAGMA table_info(report_details)").use { cursor ->
                val columns = buildSet { while (cursor.moveToNext()) add(cursor.getString(1)) }
                check("fishingStartAt" in columns) { "report_details must contain fishingStartAt, found: $columns" }
                check("fishingEndAt" in columns) { "report_details must contain fishingEndAt, found: $columns" }
                check("fishingTime" !in columns) { "report_details must not contain legacy fishingTime, found: $columns" }
            }

            sqlite.query("SELECT fishingStartAt FROM markers WHERE id = X'00000000000000000000000000000001'").use { cursor ->
                check(cursor.moveToFirst()) { "marker row was lost" }
                check(cursor.getString(0) == "2026-07-15T06:26:53.197Z") {
                    "marker fishingStartAt must contain legacy fishingTime value, was: ${cursor.getString(0)}"
                }
            }

            sqlite.query(
                "SELECT fishingStartAt, fishingEndAt FROM report_details WHERE id = X'00000000000000000000000000000002'"
            ).use { cursor ->
                check(cursor.moveToFirst()) { "report row was lost" }
                check(cursor.getString(0) == "2026-07-15T06:26:53.197Z") { "fishingStartAt not copied" }
                check(cursor.isNull(1)) { "fishingEndAt must stay NULL for legacy data, was: ${cursor.getString(1)}" }
            }
        }
        db.close()
    }

    companion object {
        private const val DB_NAME = "migration_9_10_test.db"
        private const val MARKER_ID_HEX = "00000000000000000000000000000001"

        // Exact DDL captured from a real user database on a device at version 9.
        private const val V9_MARKERS_DDL =
            "CREATE TABLE `markers` (`id` BLOB NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT NOT NULL, " +
                "`waterLat` REAL NOT NULL, `waterLng` REAL NOT NULL, `type` TEXT NOT NULL, `fishingMethod` TEXT NOT NULL, " +
                "`fishingTime` TEXT NOT NULL, `isPublic` INTEGER NOT NULL, `isPaidWater` INTEGER NOT NULL, " +
                "`fishNames` TEXT NOT NULL, PRIMARY KEY(`id`))"

        private const val V9_REPORT_DETAILS_DDL =
            "CREATE TABLE `report_details` (`id` BLOB NOT NULL, `userId` BLOB NOT NULL, `publishedAt` TEXT, " +
                "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `waterName` TEXT, `waterLat` REAL, `waterLng` REAL, " +
                "`waterPaid` INTEGER NOT NULL, `spotLat` REAL, `spotLng` REAL, `fishingTime` TEXT NOT NULL, " +
                "`weight` REAL NOT NULL, `fishingMethod` TEXT, `comment` TEXT, `shore` INTEGER NOT NULL, " +
                "`isPublic` INTEGER NOT NULL, `imageUrls` TEXT NOT NULL, `fishJson` TEXT NOT NULL, " +
                "`baitsJson` TEXT NOT NULL, `authorName` TEXT, `authorAvatar` TEXT, `createdAt` TEXT, PRIMARY KEY(`id`))"

        private const val V9_FAVORITES_DDL =
            "CREATE TABLE `favorites` (`userId` BLOB NOT NULL, `reportId` BLOB NOT NULL, PRIMARY KEY(`userId`, `reportId`))"
    }
}