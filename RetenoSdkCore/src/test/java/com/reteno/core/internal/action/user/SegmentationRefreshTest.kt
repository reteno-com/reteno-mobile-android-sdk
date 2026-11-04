package com.reteno.core.internal.action.user

import com.reteno.core.data.remote.model.iam.message.InAppMessage
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.internal.action.iam.CheckInAppSegments
import com.reteno.core.internal.iam.InAppMessagesStateImpl
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import com.reteno.test.workScope
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SegmentationRefreshTest {

    private class SutFixture(scope: TestScope) {
        val checks = mutableListOf<Pair<List<Long>, Boolean>>()
        var outcome: Result<Unit> = Result.success(Unit)
        val state = InAppMessagesStateImpl()
        val checkSegments = mockk<CheckInAppSegments>().also {
            coEvery { it(any(), any()) } answers {
                checks += firstArg<List<InAppMessage>>().map { message -> message.messageId } to secondArg<Boolean>()
                outcome
            }
        }
        val sut = SegmentationRefreshImpl(
            checkSegments = checkSegments,
            state = state,
            scope = scope.workScope()
        )
    }

    @Test
    fun `should refresh once after requests stop for debounce period`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        runCurrent()

        whenn()
        repeat(3) {
            fixture.sut.request()
            advanceTimeBy(1_000)
        }
        advanceTimeBy(SegmentationRefreshImpl.DEBOUNCE_MILLIS - 1_001)
        runCurrent()
        val beforeQuietPeriodEnds = fixture.checks.size
        advanceTimeBy(1_001)
        runCurrent()

        then()
        assertEquals(0, beforeQuietPeriodEnds)
        assertEquals(1, fixture.checks.size)
    }

    @Test
    fun `should check segments of current in-app messages without saving them`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.state.replaceMessages(listOf(stubInAppMessage(messageId = 4), stubInAppMessage(messageId = 9)))
        runCurrent()

        whenn()
        fixture.sut.request()
        advanceTimeBy(SegmentationRefreshImpl.DEBOUNCE_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(listOf(listOf(4L, 9L) to false), fixture.checks)
    }

    @Test
    fun `should keep refreshing on later requests after a failed refresh`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.outcome = Result.failure(IllegalStateException("no network"))
        runCurrent()

        whenn()
        fixture.sut.request()
        advanceTimeBy(SegmentationRefreshImpl.DEBOUNCE_MILLIS + 1)
        runCurrent()
        fixture.outcome = Result.success(Unit)
        fixture.sut.request()
        advanceTimeBy(SegmentationRefreshImpl.DEBOUNCE_MILLIS + 1)
        runCurrent()

        then()
        assertEquals(2, fixture.checks.size)
    }
}
