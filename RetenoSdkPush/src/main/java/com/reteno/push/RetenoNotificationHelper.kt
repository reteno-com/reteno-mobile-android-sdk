package com.reteno.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.reteno.core.data.remote.mapper.listFromJson
import com.reteno.core.util.BitmapUtil
import com.reteno.core.util.BitmapUtil.resize
import com.reteno.core.util.BuildUtil
import com.reteno.core.util.Logger
import com.reteno.core.util.getAppName
import com.reteno.core.util.getApplicationMetaData
import com.reteno.core.util.toStringVerbose
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
import com.reteno.push.Constants.NOTIFICATION_CAROUSEL_IMAGE_MAX_HEIGHT_PX
import com.reteno.push.Constants.NOTIFICATION_CAROUSEL_IMAGE_MAX_WIDTH_PX
import com.reteno.push.JsonUtils.getJSONObjectOrNull
import com.reteno.push.JsonUtils.getStringOrNull
import com.reteno.push.channel.RetenoNotificationChannel.DEFAULT_CHANNEL_ID
import com.reteno.push.interceptor.click.RetenoNotificationClickedActivity
import com.reteno.push.interceptor.click.RetenoNotificationClickedReceiver
import org.json.JSONArray
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Random


internal class RetenoNotificationHelper(private val context: Context) {

    private val TAG: String = RetenoNotificationHelper::class.java.simpleName

    companion object {
        private const val RETENO_DEFAULT_PUSH_ICON = "reteno_default_push_icon"

        private const val NOTIFICATION_ID_DEFAULT = 1
    }

    internal fun getNotificationBuilderCompat(bundle: Bundle): NotificationCompat.Builder {
        /*@formatter:off*/ Logger.i(TAG, "getNotificationBuilderCompat(): ", "context = [" , context , "], bundle = [" , bundle.toStringVerbose() , "]")
        /*@formatter:on*/
        val icon = getNotificationIcon()
        val color = getNotificationIconColor()
        val title = getNotificationTitle(bundle)
        val text = getNotificationText(bundle)
        val bigPicture = getNotificationBigPictureBitmap(bundle)
        val buttons = getNotificationButtons(bundle)
        val badgeCount = getNotificationBadgeCount(bundle)
        val imageCarouselRemoteViews: RemoteViews? = getImageCarouselRemoteViews(bundle)

        val builder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        color.takeUnless { it == 0 }?.let {
            builder
                .setColor(ContextCompat.getColor(context, it))
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }
        text?.let(builder::setContentText)
        badgeCount?.let(builder::setNumber)

        bigPicture?.let { bitmap ->
            builder.setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setBigContentTitle(title)
                        .setSummaryText(text)
                        .clearLargeIcon()
                )
        }
        imageCarouselRemoteViews?.let {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            builder.setCustomBigContentView(it)
        }

        buttons?.forEach { action ->
            builder.addAction(action)
        }

        createDeleteIntent(bundle)?.let {
            builder.setDeleteIntent(it)
        }

        val pendingIntent = createPendingIntent(bundle)
        builder.setContentIntent(pendingIntent)

