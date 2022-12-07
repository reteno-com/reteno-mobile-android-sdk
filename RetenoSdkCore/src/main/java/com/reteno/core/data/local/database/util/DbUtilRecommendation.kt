package com.reteno.core.data.local.database.util

import android.content.ContentValues
import androidx.core.database.getStringOrNull
import com.reteno.core.data.local.database.schema.RecomEventsSchema
import com.reteno.core.data.local.model.recommendation.RecomEventDb
import com.reteno.core.data.local.model.recommendation.RecomEventTypeDb
import com.reteno.core.util.allElementsNotNull
import net.sqlcipher.Cursor

fun ContentValues.putRecomVariantId(recomVariantId: String) {
    put(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID, recomVariantId)
}

fun List<RecomEventDb>.toContentValuesList(recomVariantId: String): List<ContentValues> {
    val contentValues = mutableListOf<ContentValues>()

    for (recomEvent in this) {
        val singleContentValues = ContentValues().apply {
            put(RecomEventsSchema.COLUMN_RECOM_VARIANT_ID, recomVariantId)
            put(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_TYPE, recomEvent.recomEventType.toString())
            put(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_PRODUCT_ID, recomEvent.productId)
            put(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED, recomEvent.occurred)
        }
        contentValues.add(singleContentValues)
    }

    return contentValues
}

fun Cursor.getRecomEvent(): RecomEventDb? {
    val productId = getStringOrNull(getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_PRODUCT_ID))
    val occurred = getStringOrNull(getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_OCCURRED))
    val recomEventTypeString = getStringOrNull(getColumnIndex(RecomEventsSchema.RecomEventSchema.COLUMN_RECOM_EVENT_TYPE))
    val recomEventType = RecomEventTypeDb.fromString(recomEventTypeString)

    return if (allElementsNotNull(recomEventType, occurred, productId)) {
        return RecomEventDb(
            recomEventType = recomEventType!!,
            occurred = occurred!!,
            productId = productId!!
        )
    } else {
        null
    }
}