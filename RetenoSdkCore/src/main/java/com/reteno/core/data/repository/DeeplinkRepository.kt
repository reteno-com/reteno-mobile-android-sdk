package com.reteno.core.data.repository

import java.time.ZonedDateTime

interface DeeplinkRepository {

    fun saveWrappedLink(wrappedLink: String)

    fun pushWrappedLink()

    fun clearOldWrappedLinks(outdatedTime: ZonedDateTime)
}