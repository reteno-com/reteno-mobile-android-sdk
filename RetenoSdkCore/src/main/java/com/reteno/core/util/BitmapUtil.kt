package com.reteno.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.WindowManager
import com.reteno.core.InternalRetenoApi
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.math.min
import kotlin.math.roundToInt

object BitmapUtil {
    private val TAG: String = BitmapUtil::class.java.simpleName

    // Layout_height for imageView is 192dp
    // https://android.googlesource.com/platform/frameworks/base
    // /+/6387d2f6dae27ba6e8481883325adad96d3010f4/core/res/res/layout
    // /notification_template_big_picture.xml
    private const val BIG_PICTURE_MAX_HEIGHT_DP = 192

    /**
     * Create a scaled bitmap.
     *
     * Aspect ratio of the source image stays unchanged.
     * If aspect ratio of a source image doesn't conform to 2:1 rule,
     * centerInside scale type applied and other areas of an image will be transparent
     *
     * @see android.widget.ImageView.ScaleType.CENTER_INSIDE
     *
     * @param imageUrl The string of URL image.
     * @return The scaled bitmap with 2:1 aspect ratio.
     */
    fun getScaledBitmap(context: Context, imageUrl: String): Bitmap? {
        // Processing an image depending on the current screen size to avoid central crop if the image
        // ratio is more than 2:1. Google aspect ~2:1 - page 78
        // http://commondatastorage.googleapis.com/io2012/presentations/live%20to%20website/105.pdf
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val pixelsHeight = (displayMetrics.density * BIG_PICTURE_MAX_HEIGHT_DP + 0.5f).roundToInt()
        val pixelsWidth = min(2 * pixelsHeight, displayMetrics.widthPixels)
        val source = getBitmapFromUrl(imageUrl, pixelsWidth, pixelsHeight) ?: return null
        return try {
            centerInside(source, pixelsWidth, pixelsHeight)
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "getScaledBitmap(): image does not fit ($pixelsWidth, $pixelsHeight)", e)
            source
        }
    }

    private fun centerInside(source: Bitmap, width: Int, height: Int): Bitmap {
        val scaled = resize(bitmap = source, maxWidth = width, maxHeight = height)
        val targetBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(targetBitmap)
        canvas.drawColor(Color.TRANSPARENT)
        val xOffset = (targetBitmap.width - scaled.width) / 2f
        val yOffset = (targetBitmap.height - scaled.height) / 2f
        canvas.drawBitmap(scaled, xOffset, yOffset, null)
        return targetBitmap
    }

    fun resize(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        var image = bitmap
        return if (maxHeight > 0 && maxWidth > 0) {
            val width = image.width
            val height = image.height
            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
            image
        } else {
            image
        }
    }


    /**
     * Opens a connection to an image with connect and read timeouts, so a slow server can't use up
     * the time a push message handler has.
     */
    @InternalRetenoApi
    fun openImageConnection(imageUrl: String): HttpURLConnection {
        val url = URL(imageUrl)
        if (url.protocol != "https" && url.protocol != "http") {
            throw MalformedURLException("Images are loaded over http(s) only")
        }
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = IMAGE_CONNECT_TIMEOUT_MS
            readTimeout = IMAGE_READ_TIMEOUT_MS
        }
    }

    /**
     * Downloads and decodes an image, or returns null when it can't be loaded.
     */
    @InternalRetenoApi
    fun downloadBitmap(imageUrl: String): Bitmap? {
        val bytes = downloadBytes(imageUrl) ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun getBitmapFromUrl(imageUrl: String, width: Int, height: Int): Bitmap? {
        val bytes = downloadBytes(imageUrl) ?: return null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        options.inSampleSize = calculateInSampleSize(options, width, height)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun downloadBytes(imageUrl: String): ByteArray? =
        try {
            val connection = openImageConnection(imageUrl)
            try {
                if (connection.contentLength > MAX_IMAGE_BYTES) null else connection.inputStream.use(::readUpToLimit)
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            Logger.e(TAG, "IOException in image download", e)
            null
        }

    private fun readUpToLimit(input: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) return output.toByteArray()
            output.write(buffer, 0, read)
            if (output.size() > MAX_IMAGE_BYTES) return null
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

    private const val IMAGE_CONNECT_TIMEOUT_MS = 3_000
    private const val IMAGE_READ_TIMEOUT_MS = 5_000
    private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    private const val DOWNLOAD_BUFFER_BYTES = 8 * 1024
}