package com.reteno.core.util

import com.reteno.core.Reteno
import com.reteno.core.RetenoInternalImpl
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class LoggerWithoutSdkInstanceTest {

    @Test
    fun `should not crash host when capturing message before sdk instance exists`() = runUnitTest {
        given()
        val previous = Reteno.instanceInternal

        whenn()
        val outcome = withoutSdkInstance(previous) { runCatching { Logger.captureMessage("before init") } }

        then()
        assertTrue(outcome.isSuccess) { "Logger threw ${outcome.exceptionOrNull()}" }
    }

    @Test
    fun `should not crash host when logging error before sdk instance exists`() = runUnitTest {
        given()
        val previous = Reteno.instanceInternal

        whenn()
        val outcome = withoutSdkInstance(previous) { runCatching { Logger.e("Tag", "method", IllegalStateException("boom")) } }

        then()
        assertTrue(outcome.isSuccess) { "Logger threw ${outcome.exceptionOrNull()}" }
    }

    private fun <T> withoutSdkInstance(previous: RetenoInternalImpl?, block: () -> T): T {
        RetenoInternalImpl.swapInstance(null)
        try {
            return block()
        } finally {
            RetenoInternalImpl.swapInstance(previous)
        }
    }
}
