package com.reteno.core.base.robolectric

import android.app.Application
import android.database.Cursor
import android.provider.Settings
import com.reteno.core.RetenoInternalImpl
import com.reteno.core.data.local.database.util.getAppInbox
import com.reteno.core.data.local.database.util.getDevice
import com.reteno.core.data.local.database.util.getEvent
import com.reteno.core.data.local.database.util.getInteraction
import com.reteno.core.data.local.database.util.getRecomEvent
import com.reteno.core.data.local.database.util.getUser
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.data.remote.PushOperationQueue
import com.reteno.core.util.Logger
import com.reteno.core.util.Util
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import java.time.ZonedDateTime
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class RetenoTestApp : Application() {

    internal val scheduler: ScheduledExecutorService = mockStaticScheduler()
    internal var retenoMock: RetenoInternalImpl = mockk()

    init {
        mockStaticLogger()
        mockObjectOperationQueue()
        mockObjectPushOperationQueue()
        mockObjectUtil()
        mockStaticUtilKt()
        mockStaticZoneDateTime()
        mockStaticCursor()
    }

    override fun onCreate() {
        super.onCreate()
        Settings.Secure.putString(contentResolver, Settings.Secure.ANDROID_ID, Constants.DEVICE_ID_ANDROID)
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

    private fun mockObjectOperationQueue() {
        mockkObject(OperationQueue)
        val currentThreadExecutor = Executor(Runnable::run)
        every { OperationQueue.addParallelOperation(any()) } answers {
            currentThreadExecutor.execute(Runnable(firstArg()))
        }
        every { OperationQueue.addOperation(any()) } answers {
            currentThreadExecutor.execute(Runnable(firstArg()))
            true
        }
        every { OperationQueue.addOperationAfterDelay(any(), any()) } answers {
            currentThreadExecutor.execute(Runnable(firstArg()))
            true
        }
        every { OperationQueue.addUiOperation(any()) } answers {
            currentThreadExecutor.execute(Runnable(firstArg()))
        }
    }

    private fun mockObjectPushOperationQueue() {
        mockkObject(PushOperationQueue)
        val currentThreadExecutor = Executor(Runnable::run)
        every { PushOperationQueue.addOperation(any()) } answers {
            currentThreadExecutor.execute(Runnable(firstArg()))
        }
    }

    private fun mockObjectUtil() {
        mockkObject(Util)
        mockkStatic(Util::class)
    }

    private fun mockStaticUtilKt() {
        mockkStatic("com.reteno.core.util.UtilKt")
    }

    private fun mockStaticZoneDateTime() {
        mockkStatic(ZonedDateTime::class)
    }

    private fun mockStaticScheduler(): ScheduledExecutorService {
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

    private fun mockStaticCursor() {
        mockkStatic(Cursor::getDevice)
        mockkStatic(Cursor::getUser)
        mockkStatic(Cursor::getInteraction)
        mockkStatic(Cursor::getAppInbox)
        mockkStatic(Cursor::getEvent)
        mockkStatic(Cursor::getRecomEvent)
    }
}
