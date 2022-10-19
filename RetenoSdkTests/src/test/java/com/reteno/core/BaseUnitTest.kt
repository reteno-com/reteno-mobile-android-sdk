package com.reteno.core

import android.util.Log
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
    }

    @After
    open fun after() {
        unmockkStatic(Log::class)
    }
}