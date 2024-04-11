package com.reteno.core.lifecycle

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.sharedpref.SharedPrefsManager
import com.reteno.core.domain.controller.EventController
import com.reteno.core.domain.model.event.Event
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
internal class RetenoSessionHandlerImplTest : BaseUnitTest() {

    @RelaxedMockK
    lateinit var sharedPrefsManager: SharedPrefsManager

    @RelaxedMockK
    lateinit var eventsController: EventController

    @Test
    fun givenAppStartedLongTImeAgo_whenAppStart_thenSessionIdMatches() = runTest {
        //Given
        val id = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns id
        every { sharedPrefsManager.getAppStoppedTimestamp() } returns System.currentTimeMillis() - (5 * 60L * 1000L) - 1

        //When
        val sut = createHandler()
        sut.start()

        //Then
        assertEquals(id.toString(), sut.getSessionId())
    }

    @Test
    fun givenAppStartedLongTImeAgo_whenAppStart_thenTrackAppStartEvent() = runTest {
        //Given
        every { sharedPrefsManager.getAppStoppedTimestamp() } returns System.currentTimeMillis() - (5 * 60L * 1000L) - 1

        //When
        val sut = createHandler()
        sut.start()

        //Then
        verify {
            eventsController.trackEvent(
                Event.sessionStart(
                    sut.getSessionId(),
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(sut.getSessionStartTimestamp()),
                        ZoneId.systemDefault()
                    )
                )
            )
        }
    }

    @Test
    fun givenAppStartedLongTImeAgo_whenAppStart_thenSessionTimeSaved() = runTest {
        //Given
        every { sharedPrefsManager.getAppStoppedTimestamp() } returns System.currentTimeMillis() - (5 * 60L * 1000L) - 1

        //When
        val sut = createHandler()
        sut.start()

        //Then
        verify {
            sharedPrefsManager.saveSessionId(sut.getSessionId())
        }
    }

    @Test
    fun givenAppStartedRecently_whenAppStart_thenSessionIdReused() = runTest {
        //Given
        val saved = UUID.randomUUID().toString()
        every { sharedPrefsManager.getAppStoppedTimestamp() } returns System.currentTimeMillis() - (4 * 60L * 1000L)
        every { sharedPrefsManager.getSessionId() } returns saved

        //When
        val sut = createHandler()
        sut.start()

        //Then
        assertEquals(saved, sut.getSessionId())
    }

    @Test
    fun givenAppStartedRecently_whenAppStart_thenStartNotSend() = runTest {
        //Given
        val saved = UUID.randomUUID()
        every { sharedPrefsManager.getAppStoppedTimestamp() } returns System.currentTimeMillis() - (4 * 60L * 1000L)
        every { sharedPrefsManager.getSessionId() } returns saved.toString()

        //When
        val sut = createHandler()
        sut.start()

        //Then
        verify(exactly = 0) {
            eventsController.trackEvent(
                Event.sessionStart(
                    sut.getSessionId(),
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(sut.getSessionStartTimestamp()),
                        ZoneId.systemDefault()
                    )
                )
            )
        }
    }


    private fun createHandler() = RetenoSessionHandlerImpl(
        sharedPrefsManager = sharedPrefsManager,
        eventController = eventsController
    )
}