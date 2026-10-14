package com.reteno.core.di

import com.reteno.core.data.remote.api.ApiContract
import com.reteno.core.domain.model.event.LifecycleEventType
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.fixtures.LocatorFixture
import com.reteno.core.fixtures.STUB_TIME
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.core.fixtures.stubUser
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.util.Constants
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ActionsLocatorFacadeWiringTest {

    @Test
    fun `should run submitted contact updates in order`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        every { fixture.configRepository.getStoredFcmToken() } returns "old"
        every { fixture.configRepository.isNotificationsEnabled() } returns false

        whenn()
        fixture.sut.submitUserAttributes("user-1", stubUser())
        fixture.sut.submitMultiAccountUserAttributes("account-1", null)
        fixture.sut.submitAnonymousUserAttributes(stubAnonymousAttributes())
        fixture.sut.submitFcmToken("new")
        fixture.sut.submitNotificationsEnabled(true)
        runCurrent()

        then()
        verify(exactly = 2) { fixture.usersDb.insertUser(any()) }
        verify(exactly = 1) { fixture.configRepository.setDeviceIdSuffix("account-1") }
        verify(exactly = 1) { fixture.configRepository.saveFcmToken("new") }
        verify(exactly = 1) { fixture.configRepository.saveNotificationsEnabled(true) }
    }

    @Test
    fun `should log submitted events`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val custom = stubCustomEvent()

        whenn()
        fixture.sut.submitEvent(custom)
        fixture.sut.submitEcomEvent(stubEcomEvent())
        fixture.sut.submitScreenView("Home")
        fixture.sut.submitRetenoLogEvent(stubRetenoLogEvent())
        runCurrent()

        then()
        assertEquals(3, fixture.insertedEvents.size)
        assertTrue(fixture.insertedEventKeys.containsAll(listOf(custom.eventTypeKey, "screenView")))
        verify(exactly = 1) { fixture.logEventsDb.insertLogEvent(any()) }
    }

    @Test
    fun `should apply submitted lifecycle and screen tracking options`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitLifecycleEventConfig(LifecycleTrackingOptions.NONE)
        fixture.sut.submitAutoScreenTracking(ScreenTrackingConfig(enable = true))
        fixture.sut.submitPushPermissionStatus()
        fixture.sut.submitSessionDuration(5_000L)
        runCurrent()

        then()
        assertFalse(fixture.lifecycleEventFilter.isEnabled(LifecycleEventType.APP_LIFECYCLE))
        assertNotNull(fixture.platform.onScreen)
        assertEquals(listOf(Constants.BROADCAST_ACTION_PUSH_PERMISSION_CHANGED), fixture.platform.broadcasts)
        verify(exactly = 1) { fixture.sessionHandler.setSessionResetDuration(5_000L) }
    }

    @Test
    fun `should run requested sync`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.requestSync()
        runCurrent()

        then()
        verify(atLeast = 1) { fixture.devicesDb.getDevices(any()) }
    }

    @Test
    fun `should record submitted push interaction through its stored request`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val cached = fixture.storeInteractionRequests()
        coEvery { fixture.configRepository.getFcmToken() } returns "token"

        whenn()
        fixture.sut.submitPushInteraction("interaction-1", InteractionStatus.DELIVERED)
        runCurrent()

        then()
        verify(exactly = 1) {
            fixture.interactionsDb.insertInteraction(
                match { it.interactionId == "interaction-1" && it.token == "token" }
            )
        }
        assertTrue(cached.isEmpty())
        verify(atLeast = 1) { fixture.interactionsDb.getInteractions(any()) }
    }

    @Test
    fun `should record submitted interaction click`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val action = InteractionAction("OPEN_URL", null, null)

        whenn()
        fixture.sut.submitInteractionClick("interaction-1", action, forcePush = false)
        runCurrent()

        then()
        verify(exactly = 1) {
            fixture.interactionsDb.insertInteraction(
                match { it.interactionId == "interaction-1" && it.action?.contains("OPEN_URL") == true }
            )
        }
    }

    @Test
    fun `should keep push interaction stored until sdk is initialized`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this, initialized = false)
        val cached = fixture.storeInteractionRequests()
        coEvery { fixture.configRepository.getFcmToken() } returns "token"
        fixture.sut.submitPushInteraction("interaction-1", InteractionStatus.DELIVERED)
        runCurrent()
        val cachedBeforeInit = cached.map { it.id }

        whenn()
        fixture.initGate.markInitialized(byUser = false)
        runCurrent()

        then()
        assertEquals(listOf("interaction-1"), cachedBeforeInit)
        verify(exactly = 1) { fixture.interactionsDb.insertInteraction(match { it.interactionId == "interaction-1" }) }
        assertTrue(cached.isEmpty())
    }

    @Test
    fun `should track submitted deeplink click`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitDeeplinkClick("https://track.example/c/1")
        runCurrent()

        then()
        verify(exactly = 1) { fixture.wrappedLinksDb.insertWrappedLink("https://track.example/c/1") }
    }

    @Test
    fun `should show in-app message from push`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitIamFromPush("interaction-1")
        runCurrent()

        then()
        assertTrue(fixture.platform.screen.calls.single().startsWith("create"))
        val widgetRequests = fixture.rest.requests.filter {
            it.contract is ApiContract.InAppMessages.GetInnAppWidgetByInteractionId
        }
        assertEquals(1, widgetRequests.size)
    }

    @Test
    fun `should save submitted default notification channel`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)

        whenn()
        fixture.sut.submitDefaultNotificationChannel("{}")
        runCurrent()

        then()
        verify(exactly = 1) { fixture.configRepository.saveDefaultNotificationChannel("{}") }
    }

    @Test
    fun `should record submitted in-app interaction`() = runUnitTest {
        given()
        val fixture = LocatorFixture(this)
        val interaction = InAppInteraction("in-app-1", STUB_TIME, 42L, InAppInteractionStatus.OPENED)

        whenn()
        fixture.sut.submitInAppInteraction(interaction)
        fixture.sut.submitInteractionClick("in-app-1", InteractionAction("OPEN_URL", null, null), forcePush = true)
        runCurrent()

        then()
        verify(exactly = 1) { fixture.inAppInteractionsDb.insertInteraction(any()) }
        verify(exactly = 1) { fixture.interactionsDb.insertInteraction(match { it.interactionId == "in-app-1" }) }
    }
}
