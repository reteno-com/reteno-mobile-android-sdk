package com.reteno.sample.util

import com.reteno.core._interop.TestErrorInternal

object SdkTestError {
    const val isAvailable = true

    fun log() {
        TestErrorInternal.logTestError()
    }
}
