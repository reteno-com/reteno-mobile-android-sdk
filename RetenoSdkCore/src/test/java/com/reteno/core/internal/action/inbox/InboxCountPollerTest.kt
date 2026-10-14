package com.reteno.core.internal.action.inbox

import com.reteno.core.domain.callback.appinbox.RetenoResultCallback
import com.reteno.core.fixtures.RecordingResultCallback
import com.reteno.core.fixtures.testDispatchers
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class InboxCountPollerTest {

    private class SutFixture(private val scope: TestScope, foreground: Boolean = true) {
        val dispatchers = scope.testDispatchers()
        val counts = ArrayDeque<Int>()
        var failNextFetch = false
        var fetches = 0
        val getCount = mockk<GetInboxMessagesCount>().also {
            coEvery { it(any()) } answers {
                fetches++
                val callback = firstArg<RetenoResultCallback<Int>>()
                if (failNextFetch) {
                    failNextFetch = false
                    callback.onFailure(503, "unavailable", null)
                } else {
                    callback.onSuccess(counts.removeFirstOrNull() ?: LAST_COUNT)
                }
                Result.success(Unit)
            }
        }
        val sut = InboxCountPoller(getCount, scope.workScope(), dispatchers)

        init {
            if (foreground) sut.resume()
        }

        fun subscriber() = RecordingResultCallback<Int>(dispatchers.markedMain)

        fun advance(millis: Long) {
            scope.advanceTimeBy(millis)
            scope.runCurrent()
        }
    }

    @Test
    fun `should fetch count right away for first subscriber`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += 3
        val subscriber = fixture.subscriber()

        whenn()
        fixture.sut.subscribe(subscriber)
        runCurrent()

        then()
        assertEquals(listOf(3), subscriber.results)
        assertEquals(1, fixture.fetches)
    }

    @Test
    fun `should poll every thirty seconds and emit only changed counts`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += listOf(3, 3, 5)
        val subscriber = fixture.subscriber()
        fixture.sut.subscribe(subscriber)
        runCurrent()

        whenn()
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS - 1)
        val fetchesBeforeInterval = fixture.fetches
        fixture.advance(1)
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS)

        then()
        assertEquals(1, fetchesBeforeInterval)
        assertEquals(3, fixture.fetches)
        assertEquals(listOf(3, 5), subscriber.results)
    }

    @Test
    fun `should pass fetch failure to every subscriber`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.failNextFetch = true
        val first = fixture.subscriber()
        val second = fixture.subscriber()

        whenn()
        fixture.sut.subscribe(first)
        fixture.sut.subscribe(second)
        runCurrent()

        then()
        assertEquals(listOf(Triple<Int?, String?, Throwable?>(503, "unavailable", null)), first.failures)
        assertEquals(listOf(Triple<Int?, String?, Throwable?>(503, "unavailable", null)), second.failures)
        assertEquals(1, fixture.fetches)
    }

    @Test
    fun `should replay last count to late subscriber on main thread`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += 4
        fixture.sut.subscribe(fixture.subscriber())
        runCurrent()
        val late = fixture.subscriber()

        whenn()
        fixture.sut.subscribe(late)
        runCurrent()

        then()
        assertEquals(listOf(4), late.results)
        assertEquals(listOf(true), late.calledOnMain)
        assertEquals(1, fixture.fetches)
    }

    @Test
    fun `should ignore subscribing the same callback twice`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += 2
        val subscriber = fixture.subscriber()
        fixture.sut.subscribe(subscriber)
        runCurrent()

        whenn()
        fixture.sut.subscribe(subscriber)
        runCurrent()

        then()
        assertEquals(listOf(2), subscriber.results)
    }

    @Test
    fun `should keep polling while another subscriber remains`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += listOf(1, 2)
        val leaving = fixture.subscriber()
        val staying = fixture.subscriber()
        fixture.sut.subscribe(leaving)
        fixture.sut.subscribe(staying)
        runCurrent()

        whenn()
        fixture.sut.unsubscribe(leaving)
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS)

        then()
        assertEquals(listOf(1), leaving.results)
        assertEquals(listOf(1, 2), staying.results)
    }

    @Test
    fun `should stop polling and forget last count when last subscriber leaves`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += listOf(6, 6)
        val first = fixture.subscriber()
        fixture.sut.subscribe(first)
        runCurrent()

        whenn()
        fixture.sut.unsubscribe(first)
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS * 2)
        val fetchesWhileUnsubscribed = fixture.fetches
        val next = fixture.subscriber()
        fixture.sut.subscribe(next)
        runCurrent()

        then()
        assertEquals(1, fetchesWhileUnsubscribed)
        assertEquals(listOf(6), next.results)
    }

    @Test
    fun `should stop polling when all subscribers are removed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = fixture.subscriber()
        val second = fixture.subscriber()
        fixture.sut.subscribe(first)
        fixture.sut.subscribe(second)
        runCurrent()

        whenn()
        fixture.sut.unsubscribeAll()
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS * 2)

        then()
        assertEquals(1, fixture.fetches)
    }

    @Test
    fun `should not poll while app is in background`() = runUnitTest {
        given()
        val fixture = SutFixture(this, foreground = false)
        val subscriber = fixture.subscriber()

        whenn()
        fixture.sut.subscribe(subscriber)
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS)

        then()
        assertEquals(0, fixture.fetches)
        assertTrue(subscriber.results.isEmpty())
    }

    @Test
    fun `should not poll for subscriber removed while app was in background`() = runUnitTest {
        given()
        val fixture = SutFixture(this, foreground = false)
        val subscriber = fixture.subscriber()
        fixture.sut.subscribe(subscriber)
        fixture.sut.unsubscribe(subscriber)

        whenn()
        fixture.sut.resume()
        runCurrent()

        then()
        assertEquals(0, fixture.fetches)
    }

    @Test
    fun `should pause polling in background and resume it in foreground`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += listOf(1, 1)
        val subscriber = fixture.subscriber()
        fixture.sut.subscribe(subscriber)
        runCurrent()

        whenn()
        fixture.sut.pause()
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS * 2)
        val fetchesWhilePaused = fixture.fetches
        fixture.sut.resume()
        runCurrent()

        then()
        assertEquals(1, fetchesWhilePaused)
        assertEquals(2, fixture.fetches)
        assertEquals(listOf(1), subscriber.results)
    }

    @Test
    fun `should not start polling on resume without subscribers`() = runUnitTest {
        given()
        val fixture = SutFixture(this, foreground = false)

        whenn()
        fixture.sut.resume()
        fixture.advance(InboxCountPoller.POLL_INTERVAL_MILLIS)

        then()
        assertEquals(0, fixture.fetches)
    }

    @Test
    fun `should refresh count right away while polling`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += listOf(2, 0)
        val subscriber = fixture.subscriber()
        fixture.sut.subscribe(subscriber)
        runCurrent()

        whenn()
        fixture.sut.refresh()
        runCurrent()

        then()
        assertEquals(listOf(2, 0), subscriber.results)
    }

    @Test
    fun `should not refresh count without active polling`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.refresh()
        runCurrent()

        then()
        assertEquals(0, fixture.fetches)
    }

    @Test
    fun `should drop count that arrives after every subscriber left`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.counts += listOf(1, 9, 1)
        fixture.sut.subscribe(fixture.subscriber())
        runCurrent()
        fixture.sut.refresh()
        fixture.sut.unsubscribeAll()
        runCurrent()
        val next = fixture.subscriber()

        whenn()
        fixture.sut.subscribe(next)
        runCurrent()

        then()
        assertEquals(listOf(1), next.results)
    }

    private companion object {
        const val LAST_COUNT = 0
    }
}
