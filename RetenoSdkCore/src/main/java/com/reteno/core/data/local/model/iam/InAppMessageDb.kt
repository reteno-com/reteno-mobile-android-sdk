package com.reteno.core.data.local.model.iam

data class InAppMessageDb(
    val rowId: String? = null,
    val createdAt: Long = 0L,
    val messageId: Long,
    val messageInstanceId: Long,
    val displayRules: String,
    val lastShowTime: Long?,
    val showCount: Long = 0,
    val layoutType: String?,
    val model: String?,
    val position: String?
) {
    var segment: SegmentDb? = null
}