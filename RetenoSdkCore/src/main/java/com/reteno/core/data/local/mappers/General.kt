package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.BooleanDb

internal fun Boolean.toDb(): BooleanDb =
    if (this == true) {
        BooleanDb.TRUE
    } else {
        BooleanDb.FALSE
    }

// for some boolean values we have to send the value in int format
fun Boolean.toIntValue(): Int {
    return if (this) 1 else 0
}