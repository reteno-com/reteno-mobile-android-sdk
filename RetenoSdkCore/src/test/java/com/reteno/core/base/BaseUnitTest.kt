package com.reteno.core.base

import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import com.reteno.core.util.isOsVersionSupported
import io.mockk.*
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import java.time.ZonedDateTime

open class BaseUnitTest {

    companion object {

        @JvmStatic
        @BeforeClass
        fun beforeClassBase() {
            mockStaticLogger()
            mockkStatic("com.reteno.core.util.UtilKt")
            every { isOsVersionSupported() } returns true
        }

        @JvmStatic
        @AfterClass
        fun afterClassBase() {
            unMockStaticLogger()
            unmockkStatic("com.reteno.core.util.UtilKt")
        }

        private fun mockStaticLogger() {
            mockkStatic(Logger::class)
            justRun { Logger.v(any(), any(), *anyVararg()) }
            justRun { Logger.d(any(), any(), *anyVararg()) }
            justRun { Logger.i(any(), any(), *anyVararg()) }
            justRun { Logger.w(any(), any(), *anyVararg()) }
            justRun { Logger.e(any(), any(), any()) }
            justRun { Logger.captureMessage(any()) }
            justRun { Logger.captureEvent(any()) }
        }

        @JvmStatic
        protected fun mockStaticZoneDateTime() {
            mockkStatic(ZonedDateTime::class)
        }

        @JvmStatic
        protected fun mockObjectUtil() {
            mockkObject(Util)
            mockkStatic(Util::class)
        }

        //------------------------------------------------------------------------------------------

        private fun unMockStaticLogger() {
            unmockkStatic(Logger::class)
        }

        @JvmStatic
        protected fun unMockStaticZoneDateTime() {
            unmockkStatic(ZonedDateTime::class)
        }

        @JvmStatic
        protected fun unMockObjectUtil() {
            unmockkObject(Util)
            unmockkStatic(Util::class)
        }
    }

    @Before
    open fun before() {
        MockKAnnotations.init(this)
    }

    @After
    open fun after() {
        clearAllMocks(
            answers = false,
            recordedCalls = true,
            childMocks = true,
            regularMocks = true,
            objectMocks = true,
            staticMocks = true,
            constructorMocks = true
        )
    }
}