        return builder
    }

    internal fun getNotificationId(bundle: Bundle): Int {
        val notificationIdString: String? = bundle.getString(KEY_ES_INTERACTION_ID)
        // FIXME notification id is calculated by hashCode() function on "es_interaction_id" string. Collisions may appear
        val notificationId: Int = notificationIdString?.hashCode() ?: NOTIFICATION_ID_DEFAULT
        /*@formatter:off*/ Logger.i(TAG, "getNotificationId(): ", "bundle = [" , bundle , "]", " notificationId = [", notificationId, "]")
        /*@formatter:on*/
        return notificationId
    }

    private fun getNotificationIconColor(): Int {

        val metadata = context.getApplicationMetaData()
        val customIconColorResName = context.resources.getString(R.string.notification_icon_color)
        return metadata.getInt(customIconColorResName)
    }

    private fun getNotificationIcon(): Int {

        val metadata = context.getApplicationMetaData()
        val customIconResName = context.resources.getString(R.string.notification_icon)
        var icon = metadata.getInt(customIconResName)

        if (icon == 0) {
            /*@formatter:off*/ Logger.i(TAG, "getNotificationIcon(): ", "context = [" , context , "], No icon in metaData.")
            /*@formatter:on*/
            icon = getDefaultNotificationIcon()
        }

        return icon
    }

    /**
     * Gets default push notification resource id for RETENO_DEFAULT_PUSH_ICON in drawable.
     *
     * @return int Resource id.
     */
    private fun getDefaultNotificationIcon(): Int {
        return try {

            val resources = context.resources
            resources.getIdentifier(RETENO_DEFAULT_PUSH_ICON, "drawable", context.packageName)
        } catch (ignored: Throwable) {
            0
        }
    }

    private fun getNotificationTitle(bundle: Bundle): String {
        val title = bundle.getString(KEY_ES_TITLE) ?: context.getAppName()
        /*@formatter:off*/ Logger.i(TAG, "getNotificationName(): ", "context = [" , context , "], bundle = [" , bundle , "] title = [", title, "]")
        /*@formatter:on*/
        return title
    }

    private fun getNotificationText(bundle: Bundle): String? {
        val notificationText = bundle.getString(KEY_ES_CONTENT)
        /*@formatter:off*/ Logger.i(TAG, "getNotificationText(): ", "bundle = [" , bundle , "] notificationText = [", notificationText, "]")
        /*@formatter:on*/
        return notificationText
    }

    /**
     * Gets bitmap for BigPicture style push notification.
     *
     * @param bundle Bundle with notification data.
     * @return Scaled bitmap for push notification with big image or null.
     */
    private fun getNotificationBigPictureBitmap(bundle: Bundle): Bitmap? {
        val imageUrl = bundle.getString(KEY_ES_NOTIFICATION_IMAGE) ?: return null

        var bigPicture: Bitmap? = null
        // BigPictureStyle support requires API 16 and higher.
        if (!TextUtils.isEmpty(imageUrl)) {
            bigPicture = BitmapUtil.getScaledBitmap(context, imageUrl)
            if (bigPicture == null) {
                /*@formatter:off*/ Logger.i(TAG, "getNotificationBigPictureBitmap(): ", "Failed to download image for push notification;", " imageUrl = [" , imageUrl , "]")
                /*@formatter:on*/
            }
        }
        return bigPicture
    }

    private fun getNotificationButtons(bundle: Bundle): List<NotificationCompat.Action>? {
        val esButtons = bundle.getString(KEY_ES_BUTTONS) ?: return null

        val actions = mutableListOf<NotificationCompat.Action>()
        val array = JSONArray(esButtons)

        for (i in 0 until array.length()) {
            val jsonObject = array.getJSONObject(i)

            val actionId = jsonObject.getStringOrNull(KEY_ES_BUTTON_ACTION_ID)
            val wrappedLink = jsonObject.getStringOrNull(KEY_ES_BUTTON_LINK_WRAPPED)
            val unwrappedLink = jsonObject.getStringOrNull(KEY_ES_BUTTON_LINK_UNWRAPPED)
            val label = jsonObject.getStringOrNull(KEY_ES_BUTTON_LABEL)
            val customData = jsonObject.getJSONObjectOrNull(KEY_ES_BUTTON_CUSTOM_DATA)
                ?.let(JsonUtils::jsonObjectToMap)

            val intent = createPendingIntentForButton(
                bundle = bundle,
                actionId = actionId,
                wrappedLink = wrappedLink,
                unwrappedLink = unwrappedLink,
                label = label,
                customData = customData
            )
            actions.add(NotificationCompat.Action(null, label, intent))
        }

        return actions.take(MAX_ACTION_BUTTONS)
    }

    private fun getNotificationBadgeCount(bundle: Bundle): Int? =
        bundle.getString(KEY_ES_BADGE_COUNT)?.toIntOrNull()

    private fun getImageCarouselRemoteViews(bundle: Bundle): RemoteViews? {
        val imageUrlList = getCarouselImageUrlList(bundle) ?: return null

        val expandedView = RemoteViews(context.packageName, R.layout.image_carousel)

        for (imageUrl in imageUrlList) {
            val viewFlipperImage =
                RemoteViews(context.packageName, R.layout.image_carousel_item)

            try {
                val remotePicture = BitmapFactory.decodeStream(
                    URL(imageUrl).content as InputStream
                )
                val bitmap = resize(
                    remotePicture,
                    NOTIFICATION_CAROUSEL_IMAGE_MAX_WIDTH_PX,
                    NOTIFICATION_CAROUSEL_IMAGE_MAX_HEIGHT_PX
                )
                viewFlipperImage.setImageViewBitmap(R.id.imageView, bitmap)
                expandedView.addView(R.id.viewFlipper, viewFlipperImage)
            } catch (e: IOException) {
                /*@formatter:off*/ Logger.e(TAG, "getImageCarouselRemoteViews(): ", e)
                /*@formatter:on*/
            }
        }

        return expandedView
    }

    private fun getCarouselImageUrlList(bundle: Bundle): List<String>? {
        val imagesJson = bundle.getString(KEY_ES_NOTIFICATION_CAROUSEL_IMAGES)
        val imageUrlListAll = imagesJson?.listFromJson<String>()

        return if (imageUrlListAll.isNullOrEmpty() ||
            imageUrlListAll.all { it.isBlank() }
        ) {
            null
        } else {
            imageUrlListAll.filter { it.isNotBlank() }
        }
    }

    private fun createPendingIntent(message: Bundle): PendingIntent {

        if (BuildUtil.shouldDisableTrampolines(context)) {
            val intent: Intent =
                createActivityIntent(message)
            return PendingIntent.getActivity(
                context,
                Random().nextInt(),
                intent,
                BuildUtil.createIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            val intent: Intent =
                createBroadcastIntent(message)
            return PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                intent,
                BuildUtil.createIntentFlags(0)
            )
        }
    }

    private fun createActivityIntent(bundle: Bundle): Intent {
        val intent = Intent(context, RetenoNotificationClickedActivity::class.java)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun createBroadcastIntent(bundle: Bundle): Intent {
        val intent = Intent(context, RetenoNotificationClickedReceiver::class.java)
        intent.putExtras(bundle)
        return intent
    }

    private fun createDeleteIntent(data: Bundle): PendingIntent? {
        try {
            val receiver = context.getApplicationMetaData()
                .getString(Constants.META_DATA_KEY_CUSTOM_RECEIVER_NOTIFICATION_DELETED)
            receiver?.let { receiverClassName ->
                val intent = Intent()
                intent.setClassName(context, receiverClassName)
                intent.putExtras(data)
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                    )
                } else {
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }
        } catch (e: Throwable) {
            Logger.e(TAG, "createDeleteIntent()", e)
        }
        return null
    }

    private fun createPendingIntentForButton(
        bundle: Bundle,
        actionId: String?,
        wrappedLink: String?,
        unwrappedLink: String?,
        label: String?,
        customData: HashMap<String, Any?>?
    ): PendingIntent {
        val bundleButton = bundle.deepCopy()
        bundleButton.putBoolean(KEY_ACTION_BUTTON, true)
        actionId?.let { bundleButton.putString(KEY_BTN_ACTION_ID, it) }
        label?.let { bundleButton.putString(KEY_BTN_ACTION_LABEL, it) }
        wrappedLink?.let { bundleButton.putString(KEY_BTN_ACTION_LINK_WRAPPED, it) }
        unwrappedLink?.let { bundleButton.putString(KEY_BTN_ACTION_LINK_UNWRAPPED, it) }
        customData?.let { bundleButton.putSerializable(KEY_BTN_ACTION_CUSTOM_DATA, it) }

        return createPendingIntent(bundleButton)
    }
}

private fun NotificationCompat.BigPictureStyle.clearLargeIcon(): NotificationCompat.BigPictureStyle {
    val nullBitmap: Bitmap? = null
    bigLargeIcon(nullBitmap)
    return this
}
