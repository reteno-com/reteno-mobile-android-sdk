package com.reteno.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.reteno.core.data.remote.mapper.listFromJson
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.getAppName
import com.reteno.core.util.getApplicationMetaData
import com.reteno.push.Constants.KEY_ACTION_BUTTON
import com.reteno.push.Constants.KEY_BTN_ACTION_CUSTOM_DATA
import com.reteno.push.Constants.KEY_BTN_ACTION_ID
import com.reteno.push.Constants.KEY_BTN_ACTION_LABEL
import com.reteno.push.Constants.KEY_BTN_ACTION_LINK_UNWRAPPED
import com.reteno.push.Constants.KEY_BTN_ACTION_LINK_WRAPPED
import com.reteno.push.Constants.KEY_ES_BADGE_COUNT
import com.reteno.push.Constants.KEY_ES_BUTTONS
import com.reteno.push.Constants.KEY_ES_BUTTON_ACTION_ID
import com.reteno.push.Constants.KEY_ES_BUTTON_CUSTOM_DATA
import com.reteno.push.Constants.KEY_ES_BUTTON_LABEL
import com.reteno.push.Constants.KEY_ES_BUTTON_LINK_UNWRAPPED
import com.reteno.push.Constants.KEY_ES_BUTTON_LINK_WRAPPED
import com.reteno.push.Constants.KEY_ES_CONTENT
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.Constants.KEY_ES_NOTIFICATION_CAROUSEL_IMAGES
import com.reteno.push.Constants.KEY_ES_NOTIFICATION_IMAGE
import com.reteno.push.Constants.KEY_ES_TITLE
import com.reteno.push.Constants.MAX_ACTION_BUTTONS
import com.reteno.push.JsonUtils.getStringOrNull
import com.reteno.push.channel.RetenoNotificationChannel.DEFAULT_CHANNEL_ID
import com.reteno.push.interceptor.click.RetenoNotificationClickedActivity
import com.reteno.push.interceptor.click.RetenoNotificationClickedReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random


