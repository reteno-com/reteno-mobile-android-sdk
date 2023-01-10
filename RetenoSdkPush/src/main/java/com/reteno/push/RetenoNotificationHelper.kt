package com.reteno.push

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.reteno.core.RetenoImpl
import com.reteno.core.util.BitmapUtil
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
import com.reteno.push.Constants.KEY_ES_NOTIFICATION_IMAGE
import com.reteno.push.Constants.KEY_ES_TITLE
import com.reteno.push.Constants.MAX_ACTION_BUTTONS
import com.reteno.push.JsonUtils.getJSONObjectOrNull
import com.reteno.push.JsonUtils.getStringOrNull
import com.reteno.push.channel.RetenoNotificationChannel.DEFAULT_CHANNEL_ID
import com.reteno.push.interceptor.click.RetenoNotificationClickedActivity
import com.reteno.push.interceptor.click.RetenoNotificationClickedReceiver
import org.json.JSONArray
import java.util.Random


internal object RetenoNotificationHelper {

    private val TAG: String = RetenoNotificationHelper::class.java.simpleName

    private const val RETENO_DEFAULT_PUSH_ICON = "reteno_default_push_icon"

    private const val NOTIFICATION_ID_DEFAULT = 1

    @JvmStatic
    internal fun getNotificationBuilderCompat(bundle: Bundle): NotificationCompat.Builder {
        val context = RetenoImpl.application
        /*@formatter:off*/ Logger.i(TAG, "getNotificationBuilderCompat(): ", "context = [" , context , "], bundle = [" , bundle.toStringVerbose() , "]")
        /*@formatter:on*/
        val icon = getNotificationIcon()
        val title = getNotificationTitle(bundle)
        val text = getNotificationText(bundle)
        val bigPicture = getNotificationBigPictureBitmap(bundle)
        val buttons = getNotificationButtons(bundle)
        val badgeCount = getNotificationBadgeCount(bundle)

        val builder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        text?.let(builder::setContentText)
        badgeCount?.let(builder::setNumber)

        bigPicture?.let {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(it)
                    .setBigContentTitle(title)
                    .setSummaryText(text)
            )
        }

        buttons?.forEach { action ->
            builder.addAction(action)
        }

        val pendingIntent = createPendingIntent(bundle)
        builder.setContentIntent(pendingIntent)

        return builder
    }

    @JvmStatic
    internal fun getNotificationId(bundle: Bundle): Int {
        val notificationIdString: String? = bundle.getString(KEY_ES_INTERACTION_ID)
        // FIXME notification id is calculated by hashCode() function on "es_interaction_id" string. Collisions may appear
        val notificationId: Int = notificationIdString?.hashCode() ?: NOTIFICATION_ID_DEFAULT
        /*@formatter:off*/ Logger.i(TAG, "getNotificationId(): ", "bundle = [" , bundle , "]", " notificationId = [", notificationId, "]")
        /*@formatter:on*/
        return notificationId
    }

    private fun getNotificationIcon(): Int {
        val context = RetenoImpl.application

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
            val context = RetenoImpl.application

            val resources = context.resources
            resources.getIdentifier(RETENO_DEFAULT_PUSH_ICON, "drawable", context.packageName)
        } catch (ignored: Throwable) {
            0
        }
    }

    private fun getNotificationTitle(bundle: Bundle): String {
        val context = RetenoImpl.application
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
            bigPicture = BitmapUtil.getScaledBitmap(imageUrl)
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

    private fun createPendingIntent(message: Bundle): PendingIntent {
        val context = RetenoImpl.application

        if (BuildUtil.shouldDisableTrampolines()) {
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
        val context = RetenoImpl.application
        val intent = Intent(context, RetenoNotificationClickedActivity::class.java)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun createBroadcastIntent(bundle: Bundle): Intent {
        val context = RetenoImpl.application
        val intent = Intent(context, RetenoNotificationClickedReceiver::class.java)
        intent.putExtras(bundle)
        return intent
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