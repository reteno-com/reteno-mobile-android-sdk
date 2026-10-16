package com.reteno.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.fixtures.FakeFacadeInfrastructure
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.fixtures.onAndroidSdk
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.identification.DeviceIdProvider
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoEntryPointsTest {

    private class SutFixture(scope: TestScope) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val actions = FakeSdkActions()
        val facade = RetenoInternalImpl(
            application = application,
            mainDispatcher = StandardTestDispatcher(scope.testScheduler),
            appLifecycleOwner = ManualLifecycleOwner(),
            infrastructure = FakeFacadeInfrastructure(actions),
            isOsSupported = { true }
        )

        fun <T> withInstance(instance: RetenoInternalImpl?, block: () -> T): T {
            val previous = Reteno.instanceInternal
            Reteno.instanceInternal = instance
            try {
                return block()
            } finally {
                Reteno.instanceInternal = previous
            }
        }
    }

    @Test
    fun `should create the sdk for the application at startup`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val initializer = RetenoInitializer()

        whenn()
        val (created, instance) = fixture.withInstance(null) {
            onAndroidSdk(UNSUPPORTED_SDK) {
                val created = initializer.create(fixture.application)
                created to Reteno.instance
            }
        }

        then()
        assertSame(instance, created)
        assertTrue(created is RetenoInternalImpl)
        assertEquals(emptyList<Any>(), initializer.dependencies())
    }

    @Test
    fun `should keep the configuration given at the static entry point`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val config = stubConfig()

        whenn()
        val initialized = fixture.withInstance(null) {
            onAndroidSdk(UNSUPPORTED_SDK) {
                Reteno.create(fixture.application)
                Reteno.initWithConfig(config)
                (Reteno.instance as RetenoInternalImpl).getDefaultNotificationChannelConfig()
            }
        }

        then()
        assertEquals(config.defaultNotificationChannelConfig, initialized)
    }

    @Test
    fun `should explain an sdk used before the application started`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val failure = fixture.withInstance(null) { runCatching { Reteno.instance }.exceptionOrNull() }

        then()
        assertTrue(failure is IllegalArgumentException)
        assertEquals("Trying to access Reteno instance before Application.onCreate()", failure?.message)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `should hand out the sdk instance through the deprecated factory`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val instance = fixture.withInstance(fixture.facade) { RetenoImpl(fixture.application) }

        then()
        assertSame(fixture.facade, instance)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `should initialize through the deprecated factory with the given access key`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val config = stubConfig()

        whenn()
        val instance = fixture.withInstance(fixture.facade) { RetenoImpl(fixture.application, "access-2", config) }

        then()
        assertSame(fixture.facade, instance)
        assertEquals(listOf("access-2"), fixture.actions.configs.map { it.accessKey })
    }

    @Suppress("DEPRECATION")
    @Test
    fun `should refuse a blank access key in the deprecated factory`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val failure = fixture.withInstance(fixture.facade) {
            runCatching { RetenoImpl(fixture.application, " ", stubConfig()) }.exceptionOrNull()
        }

        then()
        assertTrue(failure is IllegalArgumentException)
        assertEquals("Access key can't be blank.", failure?.message)
        assertTrue(fixture.actions.configs.isEmpty())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `should still fail the deprecated factory without a config because its default config has no access key`() =
        runUnitTest {
            given()
            val fixture = SutFixture(this)

            whenn()
            val failure = fixture.withInstance(fixture.facade) {
                runCatching { RetenoImpl(fixture.application, "access-1") }.exceptionOrNull()
            }

            then()
            assertTrue(failure is IllegalArgumentException)
            assertTrue(fixture.actions.configs.isEmpty())
        }

    @Suppress("DEPRECATION")
    @Test
    fun `should build a config through the deprecated config factory`() = runUnitTest {
        given()
        val provider = DeviceIdProvider { "custom-id" }

        whenn()
        val config = RetenoConfig(
            isPausedInAppMessages = true,
            userIdProvider = provider,
            lifecycleTrackingOptions = LifecycleTrackingOptions.NONE,
            accessKey = "access-1",
            isPausedPushInAppMessages = true
        )

        then()
        val settings = with(config) {
            listOf(
                isPausedInAppMessages,
                userIdProvider,
                lifecycleTrackingOptions,
                accessKey,
                isPausedPushInAppMessages
            )
        }
        assertEquals(listOf(true, provider, LifecycleTrackingOptions.NONE, "access-1", true), settings)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `should still fail the deprecated config factory without an access key`() = runUnitTest {
        given()
        val defaults = LifecycleTrackingOptions.DEFAULT

        whenn()
        val failure = runCatching { RetenoConfig(lifecycleTrackingOptions = defaults) }.exceptionOrNull()

        then()
        assertTrue(failure is IllegalArgumentException)
        assertFalse(failure?.message.isNullOrBlank())
    }

    companion object {
        private const val UNSUPPORTED_SDK = 25
    }
}
