package com.reteno.core.data.local.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.SystemClock
import com.reteno.core.data.local.database.schema.AppInboxSchema
import com.reteno.core.data.local.database.schema.DbSchema.DATABASE_NAME
import com.reteno.core.data.local.database.schema.DbSchema.DATABASE_VERSION
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
import com.reteno.core.util.Logger

internal class RetenoDatabaseImpl(
    private val context: Context,
    private val sleep: (Long) -> Unit = SystemClock::sleep
) : RetenoDatabase, SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val lock = Any()

    private class ColumnMigration(val table: String, val column: String, val statement: String)

    private val migrations = listOf(
        2 to listOf(
            ColumnMigration(
                DeviceSchema.TABLE_NAME,
                DeviceSchema.COLUMN_PUSH_SUBSCRIBED,
                DeviceSchema.SQL_UPGRADE_TABLE_VERSION_2
            )
        ),
        4 to listOf(
            ColumnMigration(
                InteractionSchema.TABLE_NAME,
                InteractionSchema.COLUMN_INTERACTION_ACTION,
                InteractionSchema.SQL_UPGRADE_TABLE_VERSION_4
            )
        ),
        6 to listOf(
            ColumnMigration(
                UserSchema.TABLE_NAME,
                UserSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND,
                UserSchema.SQL_UPGRADE_TABLE_VERSION_6
            ),
            ColumnMigration(
                DeviceSchema.TABLE_NAME,
                DeviceSchema.COLUMN_SYNCHRONIZED_WITH_BACKEND,
                DeviceSchema.SQL_UPGRADE_TABLE_VERSION_6
            )
        ),
        8 to listOf(
            ColumnMigration(
                DeviceSchema.TABLE_NAME,
                DeviceSchema.COLUMN_EMAIL,
                DeviceSchema.SQL_UPGRADE_TABLE_VERSION_8_EMAIL
            ),
            ColumnMigration(
                DeviceSchema.TABLE_NAME,
                DeviceSchema.COLUMN_PHONE,
                DeviceSchema.SQL_UPGRADE_TABLE_VERSION_8_PHONE
            )
        ),
        10 to listOf(
            ColumnMigration(
                InAppMessageSchema.TABLE_NAME,
                InAppMessageSchema.COLUMN_IAM_POSITION,
                InAppMessageSchema.SQL_UPGRADE_TABLE_VERSION_9
            )
        ),
        11 to listOf(
            ColumnMigration(
                UserSchema.UserAttributesSchema.TABLE_NAME_USER_ATTRIBUTES,
                UserSchema.UserAttributesSchema.COLUMN_MARKET_ID,
                UserSchema.UserAttributesSchema.SQL_UPGRADE_TABLE_VERSION_10
            )
        )
    )

    private val writableSQLDatabase: SQLiteDatabase by lazy { writableDatabase }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys=ON")
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        createTables(db)
        migrations
            .filter { (version, _) -> version > oldVersion }
            .forEach { (_, columns) -> columns.forEach { migrate(db, it) } }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        context.deleteDatabase(DATABASE_NAME)
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL(DeviceSchema.SQL_CREATE_TABLE)
        db.execSQL(UserSchema.SQL_CREATE_TABLE)
        db.execSQL(UserSchema.UserAttributesSchema.SQL_CREATE_TABLE)
        db.execSQL(UserSchema.UserAddressSchema.SQL_CREATE_TABLE)
        db.execSQL(InteractionSchema.SQL_CREATE_TABLE)
        db.execSQL(InAppInteractionSchema.SQL_CREATE_TABLE)
        db.execSQL(EventsSchema.SQL_CREATE_TABLE)
        db.execSQL(EventsSchema.EventSchema.SQL_CREATE_TABLE)
        db.execSQL(AppInboxSchema.SQL_CREATE_TABLE)
        db.execSQL(RecomEventsSchema.SQL_CREATE_TABLE)
        db.execSQL(RecomEventsSchema.RecomEventSchema.SQL_CREATE_TABLE)
        db.execSQL(WrappedLinkSchema.SQL_CREATE_TABLE)
        db.execSQL(LogEventSchema.SQL_CREATE_TABLE)
        db.execSQL(InAppMessageSchema.SQL_CREATE_TABLE)
        db.execSQL(InAppMessageSchema.SegmentSchema.SQL_CREATE_TABLE)
        db.execSQL(InteractionRequestSchema.SQL_CREATE_TABLE)
    }

    private fun migrate(db: SQLiteDatabase, migration: ColumnMigration) {
        if (migration.column in db.columnsOf(migration.table)) return
        try {
            db.execSQL(migration.statement)
        } catch (e: SQLiteException) {
            Logger.e(TAG, "onUpgrade(): migration failed", e)
        }
    }

    private fun SQLiteDatabase.columnsOf(table: String): Set<String> =
        rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(name) else null }.toSet()
        }

    override fun query(
        table: String,
        columns: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String?
    ): Cursor = read {
        it.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)
    }

    override fun rawQuery(rawQuery: String, selectionArgs: Array<out String>?): Cursor = read {
        it.rawQuery(rawQuery, selectionArgs)
    }

    override fun insert(table: String, nullColumnHack: String?, contentValues: ContentValues): Long =
        write("insert", table, failed = -1L) {
            it.insert(table, nullColumnHack, contentValues)
        }

    override fun insertMultiple(
        table: String,
        nullColumnHack: String?,
        contentValues: List<ContentValues>
    ): List<Long> = write("insertMultiple", table, failed = emptyList()) { db ->
        contentValues.map { db.insert(table, nullColumnHack, it) }
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<String?>?): Int =
        write("delete", table, failed = 0) {
            it.delete(table, whereClause, whereArgs)
        }

    override fun getRowCount(tableName: String, whereClause: String?, whereArgs: Array<String?>?): Long =
        try {
            DatabaseUtils.queryNumEntries(openWithRetries(), tableName, whereClause, whereArgs)
        } catch (e: SQLiteException) {
            Logger.e(TAG, "getRowCount(): failed on table $tableName", e)
            mitigate(e)
            0L
        } catch (e: IllegalStateException) {
            Logger.e(TAG, "getRowCount(): failed on table $tableName", e)
            0L
        }

    override fun cleanUnlinkedEvents() {
        execute(
            "DELETE FROM ${EventsSchema.TABLE_NAME_EVENTS} WHERE ${EventsSchema.COLUMN_EVENTS_ID} NOT IN " +
                "(SELECT ${EventsSchema.COLUMN_EVENTS_ID} FROM ${EventsSchema.EventSchema.TABLE_NAME_EVENT})"
        )
    }

    override fun cleanUnlinkedRecomVariantIds() {
        execute(
            "DELETE FROM ${RecomEventsSchema.TABLE_NAME_RECOM_EVENTS} " +
                "WHERE ${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} NOT IN " +
                "(SELECT ${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} " +
                "FROM ${RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT})"
        )
    }

    private fun execute(statement: String) {
        try {
            openWithRetries().execSQL(statement)
        } catch (e: SQLiteException) {
            mitigate(e)
        }
    }

    private fun <T> read(query: (SQLiteDatabase) -> T): T =
        try {
            query(openWithRetries())
        } catch (e: SQLiteException) {
            mitigate(e)
            throw e
        }

    private fun <T> write(operation: String, table: String, failed: T, change: (SQLiteDatabase) -> T): T =
        synchronized(lock) {
            val db = openWithRetries()
            try {
                db.beginTransaction()
                try {
                    val result = change(db)
                    db.setTransactionSuccessful()
                    result
                } finally {
                    db.endTransaction()
                }
            } catch (e: SQLiteException) {
                Logger.e(TAG, "$operation(): failed on table $table", e)
                mitigate(e)
                failed
            } catch (e: IllegalStateException) {
                Logger.e(TAG, "$operation(): failed on table $table", e)
                failed
            }
        }

    private fun openWithRetries(): SQLiteDatabase = synchronized(lock) {
        openRetrying(sleep) { writableSQLDatabase }
    }

    private fun mitigate(e: SQLiteException) {
        val message = "${e.message}"
        if (message.contains("no such table")) {
            createTables(writableSQLDatabase)
            Logger.e(TAG, "mitigate(): tables created", e)
        } else if (message.contains("no such column")) {
            onUpgrade(writableSQLDatabase, 1, DATABASE_VERSION)
            Logger.e(TAG, "mitigate(): database upgraded", e)
        }
    }

    private companion object {
        const val TAG = "RetenoDatabaseImpl"
    }
}

internal fun <T> openRetrying(sleep: (Long) -> Unit, open: () -> T): T {
    val failures = mutableListOf<SQLiteException>()
    while (true) {
        try {
            return open()
        } catch (e: SQLiteCantOpenDatabaseException) {
            Logger.e("RetenoDatabaseImpl", "openRetrying(): database cannot be opened", e)
            failures += e
        } catch (e: SQLiteDatabaseLockedException) {
            Logger.e("RetenoDatabaseImpl", "openRetrying(): database is locked", e)
            failures += e
        }
        if (failures.size >= DB_OPEN_RETRY_MAX) throw SQLiteException(failures.first().message)
        sleep(failures.size * DB_OPEN_RETRY_BACKOFF_MILLIS)
    }
}

private const val DB_OPEN_RETRY_MAX = 5
private const val DB_OPEN_RETRY_BACKOFF_MILLIS = 400L
