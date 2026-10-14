package com.reteno.core

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.core.fixtures.stubUser
import com.reteno.core.internal.coroutine.retenoDispatchers
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.Procedure
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class RetenoInternalImplTest {

    private class SutFixture(scope: TestScope, osSupported: Boolean = true) {
        private val application = ApplicationProvider.getApplicationContext<Application>()
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val serviceLocator = ServiceLocator(application, retenoDispatchers(dispatcher, dispatcher))
        val actions = FakeSdkActions()
        val owner = ManualLifecycleOwner()
        val sut = RetenoInternalImpl(
            application = application,
            mainDispatcher = dispatcher,
            appLifecycleOwner = owner,
            serviceLocator = serviceLocator,
            sdkActions = { actions },
            isOsSupported = { osSupported }
        )

        fun callEveryDeferredApi() {
            sut.setUserAttributes("user-1", stubUser())
            sut.setMultiAccountUserAttributes("account-1", null)
            sut.setAnonymousUserAttributes(stubAnonymousAttributes())
            sut.onNewFcmToken("token")
            sut.notificationsEnabled(true)
            sut.logEvent(stubCustomEvent())
            sut.logEcommerceEvent(stubEcomEvent())
            sut.logScreenView("Home")
            sut.logRetenoEvent(stubRetenoLogEvent())
            sut.setLifecycleEventConfig(LifecycleTrackingOptions.NONE)
            sut.autoScreenTracking(ScreenTrackingConfig(enable = true))
            sut.updatePushPermissionStatus()
            sut.setSessionDuration(5_000L)
            sut.forcePushData()
            sut.recordInteractionAction("interaction-2", InteractionAction("OPEN_URL", null, null), forcePush = true)
            sut.deeplinkClicked("https://track.example/c/1", "https://example.com")
            sut.initializeIamView("interaction-3")
            sut.saveDefaultNotificationChannel("{}")
        }
    }

    @Test
    fun `should boot sdk and follow app lifecycle`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        then()
        assertEquals(listOf("boot", "requestStart", "requestStop"), fixture.actions.calls)
    }

    @Test
    fun `should apply only the first user configuration`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val first = stubConfig()

        whenn()
        fixture.sut.setConfig(first)
        fixture.sut.setConfig(stubConfig(sessionDuration = 1L))

        then()
        assertEquals(listOf(first), fixture.actions.configs)
    }

    @Test
    fun `should hold api calls until sdk is initialized and keep their order`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.callEveryDeferredApi()
        runCurrent()
        val beforeInit = fixture.actions.calls.toList()

        whenn()
        fixture.serviceLocator.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(listOf("boot"), beforeInit)
        assertEquals(
            listOf(
                "boot",
                "submitUserAttributes user-1",
                "submitMultiAccountUserAttributes account-1",
                "submitAnonymousUserAttributes",
                "submitFcmToken",
                "submitNotificationsEnabled true",
                "submitEvent ${stubCustomEvent().eventTypeKey}",
                "submitEcomEvent",
                "submitScreenView Home",
                "submitRetenoLogEvent",
                "submitLifecycleEventConfig",
                "submitAutoScreenTracking",
                "submitPushPermissionStatus",
                "submitSessionDuration 5000",
                "requestSync",
                "submitInteractionClick interaction-2 OPEN_URL true",
                "submitDeeplinkClick https://track.example/c/1",
                "submitIamFromPush interaction-3",
                "submitDefaultNotificationChannel {}"
            ),
            fixture.actions.calls
        )
    }

    @Test
    fun `should submit api calls immediately once initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)

        whenn()
        fixture.sut.logScreenView("Home")

        then()
        assertEquals(listOf("boot", "submitScreenView Home"), fixture.actions.calls)
    }

    @Test
    fun `should forward no user when user attributes are set with external id only`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)

        whenn()
        fixture.sut.setUserAttributes("user-1")

        then()
        assertEquals(listOf<Any?>(null), fixture.actions.users)
    }

    @Test
    fun `should forward the same user with user attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)
        val user = stubUser()

        whenn()
        fixture.sut.setUserAttributes("user-1", user)

        then()
        assertSame(user, fixture.actions.users.single())
    }

    @Test
    fun `should forward the same user with multi account user attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)
        val user = stubUser()

        whenn()
        fixture.sut.setMultiAccountUserAttributes("account-1", user)

        then()
        assertSame(user, fixture.actions.multiAccountUsers.single())
    }

    @Test
    fun `should forward the same anonymous user attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)
        val attributes = stubAnonymousAttributes()

        whenn()
        fixture.sut.setAnonymousUserAttributes(attributes)

        then()
        assertSame(attributes, fixture.actions.anonymousAttributes.single())
    }

    @Test
    fun `should forward the same ecommerce event`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)
        val event = stubEcomEvent()

        whenn()
        fixture.sut.logEcommerceEvent(event)

        then()
        assertSame(event, fixture.actions.ecomEvents.single())
    }

    @Test
    fun `should forward the same auto screen tracking config`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)
        val config = ScreenTrackingConfig(enable = true, excludeScreens = listOf("Splash"))

        whenn()
        fixture.sut.autoScreenTracking(config)

        then()
        assertSame(config, fixture.actions.screenTrackingConfigs.single())
    }

    @Test
    fun `should run after init callbacks once initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val ran = mutableListOf<String>()
        fixture.sut.executeAfterInit { ran += "before init" }
        runCurrent()
        val beforeInit = ran.toList()

        whenn()
        fixture.serviceLocator.initGate.markInitialized(byUser = false)
        runCurrent()
        fixture.sut.executeAfterInit { ran += "after init" }

        then()
        assertEquals(emptyList<String>(), beforeInit)
        assertEquals(listOf("before init", "after init"), ran)
    }

    @Test
    fun `should expose init and start state`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val beforeInit = listOf(fixture.sut.isInitialized, fixture.sut.isInitializedByUser)
        fixture.actions.isStarted = true

        whenn()
        fixture.serviceLocator.initGate.markInitialized(byUser = false)

        then()
        assertEquals(listOf(false, false), beforeInit)
        assertTrue(fixture.sut.isInitialized)
        assertFalse(fixture.sut.isInitializedByUser)
        assertTrue(fixture.sut.isStarted)
    }

    @Test
    fun `should only keep configuration on unsupported os`() = runUnitTest {
        given()
        val fixture = SutFixture(this, osSupported = false)
        val config = stubConfig()

        whenn()
        fixture.sut.setConfig(config)
        fixture.serviceLocator.initGate.markInitialized(byUser = true)
        fixture.callEveryDeferredApi()
        fixture.sut.start()
        fixture.sut.stop()
        fixture.owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        runCurrent()

        then()
        assertEquals(emptyList<String>(), fixture.actions.calls)
        assertSame(config, fixture.serviceLocator.configuration.config)
    }

    @Test
    fun `should pass push interaction to sdk without waiting for init`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.recordInteraction("interaction-1", InteractionStatus.DELIVERED, forcePush = true)

        then()
        assertEquals(listOf("boot", "submitPushInteraction interaction-1 DELIVERED"), fixture.actions.calls)
    }

    @Test
    fun `should ignore push interaction on unsupported os`() = runUnitTest {
        given()
        val fixture = SutFixture(this, osSupported = false)

        whenn()
        fixture.sut.recordInteraction("interaction-1", InteractionStatus.CLICKED, forcePush = true)

        then()
        assertTrue(fixture.actions.calls.isEmpty())
    }

    @Test
    fun `should have no default notification channel config before configuration`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val config = fixture.sut.getDefaultNotificationChannelConfig()

        then()
        assertNull(config)
    }

    @Test
    fun `should expose default notification channel config of applied configuration`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val channelConfig = Procedure<NotificationChannelCompat.Builder> { }
        val config = RetenoConfig.Builder().accessKey("key").defaultNotificationChannelConfig(channelConfig).build()

        whenn()
        fixture.serviceLocator.configuration.useUserConfig(config)

        then()
        assertSame(channelConfig, fixture.sut.getDefaultNotificationChannelConfig())
    }
}
