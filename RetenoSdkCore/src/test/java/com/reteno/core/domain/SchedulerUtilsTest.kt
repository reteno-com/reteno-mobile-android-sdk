package com.reteno.core.domain

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.util.Util
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZonedDateTime

class SchedulerUtilsTest: BaseUnitTest() {

    companion object {

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            mockStaticZoneDateTime()
            mockObjectUtil()
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            unMockStaticZoneDateTime()
            unMockObjectUtil()
        }
    }

    @Test
    fun whenGetOutdatedData_thenReturnDataMinusDay() {
        val mockDate = mockk<ZonedDateTime>()
        val mockOutDatedData = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns false
        every { ZonedDateTime.now() } returns mockDate
        every { mockDate.minusHours(any()) } returns mockOutDatedData

        val result = SchedulerUtils.getOutdatedTime()

        assertEquals(mockOutDatedData, result)
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockDate.minusHours(24) }
    }

    @Test
    fun givenDebugMode_whenGetOutdatedData_thenReturnDataMinusOneHour() {
        val mockData = mockk<ZonedDateTime>()
        val mockOutDatedDate = mockk<ZonedDateTime>()
        every { Util.isDebugView() } returns true
        every { ZonedDateTime.now() } returns mockData
        every { mockData.minusHours(any()) } returns mockOutDatedDate

        val result = SchedulerUtils.getOutdatedTime()

        assertEquals(mockOutDatedDate, result)
        verify(exactly = 1) { ZonedDateTime.now() }
        verify(exactly = 1) { mockData.minusHours(1) }
    }

}