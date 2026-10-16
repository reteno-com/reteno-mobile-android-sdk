package com.reteno.core

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.domain.model.recommendation.post.RecomEvents
import com.reteno.core.domain.model.user.User
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.fixtures.FakeFacadeInfrastructure
import com.reteno.core.fixtures.FakeSdkActions
import com.reteno.core.fixtures.ManualLifecycleOwner
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.stubUser
import com.reteno.core.internal.action.sync.BackgroundSync
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.Procedure
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.mockk
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

    private class SutFixture(scope: TestScope) {
        private val application = ApplicationProvider.getApplicationContext<Application>()
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val actions = FakeSdkActions()
        val infrastructure = FakeFacadeInfrastructure(actions, dispatcher)
        val owner = ManualLifecycleOwner()
        val sut = RetenoInternalImpl(
            application = application,
            appLifecycleOwner = owner,
            infrastructure = infrastructure
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
            sut.setLifecycleEventConfig(LifecycleTrackingOptions.NONE)
            sut.autoScreenTracking(ScreenTrackingConfig(enable = true))
            sut.updatePushPermissionStatus()
            sut.setSessionDuration(5_000L)
            sut.forcePushData()
            sut.recordInteractionAction("interaction-2", InteractionAction("OPEN_URL", null, null), forcePush = true)
            sut.deeplinkClicked("https://track.example/c/1", "https://example.com")
            sut.initializeIamView("interaction-3")
            sut.saveDefaultNotificationChannel("{}")
            sut.pausePushInAppMessages(true)
            sut.setPushInAppMessagesPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
            sut.setInAppLifecycleCallback(null)
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
        assertEquals(listOf("boot", "requestStart", "requestStop"), fixture.actions.log.calls)
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
        assertEquals(listOf(first), fixture.actions.control.configs)
    }

    @Test
    fun `should hold api calls until sdk is initialized and keep their order`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.callEveryDeferredApi()
        runCurrent()
        val beforeInit = fixture.actions.log.calls.toList()

        whenn()
        fixture.actions.control.markInitialized()
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
                "submitLifecycleEventConfig",
                "submitAutoScreenTracking",
                "submitPushPermissionStatus",
                "submitSessionDuration 5000",
                "requestSync",
                "submitInteractionClick interaction-2 OPEN_URL true",
                "submitDeeplinkClick https://track.example/c/1",
                "submitIamFromPush interaction-3",
                "submitDefaultNotificationChannel {}",
                "submitPushInAppPause true",
                "submitPushInAppPauseBehaviour SKIP_IN_APPS",
                "submitInAppLifecycleCallback false"
            ),
            fixture.actions.log.calls
        )
    }

    @Test
    fun `should submit api calls immediately once initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()

        whenn()
        fixture.sut.logScreenView("Home")

        then()
        assertEquals(listOf("boot", "submitScreenView Home"), fixture.actions.log.calls)
    }

    @Test
    fun `should forward no user when user attributes are set with external id only`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()

        whenn()
        fixture.sut.setUserAttributes("user-1")

        then()
        assertEquals(listOf<Any?>(null), fixture.actions.contact.users)
    }

    @Test
    fun `should forward the same user with user attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()
        val user = stubUser()

        whenn()
        fixture.sut.setUserAttributes("user-1", user)

        then()
        assertSame(user, fixture.actions.contact.users.single())
    }

    @Test
    fun `should forward the same user with multi account user attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()
        val user = stubUser()

        whenn()
        fixture.sut.setMultiAccountUserAttributes("account-1", user)

        then()
        assertSame(user, fixture.actions.contact.multiAccountUsers.single())
    }

    @Test
    fun `should forward the same anonymous user attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()
        val attributes = stubAnonymousAttributes()

        whenn()
        fixture.sut.setAnonymousUserAttributes(attributes)

        then()
        assertSame(attributes, fixture.actions.contact.anonymousAttributes.single())
    }

    @Test
    fun `should forward the same ecommerce event`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()
        val event = stubEcomEvent()

        whenn()
        fixture.sut.logEcommerceEvent(event)

        then()
        assertSame(event, fixture.actions.events.ecomEvents.single())
    }

    @Test
    fun `should forward the same auto screen tracking config`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()
        val config = ScreenTrackingConfig(enable = true, excludeScreens = listOf("Splash"))

        whenn()
        fixture.sut.autoScreenTracking(config)

        then()
        assertSame(config, fixture.actions.lifecycle.screenTrackingConfigs.single())
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
        fixture.actions.control.markInitialized()
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
        val initializedBefore = fixture.sut.isInitialized
        fixture.actions.control.isStarted = true

        whenn()
        fixture.actions.control.markInitialized()

        then()
        assertFalse(initializedBefore)
        assertTrue(fixture.sut.isInitialized)
        assertTrue(fixture.sut.isStarted)
    }

    @Test
    fun `should pass push interaction to sdk without waiting for init`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        fixture.sut.recordInteraction("interaction-1", InteractionStatus.DELIVERED, forcePush = true)

        then()
        assertEquals(listOf("boot", "submitPushInteraction interaction-1 DELIVERED"), fixture.actions.log.calls)
    }

    @Test
    fun `should pass in-app pause to sdk once initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.sut.pauseInAppMessages(true)
        runCurrent()
        val beforeInit = fixture.actions.log.calls.toList()

        whenn()
        fixture.actions.control.markInitialized()
        runCurrent()

        then()
        assertEquals(listOf("boot"), beforeInit)
        assertEquals(listOf("boot", "submitInAppPause true"), fixture.actions.log.calls)
    }

    @Test
    fun `should pass in-app pause behaviour to sdk once initialized`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()

        whenn()
        fixture.sut.setInAppMessagesPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)

        then()
        assertEquals(listOf("boot", "submitInAppPauseBehaviour SKIP_IN_APPS"), fixture.actions.log.calls)
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
        fixture.infrastructure.configuration.useUserConfig(config)

        then()
        assertSame(channelConfig, fixture.sut.getDefaultNotificationChannelConfig())
    }

    @Test
    fun `should keep deprecated instance pointing at the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val previous = Reteno.instanceInternal

        whenn()
        val instance = try {
            Reteno.instanceInternal = fixture.sut
            @Suppress("DEPRECATION")
            RetenoInternalImpl.instance
        } finally {
            Reteno.instanceInternal = previous
        }

        then()
        assertSame(fixture.sut, instance)
    }

    @Test
    fun `should answer presentation, device id, channel and pending data from the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.queries.canPresent = true
        fixture.actions.queries.activityPresented = true
        fixture.actions.queries.deviceId = "device-1"
        fixture.actions.queries.defaultNotificationChannel = "{}"
        fixture.actions.queries.dataForSync = false

        whenn()
        val answers = with(fixture.sut) {
            listOf<Any>(
                canPresentMessages(),
                isActivityPresented(),
                getDeviceId(),
                getDefaultNotificationChannel(),
                hasDataForSync()
            )
        }

        then()
        assertEquals(listOf<Any>(true, true, "device-1", "{}", false), answers)
    }

    @Test
    fun `should accept user attributes for a user without attributes`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()
        val user = User(subscriptionKeys = listOf("news"))

        whenn()
        fixture.sut.setUserAttributes("user-1", user)
        fixture.sut.setMultiAccountUserAttributes("account-1", user)

        then()
        val recorded = listOf(fixture.actions.contact.users, fixture.actions.contact.multiAccountUsers)
        assertEquals(listOf(listOf(user), listOf(user)), recorded)
    }

    @Test
    fun `should record interactions without forcing a push by default`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        fixture.actions.control.markInitialized()
        val facade: RetenoInternalFacade = fixture.sut

        whenn()
        facade.recordInteraction("interaction-1", InteractionStatus.CLICKED)
        facade.recordInteractionAction("interaction-2", InteractionAction("OPEN_URL", null, null))

        then()
        val expected = listOf(
            "submitPushInteraction interaction-1 CLICKED",
            "submitInteractionClick interaction-2 OPEN_URL false"
        )
        assertEquals(expected, fixture.actions.log.calls.filter { it.startsWith("submit") })
    }

    @Test
    fun `should give the same in-app inbox and recommendations every time`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val first = listOf(fixture.sut.appInbox, fixture.sut.recommendation)
        val second = listOf(fixture.sut.appInbox, fixture.sut.recommendation)

        fixture.sut.appInbox.markAsOpened("m1")
        fixture.sut.recommendation.logRecommendations(RecomEvents("variant", emptyList()))

        then()
        first.zip(second).forEach { (a, b) -> assertSame(a, b) }
        val forwarded = listOf("submitInboxMessageOpened m1", "submitRecommendationEvents variant")
        assertEquals(forwarded, fixture.actions.log.calls.filter { it in forwarded })
    }

    @Test
    fun `should run background sync of the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val sync = mockk<BackgroundSync>()
        coEvery { sync.invoke() } returns Result.success(BackgroundSyncOutcome.SYNCED)
        fixture.actions.sync.backgroundSyncAnswer = sync

        whenn()
        val outcome = fixture.sut.backgroundSync()

        then()
        assertEquals(BackgroundSyncOutcome.SYNCED, outcome.getOrNull())
    }

    @Test
    fun `should give no permission checker when no activity is resumed`() = runUnitTest {
        given()
        val fixture = SutFixture(this)

        whenn()
        val checker = fixture.sut.requestPermissionChecker()

        then()
        assertNull(checker)
    }

    @Test
    fun `should initialize through the static entry point from the deprecated instance api`() = runUnitTest {
        given()
        val fixture = SutFixture(this)
        val previous = Reteno.instanceInternal
        val config = stubConfig()

        whenn()
        try {
            Reteno.instanceInternal = fixture.sut
            @Suppress("DEPRECATION")
            fixture.sut.initWith(config)
        } finally {
            Reteno.instanceInternal = previous
        }

        then()
        assertEquals(listOf(config), fixture.actions.control.configs)
    }
}