internal class RetenoNotificationHelper(
    private val context: Context,
    private val images: NotificationImages = RemoteNotificationImages(context)
) {

    companion object {
        private const val RETENO_DEFAULT_PUSH_ICON = "reteno_default_push_icon"
        private const val LONG_TEXT_LENGTH = 100

        internal const val NOTIFICATION_ID_DEFAULT = 1
    }

    internal fun getNotificationBuilderCompat(bundle: Bundle): NotificationCompat.Builder {
        val title = bundle.getString(KEY_ES_TITLE) ?: context.getAppName()
        val text = bundle.getString(KEY_ES_CONTENT)
        val bigPicture = getNotificationBigPictureBitmap(bundle)
        val carousel = getImageCarouselRemoteViews(bundle)

        val builder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setSmallIcon(getNotificationIcon())
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        applyIconColor(builder)
        if (text != null) applyText(builder, text, hasDecorations = bigPicture != null || carousel != null)
        bundle.getString(KEY_ES_BADGE_COUNT)?.toIntOrNull()?.let(builder::setNumber)
        if (bigPicture != null) applyBigPicture(builder, bigPicture, title, text)
        if (carousel != null) {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            builder.setCustomBigContentView(carousel)
        }
        getNotificationButtons(bundle).forEach(builder::addAction)
        createDeleteIntent(bundle)?.let(builder::setDeleteIntent)
        groupOf(bundle)?.let(builder::setGroup)
        builder.setContentIntent(createPendingIntent(bundle))
        return builder
    }

    internal fun getNotificationId(bundle: Bundle): Int =
        bundle.getString(KEY_ES_INTERACTION_ID)?.hashCode() ?: NOTIFICATION_ID_DEFAULT

    private fun applyIconColor(builder: NotificationCompat.Builder) {
        val color = context.getApplicationMetaData().getInt(context.resources.getString(R.string.notification_icon_color))
        if (color == 0) return
        builder
            .setColor(ContextCompat.getColor(context, color))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
    }

    private fun applyText(builder: NotificationCompat.Builder, text: String, hasDecorations: Boolean) {
        if (text.length > LONG_TEXT_LENGTH && !hasDecorations) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        } else {
            builder.setContentText(text)
        }
    }

    private fun applyBigPicture(builder: NotificationCompat.Builder, bitmap: Bitmap, title: String, text: String?) {
        builder.setLargeIcon(bitmap)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .setBigContentTitle(title)
                    .setSummaryText(text)
                    .clearLargeIcon()
            )
    }

    private fun getNotificationIcon(): Int {
        val icon = context.getApplicationMetaData().getInt(context.resources.getString(R.string.notification_icon))
        if (icon != 0) return icon
        return context.resources.getIdentifier(RETENO_DEFAULT_PUSH_ICON, "drawable", context.packageName)
    }

    private fun getNotificationBigPictureBitmap(bundle: Bundle): Bitmap? {
        val imageUrl = bundle.getString(KEY_ES_NOTIFICATION_IMAGE)?.takeIf { it.isNotBlank() } ?: return null
        return images.bigPicture(imageUrl)
    }

    private fun getNotificationButtons(bundle: Bundle): List<NotificationCompat.Action> {
        val esButtons = bundle.getString(KEY_ES_BUTTONS) ?: return emptyList()
        val array = JSONArray(esButtons)
        return (0 until minOf(array.length(), MAX_ACTION_BUTTONS)).map { index ->
            val button = array.getJSONObject(index)
            val label = button.getStringOrNull(KEY_ES_BUTTON_LABEL)
            NotificationCompat.Action(null, label, createPendingIntentForButton(bundle, button, label))
        }
    }

    private fun getImageCarouselRemoteViews(bundle: Bundle): RemoteViews? {
        val imageUrls = getCarouselImageUrls(bundle)
        if (imageUrls.isEmpty()) return null

        val expandedView = RemoteViews(context.packageName, R.layout.image_carousel)
        for (imageUrl in imageUrls) {
            val bitmap = images.carouselImage(imageUrl) ?: continue
            val viewFlipperImage = RemoteViews(context.packageName, R.layout.image_carousel_item)
            viewFlipperImage.setImageViewBitmap(R.id.imageView, bitmap)
            expandedView.addView(R.id.viewFlipper, viewFlipperImage)
        }
        return expandedView
    }

    private fun getCarouselImageUrls(bundle: Bundle): List<String> {
        val imagesJson = bundle.getString(KEY_ES_NOTIFICATION_CAROUSEL_IMAGES) ?: return emptyList()
        val imageUrls = runCatching { imagesJson.listFromJson<String>() }.getOrNull() ?: return emptyList()
        return imageUrls.filter { it.isNotBlank() }
    }

    private fun groupOf(bundle: Bundle): String? {
        val groupingRule = RetenoNotifications.groupingRule ?: return null
        return groupingRule(bundle.toStringMap())
    }

    private fun createPendingIntent(message: Bundle): PendingIntent {
        return if (BuildUtil.shouldDisableTrampolines(context)) {
            val intent = Intent(context, RetenoNotificationClickedActivity::class.java)
                .putExtras(message)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            PendingIntent.getActivity(
                context,
                Random().nextInt(),
                intent,
                BuildUtil.createIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            val intent = Intent(context, RetenoNotificationClickedReceiver::class.java).putExtras(message)
            PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                intent,
                BuildUtil.createIntentFlags(0)
            )
        }
    }

    private fun createDeleteIntent(data: Bundle): PendingIntent? {
        val receiver = context.getApplicationMetaData()
            .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_DELETED) ?: return null
        val intent = Intent().setClassName(context, receiver).putExtras(data)
        return PendingIntent.getBroadcast(
            context,
            getNotificationId(data),
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createPendingIntentForButton(bundle: Bundle, button: JSONObject, label: String?): PendingIntent {
        val bundleButton = bundle.deepCopy()
        bundleButton.putBoolean(KEY_ACTION_BUTTON, true)
        button.getStringOrNull(KEY_ES_BUTTON_ACTION_ID)?.let { bundleButton.putString(KEY_BTN_ACTION_ID, it) }
        label?.let { bundleButton.putString(KEY_BTN_ACTION_LABEL, it) }
        button.getStringOrNull(KEY_ES_BUTTON_LINK_WRAPPED)?.let { bundleButton.putString(KEY_BTN_ACTION_LINK_WRAPPED, it) }
        button.getStringOrNull(KEY_ES_BUTTON_LINK_UNWRAPPED)?.let { bundleButton.putString(KEY_BTN_ACTION_LINK_UNWRAPPED, it) }
        button.optJSONObject(KEY_ES_BUTTON_CUSTOM_DATA)?.let { bundleButton.putSerializable(KEY_BTN_ACTION_CUSTOM_DATA, JsonUtils.jsonObjectToMap(it)) }
        return createPendingIntent(bundleButton)
    }
}

private fun NotificationCompat.BigPictureStyle.clearLargeIcon(): NotificationCompat.BigPictureStyle {
    val nullBitmap: Bitmap? = null
    bigLargeIcon(nullBitmap)
    return this
}
