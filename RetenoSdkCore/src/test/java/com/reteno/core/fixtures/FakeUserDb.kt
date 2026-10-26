package com.reteno.core.fixtures

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.model.BooleanDb
import com.reteno.core.data.local.model.user.UserDb

internal class FakeUserDb(stored: List<UserDb>) : RetenoDatabaseManagerUser {
    val rows = stored.toMutableList()
    private var nextRowId = stored.size + 100

    override fun insertUser(user: UserDb) {
        rows += user.copy(rowId = "${nextRowId++}")
    }

    override fun getUsers(limit: Int?): List<UserDb> = rows.toList()

    override fun getUnSyncedUserCount(): Long =
        rows.count { it.isSynchronizedWithBackend != BooleanDb.TRUE }.toLong()

    override fun deleteUser(user: UserDb): Boolean = rows.removeAll { it.rowId == user.rowId }

    override fun deleteUsers(users: List<UserDb>) {
        val ids = users.map { it.rowId }.toSet()
        rows.removeAll { it.rowId in ids }
    }
}
