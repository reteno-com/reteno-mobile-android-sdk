package com.reteno.core.data.local.database

import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.data.local.database.schema.DeviceSchema
import com.reteno.core.data.local.database.schema.EventsSchema
import com.reteno.core.data.local.database.schema.InAppInteractionSchema
import com.reteno.core.data.local.database.schema.InAppMessageSchema
import com.reteno.core.data.local.database.schema.InteractionRequestSchema
import com.reteno.core.data.local.database.schema.InteractionSchema
import com.reteno.core.data.local.database.schema.LogEventSchema
import com.reteno.core.data.local.database.schema.RecomEventsSchema
import com.reteno.core.data.local.database.schema.UserSchema
import com.reteno.core.data.local.database.schema.WrappedLinkSchema
import com.reteno.core.fixtures.FakeLogSink
import com.reteno.core.fixtures.FakeRemoteLog
import com.reteno.core.fixtures.installCapturing
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoDatabaseImplTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val databaseFile = application.getDatabasePath(DbSchema.DATABASE_NAME)
        val remoteLog = FakeRemoteLog()
        val sleeps = mutableListOf<Long>()

        init {
            application.deleteDatabase(DbSchema.DATABASE_NAME)
        }

        fun <T> withDatabase(block: (RetenoDatabaseImpl) -> T): T {
            val database = RetenoDatabaseImpl(application) { sleeps += it }
            Logger.installCapturing(FakeLogSink(), remoteLog)
            try {
                return block(database)
            } finally {
                Logger.install(AndroidLogSink(), null)
                database.close()
            }
        }

        fun storeOldDatabase(version: Int, vararg statements: String) {
            databaseFile.parentFile?.mkdirs()
            val db = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
            statements.forEach { db.execSQL(it) }
            db.version = version
            db.close()
        }

        fun tables(database: RetenoDatabaseImpl): Set<String> =
            database.rawQuery("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toSet()
            }

        fun columnList(database: RetenoDatabaseImpl, table: String): List<String> =
            database.rawQuery("PRAGMA table_info($table)").use(::columnNames)

        fun columnNames(tableInfo: Cursor): List<String> {
            val columns = mutableListOf<String>()
            while (tableInfo.moveToNext()) {
                columns += tableInfo.getString(tableInfo.getColumnIndexOrThrow("name"))
            }
            return columns
        }

        fun values(database: RetenoDatabaseImpl, sql: String): List<String?> = database.rawQuery(sql).use { cursor ->
            val values = mutableListOf<String?>()
            while (cursor.moveToNext()) {
                values += cursor.getString(0)
            }
            values
        }

        fun tableSql(database: RetenoDatabaseImpl): Map<String, String> =
            database.rawQuery(SDK_TABLES_SQL).use { cursor ->
                val tables = mutableMapOf<String, String>()
                while (cursor.moveToNext()) {
                    tables[cursor.getString(0)] = cursor.getString(1)
                }
                tables
            }

        fun storeReleasedDatabase(name: String, vararg rows: String): Map<String, List<String>> {
            val statements = javaClass.classLoader!!.getResource("$HISTORY/$name")!!.readText()
                .lines()
                .filter { it.startsWith("CREATE TABLE") }
            val version = name.substring(1, 3).toInt()
            application.deleteDatabase(DbSchema.DATABASE_NAME)
            storeOldDatabase(version, *(statements + rows).toTypedArray())
            val db = SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val tables = statements.map { Regex("EXISTS (\\w+)").find(it)!!.groupValues[1] }
                return tables.associateWith { table ->
                    db.rawQuery("PRAGMA table_info($table)", null).use(::columnNames)
                }
            } finally {
                db.close()
            }
        }

        fun count(database: RetenoDatabaseImpl, table: String): Long = database.getRowCount(table)
    }

    private fun device(deviceId: String = "device-1") = ContentValues().apply {
        put(DeviceSchema.COLUMN_DEVICE_ID, deviceId)
        put(DeviceSchema.COLUMN_CATEGORY, "MOBILE")
        put(DeviceSchema.COLUMN_OS_TYPE, "ANDROID")
    }

    @Test
    fun `should create every table on first open`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val tables = fixture.withDatabase { fixture.tables(it) }

        then()
        assertTrue(tables.containsAll(ALL_TABLES)) { "$tables" }
    }

    @Test
    fun `should enforce foreign keys`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val enabled = fixture.withDatabase { db ->
            db.rawQuery("PRAGMA foreign_keys").use { it.moveToFirst(); it.getInt(0) }
        }

        then()
        assertEquals(1, enabled)
    }

    @Test
    fun `should read back inserted row`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (rowId, deviceIds) = fixture.withDatabase { db ->
            val rowId = db.insert(DeviceSchema.TABLE_NAME, contentValues = device())
            val ids = db.query(DeviceSchema.TABLE_NAME, arrayOf(DeviceSchema.COLUMN_DEVICE_ID)).use { cursor ->
                generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
            }
            rowId to ids
        }

        then()
        assertEquals(1L, rowId)
        assertEquals(listOf("device-1"), deviceIds)
    }

    @Test
    fun `should insert several rows in one transaction`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (rowIds, stored) = fixture.withDatabase { db ->
            val ids = db.insertMultiple(DeviceSchema.TABLE_NAME, contentValues = listOf(device("a"), device("b")))
            ids to fixture.count(db, DeviceSchema.TABLE_NAME)
        }

        then()
        assertEquals(listOf(1L, 2L), rowIds)
        assertEquals(2L, stored)
    }

    @Test
    fun `should keep valid rows of several when one is invalid`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val invalid = ContentValues().apply { put(DeviceSchema.COLUMN_DEVICE_ID, "no category") }

        whenn()
        val (rowIds, stored) = fixture.withDatabase { db ->
            val ids = db.insertMultiple(DeviceSchema.TABLE_NAME, contentValues = listOf(device("a"), invalid))
            ids to fixture.count(db, DeviceSchema.TABLE_NAME)
        }

        then()
        assertEquals(listOf(1L, -1L), rowIds)
        assertEquals(1L, stored)
    }

    @Test
    fun `should give no row id for a row that cannot be inserted`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val invalid = ContentValues().apply { put(DeviceSchema.COLUMN_EMAIL, "john@example.com") }

        whenn()
        val (rowId, stored) = fixture.withDatabase { db ->
            db.insert(DeviceSchema.TABLE_NAME, contentValues = invalid) to fixture.count(db, DeviceSchema.TABLE_NAME)
        }

        then()
        assertEquals(-1L to 0L, rowId to stored)
    }

    @Test
    fun `should delete matching rows and count the rest`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (deleted, remaining, unmatched) = fixture.withDatabase { db ->
            db.insertMultiple(DeviceSchema.TABLE_NAME, contentValues = listOf(device("a"), device("b"), device("c")))
            val deleted = db.delete(DeviceSchema.TABLE_NAME, "${DeviceSchema.COLUMN_DEVICE_ID}=?", arrayOf("a"))
            val unmatched = db.getRowCount(DeviceSchema.TABLE_NAME, "${DeviceSchema.COLUMN_DEVICE_ID}=?", arrayOf("a"))
            Triple(deleted, fixture.count(db, DeviceSchema.TABLE_NAME), unmatched)
        }

        then()
        assertEquals(Triple(1, 2L, 0L), Triple(deleted, remaining, unmatched))
    }

    @Test
    fun `should recreate a missing table after a failed write`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (deleted, rowId) = fixture.withDatabase { db ->
            db.rawQuery("SELECT 1").close()
            db.writableDatabase.execSQL("DROP TABLE ${DeviceSchema.TABLE_NAME}")
            db.delete(DeviceSchema.TABLE_NAME) to db.insert(DeviceSchema.TABLE_NAME, contentValues = device())
        }

        then()
        assertEquals(0 to 1L, deleted to rowId)
        assertEquals(
            listOf("delete(): failed on table Device", "mitigate(): tables created"),
            fixture.remoteLog.errors.map { it.first }
        )
    }

    @Test
    fun `should count no rows of a missing table and recreate it`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (count, tables) = fixture.withDatabase { db ->
            db.rawQuery("SELECT 1").close()
            db.writableDatabase.execSQL("DROP TABLE ${DeviceSchema.TABLE_NAME}")
            db.getRowCount(DeviceSchema.TABLE_NAME) to fixture.tables(db)
        }

        then()
        assertEquals(0L, count)
        assertTrue(DeviceSchema.TABLE_NAME in tables)
    }

    @Test
    fun `should upgrade database and rethrow when a query names a missing column`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val failure = fixture.withDatabase { db ->
            runCatching { db.query(DeviceSchema.TABLE_NAME, arrayOf("noSuchColumn")) }.exceptionOrNull()
        }

        then()
        assertTrue(failure is SQLiteException)
        assertTrue("mitigate(): database upgraded" in fixture.remoteLog.errors.map { it.first })
    }

    @Test
    fun `should rethrow raw query that fails for another reason`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val failure = fixture.withDatabase { db -> runCatching { db.rawQuery("SELEC 1") }.exceptionOrNull() }

        then()
        assertTrue(failure is SQLiteException)
        assertTrue(fixture.remoteLog.errors.isEmpty())
    }

    @Test
    fun `should give up writes and counts on a closed database`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val (rowId, count) = fixture.withDatabase { db ->
            db.rawQuery("SELECT 1").close()
            db.writableDatabase.close()
            db.insert(DeviceSchema.TABLE_NAME, contentValues = device()) to db.getRowCount(DeviceSchema.TABLE_NAME)
        }

        then()
        assertEquals(-1L to 0L, rowId to count)
        assertTrue(fixture.remoteLog.errors.all { it.second is IllegalStateException })
    }

    @Test
    fun `should remove event groups that have no events left`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val remaining = fixture.withDatabase { db ->
            val kept = db.insert(EventsSchema.TABLE_NAME_EVENTS, contentValues = eventsGroup("kept"))
            db.insert(EventsSchema.TABLE_NAME_EVENTS, contentValues = eventsGroup("empty"))
            db.insert(EventsSchema.EventSchema.TABLE_NAME_EVENT, contentValues = event(kept))
            db.cleanUnlinkedEvents()
            fixture.count(db, EventsSchema.TABLE_NAME_EVENTS)
        }

        then()
        assertEquals(1L, remaining)
    }

    @Test
    fun `should remove recommendation variants that have no events left`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val remaining = fixture.withDatabase { db ->
            db.insert(RecomEventsSchema.TABLE_NAME_RECOM_EVENTS, contentValues = recomVariant("r1"))
            db.insert(RecomEventsSchema.TABLE_NAME_RECOM_EVENTS, contentValues = recomVariant("r2"))
            db.insert(RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT, contentValues = recomEvent("r1"))
            db.cleanUnlinkedRecomVariantIds()
            fixture.count(db, RecomEventsSchema.TABLE_NAME_RECOM_EVENTS)
        }

        then()
        assertEquals(1L, remaining)
    }

    @Test
    fun `should recreate tables when cleaning a missing table`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val tables = fixture.withDatabase { db ->
            db.rawQuery("SELECT 1").close()
            db.writableDatabase.execSQL("DROP TABLE ${EventsSchema.EventSchema.TABLE_NAME_EVENT}")
            db.cleanUnlinkedEvents()
            fixture.tables(db)
        }

        then()
        assertTrue(EventsSchema.EventSchema.TABLE_NAME_EVENT in tables)
    }

    @Test
    fun `should create every table exactly as its schema declares`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val stored = fixture.withDatabase { fixture.tableSql(it) }

        then()
        val expected = CURRENT_SCHEMA.associate { sql ->
            val table = Regex("EXISTS (\\w+)").find(sql)!!.groupValues[1]
            table to sql.replace("CREATE TABLE IF NOT EXISTS ", "CREATE TABLE ")
        }
        assertEquals(expected, stored)
    }

    @Test
    fun `should upgrade every released schema to the current columns keeping existing columns in place`() =
        runUnitTest {
            given()
            val fixture = SutFixture()
            val current = fixture.withDatabase { db -> fixture.tables(db).associateWith { fixture.columnList(db, it) } }

            whenn()
            val upgraded = RELEASED_SCHEMAS.associateWith { name ->
                val old = fixture.storeReleasedDatabase(name)
                val columns = fixture.withDatabase { db -> current.keys.associateWith { fixture.columnList(db, it) } }
                old to columns
            }

            then()
            val mismatches = upgraded.flatMap { (name, result) ->
                val (old, columns) = result
                current.mapNotNull { (table, currentColumns) ->
                    val upgradedColumns = columns.getValue(table)
                    val oldColumns = old[table].orEmpty()
                    val missing = currentColumns - upgradedColumns.toSet()
                    val extra = upgradedColumns - currentColumns.toSet()
                    val moved = oldColumns != upgradedColumns.take(oldColumns.size)
                    if (missing.isEmpty() && extra.isEmpty() && !moved) {
                        null
                    } else {
                        "$name $table: missing=$missing extra=$extra moved=$moved"
                    }
                }
            }
            assertEquals(emptyList<String>(), mismatches)
        }

    @Test
    fun `should upgrade every released schema without reporting errors`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val errors = RELEASED_SCHEMAS.associateWith { name ->
            fixture.storeReleasedDatabase(name)
            fixture.withDatabase { db -> fixture.tables(db) }
            fixture.remoteLog.errors.map { it.first }.also { fixture.remoteLog.errors.clear() }
        }

        then()
        assertEquals(RELEASED_SCHEMAS.associateWith { emptyList<String>() }, errors)
    }

    @Test
    fun `should report a migration that fails for another reason than an existing column`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.storeOldDatabase(version = 9, IN_APP_MESSAGES_AS_VIEW)

        whenn()
        fixture.withDatabase { db -> fixture.tables(db) }

        then()
        assertEquals(listOf("onUpgrade(): migration failed"), fixture.remoteLog.errors.map { it.first })
    }

    @Test
    fun `should keep stored devices and users when upgrading every released schema`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val kept = RELEASED_SCHEMAS.associateWith { name ->
            fixture.storeReleasedDatabase(name, STORED_DEVICE, STORED_USER, STORED_USER_ATTRIBUTES)
            fixture.withDatabase { db ->
                listOf(
                    fixture.values(db, "SELECT deviceId FROM Device"),
                    fixture.values(db, "SELECT externalUserId FROM User"),
                    fixture.values(db, "SELECT email FROM UserAttributes WHERE user_row_id = 1")
                )
            }
        }

        then()
        val expected = listOf(listOf("old-device"), listOf("old-user"), listOf("old@example.com"))
        kept.forEach { (name, values) -> assertEquals(expected, values) { name } }
    }

    @Test
    fun `should start over when database comes from a newer sdk version`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.storeOldDatabase(version = DbSchema.DATABASE_VERSION + 1, "CREATE TABLE Future (id INTEGER)")

        whenn()
        val (version, tables) = fixture.withDatabase { db -> db.writableDatabase.version to fixture.tables(db) }

        then()
        assertEquals(DbSchema.DATABASE_VERSION, version)
        assertTrue(tables.containsAll(ALL_TABLES))
    }

    @Test
    fun `should drop database left encrypted by an old sdk version`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.databaseFile.parentFile?.mkdirs()
        fixture.databaseFile.writeBytes(ByteArray(4096) { 0x5A })

        whenn()
        val (rowId, tables) = fixture.withDatabase { db ->
            db.insert(DeviceSchema.TABLE_NAME, contentValues = device()) to fixture.tables(db)
        }

        then()
        assertEquals(1L, rowId)
        assertTrue(tables.containsAll(ALL_TABLES))
    }

    @Test
    fun `should open database after it was busy or unavailable for a while`() = runUnitTest {
        given()
        val sleeps = mutableListOf<Long>()
        val failures = mutableListOf<SQLiteException>(
            SQLiteCantOpenDatabaseException("cannot open"),
            SQLiteDatabaseLockedException("locked")
        )

        whenn()
        val opened = openRetrying({ sleeps += it }) { failures.removeFirstOrNull()?.let { throw it } ?: "db" }

        then()
        assertEquals("db", opened)
        assertEquals(listOf(400L, 800L), sleeps)
    }

    @Test
    fun `should give up opening database after five attempts with the first failure`() = runUnitTest {
        given()
        val sleeps = mutableListOf<Long>()
        var attempts = 0

        whenn()
        val failure = runCatching {
            openRetrying({ sleeps += it }) { throw SQLiteDatabaseLockedException("locked ${++attempts}") }
        }.exceptionOrNull()

        then()
        assertTrue(failure is SQLiteException)
        assertEquals("locked 1", failure?.message)
        assertEquals(listOf(400L, 800L, 1200L, 1600L), sleeps)
    }

    private fun eventsGroup(deviceId: String) = ContentValues().apply {
        put(EventsSchema.COLUMN_EVENTS_DEVICE_ID, deviceId)
    }

    private fun event(groupId: Long) = ContentValues().apply {
        put(EventsSchema.COLUMN_EVENTS_ID, groupId)
        put(EventsSchema.EventSchema.COLUMN_EVENT_TYPE_KEY, "screenView")
    }

    private fun recomVariant(variantId: String) = ContentValues().apply {
        put(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID, variantId)
    }

    private fun recomEvent(variantId: String) = ContentValues().apply {
        put(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID, variantId)
        put(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_PRODUCT_ID, "sku-1")
        put(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_TYPE, "IMPRESSIONS")
        put(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED, "2024-01-01T00:00:00Z")
    }

    companion object {
        private val ALL_TABLES = setOf(
            "Device", "User", "UserAttributes", "UserAddress", "Interaction", "InAppInteraction", "Events", "Event",
            "AppInbox", "RecomEvents", "RecomEvent", "WrappedLink", "LogEvent", "InAppMessage", "Segment",
            "InteractionRequest"
        )

        private const val HISTORY = "database/history"
        private const val IN_APP_MESSAGES_AS_VIEW = "CREATE VIEW InAppMessage AS SELECT 1 AS row_id"
        private const val SDK_TABLES_SQL =
            "SELECT name, sql FROM sqlite_master WHERE type='table' AND name <> 'android_metadata'"

        private val RELEASED_SCHEMAS = listOf(
            "v01_fb65649.sql", "v03_216c722.sql", "v04_b8633fd.sql", "v04_091171a.sql", "v06_dd304e7.sql",
            "v07_e6a793f.sql", "v07_50f7e9b.sql", "v07_f78171d.sql", "v08_da24b58.sql", "v09_3f28644.sql",
            "v09_d720e0d.sql", "v10_6766a84.sql"
        )

        private val CURRENT_SCHEMA = listOf(
            DeviceSchema.SQL_CREATE_TABLE,
            UserSchema.SQL_CREATE_TABLE,
            UserSchema.UserAttributesSchema.SQL_CREATE_TABLE,
            UserSchema.UserAddressSchema.SQL_CREATE_TABLE,
            InteractionSchema.SQL_CREATE_TABLE,
            InAppInteractionSchema.SQL_CREATE_TABLE,
            EventsSchema.SQL_CREATE_TABLE,
            EventsSchema.EventSchema.SQL_CREATE_TABLE,
            AppInboxSchema.SQL_CREATE_TABLE,
            RecomEventsSchema.SQL_CREATE_TABLE,
            RecomEventsSchema.RecomEventSchema.SQL_CREATE_TABLE,
            WrappedLinkSchema.SQL_CREATE_TABLE,
            LogEventSchema.SQL_CREATE_TABLE,
            InAppMessageSchema.SQL_CREATE_TABLE,
            InAppMessageSchema.SegmentSchema.SQL_CREATE_TABLE,
            InteractionRequestSchema.SQL_CREATE_TABLE
        )

        private const val STORED_DEVICE =
            "INSERT INTO Device (deviceId, category, osType) VALUES ('old-device', 'MOBILE', 'ANDROID')"
        private const val STORED_USER = "INSERT INTO User (deviceId, externalUserId) VALUES ('old-device', 'old-user')"
        private const val STORED_USER_ATTRIBUTES =
            "INSERT INTO UserAttributes (user_row_id, email) VALUES (1, 'old@example.com')"
    }
}
