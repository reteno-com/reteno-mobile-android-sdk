package com.reteno.core

import android.app.Application
import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.ScheduleController
import com.reteno.core.model.event.Event
import com.reteno.core.model.event.Parameter
import com.reteno.core.model.user.User
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class RetenoImplTest : BaseUnitTest() {

    @RelaxedMockK
    private lateinit var contactController: ContactController

    @RelaxedMockK
    private lateinit var scheduleController: ScheduleController

    @RelaxedMockK
    private lateinit var eventController: EventController

    companion object {
        private const val EXTERNAL_USER_ID = "external_user_ID"
    }

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

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }
    }

    @Test
    fun externalIdAndUserNull_whenSetUserAttributesWithUser_thenInteractWithController() {
        val application = mockk<Application>()

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, null)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }
    }

    @Test
    fun externalIdAndUser_whenSetUserAttributesWithUser_thenInteractWithController() {
        val user = mockk<User>()
        val application = mockk<Application>()

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, user)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify { contactController.setUserData(user) }
    }

    @Test
    fun whenLogEvent_thenInteractWithEventController() {
        val application = mockk<Application>()
        val retenoImpl = RetenoImpl(application)

        val event = Event(
            eventTypeKey = "EVENT_TYPE_KEY",
            occurred = ZonedDateTime.now(),
            params = listOf(Parameter("key", "value"))
        )
        retenoImpl.logEvent(event.eventTypeKey, event.occurred, event.params)
        verify { eventController.saveEvent(event) }
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

        val retenoImpl = RetenoImpl(application)

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

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setDeviceIdMode(deviceIdMode, callback)
        verify { contactController.setDeviceIdMode(deviceIdMode, any()) }
        assertFalse(lambdaCalled)
    }

    @Test
    fun whenResumeApp_thenStartScheduler() {
        val retenoImpl = RetenoImpl(mockk())
        retenoImpl.resume(mockk())

        verify { scheduleController.startScheduler() }
    }

    @Test
    fun whenPauseApp_thenStopScheduler() {
        val retenoImpl = RetenoImpl(mockk())
        retenoImpl.pause(mockk())

        verify { scheduleController.stopScheduler() }
    }

}