package com.reteno.core.features.iam

import androidx.annotation.Keep

@Keep
internal enum class IamJsEventType {
    WIDGET_INIT_SUCCESS,
    WIDGET_INIT_FAILED,
    WIDGET_RUNTIME_ERROR,
    CLOSE_WIDGET,
    OPEN_URL,
    CLICK,
}