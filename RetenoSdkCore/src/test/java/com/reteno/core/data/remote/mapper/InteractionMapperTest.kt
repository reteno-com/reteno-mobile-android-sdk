package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.data.remote.model.interaction.InteractionStatusRemote
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.fixtures.stubInAppInteractionDb
import com.reteno.core.fixtures.stubInteractionDb
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class InteractionMapperTest {

    @Test
    fun `should send interaction with its stored action`() = runUnitTest {
        given()
        val storedAction = """{"type":"OPEN_URL","targetComponentId":"btn","url":"u"}"""
        val interaction = stubInteractionDb().copy(action = storedAction)

        whenn()
        val remote = interaction.toRemote()

        then()
        assertEquals(
            listOf(InteractionStatusRemote.CLICKED, "2026-01-02T03:04:05Z", "token"),
            listOf(remote.status, remote.time, remote.token)
        )
        val action = requireNotNull(remote.action)
        assertEquals(listOf("OPEN_URL", "btn", "u"), listOf(action.type, action.targetComponentId, action.url))
    }

    @Test
    fun `should send interaction without action`() = runUnitTest {
        given()
        val interaction = stubInteractionDb()

        whenn()
        val remote = interaction.toRemote()

        then()
        assertNull(remote.action)
    }

    @Test
    fun `should send every interaction status`() = runUnitTest {
        given()
        val statuses = InteractionStatusDb.values().toList()

        whenn()
        val sent = statuses.map { it.toRemote() }

        then()
        assertEquals(
            listOf(InteractionStatusRemote.DELIVERED, InteractionStatusRemote.CLICKED, InteractionStatusRemote.OPENED),
            sent
        )
    }

    @Test
    fun `should send every in-app interaction field`() = runUnitTest {
        given()
        val interaction = stubInAppInteractionDb().copy(statusDescription = "script error")

        whenn()
        val remote = interaction.toRemote()

        then()
        assertEquals(
            listOf("in-app-1", "2026-01-02T03:04:05Z", 7L, "OPEN", "script error"),
            listOf(remote.interactionId, remote.time, remote.messageInstanceId, remote.status, remote.statusDescription)
        )
    }

    @Test
    fun `should keep stored action of interaction readable as domain action`() = runUnitTest {
        given()
        val action = InteractionAction("CLICK", null, null)

        whenn()
        val remote = stubInteractionDb().copy(action = action.toJson()).toRemote()

        then()
        assertEquals(action, remote.action)
    }
}
