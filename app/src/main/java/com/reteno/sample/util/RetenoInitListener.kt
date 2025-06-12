package com.reteno.sample.util

import com.reteno.core.RetenoInternalImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RetenoInitListener(
    private val retenoImpl: RetenoInternalImpl,
    private val onSuccessListener: suspend () -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            while (!retenoImpl.isInitialized) {
                delay(500L)
            }
            withContext(Dispatchers.Main) {
                onSuccessListener()
            }
        }
    }
}