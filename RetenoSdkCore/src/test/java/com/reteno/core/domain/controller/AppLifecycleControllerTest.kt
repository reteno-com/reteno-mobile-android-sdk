package com.reteno.core.domain.controller

import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.base.robolectric.RetenoTestApp
import com.reteno.core.data.repository.ConfigRepository
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoSessionHandler
import com.reteno.core.lifecycle.RetenoSessionHandler.SessionEvent
import com.reteno.core.util.Util
import com.reteno.core.util.Util.asZonedDateTime
import io.mockk.MockKVerificationScope
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.milliseconds

@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
class AppLifecycleControllerTest : BaseRobolectricTest() {


    @RelaxedMockK
    private lateinit var configRepository: ConfigRepository

    @RelaxedMockK
    private lateinit var eventController: EventController

    @RelaxedMockK
    private lateinit var sessionHandler: RetenoSessionHandler

    @RelaxedMockK
    private lateinit var activityHelper: RetenoActivityHelper

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    @RelaxedMockK
    private lateinit var iamController: IamController

    override fun before() {
        super.before()
        unmockkStatic(ZonedDateTime::class)
        unmockkStatic("com.reteno.core.util.UtilKt")
        unmockkObject(Util)
        unmockkStatic(Util::class)
    }

    @Test
    fun whenApplicationOpen_thenAppOpenEventSent() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        val sut = createSUT(LifecycleTrackingOptions.ALL)

        sut.start()

