package com.reteno.core.base

import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.data.workmanager.PushDataWorker
import com.reteno.core.domain.SchedulerUtils
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import io.mockk.*
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.mockito.ArgumentMatchers.anyString
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

open class BaseUnitTest {

    companion object {

        @JvmStatic
        @BeforeClass
        fun beforeClassBase() {
            mockStaticLogger()
        }

        @JvmStatic
        @AfterClass
        fun afterClassBase() {
            unMockStaticLogger()
        }

        private fun mockStaticLogger() {
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

        @JvmStatic
        protected fun mockStaticLog() {
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

        @JvmStatic
        protected fun mockStaticScheduler(): ScheduledExecutorService {
            val scheduler: ScheduledExecutorService = mockk(relaxed = true)
            val currentThreadExecutor = Executor(Runnable::run)
            every { scheduler.scheduleAtFixedRate(any(), any(), any(), any()) } answers {
                currentThreadExecutor.execute(firstArg())
                mockk()
            }
            every { scheduler.shutdownNow() } returns listOf()

            mockkStatic(Executors::class)
            every { Executors.newScheduledThreadPool(any(), any()) } returns scheduler

            return scheduler
        }

        @JvmStatic
        protected fun mockObjectPushDataWorker() {
            mockkObject(PushDataWorker)
            every { PushDataWorker.enqueuePeriodicWork(any()) } returns UUID.randomUUID()
        }

        @JvmStatic
        protected fun mockObjectOperationQueue() {
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

        @JvmStatic
        protected fun mockObjectPushOperationQueue() {
            mockkObject(PushOperationQueue)
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

        @JvmStatic
        protected fun mockStaticTextUtil() {
            mockkStatic(TextUtils::class)
        }

        @JvmStatic
        protected fun mockObjectSchedulerUtils() {
            mockkObject(SchedulerUtils)
        }

        //------------------------------------------------------------------------------------------

        private fun unMockStaticLogger() {
            unmockkStatic(Logger::class)
        }

        @JvmStatic
        protected fun unMockStaticLog() {
            unmockkStatic(Log::class)
        }

        @JvmStatic
        protected fun unMockStaticScheduler() {
            unmockkStatic(Executors::class)
        }

        @JvmStatic
        protected fun unMockObjectPushDataWorker() {
            unmockkObject(PushDataWorker)
        }

        @JvmStatic
        protected fun unMockObjectOperationQueue() {
            unmockkObject(OperationQueue)
            unmockkConstructor(HandlerThread::class)
            unmockkStatic(Looper::class)
        }

        @JvmStatic
        protected fun unMockObjectPushOperationQueue() {
            unmockkObject(PushOperationQueue)
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

        @JvmStatic
        protected fun unMockStaticTextUtil() {
            unmockkStatic(TextUtils::class)
        }

        @JvmStatic
        protected fun unMockObjectSchedulerUtils() {
            unmockkObject(SchedulerUtils)
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