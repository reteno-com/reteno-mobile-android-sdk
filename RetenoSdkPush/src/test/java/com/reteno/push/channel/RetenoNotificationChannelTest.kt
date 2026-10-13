package com.reteno.push.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import com.reteno.core.util.Procedure
import com.reteno.core.util.Util
import com.reteno.push.fixtures.FakeRetenoFacade
import com.reteno.push.fixtures.notificationManager
import com.reteno.push.fixtures.onAndroidSdk
import com.reteno.push.fixtures.testApplication
import com.reteno.push.fixtures.withFakeFacade
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RetenoNotificationChannelTest {

    private class SutFixture {
        val application = testApplication()
        val manager = application.notificationManager()
        var configApplied = 0
        val hostConfig = Procedure<NotificationChannelCompat.Builder> {
            configApplied++
            it.setName("Host name")
        }

        fun channel(id: String = BUNDLED_ID): NotificationChannel? = manager.getNotificationChannel(id)

        inline fun withBundledChannel(json: String?, block: () -> Unit) {
            mockkStatic(Util::class)
            try {
                every { Util.readFromRaw(any(), any()) } returns json
                block()
            } finally {
                unmockkStatic(Util::class)
            }
        }

        fun restoreDefaultChannelId() {
            withFakeFacade(FakeRetenoFacade()) { RetenoNotificationChannel.createDefaultChannel(application) }
        }
    }

    @Test
    fun `should create default channel from bundled description and store it`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            RetenoNotificationChannel.createDefaultChannel(fixture.application)

            then()
            val channel = fixture.channel()!!
            assertEquals("name", channel.name)
            assertEquals("description", channel.description)
            assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
            assertTrue(channel.canShowBadge())
            assertEquals(listOf("saveDefaultNotificationChannel"), facade.calls)
            assertTrue(facade.storedChannel.contains("\"id\":\"defaultId\""))
        }
    }

    @Test
    fun `should create default channel from stored description`() = runUnitTest {
        val fixture = SutFixture()
        try {
            withFakeFacade { facade ->
                given()
                facade.storedChannel = """{"id":"stored-id","name":"Stored","description":"Stored channel","importance":2,"vibration_pattern":[10,20]}"""

                whenn()
                RetenoNotificationChannel.createDefaultChannel(fixture.application)

                then()
                val channel = fixture.channel("stored-id")!!
                assertEquals("Stored", channel.name)
                assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
                assertArrayEquals(longArrayOf(10, 20), channel.vibrationPattern)
                assertEquals("stored-id", RetenoNotificationChannel.DEFAULT_CHANNEL_ID)
                assertTrue(facade.calls.isEmpty())
            }
        } finally {
            fixture.restoreDefaultChannelId()
        }
    }

    @Test
    fun `should keep vibration enabled when stored description has no pattern`() = runUnitTest {
        val fixture = SutFixture()
        try {
            withFakeFacade { facade ->
                given()
                facade.storedChannel = """{"id":"vibrating","name":"Vibrating","description":"d","enable_vibration":true}"""

                whenn()
                RetenoNotificationChannel.createDefaultChannel(fixture.application)

                then()
                assertTrue(fixture.channel("vibrating")!!.shouldVibrate())
            }
        } finally {
            fixture.restoreDefaultChannelId()
        }
    }

    @Test
    fun `should use bundled description when stored one is not valid json`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.storedChannel = "{not json"

            whenn()
            RetenoNotificationChannel.createDefaultChannel(fixture.application)

            then()
            assertEquals("name", fixture.channel()!!.name)
        }
    }

    @Test
    fun `should create fallback channel when bundled description cannot be read`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.withBundledChannel(null) { RetenoNotificationChannel.createDefaultChannel(fixture.application) }

            then()
            assertFallbackChannel(fixture.channel()!!)
            assertTrue(facade.calls.isEmpty())
        }
    }

    @Test
    fun `should create fallback channel when bundled description is not valid json`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()

            whenn()
            fixture.withBundledChannel("[broken") { RetenoNotificationChannel.createDefaultChannel(fixture.application) }

            then()
            assertFallbackChannel(fixture.channel()!!)
            assertTrue(facade.calls.isEmpty())
        }
    }

    @Test
    fun `should create default channel from host configuration`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.channelConfig = fixture.hostConfig

            whenn()
            RetenoNotificationChannel.createDefaultChannel(fixture.application)

            then()
            val channel = fixture.channel()!!
            assertEquals("Host name", channel.name)
            assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
            assertTrue(facade.calls.isEmpty())
        }
    }

    @Test
    fun `should not create channel before android oreo`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.channelConfig = fixture.hostConfig

            whenn()
            onAndroidSdk(Build.VERSION_CODES.N_MR1) { RetenoNotificationChannel.createDefaultChannel(fixture.application) }

            then()
            assertEquals(0, fixture.configApplied)
            assertTrue(facade.calls.isEmpty())
        }
    }

    @Test
    fun `should report notifications enabled state`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()
            val enabledBefore = RetenoNotificationChannel.isNotificationsEnabled(fixture.application)

            whenn()
            shadowOf(fixture.manager).setNotificationsEnabled(false)

            then()
            assertTrue(enabledBefore)
            assertFalse(RetenoNotificationChannel.isNotificationsEnabled(fixture.application))
        }
    }

    @Test
    fun `should treat missing channel id as disabled channel`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            val results = listOf(null, " ").map { RetenoNotificationChannel.isNotificationChannelEnabled(fixture.application, it) }

            then()
            assertEquals(listOf(false, false), results)
            assertNull(fixture.channel())
        }
    }

    @Test
    fun `should create default channel when checked channel does not exist yet`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()

            whenn()
            val enabled = RetenoNotificationChannel.isNotificationChannelEnabled(fixture.application, BUNDLED_ID)

            then()
            assertTrue(enabled)
            assertEquals("name", fixture.channel()!!.name)
        }
    }

    @Test
    fun `should report blocked channel as disabled`() = runUnitTest {
        withFakeFacade {
            given()
            val fixture = SutFixture()
            fixture.manager.createNotificationChannel(NotificationChannel(BUNDLED_ID, "name", NotificationManager.IMPORTANCE_NONE))

            whenn()
            val enabled = RetenoNotificationChannel.isNotificationChannelEnabled(fixture.application, BUNDLED_ID)

            then()
            assertFalse(enabled)
        }
    }

    @Test
    fun `should apply host configuration to existing channel when checking it`() = runUnitTest {
        withFakeFacade { facade ->
            given()
            val fixture = SutFixture()
            facade.channelConfig = fixture.hostConfig
            fixture.manager.createNotificationChannel(NotificationChannel(BUNDLED_ID, "name", NotificationManager.IMPORTANCE_HIGH))

            whenn()
            val enabled = RetenoNotificationChannel.isNotificationChannelEnabled(fixture.application, BUNDLED_ID)

            then()
            assertTrue(enabled)
            assertEquals(1, fixture.configApplied)
            assertEquals("Host name", fixture.channel()!!.name)
        }
    }

    private fun assertFallbackChannel(channel: NotificationChannel) {
        assertEquals("default", channel.name)
        assertEquals("description", channel.description)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    private companion object {
        const val BUNDLED_ID = "defaultId"
    }
}
