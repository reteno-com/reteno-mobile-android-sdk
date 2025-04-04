package com.reteno.core.domain.controller

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.reteno.core.RetenoInternalImpl
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

//Integrational test for ScreenTrackerController
@OptIn(ExperimentalCoroutinesApi::class)
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

    @RelaxedMockK
    private lateinit var contactController: ContactController
    //endregion helper fields

    @Test
    fun givenInit_whenFragmentOpens_thenEventNotRecorded() = runTest {
        //Given
        val helper = RetenoActivityHelperImpl()
        val controller = createController(helper)
        every { anyConstructed<ServiceLocator>().screenTrackingControllerProvider.get() } returns controller
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns helper
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        val reteno = createRetenoAndAdvanceInit()
        advanceUntilIdle()
        //When
        val scenario = launchFragmentInContainer<Fragment>(initialState = Lifecycle.State.CREATED)
        scenario.onFragment {
            scenario.moveToState(Lifecycle.State.STARTED)
        }
        //Then
        coVerify(exactly = 0) { eventController.trackScreenViewEvent(any()) }
        RetenoInternalImpl.swapInstance(null)
    }

    @Test
    fun givenAutoScreenTrackingEnabled_whenFragmentOpens_thenEventRecorded() = runTest {
        //Given
        val helper = RetenoActivityHelperImpl()
        val controller = createController(helper)
        every { anyConstructed<ServiceLocator>().screenTrackingControllerProvider.get() } returns controller
        every { anyConstructed<ServiceLocator>().retenoActivityHelperProvider.get() } returns helper
        every { anyConstructed<ServiceLocator>().contactControllerProvider.get() } returns contactController
        val reteno = createRetenoAndAdvanceInit()
        advanceUntilIdle()
        reteno.autoScreenTracking(ScreenTrackingConfig(true))
        //When
        val scenario = launchFragmentInContainer<Fragment>(initialState = Lifecycle.State.CREATED)
        scenario.onFragment {
            scenario.moveToState(Lifecycle.State.STARTED)
        }
        //Then
        coVerify(exactly = 1) { eventController.trackScreenViewEvent(any()) }
        RetenoInternalImpl.swapInstance(null)
    }

    private fun createController(retenoActivityHelper: RetenoActivityHelper): ScreenTrackingController {
        return ScreenTrackingController(retenoActivityHelper, eventController)
    }
}