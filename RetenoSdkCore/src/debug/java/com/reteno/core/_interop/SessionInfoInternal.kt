package com.reteno.core._interop

import android.content.Context
import com.reteno.core.RetenoInternalEntryPoint
import com.reteno.core.data.local.sharedpref.SharedPrefsManager

object SessionInfoInternal {

    fun sessionStartTimestamp(context: Context): Long =
        SharedPrefsManager(context.applicationContext).getSessionStartTimestamp()

    fun setSessionDuration(durationMillis: Long) = RetenoInternalEntryPoint.facade.setSessionDuration(durationMillis)
}
