package com.reteno.push

import android.content.Context
import android.graphics.Bitmap
import com.reteno.core.util.BitmapUtil
import com.reteno.push.Constants.NOTIFICATION_CAROUSEL_IMAGE_MAX_HEIGHT_PX
import com.reteno.push.Constants.NOTIFICATION_CAROUSEL_IMAGE_MAX_WIDTH_PX

internal interface NotificationImages {
    fun bigPicture(imageUrl: String): Bitmap?

    fun carouselImage(imageUrl: String): Bitmap?
}

internal class RemoteNotificationImages(private val context: Context) : NotificationImages {

    override fun bigPicture(imageUrl: String): Bitmap? = BitmapUtil.getScaledBitmap(context, imageUrl)

    override fun carouselImage(imageUrl: String): Bitmap? {
        val bitmap = BitmapUtil.downloadBitmap(imageUrl) ?: return null
        return BitmapUtil.resize(bitmap, NOTIFICATION_CAROUSEL_IMAGE_MAX_WIDTH_PX, NOTIFICATION_CAROUSEL_IMAGE_MAX_HEIGHT_PX)
    }
}
