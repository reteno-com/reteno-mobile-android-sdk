package com.reteno.core.internal.action.recommendation

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerRecomEvents
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.data.local.model.recommendation.RecomEventsDb
import com.reteno.core.fixtures.FailureReport
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubRecomEvents
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

internal class LogRecommendationEventsActionTest {

    private class SutFixture(scope: TestScope) {
        val recomEventsDb = mockk<RetenoDatabaseManagerRecomEvents>()
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        var insertedOnIo = false
        val sut = LogRecommendationEventsAction(recomEventsDb, dispatchers, traced.runner)

        init {
            every { recomEventsDb.insertRecomEvents(any()) } answers {
                insertedOnIo = dispatchers.markedIo.isRunningHere
            }
        }
    }

    @Test
    fun `should save recommendation events to local database on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(stubRecomEvents())

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 1) {
            fixture.recomEventsDb.insertRecomEvents(
                RecomEventsDb(
                    recomVariantId = "r1v2",
                    recomEvents = listOf(
                        RecomEventDb(
                            recomEventType = RecomEventTypeDb.CLICKS,
                            occurred = "2026-01-02T03:04:05Z",
                            productId = "sku-1"
                        )
                    )
                )
            )
        }
        assertTrue(fixture.insertedOnIo)
    }

    @Test
    fun `should save nothing when there are no events`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(stubRecomEvents(recomEvents = emptyList()))

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 0) { fixture.recomEventsDb.insertRecomEvents(any()) }
        assertTrue(fixture.traced.sink.lines.any { it.contains("no recommendation events, nothing to save") })
    }

    @Test
    fun `should save nothing when events are missing`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val result = fixture.sut(stubRecomEvents(recomEvents = null))

        then()
        assertTrue(result.isSuccess)
        verify(exactly = 0) { fixture.recomEventsDb.insertRecomEvents(any()) }
    }

    @Test
    fun `should report failure once when events cannot be saved`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        every { fixture.recomEventsDb.insertRecomEvents(any()) } throws IOException("disk full")

        whenn()
        val result = fixture.sut(stubRecomEvents())

        then()
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(
            listOf(
                FailureReport(
                    "LogRecommendationEvents",
                    "save recommendation events to local database",
                    IOException::class.java,
                    "disk full"
                )
            ),
            fixture.traced.reports
        )
    }

    @Test
    fun `should trace variant and event count`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(stubRecomEvents())

        then()
        assertEquals(
            fixture.traced.successfulTrace(
                "LogRecommendationEvents",
                "recomVariantId=r1v2  events=1",
                "save recommendation events to local database"
            ),
            fixture.traced.sink.lines
        )
    }
}
