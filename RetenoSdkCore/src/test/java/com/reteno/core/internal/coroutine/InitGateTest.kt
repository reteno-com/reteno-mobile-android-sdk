package com.reteno.core.internal.coroutine

import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InitGateTest {

    private class SutFixture {
        val sut = InitGate()
    }

    @Test
    fun `should start not initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val state = fixture.sut.state

        then()
        assertEquals(InitState.NOT_INITIALIZED, state)
        assertFalse(fixture.sut.isInitialized)
        assertFalse(fixture.sut.isInitializedByUser)
    }

    @Test
    fun `should keep init waiters suspended while not initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val waiter = workScope().async { fixture.sut.awaitInit() }

        whenn()
        runCurrent()

        then()
        assertFalse(waiter.isCompleted)
    }

    @Test
    fun `should release init waiters when initialized from cache`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val waiter = async { fixture.sut.awaitInit() }
        runCurrent()

        whenn()
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        then()
        assertTrue(waiter.isCompleted)
    }

    @Test
    fun `should report initialized but not by user when initialized from cache`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.markInitialized(byUser = false)

        then()
        assertEquals(InitState.FROM_CACHE, fixture.sut.state)
        assertTrue(fixture.sut.isInitialized)
        assertFalse(fixture.sut.isInitializedByUser)
    }

    @Test
    fun `should keep user init waiters suspended when initialized from cache`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val waiter = workScope().async { fixture.sut.awaitUserInit() }

        whenn()
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        then()
        assertFalse(waiter.isCompleted)
    }

    @Test
    fun `should release all waiters when initialized by user`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val initWaiter = async { fixture.sut.awaitInit() }
        val userWaiter = async { fixture.sut.awaitUserInit() }

        whenn()
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        then()
        assertTrue(initWaiter.isCompleted)
        assertTrue(userWaiter.isCompleted)
        assertTrue(fixture.sut.isInitializedByUser)
    }

    @Test
    fun `should never go back from user init to cache init`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.markInitialized(byUser = true)

        whenn()
        fixture.sut.markInitialized(byUser = false)

        then()
        assertEquals(InitState.BY_USER, fixture.sut.state)
    }

    @Test
    fun `should return immediately when already initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.markInitialized(byUser = true)

        whenn()
        fixture.sut.awaitInit()
        fixture.sut.awaitUserInit()

        then()
        assertTrue(fixture.sut.isInitialized)
    }

    @Test
    fun `should release init waiters in the order they started waiting`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val released = mutableListOf<Int>()
        val waiters = (0..3).map { index ->
            launch {
                fixture.sut.awaitInit()
                released += index
            }
        }
        runCurrent()
        waiters.first().cancel()
        runCurrent()
        launch {
            fixture.sut.awaitInit()
            released += 4
        }
        runCurrent()

        whenn()
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(listOf(1, 2, 3, 4), released)
    }

    @Test
    fun `should hold operations until initialized and then run them in call order`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterInit(workScope()) { ran += "first" }
        fixture.sut.runAfterInit(workScope()) { ran += "second" }
        runCurrent()
        val ranBefore = ran.toList()

        whenn()
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), ranBefore)
        assertEquals(listOf("first", "second"), ran)
    }

    @Test
    fun `should run operation at once on the caller when already initialized`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.markInitialized(byUser = true)
        val ran = mutableListOf<String>()

        whenn()
        fixture.sut.runAfterInit(workScope()) { ran += "now" }

        then()
        assertEquals(listOf("now"), ran)
    }

    @Test
    fun `should run held operation on the dispatcher of the given scope`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val dispatchers = testDispatchers()
        var ranOnMain: Boolean? = null
        fixture.sut.runAfterInit(workScope() + dispatchers.main) { ranOnMain = dispatchers.markedMain.isRunningHere }

        whenn()
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(true, ranOnMain)
    }

    @Test
    fun `should drop held operation when its scope is cancelled before init`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val scope = workScope()
        var ran = false
        fixture.sut.runAfterInit(scope) { ran = true }
        runCurrent()
        scope.cancel()

        whenn()
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        then()
        assertFalse(ran)
    }

    @Test
    fun `should hold host operations while initialized only from cache`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterUserInit(workScope()) { ran += "host call" }

        whenn()
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        then()
        assertTrue(ran.isEmpty())
    }

    @Test
    fun `should run host operations in call order once initialized by user`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterUserInit(workScope()) { ran += "first" }
        fixture.sut.runAfterUserInit(workScope()) { ran += "second" }
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        whenn()
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(listOf("first", "second"), ran)
    }

    @Test
    fun `should run host operation at once on the caller when already initialized by user`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.markInitialized(byUser = true)
        val ran = mutableListOf<String>()

        whenn()
        fixture.sut.runAfterUserInit(workScope()) { ran += "now" }

        then()
        assertEquals(listOf("now"), ran)
    }

    @Test
    fun `should release both kinds of held operations in call order when initialized by user`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterUserInit(workScope()) { ran += "host 1" }
        fixture.sut.runAfterInit(workScope()) { ran += "push 1" }
        fixture.sut.runAfterUserInit(workScope()) { ran += "host 2" }
        runCurrent()

        whenn()
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(listOf("host 1", "push 1", "host 2"), ran)
    }

    @Test
    fun `should release only operations that need init when initialized from cache`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterUserInit(workScope()) { ran += "host" }
        fixture.sut.runAfterInit(workScope()) { ran += "push" }

        whenn()
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(listOf("push"), ran)
    }

    @Test
    fun `should run an operation submitted after init behind held operations that did not run yet`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterUserInit(workScope()) { ran += "held" }
        runCurrent()
        fixture.sut.markInitialized(byUser = true)

        whenn()
        fixture.sut.runAfterUserInit(workScope()) { ran += "submitted after init" }
        val ranBeforeDispatch = ran.toList()
        runCurrent()

        then()
        assertTrue(ranBeforeDispatch.isEmpty())
        assertEquals(listOf("held", "submitted after init"), ran)
    }

    @Test
    fun `should run operations at once again after held operations ran`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterUserInit(workScope()) { ran += "held" }
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        whenn()
        fixture.sut.runAfterUserInit(workScope()) { ran += "now" }

        then()
        assertEquals(listOf("held", "now"), ran)
    }

    @Test
    fun `should run operations at once after a held operation of a cancelled scope was dropped`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val scope = workScope()
        val ran = mutableListOf<String>()
        fixture.sut.runAfterInit(scope) { ran += "dropped" }
        scope.cancel()
        fixture.sut.markInitialized(byUser = true)

        whenn()
        fixture.sut.runAfterInit(workScope()) { ran += "now" }

        then()
        assertEquals(listOf("now"), ran)
    }
}
