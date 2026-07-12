package com.reteno.core.data.repository

interface LogEventRepository {

    fun pushLogEvents(limit: Int? = null)
}