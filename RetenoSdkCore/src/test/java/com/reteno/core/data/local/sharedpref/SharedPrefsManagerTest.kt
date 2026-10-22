package com.reteno.core.data.local.sharedpref

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoConfig
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.internal.lifecycle.SessionTracker
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class SharedPrefsManagerTest {

    private class SutFixture {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val raw = application.getSharedPreferences("reteno_shared_prefs", Context.MODE_PRIVATE)
            .also { it.edit().clear().commit() }
        val sut = SharedPrefsManager(application)
    }

    @Test
    fun `should create random device uuid once and keep it`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val ids = listOf(fixture.sut.getDeviceIdUuid(), fixture.sut.getDeviceIdUuid())

        then()
        assertEquals(36, ids[0].length)
        assertEquals(ids[0], ids[1])
        assertEquals(ids[0], fixture.raw.getString("device_id", null))
    }

    @Test
    fun `should replace a blank stored device uuid`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.raw.edit().putString("device_id", " ").commit()

        whenn()
        val id = fixture.sut.getDeviceIdUuid()

        then()
        assertNotEquals(" ", id)
        assertEquals(id, fixture.raw.getString("device_id", null))
    }

    @Test
    fun `should read text values as empty or absent until stored`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val values = with(fixture.sut) {
            listOf(
                getFcmToken(), getDefaultNotificationChannel(), getIamBaseHtmlContent(), getAppVersion(),
                getLastStoredId(), getIamBaseUrl(), getIamBaseHtmlVersion(), getIamEtag(), getSessionId(),
                getExternalUserId(), getDeviceIdSuffix(), getPhone(), getEmail()
            )
        }

        then()
        assertEquals(List(5) { "" } + List(8) { null }, values)
    }

    @Test
    fun `should read back every stored text value`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        with(fixture.sut) {
            saveFcmToken("token")
            saveDefaultNotificationChannel("{}")
            saveAppVersion("2.0")
            saveIamBaseHtmlVersion("v3")
            saveIamEtag("etag-1")
            saveSessionId("session-1")
            saveExternalUserId("user-1")
            saveDeviceIdSuffix("suffix")
            saveDevicePhone("+100")
            saveDeviceEmail("a@b.c")
        }

        then()
        val values = with(fixture.sut) {
            listOf(
                getFcmToken(), getDefaultNotificationChannel(), getAppVersion(), getIamBaseHtmlVersion(),
                getIamEtag(), getSessionId(), getExternalUserId(), getDeviceIdSuffix(), getPhone(), getEmail()
            )
        }
        assertEquals(listOf("token", "{}", "2.0", "v3", "etag-1", "session-1", "user-1", "suffix", "+100", "a@b.c"), values)
    }

    @Test
    fun `should forget a text value stored as nothing`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.saveIamEtag("etag-1")
        fixture.sut.saveDeviceEmail("a@b.c")

        whenn()
        fixture.sut.saveIamEtag(null)
        fixture.sut.saveDeviceEmail(null)

        then()
        assertEquals(listOf(null, null), listOf(fixture.sut.getIamEtag(), fixture.sut.getEmail()))
    }

    @Test
    fun `should read in-app base url and drop legacy base html content`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.raw.edit()
            .putString("iam_base_url", "https://cdn.example/base.html")
            .putString("in_app_messages_base_html_content", "<html/>")
            .commit()
        val contentBefore = fixture.sut.getIamBaseHtmlContent()

        whenn()
        fixture.sut.clearBaseHtmlContent()

        then()
        assertEquals("https://cdn.example/base.html", fixture.sut.getIamBaseUrl())
        assertEquals(listOf("<html/>", ""), listOf(contentBefore, fixture.sut.getIamBaseHtmlContent()))
    }

    @Test
    fun `should store in-app base url under the key the sdk reads`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.saveIamBaseUrl("https://cdn.example/base.html")

        then()
        assertEquals("https://cdn.example/base.html", fixture.raw.getString("iam_base_url", null))
    }

    @Test
    fun `should forget in-app base url stored as nothing`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.saveIamBaseUrl("https://cdn.example/base.html")

        whenn()
        fixture.sut.saveIamBaseUrl(null)

        then()
        assertNull(fixture.sut.getIamBaseUrl())
    }

    @Test
    fun `should read flags and counters as defaults until stored`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val values = with(fixture.sut) {
            listOf<Any>(
                isNotificationsEnabled(), isDeviceRegistered(), getLastInteractionTime(), getSessionStartTimestamp(),
                getAppSessionTime(), getAppBuildNumber(), getOpenCount(), getBackgroundCount()
            )
        }

        then()
        assertEquals(listOf<Any>(false, false, 0L, 0L, 0L, 0L, 0, 0), values)
    }

    @Test
    fun `should read back every stored flag and counter`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        with(fixture.sut) {
            saveNotificationsEnabled(true)
            saveDeviceRegistered(true)
            saveAppStoppedTimestamp(5L)
            saveSessionStartTimestamp(6L)
            saveAppSessionTime(7L)
            saveAppBuildNumber(8L)
            saveOpenCount(2)
            saveBackgroundCount(3)
        }

        then()
        val values = with(fixture.sut) {
            listOf<Any>(
                isNotificationsEnabled(), isDeviceRegistered(), getSessionStartTimestamp(), getAppSessionTime(),
                getAppBuildNumber(), getOpenCount(), getBackgroundCount()
            )
        }
        assertEquals(listOf<Any>(true, true, 6L, 7L, 8L, 2, 3), values)
        assertEquals(5L, fixture.raw.getLong("app_stopped_timestamp", 0L))
    }

    @Test
    fun `should store session and app counters together`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.sut.batchSaveSession(sessionTime = 10L, lastInteractionTime = 11L)
        fixture.sut.batchSave(openCount = 4, backgroundCount = 5, lastInteractionTime = 12L)

        then()
        assertEquals(
            listOf<Any>(10L, 4, 5, 12L),
            listOf<Any>(
                fixture.sut.getAppSessionTime(),
                fixture.sut.getOpenCount(),
                fixture.sut.getBackgroundCount(),
                fixture.sut.getLastInteractionTime()
            )
        )
    }

    @Test
    fun `should have no cached configuration until one is cached`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val config = fixture.sut.getCachedConfiguration()

        then()
        assertNull(config)
    }

    @Test
    fun `should restore every cached configuration option`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val options = LifecycleTrackingOptions(
            appLifecycleEnabled = false,
            foregroundLifecycleEnabled = true,
            sessionStartEventsEnabled = false,
            sessionEndEventsEnabled = true,
            pushSubscriptionEnabled = false
        )
        val config = RetenoConfig.Builder()
            .accessKey("access")
            .lifecycleTrackingOptions(options)
            .pauseInAppMessages(true)
            .pausePushInAppMessages(true)
            .setPlatform("flutter")
            .setDebug(true)
            .sessionDuration(90_000L)
            .customDeviceIdProvider { "ignored" }
            .build()
        fixture.sut.cacheConfiguration("device-body:suffix", config)

        whenn()
        val cached = requireNotNull(fixture.sut.getCachedConfiguration())

        then()
        assertEquals(
            listOf<Any?>("access", options, true, true, "flutter", true, 90_000L, "device-body"),
            listOf<Any?>(
                cached.accessKey, cached.lifecycleTrackingOptions, cached.isPausedInAppMessages,
                cached.isPausedPushInAppMessages, cached.platform, cached.isDebug, cached.sessionDuration,
                cached.userIdProvider?.getDeviceId()
            )
        )
        assertEquals("device-body:suffix", fixture.sut.getLastStoredId())
    }

    @Test
    fun `should restore cached configuration without custom device id with defaults`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.raw.edit()
            .putString("config_last_id", "device-body")
            .putString("config_access_key", "access")
            .commit()

        whenn()
        val cached = requireNotNull(fixture.sut.getCachedConfiguration())

        then()
        assertEquals(
            listOf<Any?>(false, false, "", false, SessionTracker.DEFAULT_SESSION_RESET_TIME, null),
            listOf<Any?>(
                cached.isPausedInAppMessages, cached.isPausedPushInAppMessages, cached.platform,
                cached.isDebug, cached.sessionDuration, cached.userIdProvider
            )
        )
        assertTrue(cached.lifecycleTrackingOptions.appLifecycleEnabled)
    }

    @Test
    fun `should restore no custom device id provider when none was cached`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.cacheConfiguration("device-body", RetenoConfig.Builder().accessKey("access").build())

        whenn()
        val cached = requireNotNull(fixture.sut.getCachedConfiguration())

        then()
        assertNull(cached.userIdProvider)
    }

    @Test
    fun `should fail to restore a cached configuration that lost its access key`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.raw.edit().putString("config_last_id", "device-body").commit()

        whenn()
        val restored = runCatching { fixture.sut.getCachedConfiguration() }

        then()
        assertTrue(restored.exceptionOrNull() is IllegalArgumentException)
    }
}
