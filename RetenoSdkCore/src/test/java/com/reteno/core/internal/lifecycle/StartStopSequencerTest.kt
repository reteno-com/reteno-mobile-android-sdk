package com.reteno.core.internal.lifecycle

import com.reteno.core.internal.action.init.StartSdk
import com.reteno.core.internal.action.init.StopSdk
import com.reteno.core.internal.coroutine.InitGate
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class StartStopSequencerTest {

    private class SutFixture(scope: TestScope, initialized: Boolean = true) {
        val initGate = InitGate().also { if (initialized) it.markInitialized(byUser = true) }
        val calls = mutableListOf<String>()
        var suspendStart = false
        val state = SdkRunState()
        private val startSdk = mockk<StartSdk>().also {
            coEvery { it() } coAnswers {
                calls += "start"
                if (suspendStart) {
                    try {
                        awaitCancellation()
                    } finally {
                        calls += "start cancelled"
                    }
                }
                Result.success(Unit)
            }
        }
        private val stopSdk = mockk<StopSdk>().also { coEvery { it() } coAnswers { calls += "stop"; Result.success(Unit) } }
        val sut = StartStopSequencer(scope.workScope(), initGate, startSdk, stopSdk, state)
    }

    @Test
    fun `should run start immediately when sdk is initialized by user`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.start()

        then()
        assertEquals(listOf("start"), fixture.calls)
    }

    @Test
    fun `should wait for user initialization before starting`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        fixture.sut.start()
        runCurrent()
        val beforeInit = fixture.calls.toList()

        whenn()
        fixture.initGate.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), beforeInit)
        assertEquals(listOf("start"), fixture.calls)
    }

    @Test
    fun `should cancel unfinished start before stopping`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.suspendStart = true
        fixture.sut.start()

        whenn()
        fixture.sut.stop()
        runCurrent()

        then()
        assertEquals(listOf("start", "start cancelled", "stop"), fixture.calls)
    }

    @Test
    fun `should keep requests in call order while waiting for initialization`() = runUnitTest {
        given()
        val fixture = SutFixture(this, initialized = false)
        fixture.sut.stop()
        fixture.sut.start()
        fixture.sut.stop()
        fixture.sut.start()

        whenn()
        fixture.initGate.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(listOf("stop", "stop", "start"), fixture.calls)
    }

    @Test
    fun `should report started state of the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val beforeStart = fixture.sut.isStarted

        whenn()
        fixture.state.markStarted()

        then()
        assertEquals(false, beforeStart)
        assertEquals(true, fixture.sut.isStarted)
    }
}
