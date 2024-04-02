package com.reteno.core.domain.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.reteno.core.RetenoImpl
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.di.ServiceLocator
import com.reteno.core.lifecycle.RetenoActivityHelper
import com.reteno.core.lifecycle.RetenoActivityHelperImpl
import com.reteno.core.lifecycle.ScreenTrackingConfig
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

//Integrational test for ScreenTrackerController
class ScreenTrackerControllerTest : BaseRobolectricTest() {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            mockkConstructor(ServiceLocator::class)
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            unmockkConstructor(ServiceLocator::class)
        }
    }

    // region helper fields ------------------------------------------------------------------------

    @RelaxedMockK
    private lateinit var eventController: EventController
    //endregion helper fields

    @Test
    fun givenInit_whenFragmentOpens_thenEventNotRecorded() {
        //Given
        val helper = RetenoActivityHelperImpl()
        val controller = createController(helper)
        every { anyConstructed<ServiceLocator>().screenTrackingControllerProvider.get() } returns controller
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns helper
        val reteno = RetenoImpl(ApplicationProvider.getApplicationContext(), "Some key")
        //When
        val scenario = launchFragmentInContainer<Fragment>(initialState = Lifecycle.State.CREATED)
        scenario.onFragment {
            scenario.moveToState(Lifecycle.State.STARTED)
        }
        //Then
        coVerify(exactly = 0) { eventController.trackScreenViewEvent(any()) }
    }

    @Test
    fun givenAutoScreenTrackingEnabled_whenFragmentOpens_thenEventNotRecorded() {
        //Given
        val helper = RetenoActivityHelperImpl()
        val controller = createController(helper)
        every { anyConstructed<ServiceLocator>().screenTrackingControllerProvider.get() } returns controller
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns helper
        val reteno = RetenoImpl(ApplicationProvider.getApplicationContext(), "Some key")
        reteno.autoScreenTracking(ScreenTrackingConfig(true))
        //When
        val scenario = launchFragmentInContainer<Fragment>(initialState = Lifecycle.State.CREATED)
        scenario.onFragment {
            scenario.moveToState(Lifecycle.State.STARTED)
        }
        //Then
        coVerify(exactly = 1) { eventController.trackScreenViewEvent(any()) }
    }

    private fun createController(retenoActivityHelper: RetenoActivityHelper): ScreenTrackingController {
        return ScreenTrackingController(retenoActivityHelper, eventController)
    }
}