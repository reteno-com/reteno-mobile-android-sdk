package com.reteno.core.internal.action.sync

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.fixtures.FakeRestClient
import com.reteno.core.fixtures.SentRequest
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.api
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

internal class PushWrappedLinksStepTest {

    private class SutFixture(scope: TestScope) {
        val linksDb = mockk<RetenoDatabaseManagerWrappedLink>()
        val rest = FakeRestClient()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val sut = PushWrappedLinksStep(linksDb, rest.api(dispatchers), dispatchers, traced.runner)

        init {
            justRun { linksDb.deleteWrappedLinks(1, true) }
        }
    }

    @Test
    fun `should report nothing to send without request when there are no links`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.linksDb.getWrappedLinks(1) } returns emptyList()

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.NOTHING_TO_SEND, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        assertTrue(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should open every link with head request and delete it`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.linksDb.getWrappedLinks(1) } returnsMany listOf(
            listOf("https://t.example/1"),
            listOf("https://t.example/2"),
            emptyList()
        )

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertEquals(
            listOf(
                SentRequest(HttpMethod.HEAD, ApiContract.Custom("https://t.example/1"), null),
                SentRequest(HttpMethod.HEAD, ApiContract.Custom("https://t.example/2"), null)
            ),
            fixture.rest.requests
        )
        verify(exactly = 2) { fixture.linksDb.deleteWrappedLinks(1, true) }
        assertFalse(fixture.traced.sink.lines.any { it.contains("→ nothing to send") })
    }

    @Test
    fun `should drop link that is not https without request`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.linksDb.getWrappedLinks(1) } returnsMany listOf(listOf("http://t.example/1"), emptyList())

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        assertTrue(fixture.rest.requests.isEmpty())
        verify(exactly = 1) { fixture.linksDb.deleteWrappedLinks(1, true) }
    }

    @Test
    fun `should abort and keep link when server fails with repeatable error`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.linksDb.getWrappedLinks(1) } returns listOf("https://t.example/1")
        fixture.rest.failWith(500)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.ABORT, outcome)
        verify(exactly = 0) { fixture.linksDb.deleteWrappedLinks(any(), any()) }
    }

    @Test
    fun `should drop rejected link and continue`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.linksDb.getWrappedLinks(1) } returnsMany listOf(listOf("https://t.example/1"), emptyList())
        fixture.rest.failWith(404)

        whenn()
        val outcome = fixture.sut().getOrThrow()

        then()
        assertEquals(StepOutcome.CONTINUE, outcome)
        verify(exactly = 1) { fixture.linksDb.deleteWrappedLinks(1, true) }
    }

    @Test
    fun `should stop after limited number of links`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.linksDb.getWrappedLinks(1) } returns listOf("https://t.example/1")

        whenn()
        fixture.sut()

        then()
        assertEquals(PushWrappedLinksStep.MAX_LINKS, fixture.rest.requests.size)
    }

    @Test
    fun `should never write link url to trace and access database on io dispatcher`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        var loadedOnIo = false
        every { fixture.linksDb.getWrappedLinks(1) } answers {
            loadedOnIo = fixture.dispatchers.markedIo.isRunningHere
            listOf("https://secret.example/track")
        } andThenAnswer { emptyList() }

        whenn()
        fixture.sut()

        then()
        assertTrue(loadedOnIo)
        val lines = fixture.traced.sink.lines
        assertTrue(lines.any { it.contains("✓ send link click to tracking server") })
        assertFalse(lines.any { it.contains("secret.example") || it.contains("https://") })
    }
}
