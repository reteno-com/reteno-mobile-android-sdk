package com.reteno.core.base

import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.util.Logger
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.mockito.Matchers.anyString
import java.util.concurrent.Executor

open class BaseUnitTest {

    @Before
    open fun before() {
        MockKAnnotations.init(this)

        mockLog()
        mockLogger()
    }

    protected fun mockOperationQueue() {
        mockkConstructor(HandlerThread::class)
        every { anyConstructed<HandlerThread>().looper } returns mockk()

        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk()

        mockkObject(OperationQueue)
        val currentThreadExecutor = Executor(Runnable::run)
        every { OperationQueue.addParallelOperation(any()) } answers {
            currentThreadExecutor.execute(
                firstArg()
            )
        }
        every { OperationQueue.addOperation(any()) } answers {
            currentThreadExecutor.execute(firstArg())
            true
        }
        every { OperationQueue.addOperationAfterDelay(any(), any()) } answers {
            currentThreadExecutor.execute(firstArg())
            true
        }
        every { OperationQueue.addUiOperation(any()) } answers {
            currentThreadExecutor.execute(firstArg())
        }
    }

    protected fun unMockOperationQueue() {
        unmockkObject(OperationQueue)
        unmockkConstructor(HandlerThread::class)
        unmockkStatic(Looper::class)
    }

    private fun mockLog() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), anyString()) } returns 0
        every { Log.w(any(), Throwable()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), Throwable()) } returns 0
    }

    private fun mockLogger() {
        mockkStatic(Logger::class)
        justRun { Logger.v(any(), any(), *anyVararg()) }
        justRun { Logger.d(any(), any(), *anyVararg()) }
        justRun { Logger.i(any(), any(), *anyVararg()) }
        justRun { Logger.w(any(), any(), *anyVararg()) }
        justRun { Logger.e(any(), any()) }
        justRun { Logger.e(any(), any(), any()) }
        justRun { Logger.captureException(any()) }
        justRun { Logger.captureEvent(any()) }
    }

    @After
    open fun after() {
        unmockkStatic(Log::class)
        unmockkStatic(Logger::class)
    }
}