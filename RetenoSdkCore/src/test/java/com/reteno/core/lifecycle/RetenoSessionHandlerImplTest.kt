package com.reteno.core.lifecycle

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.model.event.Event.Companion.SESSION_START_EVENT_TYPE_KEY
import com.reteno.core.domain.model.event.LifecycleTrackingOptions
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.UUID


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
internal class RetenoSessionHandlerImplTest : BaseUnitTest() {

    @RelaxedMockK
    lateinit var sharedPrefsManager: SharedPrefsManager

    @RelaxedMockK
    lateinit var eventsController: EventController

    @Test
    fun givenAppStartedLongTImeAgo_whenAppStart_thenTrackAppStartEvent() = runTest {
        //Given
        every { sharedPrefsManager.getSessionStartTimestamp() } returns System.currentTimeMillis() - RetenoSessionHandlerImpl.DEFAULT_SESSION_RESET_TIME - 1

        //When
        val testScope = TestScope()
        val sut = createHandler(scope = testScope)
        sut.start()

        testScope.advanceTimeBy(5000)

        //Then
        coVerify { eventsController.trackEvent(match { it.eventTypeKey == SESSION_START_EVENT_TYPE_KEY }) }
    }

    @Test
    fun givenAppStartedLongTImeAgo_whenAppStart_thenSessionTimeSaved() = runTest {
        //Given
        every { sharedPrefsManager.getSessionStartTimestamp() } returns System.currentTimeMillis() - RetenoSessionHandlerImpl.DEFAULT_SESSION_RESET_TIME - 1

        //When
        val testScope = TestScope()
        val sut = createHandler(scope = testScope)
        sut.start()

        testScope.advanceTimeBy(5000L)

        //Then
        verify {
            sharedPrefsManager.saveSessionId(sut.getSessionId())
        }
    }

    @Test
    fun givenAppStartedRecently_whenAppStart_thenSessionIdReused() = runTest {
        //Given
        val saved = UUID.randomUUID().toString()
        every { sharedPrefsManager.getSessionStartTimestamp() } returns System.currentTimeMillis() - RetenoSessionHandlerImpl.DEFAULT_SESSION_RESET_TIME + 10000
        every { sharedPrefsManager.getSessionId() } returns saved

        //When
        val testScope = TestScope()
        val sut = createHandler(scope = testScope)
        sut.start()
        testScope.advanceTimeBy(5000L)

        //Then
        assertEquals(saved, sut.getSessionId())
    }

    @Test
    fun givenAppStartedRecently_whenAppStart_thenStartNotSend() = runTest {
        //Given
        val saved = UUID.randomUUID()
        every { sharedPrefsManager.getSessionStartTimestamp() } returns System.currentTimeMillis() - RetenoSessionHandlerImpl.DEFAULT_SESSION_RESET_TIME + 10000
        every { sharedPrefsManager.getSessionId() } returns saved.toString()

        //When
        val testScope = TestScope()
        val sut = createHandler(scope = testScope)
        sut.start()
        testScope.advanceTimeBy(5000L)

        //Then
        coVerify(exactly = 0) { eventsController.trackEvent(any()) }
    }


    private fun createHandler(
        options: LifecycleTrackingOptions = LifecycleTrackingOptions.ALL,
        scope: CoroutineScope
    ) = RetenoSessionHandlerImpl(
        eventController = eventsController,
        sharedPrefsManager = sharedPrefsManager,
        lifecycleTrackingOptions = options,
        scope = scope
    )
}