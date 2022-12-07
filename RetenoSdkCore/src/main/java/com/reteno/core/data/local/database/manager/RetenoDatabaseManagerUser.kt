package com.reteno.core.data.local.database.manager

import com.reteno.core.data.local.model.user.UserDb

interface RetenoDatabaseManagerUser {
    fun insertUser(user: UserDb)
    fun getUser(limit: Int? = null): List<UserDb>
    fun getUserCount(): Long
    fun deleteUsers(count: Int, oldest: Boolean = true)
}