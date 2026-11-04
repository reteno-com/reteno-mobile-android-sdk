package com.reteno.core._interop

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.RetenoInternalFacade
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class SessionInfoInternalTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val raw = application.getSharedPreferences("reteno_shared_prefs", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
        val facade = mockk<RetenoInternalFacade>(relaxed = true)

        fun withFacade(block: () -> Unit) {
            RetenoInternalEntryPoint.install(facade)
            try {
                block()
            } finally {
                RetenoInternalEntryPoint.install(null)
            }
        }
    }

    @Test
    fun `should read session start timestamp stored by the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.raw.edit().putLong("session_start_timestamp", SESSION_START).commit()

        whenn()
        val timestamp = SessionInfoInternal.sessionStartTimestamp(fixture.application)

        then()
        assertEquals(SESSION_START, timestamp)
    }

    @Test
    fun `should answer zero when no session has started yet`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val timestamp = SessionInfoInternal.sessionStartTimestamp(fixture.application)

        then()
        assertEquals(0L, timestamp)
    }

    @Test
    fun `should hand a new session duration to the running sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.withFacade {
            SessionInfoInternal.setSessionDuration(SESSION_DURATION)
        }

        then()
        verify(exactly = 1) { fixture.facade.setSessionDuration(SESSION_DURATION) }
    }

    companion object {
        private const val SESSION_DURATION = 30_000L
        private const val SESSION_START = 1_700_000_000_000L
    }
}
