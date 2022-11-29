package com.reteno.core.data.local.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.SystemClock
import com.reteno.core.BuildConfig
import com.reteno.core.data.local.database.DbSchema.DATABASE_NAME
import com.reteno.core.data.local.database.DbSchema.DATABASE_VERSION
import com.reteno.core.util.Logger
import net.sqlcipher.Cursor
import net.sqlcipher.DatabaseUtils
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteException
import net.sqlcipher.database.SQLiteOpenHelper

class RetenoDatabaseImpl(context: Context) : RetenoDatabase,
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val writableDatabase = getWritableDatabase(BuildConfig.SQL_PASSWORD)

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        db?.execSQL("PRAGMA foreign_keys=ON");
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DbSchema.DeviceSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.UserSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.UserAttributesSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.UserAddressSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.InteractionSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.EventsSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.EventSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.AppInboxSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.RecomEventsSchema.SQL_CREATE_TABLE)
        db.execSQL(DbSchema.RecomEventSchema.SQL_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Not used for now
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
    ): Cursor =
        getSQLiteDatabaseWithRetries().query(
            table,
            columns,
            selection,
            selectionArgs,
            groupBy,
            having,
            orderBy,
            limit
        )

    override fun rawQuery(rawQuery: String, selectionArgs: Array<out String>?): Cursor =
        getSQLiteDatabaseWithRetries().rawQuery(rawQuery, selectionArgs)

    override fun insert(
        table: String,
        nullColumnHack: String?,
        contentValues: ContentValues
    ): Long {
        var rowId: Long = -1

        synchronized(LOCK) {
            val writableDb = getSQLiteDatabaseWithRetries()
            try {
                writableDb.beginTransaction()
                rowId = writableDb.insert(table, nullColumnHack, contentValues)
                writableDb.setTransactionSuccessful()
                return rowId
            } catch (e: SQLiteException) {
                /*@formatter:off*/ Logger.e(TAG, "insert(): Error inserting on table: $table with nullColumnHack: $nullColumnHack and values: $contentValues", e)
                /*@formatter:on*/
            } catch (e: IllegalStateException) {
                /*@formatter:off*/ Logger.e(TAG, "insert(): Error under inserting transaction under table: $table with nullColumnHack: $nullColumnHack and values: $contentValues", e)
                /*@formatter:on*/
            } finally {
                try {
                    writableDb.endTransaction() // May throw if transaction was never opened or DB is full.
                } catch (e: IllegalStateException) {
                    /*@formatter:off*/ Logger.e(TAG, "insert(): Error closing transaction! ", e)
                    /*@formatter:on*/
                } catch (e: SQLiteException) {
                    /*@formatter:off*/ Logger.e(TAG, "insert(): Error closing transaction! ", e)
                    /*@formatter:on*/
                }
            }
        }

        return rowId
    }

    override fun insertMultiple(
        table: String,
        nullColumnHack: String?,
        contentValues: List<ContentValues>
    ): List<Long> {
        val rowIds = mutableListOf<Long>()

        synchronized(LOCK) {
            val writableDb = getSQLiteDatabaseWithRetries()
            try {
                writableDb.beginTransaction()

                for (contentValue in contentValues) {
                    val rowId = writableDb.insert(table, nullColumnHack, contentValue)
                    rowIds.add(rowId)
                }

                writableDb.setTransactionSuccessful()
                return rowIds
            } catch (e: SQLiteException) {
                /*@formatter:off*/ Logger.e(TAG, "insert(): Error inserting on table: $table with nullColumnHack: $nullColumnHack and values: $contentValues", e)
                /*@formatter:on*/
            } catch (e: IllegalStateException) {
                /*@formatter:off*/ Logger.e(TAG, "insert(): Error under inserting transaction under table: $table with nullColumnHack: $nullColumnHack and values: $contentValues", e)
                /*@formatter:on*/
            } finally {
                try {
                    writableDb.endTransaction() // May throw if transaction was never opened or DB is full.
                } catch (e: IllegalStateException) {
                    /*@formatter:off*/ Logger.e(TAG, "insert(): Error closing transaction! ", e)
                    /*@formatter:on*/
                } catch (e: SQLiteException) {
                    /*@formatter:off*/ Logger.e(TAG, "insert(): Error closing transaction! ", e)
                    /*@formatter:on*/
                }
            }
        }

        return rowIds
    }

    override fun insertOrThrow(
        table: String,
        nullColumnHack: String?,
        contentValues: ContentValues
    ): Long {
        var rowId: Long = -1

        synchronized(LOCK) {
            val writableDb = getSQLiteDatabaseWithRetries()
            try {
                writableDb.beginTransaction()
                rowId = writableDb.insertOrThrow(table, nullColumnHack, contentValues)
                writableDb.setTransactionSuccessful()
            } catch (e: SQLiteException) {
                /*@formatter:off*/ Logger.e(TAG, "insertOrThrow(): Error inserting or throw on table: $table with nullColumnHack: $nullColumnHack and values: $contentValues", e)
                /*@formatter:on*/
            } catch (e: IllegalStateException) {
                /*@formatter:off*/ Logger.e(TAG, "insertOrThrow(): Error under inserting or throw transaction under table: $table with nullColumnHack: $nullColumnHack and values: $contentValues", e)
                /*@formatter:on*/
            } finally {
                try {
                    writableDb.endTransaction() // May throw if transaction was never opened or DB is full.
                } catch (e: IllegalStateException) {
                    /*@formatter:off*/ Logger.e(TAG, "insertOrThrow(): Error closing transaction! ", e)
                    /*@formatter:on*/
                } catch (e: SQLiteException) {
                    /*@formatter:off*/ Logger.e(TAG, "insertOrThrow(): Error closing transaction! ", e)
                    /*@formatter:on*/
                }
            }
        }

        return rowId
    }

    override fun update(
        table: String,
        contentValues: ContentValues,
        whereClause: String?,
        whereArgs: Array<String?>?
    ): Int {
        var result = 0
        if (contentValues.toString().isEmpty()) return result

        synchronized(LOCK) {
            val writableDb = getSQLiteDatabaseWithRetries()
            try {
                writableDb.beginTransaction()
                result = writableDb.update(table, contentValues, whereClause, whereArgs)
                writableDb.setTransactionSuccessful()
            } catch (e: SQLiteException) {
                /*@formatter:off*/ Logger.e(TAG, "update(): Error updating on table: $table with whereClause: $whereClause and whereArgs: $whereArgs", e)
                /*@formatter:on*/
            } catch (e: IllegalStateException) {
                /*@formatter:off*/ Logger.e(TAG, "update(): \"Error under update transaction under table: $table with whereClause: $whereClause and whereArgs: $whereArgs", e)
                /*@formatter:on*/
            } finally {
                try {
                    writableDb.endTransaction() // May throw if transaction was never opened or DB is full.
                } catch (e: IllegalStateException) {
                    /*@formatter:off*/ Logger.e(TAG, "update(): Error closing transaction! ", e)
                    /*@formatter:on*/
                } catch (e: SQLiteException) {
                    /*@formatter:off*/ Logger.e(TAG, "update(): Error closing transaction! ", e)
                    /*@formatter:on*/
                }
            }
        }
        return result
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<String?>?) : Int {
        val writableDb = getSQLiteDatabaseWithRetries()

        var count = 0

        synchronized(LOCK) {
            try {
                writableDb.beginTransaction()
                count = writableDb.delete(table, whereClause, whereArgs)
                writableDb.setTransactionSuccessful()
            } catch (e: SQLiteException) {
                /*@formatter:off*/ Logger.e(TAG, "delete(): Error deleting on table: $table with whereClause: $whereClause and whereArgs: $whereArgs", e)
                /*@formatter:on*/
            } catch (e: IllegalStateException) {
                /*@formatter:off*/ Logger.e(TAG, "delete(): Error under delete transaction under table: $table with whereClause: $whereClause and whereArgs: $whereArgs", e)
                /*@formatter:on*/
            } finally {
                try {
                    writableDatabase.endTransaction() // May throw if transaction was never opened or DB is full.
                } catch (e: IllegalStateException) {
                    /*@formatter:off*/ Logger.i(TAG, "delete(): Error closing transaction! ", e)
                    /*@formatter:on*/
                } catch (e: SQLiteException) {
                    /*@formatter:off*/ Logger.i(TAG, "delete(): Error closing transaction! ", e)
                    /*@formatter:on*/
                }
            }
        }
        return count
    }

    override fun getRowCount(tableName: String): Long {
        var count: Long = 0

        try {
            count = DatabaseUtils.queryNumEntries(getSQLiteDatabaseWithRetries(), tableName)
        } catch (t: Throwable) {
            handleSQLiteError("Unable to get a number of rows in the table.", t)
        }
        return count
    }

    /**
     * Retry backoff logic based attempt to call [SQLiteOpenHelper.getWritableDatabase] until too many attempts or
     * until [SQLiteCantOpenDatabaseException] or [SQLiteDatabaseLockedException] aren't thrown
     * <br></br><br></br>
     * @see getSQLiteDatabase
     */
    private fun getSQLiteDatabaseWithRetries(): SQLiteDatabase {
        synchronized(LOCK) {
            var firstSQLiteException: SQLiteException? = null
            var count = 0
            while (true) {
                try {
                    return getSQLiteDatabase()
                } catch (e: SQLiteCantOpenDatabaseException) {
                    if (firstSQLiteException == null) {
                        firstSQLiteException = SQLiteException(e.message)
                    }
                    if (++count >= DB_OPEN_RETRY_MAX) throw firstSQLiteException
                    SystemClock.sleep((count * DB_OPEN_RETRY_BACKOFF).toLong())
                } catch (e: SQLiteDatabaseLockedException) {
                    if (firstSQLiteException == null) {
                        firstSQLiteException = SQLiteException(e.message)
                    }
                    if (++count >= DB_OPEN_RETRY_MAX) throw firstSQLiteException
                    SystemClock.sleep((count * DB_OPEN_RETRY_BACKOFF).toLong())
                }
            }
        }
    }

    /**
     * Call this method each time you remove any record from Event table (Child table)
     */
    override fun cleanUnlinkedEvents() {
        val rawQuery = "DELETE FROM ${DbSchema.EventsSchema.TABLE_NAME_EVENTS} WHERE ${DbSchema.EventsSchema.COLUMN_EVENTS_ID} NOT IN " +
                "(SELECT ${DbSchema.EventsSchema.COLUMN_EVENTS_ID} FROM ${DbSchema.EventSchema.TABLE_NAME_EVENT})"
        getSQLiteDatabaseWithRetries().execSQL(rawQuery)
    }

    /**
     * Call this method each time you remove any record from Event table (Child table)
     */
    override fun cleanUnlinkedRecomEvents() {
        val rawQuery = "DELETE FROM ${DbSchema.RecomEventsSchema.TABLE_NAME_RECOM_EVENTS} WHERE ${DbSchema.RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} NOT IN " +
                "(SELECT ${DbSchema.RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} FROM ${DbSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT})"
        getSQLiteDatabaseWithRetries().execSQL(rawQuery)
    }

    /**
     * Should be used in the event that we don't want to retry getting the a [SQLiteDatabase] instance
     * Replaced all [SQLiteOpenHelper.getReadableDatabase] with [SQLiteOpenHelper.getWritableDatabase]
     * as the internals call the same method and not much of a performance benefit between them
     * <br></br><br></br>
     * [getSQLiteDatabaseWithRetries] has similar logic and throws the same Exceptions
     * <br></br><br></br>
     * @see [StackOverflow | What are best practices for SQLite on Android](https://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android/3689883.3689883)
     */
    private fun getSQLiteDatabase(): SQLiteDatabase {
        synchronized(LOCK) {
            return try {
                writableDatabase
            } catch (e: SQLiteCantOpenDatabaseException) {
                /*@formatter:off*/ Logger.e(TAG, "getSQLiteDatabase(): ", e)
                /*@formatter:on*/
                throw e
            } catch (e: SQLiteDatabaseLockedException) {
                /*@formatter:off*/ Logger.e(TAG, "getSQLiteDatabase(): ", e)
                /*@formatter:on*/
                throw e
            }
        }
    }

    private fun handleSQLiteError(log: String, t: Throwable) {
        /*@formatter:off*/ Logger.e(TAG, "handleSQLiteError(): $log", t)
        /*@formatter:on*/
    }

    companion object {
        private val LOCK = Any()

        private const val DB_OPEN_RETRY_MAX = 5
        private const val DB_OPEN_RETRY_BACKOFF = 400

        val TAG: String = RetenoDatabaseImpl::class.java.simpleName
    }
}