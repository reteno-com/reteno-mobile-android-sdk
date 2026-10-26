package com.reteno.core.internal.action.iam

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerInAppMessages
import com.reteno.core.data.local.model.iam.InAppMessageDb
import com.reteno.core.data.remote.model.iam.displayrules.frequency.FrequencyRule
import com.reteno.core.fixtures.TestActionRunner
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.testDispatchers
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class RecordInAppShownActionTest {

    private class SutFixture(scope: TestScope) {
        val inAppDb = mockk<RetenoDatabaseManagerInAppMessages>(relaxed = true)
        val dispatchers = scope.testDispatchers()
        val traced = TestActionRunner(scope)
        val saved = mutableListOf<List<InAppMessageDb>>()
        var savedOnIo = false
        val sut = RecordInAppShownAction(
            cache = InAppMessagesCache(inAppDb, dispatchers),
            clock = { NOW },
            lock = Mutex(),
            runner = traced.runner
        )

        init {
            every { inAppDb.insertInAppMessages(any()) } answers {
                saved += firstArg<List<InAppMessageDb>>()
                savedOnIo = dispatchers.markedIo.isRunningHere
            }
        }
    }

    @Test
    fun `should count first show`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage()

        whenn()
        fixture.sut(message)

        then()
        assertEquals(1L, message.showCount)
        assertEquals(NOW, message.lastShowTime)
    }

    @Test
    fun `should add show to count of message without frequency rules`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage(lastShowTime = 1L, showCount = 4)

        whenn()
        fixture.sut(message)

        then()
        assertEquals(5L, message.showCount)
    }

    @Test
    fun `should add show to count of message without times per time unit rule`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage(frequency = listOf(FrequencyRule.NoLimit), lastShowTime = 1L, showCount = 4)

        whenn()
        fixture.sut(message)

        then()
        assertEquals(5L, message.showCount)
    }

    @Test
    fun `should add show to count within the time unit`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage(frequency = listOf(TWICE_PER_MINUTE), lastShowTime = NOW - MINUTE, showCount = 1)

        whenn()
        fixture.sut(message)

        then()
        assertEquals(2L, message.showCount)
    }

    @Test
    fun `should restart count once a time unit has passed since last show`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage(
            frequency = listOf(TWICE_PER_MINUTE),
            lastShowTime = NOW - MINUTE - 1,
            showCount = 2
        )

        whenn()
        fixture.sut(message)

        then()
        assertEquals(1L, message.showCount)
    }

    @Test
    fun `should restart count only once the longest time unit has passed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage(
            frequency = listOf(TWICE_PER_MINUTE, FIVE_PER_HOUR, ONCE_PER_SECOND),
            lastShowTime = NOW - MINUTE - 1,
            showCount = 3
        )

        whenn()
        fixture.sut(message)

        then()
        assertEquals(4L, message.showCount)
    }

    @Test
    fun `should start count of never shown times per time unit message at one`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage(frequency = listOf(TWICE_PER_MINUTE))

        whenn()
        fixture.sut(message)

        then()
        assertEquals(1L, message.showCount)
    }

    @Test
    fun `should save show history to local database on io`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val message = stubInAppMessage(messageId = 7)

        whenn()
        fixture.sut(message)

        then()
        val row = fixture.saved.single().single()
        assertEquals(7L, row.messageId)
        assertEquals(NOW, row.lastShowTime)
        assertEquals(1L, row.showCount)
        assertTrue(fixture.savedOnIo)
    }

    @Test
    fun `should trace message and new count`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut(stubInAppMessage(messageId = 7))

        then()
        val lines = fixture.traced.sink.lines
        assertTrue(lines.first().contains("RecordInAppShown  messageId=7"))
        assertTrue(lines.any { it.contains("✓ count show in in-app message") && it.contains("count=1") })
    }

    private companion object {
        const val NOW = 10_000_000L
        val MINUTE = TimeUnit.MINUTES.toMillis(1)
        val TWICE_PER_MINUTE = FrequencyRule.TimesPerTimeUnit(TimeUnit.MINUTES, 2)
        val FIVE_PER_HOUR = FrequencyRule.TimesPerTimeUnit(TimeUnit.HOURS, 5)
        val ONCE_PER_SECOND = FrequencyRule.TimesPerTimeUnit(TimeUnit.SECONDS, 1)
    }
}
