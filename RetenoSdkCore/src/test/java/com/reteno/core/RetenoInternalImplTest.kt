package com.reteno.core

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
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
            ioDispatcher = dispatcher,
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
                "requestSync"
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
        assertSame(config, fixture.serviceLocator.currentConfig)
    }
}
