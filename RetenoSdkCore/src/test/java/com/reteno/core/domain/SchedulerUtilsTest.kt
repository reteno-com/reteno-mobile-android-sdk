package com.reteno.core.domain

import com.reteno.core.base.robolectric.BaseRobolectricTest
import com.reteno.core.util.Util
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Test
import java.time.ZonedDateTime

class SchedulerUtilsTest: BaseRobolectricTest() {

    @Test
    fun whenGetOutdatedData_thenReturnDataMinusDay() {
        // Given
        val mockDate = mockk<ZonedDateTime>()
        val mockOutDatedData = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns false
        every { ZonedDateTime.now() } returns mockDate
        every { mockDate.minusHours(any()) } returns mockOutDatedData

        // When
        val result = SchedulerUtils.getOutdatedTime()

        // Then
        assertEquals(mockOutDatedData, result)
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockDate.minusHours(24) }
    }

    @Test
    fun givenDebugMode_whenGetOutdatedData_thenReturnDataMinusOneHour() {
        // Given
        val mockData = mockk<ZonedDateTime>()
        val mockOutDatedDate = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns true
        every { ZonedDateTime.now() } returns mockData
        every { mockData.minusHours(any()) } returns mockOutDatedDate

        // When
        val result = SchedulerUtils.getOutdatedTime()

        // Then
        assertEquals(mockOutDatedDate, result)
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockData.minusHours(1) }
    }

}