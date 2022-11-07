package com.reteno.core.data.repository

interface DeeplinkRepository {

    fun triggerWrappedLinkClicked(wrappedLink: String)
}