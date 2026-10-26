package com.reteno.core.internal.action.trace

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MonotonicClockTest {

    @Test
    fun `should never go backwards`() = runUnitTest {
        given()
        val sut = SystemMonotonicClock
        val first = sut.nanos()

        whenn()
        val second = sut.nanos()

        then()
        assertTrue(second >= first)
    }
}
