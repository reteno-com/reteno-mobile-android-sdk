package com.reteno.sample.testscreens

import android.os.Bundle
import androidx.navigation.NavDirections
import java.util.Objects

data class ScreenItem @JvmOverloads constructor(
    val name: String,
    val navigationId: Int = -1,
    val direction: NavDirections? = null,
    val bundle: Bundle? = null
)
