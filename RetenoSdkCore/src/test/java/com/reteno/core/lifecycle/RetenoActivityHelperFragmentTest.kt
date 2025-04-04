package com.reteno.core.lifecycle

import android.app.Activity
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.controller.ScreenTrackingController
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController


@OptIn(ExperimentalCoroutinesApi::class)
class RetenoActivityHelperFragmentTest : BaseRobolectricTest() {
    // region helper fields ------------------------------------------------------------------------

    @RelaxedMockK
    private lateinit var eventController: EventController

    @RelaxedMockK
    private lateinit var retenoLifecycleCallbacks: RetenoLifecycleCallbacks

    private lateinit var activityController: ActivityController<Activity>

    // endregion helper fields ---------------------------------------------------------------------


    override fun before() {
        super.before()
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
    }

    @Test
    fun givenScreenTrackingDisabled_whenFragmentStarted_thenScreenViewNotTracked() = runRetenoTest {
        // Given
        val sut = createSUT()
        sut.enableLifecycleCallbacks(activityController.get().application)
        sut.registerActivityLifecycleCallbacks("KEY", retenoLifecycleCallbacks)
        val screenTrackingController = ScreenTrackingController(sut, eventController)
        val config = ScreenTrackingConfig(false)
        screenTrackingController.autoScreenTracking(config)

        // When
        val fragmentScenario = launchFragmentInContainer<TestFragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenTrackingEnabled_whenFragmentStarted_thenScreenViewNotTracked() = runRetenoTest {
        // Given
        val sut = createSUT()
        sut.enableLifecycleCallbacks(activityController.get().application)
        sut.registerActivityLifecycleCallbacks("KEY", retenoLifecycleCallbacks)
        val screenTrackingController = ScreenTrackingController(sut, eventController)
        val config = ScreenTrackingConfig(true)
        screenTrackingController.autoScreenTracking(config)

        // When
        val fragmentScenario = launchFragmentInContainer<TestFragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 1) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenTrackingOnResume_whenFragmentStarted_thenScreenViewNotTracked() = runRetenoTest {
        // Given
        val sut = createSUT()
        sut.enableLifecycleCallbacks(activityController.get().application)
        sut.registerActivityLifecycleCallbacks("KEY", retenoLifecycleCallbacks)
        val screenTrackingController = ScreenTrackingController(sut, eventController)
        val config = ScreenTrackingConfig(enable = true, trigger = ScreenTrackingTrigger.ON_RESUME)
        screenTrackingController.autoScreenTracking(config)

        // When
        val fragmentScenario = launchFragmentInContainer<TestFragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenTrackingOnResume_whenFragmentResumed_thenScreenViewTracked() = runRetenoTest {
        // Given
        val sut = createSUT()
        sut.enableLifecycleCallbacks(activityController.get().application)
        sut.registerActivityLifecycleCallbacks("KEY", retenoLifecycleCallbacks)
        val screenTrackingController = ScreenTrackingController(sut, eventController)
        val config = ScreenTrackingConfig(enable = true, trigger = ScreenTrackingTrigger.ON_RESUME)
        screenTrackingController.autoScreenTracking(config)

        // When
        val fragmentScenario = launchFragmentInContainer<TestFragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        // Then
        verify(exactly = 1) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenExcluded_whenFragmentStarted_thenScreenViewNotTracked() = runRetenoTest {
        // Given
        val sut = createSUT()
        sut.enableLifecycleCallbacks(activityController.get().application)
        sut.registerActivityLifecycleCallbacks("KEY", retenoLifecycleCallbacks)
        val screenTrackingController = ScreenTrackingController(sut, eventController)
        val config = ScreenTrackingConfig(enable = true, excludeScreens = listOf(TestFragment::class.java.simpleName))
        screenTrackingController.autoScreenTracking(config)

        // When
        val fragmentScenario = launchFragmentInContainer<TestFragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    @Test
    fun givenScreenExcluded_whenFragmentResumed_thenScreenViewNotTracked() = runRetenoTest {
        // Given
        val sut = createSUT()
        sut.enableLifecycleCallbacks(activityController.get().application)
        sut.registerActivityLifecycleCallbacks("KEY", retenoLifecycleCallbacks)
        val screenTrackingController = ScreenTrackingController(sut, eventController)
        val config = ScreenTrackingConfig(enable = true, excludeScreens = listOf(TestFragment::class.java.simpleName))
        screenTrackingController.autoScreenTracking(config)

        // When
        val fragmentScenario = launchFragmentInContainer<TestFragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.moveToState(Lifecycle.State.CREATED)
        fragmentScenario.moveToState(Lifecycle.State.STARTED)
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        // Then
        verify(exactly = 0) { eventController.trackScreenViewEvent(TestFragment::class.java.simpleName) }
    }

    private fun createSUT() = RetenoActivityHelperImpl()
}