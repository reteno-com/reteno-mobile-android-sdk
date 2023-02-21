package com.reteno.core.data.remote

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import com.reteno.core.util.Logger
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
    fun addParallelOperation(operation: () -> Unit) {
        val catchableBlock: () -> Unit = {
            try {
                operation()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "addParallelOperation(): ", ex)
                /*@formatter:on*/
            }
        }
        executor.execute(catchableBlock)
    }

    /**
     * Add operation to UI Handler to be run on main thread
     * @param operation The operation that will be executed.
     */
    fun addUiOperation(operation: () -> Unit) {
        val catchableBlock: () -> Unit = {
            try {
                operation()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "addUiOperation(): ", ex)
                /*@formatter:on*/
            }
        }

        uiHandler.post(catchableBlock)
    }

    /**
     * Add operation to OperationQueue at the end
     * @param operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperation(operation: () -> Unit): Boolean {
        val catchableBlock: () -> Unit = {
            try {
                operation()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "addOperation(): ", ex)
                /*@formatter:on*/
            }
        }

        return handler.post(catchableBlock)
    }

    /**
     * Add operation to OperationQueue at the front
     * @param operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperationAtFront(operation: () -> Unit): Boolean {
        val catchableBlock: () -> Unit = {
            try {
                operation()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "addOperationAtFront(): ", ex)
                /*@formatter:on*/
            }
        }

        return handler.postAtFrontOfQueue(catchableBlock)
    }

    /**
     * Add operation to OperationQueue, to be run at a specific time given by millis.
     * @param operation operation The operation that will be executed.
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperationAtTime(operation: () -> Unit, millis: Long): Boolean {
        val catchableBlock: () -> Unit = {
            try {
                operation()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "addOperationAtTime(): ", ex)
                /*@formatter:on*/
            }
        }

        return handler.postAtTime(catchableBlock, millis)
    }

    /**
     * Add operation to OperationQueue, to be run after the specific time given by delayMillis.
     * @param operation operation operation The operation that will be executed.
     * @param delayMillis
     * @return return true if the operation was successfully placed in to the operation queue. Returns false on failure.
     */
    fun addOperationAfterDelay(operation: () -> Unit, delayMillis: Long): Boolean {
        val catchableBlock: () -> Unit = {
            try {
                operation()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "addOperationAfterDelay(): ", ex)
                /*@formatter:on*/
            }
        }

        return handler.postDelayed(catchableBlock, delayMillis)
    }

    /**
     * Removes pending operation from OperationQueue.
     */
    fun removeOperation(operation: () -> Unit) {
        val catchableBlock: () -> Unit = {
            try {
                operation()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e(TAG, "removeOperation(): ", ex)
                /*@formatter:on*/
            }
        }

        handler.removeCallbacks(catchableBlock)
    }

    /**
     * Remove all pending Operations that are in OperationQueue
     *
     * IMPORTANT: BE careful with this method as it may cause data loss
     */
    fun removeAllOperations() {
        handler.removeCallbacksAndMessages(null)
    }

    private val TAG: String = OperationQueue::class.java.simpleName
}