package com.reteno.core.lifecycle

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.domain.controller.EventController
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController


class RetenoActivityHelperFragmentTest : BaseRobolectricTest() {

    // region helper fields ------------------------------------------------------------------------
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
        SUT = RetenoActivityHelper(eventController)
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
        SUT.enableLifecycleCallbacks(retenoLifecycleCallbacks)

        // Disable screenTracking to prevent eventController calls for the first Fragment start
        SUT.autoScreenTracking(ScreenTrackingConfig(false))
        // First fragment start
        fragmentScenario = FragmentScenario.launch(TestFragment::class.java, Bundle.EMPTY)
    }

    @Test
    fun givenScreenTrackingDisabled_whenFragmentStarted_thenScreenViewNotTracked() {
        // Given
        val config = ScreenTrackingConfig(false)
        SUT.autoScreenTracking(config)

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
        SUT.autoScreenTracking(config)

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
        SUT.autoScreenTracking(config)

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
        SUT.autoScreenTracking(config)

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
        SUT.autoScreenTracking(config)

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
        SUT.autoScreenTracking(config)

        // When
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }
}