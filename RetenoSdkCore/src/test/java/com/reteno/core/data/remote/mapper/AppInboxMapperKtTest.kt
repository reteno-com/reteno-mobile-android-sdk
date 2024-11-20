package com.reteno.core.data.remote.mapper

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.remote.model.inbox.InboxMessageRemote
import com.reteno.core.data.remote.model.inbox.InboxMessageStatusRemote
import com.reteno.core.data.remote.model.inbox.InboxMessagesRemote
import com.reteno.core.domain.model.appinbox.AppInboxMessage
import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.features.appinbox.AppInboxStatus
import junit.framework.TestCase.assertEquals
import org.junit.Test


class AppInboxMapperKtTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val TOTAL_PAGES = 12
        private const val TOTAL_PAGES_DEFAULT = 1

        private const val MESSAGE_CONTENT = "MESSAGE_CONTENT"
        private const val CREATED_DATE = "CREATED_DATE"
        private const val ID = "ID"
        private const val IMAGE_URL = "IMAGE_URL"
        private const val LINK_URL = "LINK_URL"
        private const val IS_NEW_MESSAGE = true
        private const val TITLE = "TITLE"
        private const val CATEGORY = "CATEGORY"
        private val CUSTOM_DATA = mapOf("custom_key" to "custom_value")
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenAppInboxMessageRemote_whenToDomain_thenAppInboxMessageDomainReturned() {
        // Given
        val appInboxMessageRemote = getAppInboxMessageRemote()
        val appInboxMessageExpected = getAppInboxMessageDomain()

        // When
        val appInboxMessageActual = appInboxMessageRemote.toDomain()

        // Then
        assertEquals(appInboxMessageExpected, appInboxMessageActual)
    }

    @Test
    fun givenAppInboxMessagesTotalPagesNotNullRemote_whenToDomain_thenAppInboxMessagesDomainReturned() {
        // Given
        val appInboxMessagesRemote = getAppInboxMessagesRemote()
        val appInboxMessagesExpected = getAppInboxMessagesDomain()

        // When
        val appInboxMessageActual = appInboxMessagesRemote.toDomain()

        // Then
        assertEquals(appInboxMessagesExpected, appInboxMessageActual)
    }

    @Test
    fun givenAppInboxMessagesTotalPagesNullRemote_whenToDomain_thenAppInboxMessagesDomainTotalPagesDefaultReturned() {
        // Given
        val appInboxMessagesRemote = getAppInboxMessagesRemoteTotalPagesNull()
        val appInboxMessagesExpected = getAppInboxMessagesDomainTotalPagesDefault()

        // When
        val appInboxMessageActual = appInboxMessagesRemote.toDomain()

        // Then
        assertEquals(appInboxMessagesExpected, appInboxMessageActual)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getAppInboxMessagesRemote() = InboxMessagesRemote(
        messages = listOf(getAppInboxMessageRemote()),
        totalPages = TOTAL_PAGES
    )

    private fun getAppInboxMessagesDomain() = AppInboxMessages(
        messages = listOf(getAppInboxMessageDomain()),
        totalPages = TOTAL_PAGES
    )

    private fun getAppInboxMessagesRemoteTotalPagesNull() = InboxMessagesRemote(
        messages = listOf(getAppInboxMessageRemote()),
        totalPages = null
    )

    private fun getAppInboxMessagesDomainTotalPagesDefault() = AppInboxMessages(
        messages = listOf(getAppInboxMessageDomain()),
        totalPages = TOTAL_PAGES_DEFAULT
    )

    private fun getAppInboxMessageRemote() = InboxMessageRemote(
        content = MESSAGE_CONTENT,
        createdDate = CREATED_DATE,
        id = ID,
        imageUrl = IMAGE_URL,
        linkUrl = LINK_URL,
        isNewMessage = IS_NEW_MESSAGE,
        title = TITLE,
        category = CATEGORY,
        status = InboxMessageStatusRemote.UNOPENED,
        customData = CUSTOM_DATA
    )

    private fun getAppInboxMessageDomain() = AppInboxMessage(
        content = MESSAGE_CONTENT,
        createdDate = CREATED_DATE,
        id = ID,
        imageUrl = IMAGE_URL,
        linkUrl = LINK_URL,
        isNewMessage = IS_NEW_MESSAGE,
        title = TITLE,
        category = CATEGORY,
        status = AppInboxStatus.UNOPENED,
        customData = CUSTOM_DATA
    )
    // endregion helper methods --------------------------------------------------------------------
}