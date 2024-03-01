package com.reteno.core.data.remote.model.iam.displayrules.async

data class SegmentRule(
    val segmentId: Long
) {
    var isInSegment: Boolean = false
    var lastCheckedTimestamp: Long = 0L
}