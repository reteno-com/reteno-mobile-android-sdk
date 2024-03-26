package com.reteno.core.data.local.model.iam

data class SegmentDb(
    val segmentId: Long,
    val isInSegment: Boolean,
    val lastCheckTime: Long?,
    val checkStatusCode: Int?,
    val retryAfter: Long?,
)