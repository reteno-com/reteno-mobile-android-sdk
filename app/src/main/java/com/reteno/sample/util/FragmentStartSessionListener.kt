package com.reteno.sample.util

import android.widget.TextView
import com.reteno.core.lifecycle.RetenoSessionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentStartSessionListener {

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var textView: TextView? = null
    private var sessionHandler: RetenoSessionHandler? = null

    fun start(sessionHandler: RetenoSessionHandler, textView: TextView) {
        this.textView = textView
        this.sessionHandler = sessionHandler

        job = scope.launch {
            while (true) {
                countTime()
                delay(1000L)
            }
        }
    }

    fun stop() {
        sessionHandler = null
        textView = null
        job?.cancel()
        job = null
    }

    private suspend fun countTime() {
        sessionHandler?.getForegroundTimeMillis()?.let { timeMillis ->
            val timeSeconds = timeMillis / 1000L
            val timeMinutes = timeSeconds / 60L
            val timeHours = timeMinutes / 60L
            val timeMinutesInHour = timeMinutes % 60L
            val timeSecondsInMinute = timeSeconds % 60L
            withContext(Dispatchers.Main) {
                val text = when {
                    timeHours > 0 -> "${timeHours}h ${timeMinutesInHour}m ${timeSecondsInMinute}s"
                    timeMinutes > 0 -> "${timeMinutesInHour}m ${timeSecondsInMinute}s"
                    else -> "${timeSecondsInMinute}s"
                }
                textView?.text = text
            }
        }
    }
}