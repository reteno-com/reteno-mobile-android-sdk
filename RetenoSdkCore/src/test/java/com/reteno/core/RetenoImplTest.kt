package com.reteno.core

import android.app.Application
import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.domain.model.event.Event
import com.reteno.core.domain.model.event.Parameter
import com.reteno.core.domain.model.user.User
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class RetenoImplTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val EXTERNAL_USER_ID = "external_user_ID"

        private val USER_SUBSCRIPTION_KEYS = listOf("SUBSCRIPTION_KEYS")
        private val USER_GROUP_NAMES_INCLUDE = listOf("GROUP_NAMES_INCLUDE")
        private val USER_GROUP_NAMES_EXCLUDE = listOf("GROUP_NAMES_EXCLUDE")

        private const val EVENT_TYPE_KEY = "EVENT_TYPE_KEY"
        private const val EVENT_PARAMETER_KEY_1 = "KEY1"
        private const val EVENT_PARAMETER_VALUE_1 = "VALUE1"
    }
    // endregion constants -------------------------------------------------------------------------

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var contactController: ContactController

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    @RelaxedMockK
    private lateinit var eventController: EventController
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { anyConstructed<ServiceLocator>().scheduleControllerProvider.get() } returns scheduleController
        every { anyConstructed<ServiceLocator>().eventsControllerProvider.get() } returns eventController
    }

    override fun after() {
        super.after()
        unmockkConstructor(ServiceLocator::class)
    }

    @Test
    fun externalId_whenSetUserAttributes_thenInteractWithController() {
        val application = mockk<Application>()

        val retenoImpl = RetenoImpl(application, "")

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }
    }

    @Test
    fun externalIdAndUserNull_whenSetUserAttributesWithUser_thenInteractWithController() {
        val application = mockk<Application>()

        val retenoImpl = RetenoImpl(application, "")

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, null)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }
    }

    @Test
    fun externalIdAndUser_whenSetUserAttributesWithUser_thenInteractWithController() {
        val userFull = User(
            userAttributes = null,
            subscriptionKeys = USER_SUBSCRIPTION_KEYS,
            groupNamesInclude = USER_GROUP_NAMES_INCLUDE,
            groupNamesExclude = USER_GROUP_NAMES_EXCLUDE
        )
        val application = mockk<Application>()

        val retenoImpl = RetenoImpl(application, "")

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, userFull)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify { contactController.setUserData(userFull) }
    }

    @Test
    fun whenLogEvent_thenInteractWithEventController() {
        val application = mockk<Application>()
        val retenoImpl = RetenoImpl(application, "")

        val event = Event.Custom(
            typeKey = EVENT_TYPE_KEY,
            dateOccurred = ZonedDateTime.now(),
            parameters = listOf(Parameter(EVENT_PARAMETER_KEY_1, EVENT_PARAMETER_VALUE_1))
        )
        retenoImpl.logEvent(event)
        verify { eventController.trackEvent(event) }
    }

    @Test
    fun whenSetDeviceIdMode_thenIdHasChanged() {
        var lambdaCalled = false
        val deviceIdMode = DeviceIdMode.ANDROID_ID
        val application = mockk<Application>()
        val callback = { lambdaCalled = true }
        every { contactController.setDeviceIdMode(any(), captureLambda()) } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val retenoImpl = RetenoImpl(application, "")

        retenoImpl.setDeviceIdMode(deviceIdMode, callback)
        verify { contactController.setDeviceIdMode(deviceIdMode, any()) }
        assertTrue(lambdaCalled)
    }

    @Test
    fun whenSetDeviceIdMode_thenIdHasNotChanged() {
        var lambdaCalled = false
        val deviceIdMode = DeviceIdMode.ANDROID_ID
        val application = mockk<Application>()
        val callback = { lambdaCalled = true }
        justRun { contactController.setDeviceIdMode(any(), captureLambda()) }

        val retenoImpl = RetenoImpl(application, "")

        retenoImpl.setDeviceIdMode(deviceIdMode, callback)
        verify { contactController.setDeviceIdMode(deviceIdMode, any()) }
        assertFalse(lambdaCalled)
    }

    @Test
    fun whenResumeApp_thenStartScheduler() {
        val retenoImpl = RetenoImpl(mockk(), "")
        retenoImpl.resume(mockk())

        verify { scheduleController.startScheduler() }
    }

    @Test
    fun whenPauseApp_thenStopScheduler() {
        val retenoImpl = RetenoImpl(mockk(), "")
        retenoImpl.pause(mockk())

        verify { scheduleController.stopScheduler() }
    }

    @Test
    fun whenForcePush_thenCallScheduleController() {
        val retenoImpl = RetenoImpl(mockk(), "")

        retenoImpl.forcePushData()

        verify(exactly = 1) { scheduleController.forcePush() }
    }

    @Test
    fun whenResumeApp_thenStartScheduler_thenCalledClearOleEvents() {
        val retenoImpl = RetenoImpl(mockk(), "")
        retenoImpl.resume(mockk())

        verify { scheduleController.clearOldData() }
    }

}