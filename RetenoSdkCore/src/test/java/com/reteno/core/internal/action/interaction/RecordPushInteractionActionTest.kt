package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.model.interaction.InteractionRequestDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecordPushInteractionActionTest {

    private class SutFixture(scope: TestScope) {
        val calls = mutableListOf<String>()
        val dispatchers = scope.testDispatchers()
        val initGate = InitGate()
        val interactionsDb = mockk<RetenoDatabaseManagerInteraction>().also {
            every { it.insertInteractionRequest(any()) } answers {
                val request = firstArg<InteractionRequestDb>()
                calls += "save ${request.id} ${request.status}" + if (dispatchers.markedIo.isRunningHere) " on io" else ""
            }
        }
        val processCachedInteractions = mockk<ProcessCachedInteractions>().also {
            coEvery { it() } answers {
                calls += "process cached interactions"
                Result.success(Unit)
            }
        }
        val traced = TestActionRunner(scope)
        val sut = RecordPushInteractionAction(interactionsDb, initGate, processCachedInteractions, dispatchers, traced.runner)
    }

    @Test
    fun `should save interaction request and process it once sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        launch { fixture.sut("interaction-1", InteractionStatus.DELIVERED) }
        runCurrent()
        val beforeInit = fixture.calls.toList()

        whenn()
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(listOf("save interaction-1 ${InteractionStatusDb.DELIVERED} on io"), beforeInit)
        assertEquals(listOf("save interaction-1 ${InteractionStatusDb.DELIVERED} on io", "process cached interactions"), fixture.calls)
    }

    @Test
    fun `should process saved interaction right away when sdk is initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.initGate.markInitialized(byUser = true)

        whenn()
        val result = fixture.sut("interaction-1", InteractionStatus.CLICKED)

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf("save interaction-1 ${InteractionStatusDb.CLICKED} on io", "process cached interactions"), fixture.calls)
        assertTrue(fixture.traced.sink.lines.first().contains("RecordPushInteraction  id=interaction-1  status=CLICKED"))
        assertTrue(fixture.traced.sink.lines.any { it.contains("✓ wait for sdk initialization") })
    }
}
