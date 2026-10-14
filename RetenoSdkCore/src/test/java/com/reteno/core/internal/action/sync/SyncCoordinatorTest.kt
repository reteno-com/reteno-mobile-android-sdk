package com.reteno.core.internal.action.sync

import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SyncCoordinatorTest {

    private class SutFixture(scope: TestScope, ready: Boolean = true) {
        val scopes = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        var outcome: Result<StepOutcome> = Result.success(StepOutcome.CONTINUE)
        var crash: Throwable? = null
        private val traced = TestActionRunner(scope)
        private val contactStep = SyncStep {
            scopes += "contact"
            gate?.await()
            crash?.let { throw it }
            outcome
        }
        private val dataStep = SyncStep {
            scopes += "data"
            Result.success(StepOutcome.CONTINUE)
        }
        val initGate = InitGate().also { if (ready) it.markInitialized(byUser = false) }
        val sut = SyncCoordinator(
            syncData = SyncDataAction(listOf(contactStep), listOf(dataStep), traced.runner),
            scope = scope.workScope(),
            initGate = initGate
        )
    }

    @Test
    fun `should run full sync when requested`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.request()
        runCurrent()

        then()
        assertEquals(listOf("contact", "data"), fixture.scopes)
    }

    @Test
    fun `should fold requests made during a running sync into one follow up sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val gate = CompletableDeferred<Unit>()
        fixture.gate = gate
        fixture.sut.request()
        runCurrent()

        whenn()
        repeat(5) { fixture.sut.request() }
        gate.complete(Unit)
        runCurrent()

        then()
        assertEquals(listOf("contact", "data", "contact", "data"), fixture.scopes)
    }

    @Test
    fun `should return result of immediate contact sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut.syncNow(SyncScope.CONTACT)

        then()
        assertEquals(SyncResult.COMPLETED, result)
        assertEquals(listOf("contact"), fixture.scopes)
    }

    @Test
    fun `should report aborted when immediate sync aborts`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.outcome = Result.success(StepOutcome.ABORT)

        whenn()
        val result = fixture.sut.syncNow()

        then()
        assertEquals(SyncResult.ABORTED, result)
    }

    @Test
    fun `should run immediate sync between steps of a running sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val gate = CompletableDeferred<Unit>()
        fixture.gate = gate
        fixture.sut.request()
        runCurrent()

        whenn()
        val immediate = async { fixture.sut.syncNow(SyncScope.CONTACT) }
        runCurrent()
        val startedBeforeRelease = fixture.scopes.size
        gate.complete(Unit)
        runCurrent()

        then()
        assertEquals(1, startedBeforeRelease)
        assertTrue(immediate.isCompleted)
        assertEquals(listOf("contact", "contact", "data"), fixture.scopes)
    }

    @Test
    fun `should do nothing until requested`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        runCurrent()

        then()
        assertFalse(fixture.scopes.isNotEmpty())
    }

    @Test
    fun `should report aborted when sync itself crashes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.crash = IllegalStateException("step threw instead of returning failure")

        whenn()
        val result = fixture.sut.syncNow()

        then()
        assertEquals(SyncResult.ABORTED, result)
    }

    @Test
    fun `should hold requested sync until sdk is ready`() = runUnitTest {
        given()
        val fixture = SutFixture(this, ready = false)

        whenn()
        fixture.sut.request()
        runCurrent()
        val beforeReady = fixture.scopes.toList()
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertTrue(beforeReady.isEmpty())
        assertEquals(listOf("contact", "data"), fixture.scopes)
    }

    @Test
    fun `should not wait for readiness on immediate sync`() = runUnitTest {
        given()
        val fixture = SutFixture(this, ready = false)

        whenn()
        val result = fixture.sut.syncNow(SyncScope.CONTACT)

        then()
        assertEquals(SyncResult.COMPLETED, result)
    }
}
