package com.reteno.core.fixtures

import androidx.work.impl.WorkManagerImpl
import org.robolectric.util.ReflectionHelpers

internal fun <T> withoutWorkManager(block: () -> T): T {
    forgetWorkManager()
    try {
        return block()
    } finally {
        forgetWorkManager()
    }
}

private fun forgetWorkManager() {
    ReflectionHelpers.setStaticField(WorkManagerImpl::class.java, "sDelegatedInstance", null)
    ReflectionHelpers.setStaticField(WorkManagerImpl::class.java, "sDefaultInstance", null)
}
