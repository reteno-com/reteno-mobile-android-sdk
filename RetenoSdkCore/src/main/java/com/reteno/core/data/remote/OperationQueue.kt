package com.reteno.core.data.remote

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object OperationQueue {

    private const val OPERATION_QUEUE_NAME = "OperationQueue"
    private const val OPERATION_QUEUE_PRIORITY = Process.THREAD_PRIORITY_DEFAULT

    private val handlerThread: HandlerThread =
        HandlerThread(OPERATION_QUEUE_NAME, OPERATION_QUEUE_PRIORITY)
    private val handler: Handler
    private val uiHandler: Handler
    private val executor: Executor

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        uiHandler = Handler(Looper.getMainLooper())
        executor = Executors.newCachedThreadPool()
    }

    /**
     * Stop OperationQueue and remove all operations
     */
    private fun stop() {
        removeAllOperations()
        handlerThread.quit()
    }

    /**
     * Add operation to Executor to be run in parallel
     * @param operation The operation that will be executed.
     */
    fun addParallelOperation(operation: Runnable) {
        executor.execute(operation)
    }

    /**
     * Add operation to UI Handler to be run on main thread
     * @param operation The operation that will be executed.
     */
    fun addUiOperation(operation: Runnable) {
        uiHandler.post(operation)
    }

    /**
     * Add operation to OperationQueue at the end
     * @param operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperation(operation: Runnable): Boolean {
        return handler.post(operation)
    }

    /**
     * Add operation to OperationQueue at the front
     * @param operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperationAtFront(operation: Runnable): Boolean {
        return handler.postAtFrontOfQueue(operation)
    }

    /**
     * Add operation to OperationQueue, to be run at a specific time given by millis.
     * @param operation operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperationAtTime(operation: Runnable, millis: Long): Boolean {
        return handler.postAtTime(operation, millis)
    }

    /**
     * Add operation to OperationQueue, to be run after the specific time given by delayMillis.
     * @param operation operation operation The operation that will be executed.
     * @param delayMillis
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperationAfterDelay(operation: Runnable, delayMillis: Long): Boolean {
        return handler.postDelayed(operation, delayMillis)
    }

    /**
     * Removes pending operation from OperationQueue.
     */
    fun removeOperation(operation: Runnable) {
        handler.removeCallbacks(operation)
    }

    /**
     * Remove all pending Operations that are in OperationQueue
     */
    fun removeAllOperations() {
        handler.removeCallbacksAndMessages(null)
    }
}