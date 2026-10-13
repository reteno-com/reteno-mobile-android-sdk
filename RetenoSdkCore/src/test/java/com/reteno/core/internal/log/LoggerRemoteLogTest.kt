package com.reteno.core.internal.log

import com.reteno.core.domain.model.logevent.LogLevel
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test

internal class LoggerRemoteLogTest {

    @Test
    fun `should capture info and error messages through logger with their levels`() = runUnitTest {
        given()
        mockkStatic(Logger::class)
        try {
            justRun { Logger.captureMessage(any(), any()) }

            whenn()
            LoggerRemoteLog.info("rows removed")
            LoggerRemoteLog.error("action failed")

            then()
            verifyOrder {
                Logger.captureMessage("rows removed", LogLevel.INFO)
                Logger.captureMessage("action failed", LogLevel.ERROR)
            }
        } finally {
            unmockkStatic(Logger::class)
        }
    }
}
