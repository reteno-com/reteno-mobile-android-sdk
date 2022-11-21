package com.reteno.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.view.WindowManager
import com.reteno.core.RetenoImpl
import java.io.IOException
import java.io.InputStream
import java.net.URL
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapUtil {
    val TAG: String = BitmapUtil::class.java.simpleName

    // Layout_height for imageView is 192dp
    // https://android.googlesource.com/platform/frameworks/base
    // /+/6387d2f6dae27ba6e8481883325adad96d3010f4/core/res/res/layout
    // /notification_template_big_picture.xml
    private const val BIG_PICTURE_MAX_HEIGHT_DP = 192

    /**
     * Create a scaled bitmap.
     *
     * @param imageUrl The string of URL image.
     * @return The scaled bitmap.
     */
    fun getScaledBitmap(imageUrl: String): Bitmap? {
        val context = RetenoImpl.application
        /*@formatter:off*/ Logger.i(TAG, "getScaledBitmap(): ", "context = [" , context , "], imageUrl = [" , imageUrl , "]")
        /*@formatter:on*/

        // Processing an image depending on the current screen size to avoid central crop if the image
        // ratio is more than 2:1. Google aspect ~2:1 - page 78
        // http://commondatastorage.googleapis.com/io2012/presentations/live%20to%20website/105.pdf
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val pixelsHeight = (displayMetrics.density * BIG_PICTURE_MAX_HEIGHT_DP + 0.5f).roundToInt()
        val pixelsWidth = min(2 * pixelsHeight, displayMetrics.widthPixels)
        var bitmap: Bitmap? = getBitmapFromUrl(imageUrl, pixelsWidth, pixelsHeight)
        try {
            bitmap = Bitmap.createScaledBitmap(bitmap!!, pixelsWidth, pixelsHeight, true)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed on scale image $imageUrl to ($pixelsWidth, $pixelsHeight)", e)
        }
        return bitmap
    }

    /**
     * Download a scaled bitmap.
     *
     * @param imageUrl The string of URL image.
     * @param width The requested width of the image.
     * @param height The requested height of the image.
     * @return The scaled bitmap downloaded form URL.
     */
    private fun getBitmapFromUrl(imageUrl: String, width: Int, height: Int): Bitmap? {
        var input: InputStream? = null
        return try {
            input = URL(imageUrl).openStream()

            // First decode with inJustDecodeBounds=true to check dimensions.
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(input, null, options)
            closeStream(input)
            input = URL(imageUrl).openStream()
            options.inSampleSize = calculateInSampleSize(options, width, height)
            // Decode bitmap with inSampleSize set.
            options.inJustDecodeBounds = false
            BitmapFactory.decodeStream(input, null, options)
        } catch (e: IOException) {
            Logger.e(TAG, String.format("IOException in image download for URL: %s.", imageUrl), e)
            null
        } finally {
            closeStream(input)
        }
    }

    /**
     * Method to calculate a sample size value that is a power of two based on a target width and
     * height. From official Android documentation:
     * https://developer.android.com/training/displaying-bitmaps/load-bitmap.html
     *
     * @param reqWidth The requested width of the image.
     * @param reqHeight The requested height of the image.
     * @return The calculated inSampleSize - power of two.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image.
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                && halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Method to close InputStream.
     *
     * @param inputStream The InputStream which must be closed.
     */
    private fun closeStream(inputStream: InputStream?) {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            Logger.e(TAG, "IOException during closing of image download stream.", e)
        }
    }
}