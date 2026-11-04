package com.reteno.core.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.data.remote.api.HttpMethod
import com.reteno.core.domain.ResponseCallback
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.fixtures.stubEventsDb
import com.reteno.core.internal.coroutine.RetenoDispatchers
import com.reteno.core.internal.log.AndroidLogSink
import com.reteno.core.util.Logger
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class SdkComponentTest {

    private class SutFixture(osSupported: Boolean = true) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val sut = SdkComponent(
            application = application,
            dispatchers = object : RetenoDispatchers {
                override val main = Dispatchers.Unconfined
                override val io = Dispatchers.Unconfined
                override val default = Dispatchers.Unconfined
            },
            isOsSupported = { osSupported }
        )

        fun <T> withSdkLogging(block: () -> T): T {
            try {
                return block()
            } finally {
                Logger.install(AndroidLogSink(), null)
            }
        }

        fun sdkParts(): List<Any> = listOf(sut.actions, sut.configuration, sut.sessionHandler)

        fun collaborators(): List<Any> = listOf(
            sut.prefs,
            sut.restConfig,
            sut.restClient,
            sut.configRepository,
            sut.fileManager,
            sut.activityHelper,
            sut.permissions,
            sut.db
        )

    }

    @Test
    fun `should hand out one instance of each shared collaborator`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val first = fixture.collaborators()

        whenn()
        System.gc()
        val second = fixture.collaborators()

        then()
        first.zip(second).forEach { (before, after) -> assertSame(before, after) }
    }

    @Test
    fun `should run sdk actions on a supported os`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val actions = fixture.withSdkLogging { fixture.sut.actions }

        then()
        assertTrue(actions is SdkActionsImpl)
    }

    @Test
    fun `should ignore sdk actions on an unsupported os`() = runUnitTest {
        given()
        val fixture = SutFixture(osSupported = false)

        whenn()
        val actions = fixture.sut.actions

        then()
        assertTrue(actions is UnsupportedOsActions)
    }

    @Test
    fun `should report unsynced data stored through a single database manager`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val before = fixture.sut.db.databaseManager.hasDataForSync()

        whenn()
        fixture.sut.db.eventsDb.insertEvents(stubEventsDb())

        then()
        assertEquals(listOf(false, true), listOf(before, fixture.sut.db.databaseManager.hasDataForSync()))
    }

    @Test
    fun `should build sdk actions, configuration and session tracking once`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val first = fixture.withSdkLogging { fixture.sdkParts() }
        val second = fixture.sdkParts()

        then()
        first.zip(second).forEach { (before, after) -> assertSame(before, after) }
    }

    @Test
    fun `should identify the device by the custom id provider of the applied configuration`() = runUnitTest {
        given()
        val fixture = SutFixture()
        fixture.sut.configuration.useUserConfig(stubConfig(customDeviceId = true))

        whenn()
        fixture.sut.restConfig.initDeviceId(DeviceIdMode.CLIENT_UUID)

        then()
        assertEquals("custom-id", fixture.sut.restConfig.deviceId.idBody)
    }

    @Test
    fun `should fail a server request made before any configuration is applied`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val failures = mutableListOf<Throwable?>()
        val callback = object : ResponseCallback {
            override fun onSuccess(response: String) = Unit

            override fun onFailure(statusCode: Int?, response: String?, throwable: Throwable?) {
                failures += throwable
            }
        }

        whenn()
        fixture.sut.restClient.makeRequest(HttpMethod.GET, ApiContract.MobileApi.Device, null, null, null, 0, callback)

        then()
        assertTrue(failures.single() is IllegalArgumentException)
    }

    @Test
    fun `should start a session with a fresh id`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val sessionId = fixture.withSdkLogging {
            fixture.sut.sessionHandler.start()
            fixture.sut.sessionHandler.getSessionId()
        }

        then()
        assertEquals(36, sessionId.length)
    }
}
