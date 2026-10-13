package com.reteno.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TestTest {

    @Test
    fun `should fail when given step is missing`() {
        val error = assertThrows(IllegalStateException::class.java) {
            runUnitTest {
                whenn()
                then()
            }
        }

        assertEquals("Given step is missing", error.message)
    }

    @Test
    fun `should fail when when step is missing`() {
        val error = assertThrows(IllegalStateException::class.java) {
            runUnitTest {
                given()
                then()
            }
        }

        assertEquals("When step is missing", error.message)
    }

    @Test
    fun `should fail when then step is missing`() {
        val error = assertThrows(IllegalStateException::class.java) {
            runUnitTest {
                given()
                whenn()
            }
        }

        assertEquals("Then step is missing", error.message)
    }

    @Test
    fun `should run infinite work scope loop with virtual time and cancel it at the end`() {
        var ticks = 0
        var job: Job? = null

        runUnitTest {
            given()
            val scope = workScope()

            whenn()
            job = scope.launch {
                while (isActive) {
                    delay(1_000)
                    ticks++
                }
            }
            runCurrent()
            advanceTimeBy(3_000)
            runCurrent()

            then()
            assertEquals(3, ticks)
        }

        assertTrue(requireNotNull(job).isCancelled)
    }

    @Test
    fun `should cancel work scope even when body throws`() {
        var job: Job? = null

        assertThrows(IllegalArgumentException::class.java) {
            runUnitTest {
                given()
                job = workScope().launch { awaitCancellation() }
                runCurrent()
                whenn()
                then()
                throw IllegalArgumentException("boom")
            }
        }

        assertFalse(requireNotNull(job).isActive)
    }
}
