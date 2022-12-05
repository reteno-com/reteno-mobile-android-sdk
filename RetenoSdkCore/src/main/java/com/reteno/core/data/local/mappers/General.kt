package com.reteno.core.data.local.mappers

import com.reteno.core.data.local.model.BooleanDb

fun Boolean.toDb(): BooleanDb =
    if (this == true) {
        BooleanDb.TRUE
    } else {
        BooleanDb.FALSE
    }