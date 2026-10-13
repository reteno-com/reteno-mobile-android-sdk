package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppInteraction
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.STUB_TIME
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
import java.io.IOException

internal class RecordInAppInteractionActionTest {

    private class SutFixture(scope: TestScope) {
        val inAppInteractionsDb = mockk<RetenoDatabaseManagerInAppInteraction>()
        val sync = FakeSyncTrigger()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var insertedOnIo = false
        val sut = RecordInAppInteractionAction(inAppInteractionsDb, sync, dispatchers, traced.runner)

        init {
            every { inAppInteractionsDb.insertInteraction(any()) } answers { insertedOnIo = dispatchers.markedIo.isRunningHere }
        }
    }

    @Test
    fun `should save in-app interaction and request sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(INTERACTION)

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.inAppInteractionsDb.insertInteraction(INTERACTION.toDb()) }
        assertTrue(fixture.insertedOnIo)
        assertEquals(1, fixture.sync.requests)
    }

    @Test
    fun `should not request sync when in-app interaction cannot be saved`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.inAppInteractionsDb.insertInteraction(any()) } throws IOException("disk full")

        whenn()
        val result = fixture.sut(INTERACTION)

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(0, fixture.sync.requests)
    }

    @Test
    fun `should trace status without failure description`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(INTERACTION)

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("RecordInAppInteraction  id=in-app-1  messageInstanceId=42  status=FAILED"))
        assertFalse(lines.any { it.contains("widget crashed") })
    }

    private companion object {
        val INTERACTION = InAppInteraction("in-app-1", STUB_TIME, 42L, InAppInteractionStatus.FAILED, "widget crashed")
    }
}
