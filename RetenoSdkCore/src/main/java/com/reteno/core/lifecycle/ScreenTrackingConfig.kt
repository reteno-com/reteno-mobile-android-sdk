package com.reteno.core.lifecycle

data class ScreenTrackingConfig @JvmOverloads constructor(
    val enable: Boolean,
    val excludeScreens: List<String> = listOf(),
    val trigger: ScreenTrackingTrigger = ScreenTrackingTrigger.ON_START
)
