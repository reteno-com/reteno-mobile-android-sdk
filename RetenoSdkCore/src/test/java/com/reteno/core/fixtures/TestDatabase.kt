package com.reteno.core.fixtures

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.data.local.database.RetenoDatabaseImpl
import com.reteno.core.data.local.database.schema.DbSchema
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger

internal class TestDatabase private constructor(val database: RetenoDatabaseImpl, val remoteLog: FakeRemoteLog) {

    fun execute(statement: String, vararg args: Any?) {
        database.writableDatabase.execSQL(statement, args)
    }

    fun count(table: String): Long = database.getRowCount(table)

    fun dropTable(table: String) {
        execute("DROP TABLE $table")
    }

    fun values(sql: String): List<String?> = database.rawQuery(sql).use { cursor ->
        val values = mutableListOf<String?>()
        while (cursor.moveToNext()) {
            values += cursor.getString(0)
        }
        values
    }

    fun stampRows(table: String, timestamp: String = STORED_AT) {
        execute("UPDATE $table SET ${DbSchema.COLUMN_TIMESTAMP} = ?", timestamp)
    }

    companion object {
        const val STORED_AT = "2026-01-02 03:04:05"

        suspend fun <T> open(block: suspend TestDatabase.() -> T): T {
            val application = ApplicationProvider.getApplicationContext<Application>()
            application.deleteDatabase(DbSchema.DATABASE_NAME)
            val remoteLog = FakeRemoteLog()
            val database = RetenoDatabaseImpl(application) { }
            Logger.installCapturing(FakeLogSink(), remoteLog)
            try {
                return TestDatabase(database, remoteLog).block()
            } finally {
                Logger.install(AndroidLogSink(), null)
                database.close()
            }
        }
    }
}
