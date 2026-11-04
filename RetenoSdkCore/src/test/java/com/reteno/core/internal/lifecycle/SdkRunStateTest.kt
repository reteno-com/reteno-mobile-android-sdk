package com.reteno.core.internal.lifecycle

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class SdkRunStateTest {

    @Test
    fun `should report first start only until stopped`() = runUnitTest {
        given()
        val sut = SdkRunState()
        val initially = sut.isStarted

        whenn()
        val first = sut.markStarted()
        val second = sut.markStarted()
        sut.markStopped()
        val afterStop = sut.markStarted()

        then()
        assertFalse(initially)
        assertEquals(listOf(true, false, true), listOf(first, second, afterStop))
        assertEquals(true, sut.isStarted)
    }
}
