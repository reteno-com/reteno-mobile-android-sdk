package com.reteno.push.base

import android.util.Log
import com.reteno.core.util.Logger
import io.mockk.*
import org.junit.After
import org.junit.Before

open class BaseUnitTest {
    @Before
    open fun before() {
        MockKAnnotations.init(this)

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0

        mockkStatic(Logger::class)
        every { Logger.v(any(), any(), *anyVararg()) } just runs
        every { Logger.d(any(), any(), *anyVararg()) } just runs
        every { Logger.i(any(), any(), *anyVararg()) } just runs
        every { Logger.w(any(), any(), *anyVararg()) } just runs
        every { Logger.e(any(), any())} just runs
        every { Logger.e(any(), any(), any()) } just runs
        every { Logger.captureException(any()) } just runs
        every { Logger.captureEvent(any()) } just runs
    }

    @After
    open fun after() {
        unmockkStatic(Log::class)
        unmockkStatic(Logger::class)
    }
}