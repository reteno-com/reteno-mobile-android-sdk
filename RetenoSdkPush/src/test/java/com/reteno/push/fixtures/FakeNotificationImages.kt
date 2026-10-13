package com.reteno.push.fixtures

import android.graphics.Bitmap
import com.reteno.push.NotificationImages

internal class FakeNotificationImages(private val available: Map<String, Bitmap> = emptyMap()) : NotificationImages {
    val requested = mutableListOf<String>()

    override fun bigPicture(imageUrl: String): Bitmap? {
        requested += imageUrl
        return available[imageUrl]
    }

    override fun carouselImage(imageUrl: String): Bitmap? {
        requested += imageUrl
        return available[imageUrl]
    }
}

internal fun stubBitmap(): Bitmap = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)
