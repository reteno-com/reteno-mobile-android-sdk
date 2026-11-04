package com.reteno.core

import androidx.core.app.NotificationChannelCompat
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.core.internal.lifecycle.SessionTracker
import com.reteno.core.util.Procedure
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RetenoConfigTest {

    private class SutFixture {
        val deviceIdProvider = DeviceIdProvider { "custom-id" }
        val channel = Procedure<NotificationChannelCompat.Builder> { }
        val tracking = LifecycleTrackingOptions(
            appLifecycleEnabled = false,
            foregroundLifecycleEnabled = true,
            sessionStartEventsEnabled = false,
            sessionEndEventsEnabled = true,
            pushSubscriptionEnabled = false
        )

        fun fullConfig(): RetenoConfig = RetenoConfig.Builder()
            .accessKey("access-1")
            .pauseInAppMessages(true)
            .pausePushInAppMessages(true)
            .customDeviceIdProvider(deviceIdProvider)
            .lifecycleTrackingOptions(tracking)
            .sessionDuration(90_000L)
            .defaultNotificationChannelConfig(channel)
            .setPlatform("flutter")
            .setDebug(true)
            .build()

        fun settings(config: RetenoConfig): List<Any?> = listOf(
            config.isPausedInAppMessages,
            config.isPausedPushInAppMessages,
            config.userIdProvider,
            config.lifecycleTrackingOptions,
            config.sessionDuration,
            config.defaultNotificationChannelConfig,
            config.platform,
            config.isDebug
        )
    }

    @Test
    fun `should build a config with every setting`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val config = fixture.fullConfig()

        then()
        val expected = listOf(
            true,
            true,
            fixture.deviceIdProvider,
            fixture.tracking,
            90_000L,
            fixture.channel,
            "flutter",
            true
        )
        assertEquals(expected, fixture.settings(config))
        assertEquals("access-1", config.accessKey)
    }

    @Test
    fun `should build a config with defaults for everything but the access key`() = runUnitTest {
        given()
        val builder = RetenoConfig.Builder().accessKey("access-1")

        whenn()
        val config = builder.build()

        then()
        val expected = listOf(
            false,
            false,
            null,
            LifecycleTrackingOptions.DEFAULT,
            SessionTracker.DEFAULT_SESSION_RESET_TIME,
            null,
            "Android",
            false
        )
        assertEquals(expected, SutFixture().settings(config))
    }

    @Test
    fun `should refuse a blank access key`() = runUnitTest {
        given()
        val builder = RetenoConfig.Builder()

        whenn()
        val failure = runCatching { builder.accessKey(" ") }.exceptionOrNull()

        then()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `should refuse to build without an access key`() = runUnitTest {
        given()
        val builder = RetenoConfig.Builder().setDebug(true)

        whenn()
        val failure = runCatching { builder.build() }.exceptionOrNull()

        then()
        assertTrue(failure is IllegalStateException)
    }

    @Test
    fun `should copy every setting of a config`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val original = fixture.fullConfig()

        whenn()
        val copy = RetenoConfig.Builder(original).build()

        then()
        assertEquals(fixture.settings(original), fixture.settings(copy))
        assertEquals("access-1", copy.accessKey)
    }

    @Test
    fun `should copy every setting of a config under another access key`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val original = fixture.fullConfig()

        whenn()
        val copy = RetenoConfig.Builder(original, "access-2").build()

        then()
        assertEquals(fixture.settings(original), fixture.settings(copy))
        assertEquals("access-2", copy.accessKey)
    }
}
