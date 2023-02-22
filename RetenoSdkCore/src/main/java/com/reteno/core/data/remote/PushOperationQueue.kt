package com.reteno.core.data.remote

import com.reteno.core.util.Logger

internal object PushOperationQueue {

    private val operationQueue = ArrayDeque<() -> Unit>()

    /**
     * Add operation to [PushOperationQueue] at the end.
     * Attention! To start the push operations queue, call [nextOperation].
     *
     * @param operation The operation that will be executed.
     */
    fun addOperation(operation: () -> Unit) {
        val catchableBlock: () -> Unit = {
            try {
                operation.invoke()
            } catch (ex: Throwable) {
                /*@formatter:off*/ Logger.e("TAG", "addOperation(): ", ex)
                /*@formatter:on*/
            }
        }

        operationQueue.add(catchableBlock)
    }

    /**
     * Sends the first operation from [PushOperationQueue] to [OperationQueue] for execution.
     */
    fun nextOperation() {
        operationQueue.removeFirstOrNull()?.let {
            OperationQueue.addOperation(it)
        }
    }

    /**
     * Remove all push Operations that are in [PushOperationQueue]
     */
    fun removeAllOperations() {
        operationQueue.clear()
    }
}