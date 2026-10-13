package com.reteno.core.internal.action.interaction

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInteraction
import com.reteno.core.data.local.model.interaction.InteractionRequestDb
import com.reteno.core.data.local.model.interaction.InteractionStatusDb
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.fixtures.FakeSyncTrigger
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProcessCachedInteractionsActionTest {

    private class SutFixture(scope: TestScope, vararg stored: InteractionRequestDb) {
        val requests = stored.toMutableList()
        val recorded = mutableListOf<Pair<String, InteractionStatus>>()
        val failingIds = mutableSetOf<String>()
        val idsWithoutToken = mutableSetOf<String>()
        var loadedOnIo = false
        var deletedOnIo = false
        val dispatchers = scope.testDispatchers()
        val interactionsDb = mockk<RetenoDatabaseManagerInteraction>().also {
            every { it.getInteractionRequests() } answers {
                loadedOnIo = dispatchers.markedIo.isRunningHere
                requests.toList()
            }
            every { it.deleteInteractionRequest(any()) } answers {
                deletedOnIo = dispatchers.markedIo.isRunningHere
                requests.remove(firstArg())
            }
        }
        val recordInteraction = mockk<RecordInteraction>().also {
            coEvery { it(any(), any()) } coAnswers {
                delay(10)
                val id = firstArg<String>()
                when (id) {
                    in failingIds -> Result.failure(IOException("disk full"))
                    in idsWithoutToken -> Result.success(false)
                    else -> Result.success(true).also { recorded += id to secondArg<InteractionStatus>() }
                }
            }
        }
        val sync = FakeSyncTrigger()
        val traced = TestActionRunner(scope)
        val sut = ProcessCachedInteractionsAction(interactionsDb, recordInteraction, sync, dispatchers, traced.runner)
    }

    @Test
    fun `should record every cached interaction and delete its request`() = runUnitTest {
        given()
        val fixture = SutFixture(this, DELIVERED, CLICKED)

        whenn()
        val result = fixture.sut()

        then()
        assertTrue(result.isSuccess)
        assertEquals(listOf("delivered-1" to InteractionStatus.DELIVERED, "clicked-1" to InteractionStatus.CLICKED), fixture.recorded)
        assertTrue(fixture.requests.isEmpty())
        assertTrue(fixture.loadedOnIo)
        assertTrue(fixture.deletedOnIo)
    }

    @Test
    fun `should request one sync after recording cached interactions`() = runUnitTest {
        given()
        val fixture = SutFixture(this, DELIVERED, CLICKED)

        whenn()
        fixture.sut()

        then()
        assertEquals(1, fixture.sync.requests)
    }

    @Test
    fun `should do nothing when no interaction is cached`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut()

        then()
        assertTrue(fixture.recorded.isEmpty())
        assertEquals(0, fixture.sync.requests)
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ no cached interactions") })
    }

    @Test
    fun `should keep request whose interaction could not be recorded`() = runUnitTest {
        given()
        val fixture = SutFixture(this, DELIVERED, CLICKED)
        fixture.failingIds += "delivered-1"

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf(DELIVERED), fixture.requests)
        assertEquals(listOf("clicked-1" to InteractionStatus.CLICKED), fixture.recorded)
        assertEquals(1, fixture.sync.requests)
    }

    @Test
    fun `should keep request whose interaction has no push token yet`() = runUnitTest {
        given()
        val fixture = SutFixture(this, DELIVERED)
        fixture.idsWithoutToken += "delivered-1"

        whenn()
        fixture.sut()

        then()
        assertEquals(listOf(DELIVERED), fixture.requests)
        assertEquals(0, fixture.sync.requests)
    }

    @Test
    fun `should not request sync when no cached interaction could be recorded`() = runUnitTest {
        given()
        val fixture = SutFixture(this, DELIVERED)
        fixture.failingIds += "delivered-1"

        whenn()
        fixture.sut()

        then()
        assertEquals(0, fixture.sync.requests)
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ interaction kept for next start") })
    }

    @Test
    fun `should record each cached interaction once when processed concurrently`() = runUnitTest {
        given()
        val fixture = SutFixture(this, DELIVERED)

        whenn()
        launch { fixture.sut() }
        launch { fixture.sut() }
        advanceUntilIdle()

        then()
        assertEquals(listOf("delivered-1" to InteractionStatus.DELIVERED), fixture.recorded)
    }

    private companion object {
        val DELIVERED = InteractionRequestDb(rowId = "1", id = "delivered-1", status = InteractionStatusDb.DELIVERED)
        val CLICKED = InteractionRequestDb(rowId = "2", id = "clicked-1", status = InteractionStatusDb.CLICKED)
    }
}
