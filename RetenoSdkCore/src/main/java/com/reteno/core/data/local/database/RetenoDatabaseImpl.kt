package com.reteno.core.data.local.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.*
import android.os.SystemClock
//import net.sqlcipher.database.SQLiteDatabase
//import net.sqlcipher.database.SQLiteOpenHelper
//import net.sqlcipher.DatabaseUtils
import com.reteno.core.data.local.database.DbConstants.COLUMN_TIMESTAMP
import com.reteno.core.data.local.database.DbConstants.DATABASE_NAME
import com.reteno.core.data.local.database.DbConstants.DATABASE_VERSION
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_ADVERTISING_ID
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_APP_VERSION
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_CATEGORY
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_DEVICE_ID
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_DEVICE_MODEL
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_EXTERNAL_USER_ID
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_LANGUAGE_CODE
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_OS_TYPE
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_OS_VERSION
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_PUSH_TOKEN
import com.reteno.core.data.local.database.DbConstants.Device.COLUMN_TIMEZONE
import com.reteno.core.data.local.database.DbConstants.Device.TABLE_NAME_DEVICE
import com.reteno.core.util.Logger

class RetenoDatabaseImpl(context: Context) : RetenoDatabase,
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Create event table.
        db.execSQL(SQL_CREATE_TABLE_DEVICE)
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
        orderBy: String?
    ): Cursor {
        return getSQLiteDatabaseWithRetries().query(
            table,
            columns,
            selection,
            selectionArgs,
            groupBy,
            having,
            orderBy
        )
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

    override fun insert(table: String, nullColumnHack: String?, contentValues: ContentValues) {
        synchronized(LOCK) {
            val writableDb = getSQLiteDatabaseWithRetries()
            try {
                writableDb.beginTransaction()
                writableDb.insert(table, nullColumnHack, contentValues)
                writableDb.setTransactionSuccessful()
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
    }

    override fun insertOrThrow(table: String, nullColumnHack: String?, contentValues: ContentValues) {
        synchronized(LOCK) {
            val writableDb = getSQLiteDatabaseWithRetries()
            try {
                writableDb.beginTransaction()
                writableDb.insertOrThrow(table, nullColumnHack, contentValues)
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
    }

    override fun update(
        table: String, contentValues: ContentValues, whereClause: String?, whereArgs: Array<String?>?
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

    override fun delete(table: String, whereClause: String?, whereArgs: Array<String?>?) {
        val writableDb = getSQLiteDatabaseWithRetries()

        synchronized(LOCK) {
            try {
                writableDb.beginTransaction()
                writableDb.delete(table, whereClause, whereArgs)
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
                        firstSQLiteException = e
                    }
                    if (++count >= DB_OPEN_RETRY_MAX) throw firstSQLiteException
                    SystemClock.sleep((count * DB_OPEN_RETRY_BACKOFF).toLong())
                } catch (e: SQLiteDatabaseLockedException) {
                    if (firstSQLiteException == null) {
                        firstSQLiteException = e
                    }
                    if (++count >= DB_OPEN_RETRY_MAX) throw firstSQLiteException
                    SystemClock.sleep((count * DB_OPEN_RETRY_BACKOFF).toLong())
                }
            }
        }
    }

    /**
     * Should be used in the event that we don't want to retry getting the a [SQLiteDatabase] instance
     * Replaced all [SQLiteOpenHelper.getReadableDatabase] with [SQLiteOpenHelper.getWritableDatabase]
     * as the internals call the same method and not much of a performance benefit between them
     * <br></br><br></br>
     * [OneSignalDbHelper.getSQLiteDatabaseWithRetries] has similar logic and throws the same Exceptions
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

        private const val SQL_CREATE_TABLE_DEVICE =
            "CREATE TABLE IF NOT EXISTS $TABLE_NAME_DEVICE(" +
                    "$COLUMN_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "$COLUMN_DEVICE_ID TEXT, " +
                    "$COLUMN_EXTERNAL_USER_ID TEXT, " +
                    "$COLUMN_PUSH_TOKEN TEXT, " +
                    "$COLUMN_CATEGORY TEXT, " +
                    "$COLUMN_OS_TYPE TEXT, " +
                    "$COLUMN_OS_VERSION TEXT, " +
                    "$COLUMN_DEVICE_MODEL TEXT, " +
                    "$COLUMN_APP_VERSION TEXT, " +
                    "$COLUMN_LANGUAGE_CODE TEXT, " +
                    "$COLUMN_TIMEZONE TEXT, " +
                    "$COLUMN_ADVERTISING_ID TEXT" +
                    ")"

        val TAG: String = RetenoDatabaseImpl::class.java.simpleName
    }
}