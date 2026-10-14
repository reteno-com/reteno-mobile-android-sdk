package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.interaction.Interaction
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RecordInteractionClickActionTest {

    private class SutFixture(scope: TestScope) {
        val interactionsDb = mockk<RetenoDatabaseManagerInteraction>()
        val sync = FakeSyncTrigger()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var insertedOnIo = false
        val sut = RecordInteractionClickAction(interactionsDb, sync, dispatchers, { TIME }, traced.runner)

        init {
            every { interactionsDb.insertInteraction(any()) } answers {
                insertedOnIo = dispatchers.markedIo.isRunningHere
            }
        }
    }

    @Test
    fun `should save clicked interaction with its action to local database`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", ACTION, forcePush = false)

        then()
        val expected = Interaction(InteractionStatus.CLICKED, TIME, action = ACTION).toDb("interaction-1")
        verify(exactly = 1) { fixture.interactionsDb.insertInteraction(expected) }
    }

    @Test
    fun `should save clicked interaction on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", ACTION, forcePush = false)

        then()
        assertTrue(fixture.insertedOnIo)
    }

    @Test
    fun `should not request sync when force push is not set`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", ACTION, forcePush = false)

        then()
        assertEquals(0, fixture.sync.requests)
    }

    @Test
    fun `should request sync when force push is set`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", ACTION, forcePush = true)

        then()
        assertEquals(1, fixture.sync.requests)
    }

    @Test
    fun `should trace interaction id action type and force push`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", ACTION, forcePush = true)

        then()
        val expected = "RecordInteractionClick  id=interaction-1  action=OPEN_URL  forcePush=true"
        assertTrue(fixture.traced.sink.lines.first().contains(expected))
    }

    @Test
    fun `should not write action url to trace`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut("interaction-1", ACTION, forcePush = true)

        then()
        assertFalse(fixture.traced.sink.lines.any { it.contains("example.com") })
    }

    private companion object {
        const val TIME = "2026-01-02T03:04:05Z"
        val ACTION = InteractionAction("OPEN_URL", "button-1", "https://example.com/offer")
    }
}