        verify {
            eventController.trackEvent(eventMatcher(Event.applicationOpen(false).event))
        }
    }

    @Test
    fun whenApplicationOpen_thenOldEventsCleared() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        val sut = createSUT(LifecycleTrackingOptions.ALL)

        sut.start()

        verify {
            scheduleController.clearOldData()
        }
    }


    @Test
    fun given_whenControllerInit_thenBaseHtmlShouldBeFetched() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()

        val sut = createSUT(LifecycleTrackingOptions.ALL)

        sut.start()

        verify {
            iamController.preloadHtml()
        }
    }

    @Test
    fun whenApplicationStop_thenAppBackgroundedEventSent() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { sessionHandler.getForegroundTimeMillis() } returns 2000L
        val sut = createSUT(LifecycleTrackingOptions.ALL)
        val startTime = System.currentTimeMillis()

        sut.start()
        sut.stop()

        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationBackgrounded(
                        startTime,
                        2
                    ).event
                )
            )
        }
    }

    @Test
    fun givenAppBackgrounded_whenApplicationOpen_thenAppOpenedSentWithBackgroundedFlag() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { sessionHandler.getForegroundTimeMillis() } returns 2000L
        val sut = createSUT(LifecycleTrackingOptions.ALL)

        sut.start()
        sut.stop()
        sut.start()

        verify {
            eventController.trackEvent(eventMatcher(Event.applicationOpen(true).event))
        }
    }

    @Test
    fun whenSessionStartEventReceived_thenSessionStartEventLogged() = runTest {
        val sessionFlow = MutableSharedFlow<SessionEvent>()
        coEvery { sessionHandler.sessionEventFlow } returns sessionFlow
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        createSUT(LifecycleTrackingOptions.ALL)

        val time = System.currentTimeMillis()

        runCurrent()
        sessionFlow.emit(SessionEvent.SessionStartEvent("10", time))
        advanceUntilIdle()

        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.sessionStart(
                        "10",
                        time.asZonedDateTime()
                    ).event
                )
            )
        }

    }

    @Test
    fun whenSessionEndEventReceived_thenSessionEndEventLogged() = runTest {
        val sessionFlow = MutableSharedFlow<SessionEvent>()
        coEvery { sessionHandler.sessionEventFlow } returns sessionFlow
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        createSUT(LifecycleTrackingOptions.ALL)

        val time = System.currentTimeMillis()
        runCurrent()
        sessionFlow.emit(SessionEvent.SessionEndEvent("10", time, 1000L, 2, 1))
        advanceUntilIdle()
        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.sessionEnd(
                        "10",
                        time.asZonedDateTime(),
                        1,
                        2,
                        1
                    ).event
                )
            )
        }
    }

    @Test
    fun whenNotificationEnabled_thenNotificationEnabledEventLogged() = runTest {
        val notificationFlow = MutableStateFlow(false)
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns notificationFlow
        createSUT(LifecycleTrackingOptions.ALL)
        runCurrent()
        notificationFlow.value = true
        runCurrent()
        advanceUntilIdle()
        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.notificationsEnabled().event
                )
            )
        }
    }

    @Test
    fun whenNotificationDisabled_thenNotificationDisabledEventLogged() = runTest {
        val notificationFlow = MutableStateFlow(true)
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns notificationFlow
        createSUT(LifecycleTrackingOptions.ALL)
        runCurrent()
        notificationFlow.value = false
        runCurrent()
        advanceUntilIdle()
        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.notificationsDisabled().event
                )
            )
        }
    }

    @Test
    fun given_AppVersionNotExist_whenInit_thenAppInstallEventLogged() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { configRepository.getAppVersion() } returns ""
        coEvery { configRepository.getAppBuildNumber() } returns 0
        coEvery { configRepository.getAppPackageInfo() } returns PackageInfo().apply {
            versionName = "1.0.0"
            versionCode = 1
        }

        val sut = createSUT(LifecycleTrackingOptions.ALL)
        sut.initMetadata()

        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationInstall("1.0.0", 1).event
                )
            )
        }
        verify {
            configRepository.saveAppVersion("1.0.0")
            configRepository.saveAppBuildNumber(1)
        }
    }

    @Test
    fun given_AppVersionExist_whenInitWithNewVersion_thenAppUpdateEventLogged() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { configRepository.getAppVersion() } returns "1.0.0"
        coEvery { configRepository.getAppBuildNumber() } returns 0
        coEvery { configRepository.getAppPackageInfo() } returns PackageInfo().apply {
            versionName = "1.0.1"
            versionCode = 2
        }

        val sut = createSUT(LifecycleTrackingOptions.ALL)
        sut.initMetadata()

        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationUpdate("1.0.1", 2L, "1.0.0", 0L).event
                )
            )
        }
        verify {
            configRepository.saveAppVersion("1.0.1")
            configRepository.saveAppBuildNumber(2)
        }
    }

    @Test
    fun given_AppVersionExist_whenInitWithNewVersionCode_thenAppUpdateEventLogged() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { configRepository.getAppVersion() } returns "1.0.0"
        coEvery { configRepository.getAppBuildNumber() } returns 0
        coEvery { configRepository.getAppPackageInfo() } returns PackageInfo().apply {
            versionName = "1.0.0"
            versionCode = 2
        }


        val sut = createSUT(LifecycleTrackingOptions.ALL)

        sut.initMetadata()

        verify {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationUpdate("1.0.0", 2L, "1.0.0", 0L).event
                )
            )
        }
        verify {
            configRepository.saveAppVersion("1.0.0")
            configRepository.saveAppBuildNumber(2L)
        }
    }

    ///
    @Test
    fun whenApplicationOpenAndEventDisabled_thenAppOpenEventSent() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        val sut = createSUT(
            LifecycleTrackingOptions(
                appLifecycleEnabled = false
            )
        )

        sut.start()

        verify(exactly = 0) {
            eventController.trackEvent(eventMatcher(Event.applicationOpen(false).event))
        }
    }

    @Test
    fun whenApplicationStopAndEventDisabled_thenAppBackgroundedEventSent() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { sessionHandler.getForegroundTimeMillis() } returns 2000L
        val sut = createSUT(LifecycleTrackingOptions(
            appLifecycleEnabled = false
        ))
        val startTime = System.currentTimeMillis()

        sut.start()
        sut.stop()

        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationBackgrounded(
                        startTime.milliseconds.inWholeSeconds,
                        2
                    ).event
                )
            )
        }
    }

    @Test
    fun whenSessionStartEventReceivedAndEventDisabled_thenSessionStartEventLogged() = runTest {
        val sessionFlow = MutableSharedFlow<SessionEvent>()
        coEvery { sessionHandler.sessionEventFlow } returns sessionFlow
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        createSUT(LifecycleTrackingOptions(
            sessionEventsEnabled = false
        ))

        val time = System.currentTimeMillis()

        runCurrent()
        sessionFlow.emit(SessionEvent.SessionStartEvent("10", time))
        advanceUntilIdle()

        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.sessionStart(
                        "10",
                        time.asZonedDateTime()
                    ).event
                )
            )
        }

    }

    @Test
    fun whenSessionEndEventReceivedAndEventDisabled_thenSessionEndEventLogged() = runTest {
        val sessionFlow = MutableSharedFlow<SessionEvent>()
        coEvery { sessionHandler.sessionEventFlow } returns sessionFlow
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        createSUT(LifecycleTrackingOptions(
            sessionEventsEnabled = false
        ))

        val time = System.currentTimeMillis()
        runCurrent()
        sessionFlow.emit(SessionEvent.SessionEndEvent("10", time, 1000L, 2, 1))
        advanceUntilIdle()
        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.sessionEnd(
                        "10",
                        time.asZonedDateTime(),
                        1,
                        2,
                        1
                    ).event
                )
            )
        }
    }

    @Test
    fun whenNotificationEnabledAndEventDisabled_thenNotificationEnabledEventLogged() = runTest {
        val notificationFlow = MutableStateFlow(false)
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns notificationFlow
        createSUT(LifecycleTrackingOptions(
            pushSubscriptionEnabled = false
        ))
        runCurrent()
        notificationFlow.value = true
        runCurrent()
        advanceUntilIdle()
        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.notificationsEnabled().event
                )
            )
        }
    }

    @Test
    fun whenNotificationDisabledAndEventDisabled_thenNotificationDisabledEventLogged() = runTest {
        val notificationFlow = MutableStateFlow(true)
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns notificationFlow
        createSUT(LifecycleTrackingOptions(
            pushSubscriptionEnabled = false
        ))
        runCurrent()
        notificationFlow.value = false
        runCurrent()
        advanceUntilIdle()
        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.notificationsDisabled().event
                )
            )
        }
    }

    @Test
    fun given_AppVersionNotExist_whenInitAndEventDisabled_thenAppInstallEventLogged() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { configRepository.getAppVersion() } returns ""
        coEvery { configRepository.getAppBuildNumber() } returns 0
        coEvery { configRepository.getAppPackageInfo() } returns PackageInfo().apply {
            versionName = "1.0.0"
            versionCode = 1
        }

        val sut = createSUT(LifecycleTrackingOptions(
            appLifecycleEnabled = false
        ))
        val app = ApplicationProvider.getApplicationContext<RetenoTestApp>()
        sut.initMetadata()

        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationInstall("1.0.0", 1).event
                )
            )
        }
        verify {
            configRepository.saveAppVersion("1.0.0")
            configRepository.saveAppBuildNumber(1L)
        }
    }

    @Test
    fun given_AppVersionExist_whenInitWithNewVersionAndEventDisabled_thenAppUpdateEventLogged() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { configRepository.getAppVersion() } returns "1.0.0"
        coEvery { configRepository.getAppPackageInfo() } returns PackageInfo().apply {
            versionName = "1.0.1"
            versionCode = 2
        }
        coEvery { configRepository.getAppBuildNumber() } returns 0


        val sut = createSUT(LifecycleTrackingOptions(
            appLifecycleEnabled = false
        ))

        sut.initMetadata()

        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationUpdate("1.0.1", 2L, "1.0.0", 0L).event
                )
            )
        }
        verify {
            configRepository.saveAppVersion("1.0.1")
            configRepository.saveAppBuildNumber(2L)
        }
    }

    @Test
    fun given_AppVersionExist_whenInitWithNewVersionCodeAndEventDisabled_thenAppUpdateEventLogged() = runRetenoTest {
        coEvery { sessionHandler.sessionEventFlow } returns MutableSharedFlow()
        coEvery { configRepository.notificationState } returns MutableSharedFlow()
        coEvery { configRepository.getAppVersion() } returns "1.0.0"
        coEvery { configRepository.getAppBuildNumber() } returns 0
        coEvery { configRepository.getAppPackageInfo() } returns PackageInfo().apply {
            versionName = "1.0.0"
            versionCode = 2
        }

        val sut = createSUT(LifecycleTrackingOptions(
            appLifecycleEnabled = false
        ))

        sut.initMetadata()

        verify(exactly = 0) {
            eventController.trackEvent(
                eventMatcher(
                    Event.applicationUpdate("1.0.0", 2L, "1.0.0", 1L).event
                )
            )
        }
        verify {
            configRepository.saveAppVersion("1.0.0")
            configRepository.saveAppBuildNumber(2L)
        }
    }

    private fun MockKVerificationScope.eventMatcher(matcher: Event): Event = match {
        it.eventTypeKey == matcher.eventTypeKey &&
                it.params == matcher.params &&
                (it.occurred.toEpochSecond() - matcher.occurred.toEpochSecond()) < 1

    }

    private fun TestScope.createSUT(lifecycleTrackingOptions: LifecycleTrackingOptions) =
        AppLifecycleController(
            configRepository = configRepository,
            eventController = eventController,
            sessionHandler = sessionHandler,
            lifecycleTrackingOptions = lifecycleTrackingOptions,
            scope = backgroundScope,
            activityHelper = activityHelper,
            scheduleController = scheduleController,
            iamController = iamController
        )
}
