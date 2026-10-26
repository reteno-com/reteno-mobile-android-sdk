package com.reteno.core.internal.log

import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RemoteLogGuardTest {

    @Test
    fun `should run block when not muted`() = runUnitTest {
        given()
        val ran = mutableListOf<String>()

        whenn()
        RemoteLogGuard.unlessMuted { ran += "recorded" }

        then()
        assertEquals(listOf("recorded"), ran)
    }

    @Test
    fun `should skip block while muted`() = runUnitTest {
        given()
        val ran = mutableListOf<String>()

        whenn()
        RemoteLogGuard.muted { RemoteLogGuard.unlessMuted { ran += "recorded" } }

        then()
        assertTrue(ran.isEmpty())
    }

    @Test
    fun `should stay muted until the outer muted block ends`() = runUnitTest {
        given()
        val ran = mutableListOf<String>()

        whenn()
        RemoteLogGuard.muted {
            RemoteLogGuard.muted { }
            RemoteLogGuard.unlessMuted { ran += "inside outer block" }
        }
        RemoteLogGuard.unlessMuted { ran += "after outer block" }

        then()
        assertEquals(listOf("after outer block"), ran)
    }

    @Test
    fun `should unmute when muted block fails`() = runUnitTest {
        given()
        val ran = mutableListOf<String>()

        whenn()
        runCatching { RemoteLogGuard.muted { throw IllegalStateException("database closed") } }
        RemoteLogGuard.unlessMuted { ran += "recorded" }

        then()
        assertEquals(listOf("recorded"), ran)
    }

    @Test
    fun `should return result of muted block`() = runUnitTest {
        given()

        whenn()
        val count = RemoteLogGuard.muted { 3 }

        then()
        assertEquals(3, count)
    }
}
