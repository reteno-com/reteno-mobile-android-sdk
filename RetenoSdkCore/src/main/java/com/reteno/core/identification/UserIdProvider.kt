package com.reteno.core.identification

fun interface UserIdProvider {
    fun getUserId(): String?
}