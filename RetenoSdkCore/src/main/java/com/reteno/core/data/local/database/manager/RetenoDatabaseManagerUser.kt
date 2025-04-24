package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.user.UserDb

interface RetenoDatabaseManagerUser {
    fun insertUser(user: UserDb)
    fun getUsers(limit: Int? = null): List<UserDb>
    fun getUnSyncedUserCount(): Long
    fun deleteUser(user: UserDb): Boolean
    fun deleteUsers(users: List<UserDb>)
}