package com.reteno.core.lifecycle

import android.app.Activity
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.ScreenTrackingController
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController


class RetenoActivityHelperFragmentTest : BaseRobolectricTest() {
    // region helper fields ------------------------------------------------------------------------
    private lateinit var screenTrackingController: ScreenTrackingController

    @RelaxedMockK
    private lateinit var eventController: EventController

    @RelaxedMockK
    private lateinit var retenoLifecycleCallbacks: RetenoLifecycleCallbacks

    private lateinit var activityController: ActivityController<Activity>
    private lateinit var fragmentScenario: FragmentScenario<TestFragment>

    private lateinit var SUT: RetenoActivityHelper
    // endregion helper fields ---------------------------------------------------------------------


    override fun before() {
        super.before()
        SUT = RetenoActivityHelperImpl()
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
        SUT.enableLifecycleCallbacks(retenoLifecycleCallbacks)

        // Disable screenTracking to prevent eventController calls for the first Fragment start
        screenTrackingController = ScreenTrackingController(SUT, eventController)
        screenTrackingController.autoScreenTracking(ScreenTrackingConfig(false))
        // First fragment start
        fragmentScenario = launchFragmentInContainer(initialState = Lifecycle.State.INITIALIZED)
    }

    @Test
    fun givenScreenTrackingDisabled_whenFragmentStarted_thenScreenViewNotTracked() {
        // Given
        val config = ScreenTrackingConfig(false)
        screenTrackingController.autoScreenTracking(config)

        // When
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenTrackingEnabled_whenFragmentStarted_thenScreenViewNotTracked() {
        // Given
        val config = ScreenTrackingConfig(true)
        screenTrackingController.autoScreenTracking(config)

        // When
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 1) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenTrackingOnResume_whenFragmentStarted_thenScreenViewNotTracked() {
        // Given
        val config = ScreenTrackingConfig(enable = true, trigger = ScreenTrackingTrigger.ON_RESUME)
        screenTrackingController.autoScreenTracking(config)

        // When
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenTrackingOnResume_whenFragmentResumed_thenScreenViewTracked() {
        // Given
        val config = ScreenTrackingConfig(enable = true, trigger = ScreenTrackingTrigger.ON_RESUME)
        screenTrackingController.autoScreenTracking(config)

        // When
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        // Then
        verify(exactly = 1) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenExcluded_whenFragmentStarted_thenScreenViewNotTracked() {
        // Given
        val config = ScreenTrackingConfig(enable = true, excludeScreens = listOf(TestFragment::class.java.simpleName))
        screenTrackingController.autoScreenTracking(config)

        // When
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenExcluded_whenFragmentResumed_thenScreenViewNotTracked() {
        // Given
        val config = ScreenTrackingConfig(enable = true, excludeScreens = listOf(TestFragment::class.java.simpleName))
        screenTrackingController.autoScreenTracking(config)

        // When
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }
}