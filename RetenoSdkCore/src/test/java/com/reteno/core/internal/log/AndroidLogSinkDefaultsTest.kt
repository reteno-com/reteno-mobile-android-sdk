package com.reteno.core.internal.log

import android.util.Log
import com.reteno.core.util.Util
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class AndroidLogSinkDefaultsTest {

    @Test
    fun `should write to android log with default printer`() = runUnitTest {
        given()
        ShadowLog.clear()
        val sut = AndroidLogSink()

        whenn()
        sut.write(LogPriority.INFO, "RetenoAction", "default printer")

        then()
        val logged = ShadowLog.getLogsForTag("RetenoAction").single()
        assertEquals(Log.INFO, logged.type)
        assertEquals("default printer", logged.msg)
    }

    @Test
    fun `should be enabled by default in debug build`() = runUnitTest {
        given()
        val previousDebugView = Util.isDebugView()
        val sut = AndroidLogSink()

        whenn()
        val enabled = try {
            Util.setIsDebug(false)
            sut.isEnabled
        } finally {
            Util.setIsDebug(previousDebugView)
        }

        then()
        assertTrue(enabled)
    }
}
