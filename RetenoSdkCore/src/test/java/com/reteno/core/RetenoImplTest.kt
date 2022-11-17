package com.reteno.core

import android.app.Application
import com.reteno.core.RetenoImpl.Companion.application
import com.reteno.core.appinbox.AppInboxImpl
import com.reteno.core.base.BaseUnitTest
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.user.User
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.ScreenTrackingConfig
import com.reteno.core.lifecycle.ScreenTrackingTrigger
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class RetenoImplTest : BaseRobolectricTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val EXTERNAL_USER_ID = "external_user_ID"
        private val USER_SUBSCRIPTION_KEYS = listOf("SUBSCRIPTION_KEYS")

        private val USER_GROUP_NAMES_INCLUDE = listOf("GROUP_NAMES_INCLUDE")
        private val USER_GROUP_NAMES_EXCLUDE = listOf("GROUP_NAMES_EXCLUDE")
        private const val EVENT_TYPE_KEY = "EVENT_TYPE_KEY"

        private const val EVENT_PARAMETER_KEY_1 = "KEY1"
        private const val EVENT_PARAMETER_VALUE_1 = "VALUE1"

        private const val TRACK_SCREEN_NAME = "ScreenNameHere"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactController: ContactController

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    @RelaxedMockK
    private lateinit var eventController: EventController

    @RelaxedMockK
    private lateinit var retenoActivityHelper: RetenoActivityHelper

    @RelaxedMockK
    private lateinit var inbox: AppInboxImpl

    private val retenoImpl by lazy { RetenoImpl(application, "") }
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().eventsControllerProvider.get() } returns eventController
        every { anyConstructed<ServiceLocator>().appInboxProvider.get() } returns inbox
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns retenoActivityHelper
    }

    override fun after() {
        super.after()
        unmockkConstructor(ServiceLocator::class)
    }

    @Test
    fun externalId_whenSetUserAttributes_thenInteractWithController() {
        // When
        retenoImpl.setUserAttributes(EXTERNAL_USER_ID)

        // Then
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }
    }

    @Test
    fun externalIdAndUserNull_whenSetUserAttributesWithUser_thenInteractWithController() {
        // When
        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, null)

        // Then
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }
    }

    @Test
    fun externalIdAndUser_whenSetUserAttributesWithUser_thenInteractWithController() {
        // Given
        val userFull = User(
            userAttributes = null,
            subscriptionKeys = USER_SUBSCRIPTION_KEYS,
            groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
            groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
        )

        // When
        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, userFull)

        // Then
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify { contactController.setUserData(userFull) }
    }

    @Test
    fun whenLogEvent_thenInteractWithEventController() {
        // Given
        val event = Event.Custom(
            typeKey = EVENT_TYPE_KEY,
            dateOccurred = ZonedDateTime.now(),
            parameters = listOf(Parameter(EVENT_PARAMETER_KEY_1, EVENT_PARAMETER_VALUE_1))
        )

        // When
        retenoImpl.logEvent(event)

        // Then
        verify { eventController.trackEvent(event) }
    }

    @Test
    fun whenLogScreenView_thenInteractWithEventController() {
        // When
        retenoImpl.logScreenView(TRACK_SCREEN_NAME)

        // Then
        verify(exactly = 1) { eventController.trackScreenViewEvent(TRACK_SCREEN_NAME) }
    }

    @Test
    fun whenAutoScreenTracking_thenInteractWithActivityHelper() {
        // Given
        val config = ScreenTrackingConfig(true, listOf(), ScreenTrackingTrigger.ON_RESUME)

        // When
        retenoImpl.autoScreenTracking(config)

        // Then
        verify(exactly = 1) { retenoActivityHelper.autoScreenTracking(config) }
    }

    @Test
    fun whenResumeApp_thenStartScheduler() {
        // When
        retenoImpl.resume(mockk())

        // Then
        verify { scheduleController.startScheduler() }
    }

    @Test
    fun whenPauseApp_thenStopScheduler() {
        // When
        retenoImpl.pause(mockk())

        // Then
        verify { scheduleController.stopScheduler() }
    }

    @Test
    fun whenForcePush_thenCallScheduleController() {
        // When
        retenoImpl.forcePushData()

        // Then
        verify(exactly = 1) { scheduleController.forcePush() }
    }

    @Test
    fun whenResumeApp_thenStartScheduler_thenCalledClearOleEvents() {
        // When
        retenoImpl.resume(mockk())

        // Then
        verify { scheduleController.clearOldData() }
    }

    @Test
    fun getAppInbox() {
        val retenoImpl = RetenoImpl(mockk(), "")

        val result = retenoImpl.appInbox
        assertEquals(inbox, result)
    }

}