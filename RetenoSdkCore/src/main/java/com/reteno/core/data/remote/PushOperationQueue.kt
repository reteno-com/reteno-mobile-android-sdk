package com.reteno.core.data.remote

object PushOperationQueue {

    private val operationQueue = ArrayDeque<Runnable>()

    /**
     * Add operation to [PushOperationQueue] at the end.
     * Attention! To start the push operations queue, call [nextOperation].
     *
     * @param operation The operation that will be executed.
     */
    fun addOperation(operation: Runnable) {
        operationQueue.add(operation)
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