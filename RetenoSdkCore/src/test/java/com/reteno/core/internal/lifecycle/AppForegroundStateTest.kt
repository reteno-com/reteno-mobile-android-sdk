package com.reteno.core.internal.lifecycle

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class AppForegroundStateTest {

    @Test
    fun `should report first foreground as not coming from background`() = runUnitTest {
        given()
        val sut = AppForegroundState()

        whenn()
        val wasBackgrounded = sut.enterForeground(now = 10L)

        then()
        assertEquals(false, wasBackgrounded)
    }

    @Test
    fun `should report foreground after background and return open time on background`() = runUnitTest {
        given()
        val sut = AppForegroundState()
        sut.enterForeground(now = 10L)

        whenn()
        val openedAt = sut.enterBackground()
        val wasBackgrounded = sut.enterForeground(now = 20L)

        then()
        assertEquals(10L, openedAt)
        assertEquals(true, wasBackgrounded)
    }

    @Test
    fun `should ignore repeated foreground and background without transition`() = runUnitTest {
        given()
        val sut = AppForegroundState()

        whenn()
        val backgroundBeforeStart = sut.enterBackground()
        sut.enterForeground(now = 10L)
        val secondForeground = sut.enterForeground(now = 20L)

        then()
        assertNull(backgroundBeforeStart)
        assertNull(secondForeground)
    }
}
