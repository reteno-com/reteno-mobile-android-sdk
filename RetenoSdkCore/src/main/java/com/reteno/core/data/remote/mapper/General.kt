package com.reteno.core.data.remote.mapper

import com.reteno.core.data.local.model.BooleanDb

internal fun BooleanDb.toRemote(): Boolean =
    when (this) {
        BooleanDb.TRUE -> true
        BooleanDb.FALSE -> false
    }
