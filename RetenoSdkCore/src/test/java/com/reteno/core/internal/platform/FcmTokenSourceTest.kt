package com.reteno.core.internal.platform

import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class FcmTokenSourceTest {

    private class SutFixture {
        val messaging = mockk<FirebaseMessaging>()
        val sut = FirebaseFcmTokenSource()

        fun <T> TestScope.withFirebase(block: suspend () -> T): T {
            mockkStatic(FirebaseMessaging::class)
            try {
                every { FirebaseMessaging.getInstance() } returns messaging
                val reading = async { block() }
                runCurrent()
                shadowOf(Looper.getMainLooper()).idle()
                runCurrent()
                return reading.getCompleted()
            } finally {
                unmockkStatic(FirebaseMessaging::class)
            }
        }
    }

    @Test
    fun `should tell whether firebase auto init is on`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.messaging.isAutoInitEnabled } returns false

        whenn()
        val enabled = with(fixture) { withFirebase { sut.isAutoInitEnabled } }

        then()
        assertEquals(false, enabled)
    }

    @Test
    fun `should fetch fcm token from firebase`() = runUnitTest {
        given()
        val fixture = SutFixture()
        every { fixture.messaging.token } returns Tasks.forResult("fcm-token")

        whenn()
        val token = with(fixture) { withFirebase { sut.fetchToken() } }

        then()
        assertEquals("fcm-token", token)
    }

    @Test
    fun `should have no fcm token when firebase fails to provide one`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val failed: Task<String> = Tasks.forException(IllegalStateException("no network"))
        every { fixture.messaging.token } returns failed

        whenn()
        val token = with(fixture) { withFirebase { sut.fetchToken() } }

        then()
        assertNull(token)
    }
}
