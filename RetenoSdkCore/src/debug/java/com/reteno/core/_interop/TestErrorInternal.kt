package com.reteno.core._interop

import com.reteno.core.util.Logger

object TestErrorInternal {

    fun logTestError() {
        Logger.e(TAG, "logTestError(): ", NullPointerException("This is a test crash in SDK"))
    }

    private const val TAG = "TestErrorInternal"
}
