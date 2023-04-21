package com.reteno.core.data.local.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.SystemClock
import com.reteno.core.BuildConfig
import com.reteno.core.data.local.database.schema.*
import com.reteno.core.data.local.database.schema.DbSchema.DATABASE_NAME
import com.reteno.core.data.local.database.schema.DbSchema.DATABASE_VERSION
import com.reteno.core.util.Logger
import net.sqlcipher.Cursor
import net.sqlcipher.DatabaseUtils
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteException
import net.sqlcipher.database.SQLiteOpenHelper

internal class RetenoDatabaseImpl(private val context: Context) : RetenoDatabase,
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val writableDatabase: SQLiteDatabase = getWritableDatabase(BuildConfig.SQL_PASSWORD)

    override fun onOpen(db: SQLiteDatabase?) {
        /*@formatter:off*/ Logger.i(TAG, "onOpen(): ", "db = [" , db , "]")
        /*@formatter:on*/
        super.onOpen(db)
        db?.execSQL("PRAGMA foreign_keys=ON")
    }

    override fun onCreate(db: SQLiteDatabase) {
        /*@formatter:off*/ Logger.i(TAG, "onCreate(): ", "db = [" , db , "]")
        /*@formatter:on*/
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        /*@formatter:off*/ Logger.i(TAG, "onUpgrade(): ", "db = [" , db , "], oldVersion = [" , oldVersion , "], newVersion = [" , newVersion , "]")
        /*@formatter:on*/
        createTables(db)
        if (oldVersion == 1 && newVersion > 1) {
            try {
                db.execSQL(DeviceSchema.SQL_UPGRADE_TABLE_VERSION_2)
            } catch (e: SQLiteException) {
                if (e.toString().startsWith("duplicate column name")) {
                    /*@formatter:off*/ Logger.e(TAG, "onUpgrade(): Ignoring this exception", e)
                    /*@formatter:on*/
                } else {
                    throw e
                }
            }
        }
        if (oldVersion < 4) {
            try {
                /*@formatter:off*/ Logger.i(TAG, "onUpgrade(): start update table \"Interaction\"", "old DB version = ",oldVersion,", newVersion = ",newVersion )
                /*@formatter:on*/
                db.execSQL(InteractionSchema.SQL_UPGRADE_TABLE_VERSION_4)
            } catch (e: SQLiteException) {
                if (e.toString().startsWith("duplicate column name")) {
                    /*@formatter:off*/ Logger.e(TAG, "onUpgrade(): Ignoring this exception", e)
                    /*@formatter:on*/
                } else {
                    throw e
                }
            }
        }
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        /*@formatter:off*/ Logger.i(TAG, "onDowngrade(): ", "db = [", db, "], oldVersion = [", oldVersion, "], newVersion = [", newVersion, "]")
        /*@formatter:on*/
        super.onDowngrade(db, oldVersion, newVersion)
        context.deleteDatabase(DATABASE_NAME)
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL(DeviceSchema.SQL_CREATE_TABLE)
        db.execSQL(UserSchema.SQL_CREATE_TABLE)
        db.execSQL(UserSchema.UserAttributesSchema.SQL_CREATE_TABLE)
        db.execSQL(UserSchema.UserAddressSchema.SQL_CREATE_TABLE)
        db.execSQL(InteractionSchema.SQL_CREATE_TABLE)
        db.execSQL(EventsSchema.SQL_CREATE_TABLE)
        db.execSQL(EventsSchema.EventSchema.SQL_CREATE_TABLE)
        db.execSQL(AppInboxSchema.SQL_CREATE_TABLE)
        db.execSQL(RecomEventsSchema.SQL_CREATE_TABLE)
        db.execSQL(RecomEventsSchema.RecomEventSchema.SQL_CREATE_TABLE)
        db.execSQL(WrappedLinkSchema.SQL_CREATE_TABLE)
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
    ): Cursor {
        try {
            return getSQLiteDatabaseWithRetries().query(
                table,
                columns,
                selection,
                selectionArgs,
                groupBy,
                having,
                orderBy,
                limit
            )
        } catch (ex: SQLiteException) {
            attemptToMitigateSqlException(ex)
            throw ex
        }
    }


    override fun rawQuery(rawQuery: String, selectionArgs: Array<out String>?): Cursor {
        try {
            return getSQLiteDatabaseWithRetries().rawQuery(rawQuery, selectionArgs)
        } catch (ex: SQLiteException) {
            attemptToMitigateSqlException(ex)
            throw ex
        }
    }


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
                attemptToMitigateSqlException(e)
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
                    attemptToMitigateSqlException(e)
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
                attemptToMitigateSqlException(e)
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
                    attemptToMitigateSqlException(e)
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
                attemptToMitigateSqlException(e)
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
                    attemptToMitigateSqlException(e)
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
                attemptToMitigateSqlException(e)
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
                    attemptToMitigateSqlException(e)
                }
            }
        }
        return result
    }

    override fun delete(table: String, whereClause: String?, whereArgs: Array<String?>?): Int {
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
                attemptToMitigateSqlException(e)
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
                    attemptToMitigateSqlException(e)
                }
            }
        }
        return count
    }

    override fun getRowCount(tableName: String): Long {
        var count: Long = 0

        try {
            count = DatabaseUtils.queryNumEntries(getSQLiteDatabaseWithRetries(), tableName)
        } catch (ex: SQLiteException) {
            /*@formatter:off*/ Logger.e(TAG, "getRowCount(): ", ex)
            /*@formatter:on*/
            attemptToMitigateSqlException(ex)
        } catch (t: Throwable) {
            /*@formatter:off*/ Logger.e(TAG, "getRowCount(): ", t)
            /*@formatter:on*/
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
        try {
            val sqlQuery =
                "DELETE FROM ${EventsSchema.TABLE_NAME_EVENTS} WHERE ${EventsSchema.COLUMN_EVENTS_ID} NOT IN " +
                        "(SELECT ${EventsSchema.COLUMN_EVENTS_ID} FROM ${EventsSchema.EventSchema.TABLE_NAME_EVENT})"
            getSQLiteDatabaseWithRetries().execSQL(sqlQuery)
        } catch (ex: SQLiteException) {
            attemptToMitigateSqlException(ex)
        }
    }

    /**
     * Call this method each time you remove any record from Event table (Child table)
     */
    override fun cleanUnlinkedRecomVariantIds() {
        try {
            val sqlQuery =
                "DELETE FROM ${RecomEventsSchema.TABLE_NAME_RECOM_EVENTS} WHERE ${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} NOT IN " +
                        "(SELECT ${RecomEventsSchema.COLUMN_RECOM_VARIANT_ID} FROM ${RecomEventsSchema.RecomEventSchema.TABLE_NAME_RECOM_EVENT})"
            getSQLiteDatabaseWithRetries().execSQL(sqlQuery)
        } catch (ex: SQLiteException) {
            attemptToMitigateSqlException(ex)
        }
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

    private fun attemptToMitigateSqlException(ex: SQLiteException) {
        if (ex.message?.contains("no such table") == true) {
            createTables(getSQLiteDatabase())
            /*@formatter:off*/ Logger.e(TAG, "attemptToMitigateSqlException(): Create tables SUCCESS", ex)
            /*@formatter:on*/
        } else if (ex.message?.contains("no such column") == true) {
            onUpgrade(getSQLiteDatabase(), 1, Integer.MAX_VALUE)
            /*@formatter:off*/ Logger.e(TAG, "attemptToMitigateSqlException(): Upgrade database SUCCESS", ex)
            /*@formatter:on*/
        }
    }

    companion object {
        private val LOCK = Any()

        private const val DB_OPEN_RETRY_MAX = 5
        private const val DB_OPEN_RETRY_BACKOFF = 400

        private val TAG: String = RetenoDatabaseImpl::class.java.simpleName
    }
}