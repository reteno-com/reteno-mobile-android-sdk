package com.reteno.core.di

import com.reteno.core.domain.model.appinbox.AppInboxMessages
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.domain.model.interaction.InAppInteraction
import com.reteno.core.domain.model.interaction.InAppInteractionStatus
import com.reteno.core.domain.model.interaction.InteractionAction
import com.reteno.core.domain.model.interaction.InteractionStatus
import com.reteno.core.features.appinbox.AppInboxStatus
import com.reteno.core.features.iam.InAppPauseBehaviour
import com.reteno.core.fixtures.FakeSdkConfiguration
import com.reteno.core.fixtures.RecordingRecommendationCallback
import com.reteno.core.fixtures.RecordingRecommendationJsonCallback
import com.reteno.core.fixtures.RecordingResultCallback
import com.reteno.core.fixtures.STUB_TIME
import com.reteno.core.fixtures.StubRecom
import com.reteno.core.fixtures.stubAnonymousAttributes
import com.reteno.core.fixtures.stubConfig
import com.reteno.core.fixtures.stubCustomEvent
import com.reteno.core.fixtures.stubEcomEvent
import com.reteno.core.fixtures.stubInAppMessage
import com.reteno.core.fixtures.stubRecomEvents
import com.reteno.core.fixtures.stubRetenoLogEvent
import com.reteno.core.fixtures.stubUser
import com.reteno.core.internal.action.sync.BackgroundSyncOutcome
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.test.given
import com.reteno.test.runUnitTest
import com.reteno.test.then
import com.reteno.test.whenn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class UnsupportedOsActionsTest {

    private class SutFixture {
        val configuration = FakeSdkConfiguration()
        val sut = UnsupportedOsActions(configuration)
        val inboxMessages = RecordingResultCallback<AppInboxMessages>()
        val inboxCount = RecordingResultCallback<Int>()
        val allOpened = RecordingResultCallback<Unit>()
        val recommendation = RecordingRecommendationCallback<StubRecom>()
        val recommendationJson = RecordingRecommendationJsonCallback()

        fun callbacksAnswered(): Int = listOf(
            inboxMessages.results.size + inboxMessages.failures.size,
            inboxCount.results.size + inboxCount.failures.size,
            allOpened.results.size + allOpened.failures.size,
            recommendation.results.size + recommendation.rawResponses.size + recommendation.failures.size,
            recommendationJson.results.size + recommendationJson.failures.size
        ).sum()

        fun callEveryOperation() {
            sut.boot()
            sut.requestStart()
            sut.requestStop()
            sut.submitUserAttributes("user-1", stubUser())
            sut.submitMultiAccountUserAttributes("account-1", null)
            sut.submitAnonymousUserAttributes(stubAnonymousAttributes())
            sut.submitFcmToken("token")
            sut.submitNotificationsEnabled(true)
            sut.submitEvent(stubCustomEvent())
            sut.submitEcomEvent(stubEcomEvent())
            sut.submitScreenView("Home")
            sut.submitRetenoLogEvent(stubRetenoLogEvent())
            sut.submitLifecycleEventConfig(LifecycleTrackingOptions.NONE)
            sut.submitAutoScreenTracking(ScreenTrackingConfig(enable = true))
            sut.submitPushPermissionStatus()
            sut.submitSessionDuration(5_000L)
            sut.requestSync()
            sut.submitPushInteraction("interaction-1", InteractionStatus.CLICKED)
            sut.submitInteractionClick("interaction-2", InteractionAction("OPEN_URL", null, null), forcePush = true)
            sut.submitInAppInteraction(InAppInteraction("in-app-1", STUB_TIME, 42L, InAppInteractionStatus.OPENED))
            sut.submitInAppShown(stubInAppMessage())
            sut.submitDeeplinkClick("https://track.example/c/1")
            sut.submitIamFromPush("interaction-3")
            sut.submitDefaultNotificationChannel("{}")
            sut.submitInAppPause(true)
            sut.submitInAppPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
            sut.submitPushInAppPause(true)
            sut.submitPushInAppPauseBehaviour(InAppPauseBehaviour.SKIP_IN_APPS)
            sut.submitInAppLifecycleCallback(null)
            sut.submitInboxMessagesRequest(1, 20, AppInboxStatus.OPENED, inboxMessages)
            sut.submitInboxCountRequest(inboxCount)
            sut.subscribeInboxCount(inboxCount)
            sut.unsubscribeInboxCount(inboxCount)
            sut.unsubscribeAllInboxCount()
            sut.submitInboxMessageOpened("message-1")
            sut.submitAllInboxMessagesOpened(allOpened)
            sut.submitRecommendationRequest("r1v2", "{}", StubRecom::class.java, recommendation)
            sut.submitRecommendationJsonRequest("r1v2", "{}", recommendationJson)
            sut.submitRecommendationEvents(stubRecomEvents())
        }
    }

    @Test
    fun `should report the sdk as neither initialized nor started`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val state = listOf(fixture.sut.isInitialized, fixture.sut.isStarted)

        then()
        assertEquals(listOf(false, false), state)
    }

    @Test
    fun `should answer push queries with safe defaults`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val answers = with(fixture.sut) {
            listOf<Any>(
                deviceId,
                defaultNotificationChannel,
                canPresentMessages(),
                isActivityPresented(),
                hasDataForSync()
            )
        }

        then()
        assertEquals(listOf<Any>("", "", false, false, true), answers)
    }

    @Test
    fun `should report nothing to sync in background`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        val outcome = fixture.sut.backgroundSync()

        then()
        assertEquals(BackgroundSyncOutcome.NO_DATA, outcome.getOrNull())
    }

    @Test
    fun `should keep the user configuration without starting the sdk`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val config = stubConfig()

        whenn()
        fixture.sut.applyUserConfig(config)

        then()
        assertSame(config, fixture.configuration.config)
    }

    @Test
    fun `should never run operations waiting for sdk initialization`() = runUnitTest {
        given()
        val fixture = SutFixture()
        val ran = mutableListOf<String>()

        whenn()
        fixture.sut.runAfterInit { ran += "operation" }

        then()
        assertTrue(ran.isEmpty())
    }

    @Test
    fun `should ignore every operation without answering callbacks`() = runUnitTest {
        given()
        val fixture = SutFixture()

        whenn()
        fixture.callEveryOperation()

        then()
        assertEquals(0, fixture.callbacksAnswered())
        assertNull(fixture.configuration.config)
    }
}
