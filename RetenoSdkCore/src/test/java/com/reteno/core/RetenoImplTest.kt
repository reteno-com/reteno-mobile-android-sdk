package com.reteno.core

import android.app.Application
import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.config.DeviceIdMode
import com.reteno.core.di.ServiceLocator
import com.reteno.core.domain.controller.ContactController
import com.reteno.core.model.user.User
import io.mockk.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetenoImplTest: BaseUnitTest() {

    companion object {
        private const val EXTERNAL_USER_ID = "external_user_ID"
    }

    @Test
    fun externalId_whenSetUserAttributes_thenInteractWithController() {
        val application = mockk<Application>()
        val contactController = mockk<ContactController>()

        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }

        unmockkConstructor(ServiceLocator::class)
    }

    @Test
    fun externalIdAndUserNull_whenSetUserAttributesWithUser_thenInteractWithController() {
        val application = mockk<Application>()
        val contactController = mockk<ContactController>()

        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, null)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify(exactly = 0) { contactController.setUserData(any()) }

        unmockkConstructor(ServiceLocator::class)
    }

    @Test
    fun externalIdAndUser_whenSetUserAttributesWithUser_thenInteractWithController() {
        val user = mockk<User>()

        val application = mockk<Application>()
        val contactController = mockk<ContactController>(relaxed = true)

        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setUserAttributes(EXTERNAL_USER_ID, user)
        verify { contactController.setExternalUserId(eq(EXTERNAL_USER_ID)) }
        verify { contactController.setUserData(user) }

        unmockkConstructor(ServiceLocator::class)
    }

    @Test
    fun whenSetDeviceIdMode_thenIdHasChanged() {
        var lambdaCalled = false
        val deviceIdMode = DeviceIdMode.ANDROID_ID
        val application = mockk<Application>()
        val contactController = mockk<ContactController>()
        val callback = { lambdaCalled = true }

        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        every { contactController.setDeviceIdMode(any(), captureLambda()) } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setDeviceIdMode(deviceIdMode, callback)
        verify { contactController.setDeviceIdMode(deviceIdMode, any()) }
        assertTrue(lambdaCalled)

        unmockkConstructor(ServiceLocator::class)
    }

    @Test
    fun whenSetDeviceIdMode_thenIdHasNotChanged() {
        var lambdaCalled = false
        val deviceIdMode = DeviceIdMode.ANDROID_ID
        val application = mockk<Application>()
        val contactController = mockk<ContactController>()
        val callback = { lambdaCalled = true }

        mockkConstructor(ServiceLocator::class)
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        justRun { contactController.setDeviceIdMode(any(), captureLambda()) }

        val retenoImpl = RetenoImpl(application)

        retenoImpl.setDeviceIdMode(deviceIdMode, callback)
        verify { contactController.setDeviceIdMode(deviceIdMode, any()) }
        assertFalse(lambdaCalled)

        unmockkConstructor(ServiceLocator::class)
    }


}