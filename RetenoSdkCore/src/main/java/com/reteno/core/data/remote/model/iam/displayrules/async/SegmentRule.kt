package com.reteno.core.data.remote.model.iam.displayrules.async

data class SegmentRule(
    val segmentId: Long
) {
    var isInSegment: Boolean = false
    var lastCheckedTimestamp: Long? = null
    var retryParams: AsyncRuleRetryParams? = null

    fun shouldCheckStatus(sessionStartTimestamp: Long): Boolean {
        val params = retryParams
        val lastCheckTime = lastCheckedTimestamp
        return when {
            lastCheckTime == null -> true
            params != null -> params.shouldRetry(sessionStartTimestamp, lastCheckTime)
            else -> System.currentTimeMillis() - lastCheckTime > AsyncRuleRetryParams.STANDARD_RETRY_TIME
        }
    }
}