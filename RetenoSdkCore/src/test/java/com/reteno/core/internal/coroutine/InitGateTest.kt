package com.reteno.core.internal.coroutine

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
    fun `should release init waiters when initialized from cache`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val waiter = async { fixture.sut.awaitInit() }
        runCurrent()
        assertFalse(waiter.isCompleted)

        whenn()
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        then()
        assertTrue(waiter.isCompleted)
        assertEquals(InitState.FROM_CACHE, fixture.sut.state)
        assertTrue(fixture.sut.isInitialized)
        assertFalse(fixture.sut.isInitializedByUser)
    }

    @Test
    fun `should keep user init waiters suspended when initialized from cache`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val waiter = async { fixture.sut.awaitUserInit() }

        whenn()
        fixture.sut.markInitialized(byUser = false)
        runCurrent()

        then()
        assertFalse(waiter.isCompleted)
        waiter.cancel()
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
        val waiters = (0..3).map { index -> launch { fixture.sut.awaitInit(); released += index } }
        runCurrent()
        waiters.first().cancel()
        runCurrent()
        launch { fixture.sut.awaitInit(); released += 4 }
        runCurrent()

        whenn()
        fixture.sut.markInitialized(byUser = true)
        runCurrent()

        then()
        assertEquals(listOf(1, 2, 3, 4), released)
    }
}
