package com.reteno.core.domain

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.util.Util
import io.mockk.*
import junit.framework.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class SchedulerUtilsTest: BaseUnitTest() {

    @Test
    fun whenGetOutdatedData_thenReturnDataMinusDay() {
        mockkStatic(ZonedDateTime::class)
        mockkObject(Util)
        val mockDate = mockk<ZonedDateTime>()
        val mockOutDatedData = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns false
        every { ZonedDateTime.now() } returns mockDate
        every { mockDate.minusHours(any()) } returns mockOutDatedData

        val result = SchedulerUtils.getOutdatedTime()

        assertEquals(mockOutDatedData, result)
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockDate.minusHours(24) }

        unmockkStatic(ZonedDateTime::class)
        unmockkObject(Util)
    }

    @Test
    fun givenDebugMode_whenGetOutdatedData_thenReturnDataMinusOneHour() {
        mockkStatic(ZonedDateTime::class)
        mockkObject(Util)
        val mockData = mockk<ZonedDateTime>()
        val mockOutDatedDate = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns true
        every { ZonedDateTime.now() } returns mockData
        every { mockData.minusHours(any()) } returns mockOutDatedDate

        val result = SchedulerUtils.getOutdatedTime()

        assertEquals(mockOutDatedDate, result)
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockData.minusHours(1) }

        unmockkStatic(ZonedDateTime::class)
        unmockkObject(Util)
    }

}