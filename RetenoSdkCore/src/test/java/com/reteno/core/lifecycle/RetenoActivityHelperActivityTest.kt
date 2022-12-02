package com.reteno.core.lifecycle

import android.app.Activity
import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.domain.controller.EventController
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController


class RetenoActivityHelperActivityTest : BaseRobolectricTest() {

    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var eventController: EventController

    @RelaxedMockK
    private lateinit var retenoLifecycleCallbacks: RetenoLifecycleCallbacks

    private lateinit var activityController: ActivityController<Activity>

    private lateinit var SUT: RetenoActivityHelper
    // endregion helper fields ---------------------------------------------------------------------


    override fun before() {
        super.before()
        SUT = RetenoActivityHelperImpl(eventController)
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
        SUT.enableLifecycleCallbacks(retenoLifecycleCallbacks)
    }

    @Test
    fun whenActivityResumed_thenLifecycleCallbackResumeCalled() {
        // When
        activityController.start().resume()

        // Then
        verify(exactly = 1) { retenoLifecycleCallbacks.resume(activityController.get()) }
    }

    @Test
    fun whenActivityStopped_thenLifecycleCallbackPauseCalled() {
        // When
        activityController.start().resume().pause().stop()

        // Then
        verify(exactly = 1) { retenoLifecycleCallbacks.pause(activityController.get()) }
    }

    @Test
    fun whenActivityIsStarted_thenCanPresentMessagesFalse() {
        // When
        activityController.start()

        // Then
        assertFalse(SUT.canPresentMessages())
    }

    @Test
    fun whenActivityIsResumed_thenCanPresentMessagesTrue() {
        // When
        activityController.start().resume()

        // Then
        assertTrue(SUT.canPresentMessages())
    }

    @Test
    fun whenActivityIsPaused_thenCanPresentMessagesFalse() {
        // When
        activityController.start().resume().pause()

        // Then
        assertFalse(SUT.canPresentMessages())
    }

    @Test
    fun whenActivityIsStopped_thenCanPresentMessagesFalse() {
        // When
        activityController.start().resume().pause().stop()

        // Then
        assertFalse(SUT.canPresentMessages())
    }
}