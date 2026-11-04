package com.reteno.core.data.remote.mapper

import com.reteno.core.data.remote.model.inbox.InboxMessageRemote
import com.reteno.core.data.remote.model.inbox.InboxMessageStatusRemote
import com.reteno.core.data.remote.model.inbox.InboxMessagesRemote
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AppInboxMapperTest {

    @Test
    fun `should show every field of an inbox message`() = runUnitTest {
        given()
        val remote = message(status = InboxMessageStatusRemote.OPENED)

        whenn()
        val message = remote.toDomain()

        then()
        val expected = listOf(
            "m-1", "Sale", "2026-01-02", true, "body", "https://img", "https://link", "promo",
            AppInboxStatus.OPENED, mapOf("k" to "v")
        )
        val actual = listOf(
            message.id, message.title, message.createdDate, message.isNewMessage, message.content,
            message.imageUrl, message.linkUrl, message.category, message.status, message.customData
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `should show unopened and unknown message status`() = runUnitTest {
        given()
        val messages = listOf(message(status = InboxMessageStatusRemote.UNOPENED), message(status = null))

        whenn()
        val statuses = messages.map { it.toDomain().status }

        then()
        assertEquals(listOf(AppInboxStatus.UNOPENED, null), statuses)
    }

    @Test
    fun `should show messages of a page with their page count`() = runUnitTest {
        given()
        val page = InboxMessagesRemote(messages = listOf(message()), totalPages = 4)

        whenn()
        val messages = page.toDomain()

        then()
        assertEquals(listOf("m-1"), messages.messages.map { it.id })
        assertEquals(4, messages.totalPages)
    }

    @Test
    fun `should count a page without page count as the only page`() = runUnitTest {
        given()
        val page = InboxMessagesRemote(messages = emptyList(), totalPages = null)

        whenn()
        val messages = page.toDomain()

        then()
        assertEquals(1, messages.totalPages)
    }

    private fun message(status: InboxMessageStatusRemote? = null) = InboxMessageRemote(
        content = "body",
        createdDate = "2026-01-02",
        id = "m-1",
        imageUrl = "https://img",
        linkUrl = "https://link",
        isNewMessage = true,
        title = "Sale",
        category = "promo",
        status = status,
        customData = mapOf("k" to "v")
    )
}
