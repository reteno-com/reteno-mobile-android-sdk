package com.reteno.core.internal.action.event

import com.reteno.core.data.local.config.RestConfig
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerEvents
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.domain.model.ecom.asRegularEvent
import com.reteno.core.fixtures.FailureReport
import com.reteno.core.fixtures.RecordingEventBus
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubDeviceId
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class LogEcomEventActionTest {

    private class SutFixture(scope: TestScope) {
        val eventsDb = mockk<RetenoDatabaseManagerEvents>()
        val deviceId = stubDeviceId()
        val events = RecordingEventBus()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val restConfig = mockk<RestConfig>().also { every { it.deviceId } returns deviceId }
        val sut = LogEcomEventAction(eventsDb, restConfig, events.bus, dispatchers, traced.runner)
    }

    @Test
    fun `should insert ecommerce event with its parameters for current device`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubEcomEvent()
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        val result = fixture.sut(event)

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) { fixture.eventsDb.insertEvents(EventsDb(fixture.deviceId, event.toDb())) }
    }

    @Test
    fun `should insert ecommerce event on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var insertedOnIo = false
        every { fixture.eventsDb.insertEvents(any()) } answers {
            insertedOnIo = fixture.dispatchers.markedIo.isRunningHere
        }

        whenn()
        fixture.sut(stubEcomEvent())

        then()
        assertTrue(insertedOnIo)
    }

    @Test
    fun `should emit ecommerce event as regular event`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val event = stubEcomEvent()
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.sut(event)

        then()
        assertEquals(listOf(event.asRegularEvent()), fixture.events.emitted)
    }

    @Test
    fun `should fail without emitting when insert fails`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val error = IOException("disk full")
        every { fixture.eventsDb.insertEvents(any()) } throws error

        whenn()
        val result = fixture.sut(stubEcomEvent())

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(fixture.events.emitted.isEmpty())
        assertEquals(
            listOf(FailureReport("LogEcomEvent", "save ecommerce event to local database", error)),
            fixture.traced.reports
        )
    }

    @Test
    fun `should trace ecommerce type and steps without order details`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        justRun { fixture.eventsDb.insertEvents(any()) }

        whenn()
        fixture.sut(stubEcomEvent(externalOrderId = "order-secret-1"))

        then()
        assertEquals(
            fixture.traced.successfulTrace(
                "LogEcomEvent",
                "type=orderCancelled",
                "save ecommerce event to local database",
                "notify in-app message triggers"
            ),
            fixture.traced.sink.lines
        )
        assertFalse(fixture.traced.sink.lines.any { it.contains("order-secret-1") })
    }
}
