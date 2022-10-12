package com.reteno.push

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.reteno.push.Constants.KEY_ES_CONTENT
import com.reteno.push.Constants.KEY_ES_INTERACTION_ID
import com.reteno.push.Constants.KEY_ES_NOTIFICATION_IMAGE
import com.reteno.push.Constants.KEY_ES_TITLE
import com.reteno.push.interceptor.click.RetenoNotificationClickedActivity
import com.reteno.push.interceptor.click.RetenoNotificationClickedReceiver
import com.reteno.push.internal.getApplicationMetaData
import com.reteno.util.BitmapUtil
import com.reteno.util.BuildUtil
import com.reteno.util.Logger
import com.reteno.util.getAppName
import java.util.*


internal object RetenoNotificationHelper {

    val TAG: String = RetenoNotificationHelper::class.java.simpleName

    private const val RETENO_DEFAULT_PUSH_ICON = "reteno_default_push_icon"
    private const val CHANNEL_DEFAULT_NAME = "default"
    private const val CHANNEL_DEFAULT_DESCRIPTION = "This is a default channel"
    const val CHANNEL_DEFAULT_ID: String = "CHANNEL_ID"

    private const val NOTIFICATION_ID_DEFAULT = 1

    internal fun getNotificationBuilderCompat(
        application: Application,
        bundle: Bundle
    ): NotificationCompat.Builder {
        val icon = getNotificationIcon(application)
        val title = getNotificationTitle(application, bundle)
        val text = getNotificationText(bundle)
        val bigPicture = getNotificationBigPictureBitmap(application.applicationContext, bundle)

        val builder = NotificationCompat.Builder(application.applicationContext, CHANNEL_DEFAULT_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        text?.let(builder::setContentText)

        bigPicture?.let {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(it)
                    .setBigContentTitle(title)
                    .setSummaryText(text)
            )
        }

        val pendingIntent = createPendingIntent(application.applicationContext, bundle)
        builder.setContentIntent(pendingIntent)

        return builder
    }

    internal fun createChannel(context: Context) {
        val name = CHANNEL_DEFAULT_NAME
        val descriptionText = CHANNEL_DEFAULT_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(CHANNEL_DEFAULT_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

    internal fun getNotificationId(bundle: Bundle): Int {
        val notificationIdString: String? = bundle.getString(KEY_ES_INTERACTION_ID)
        // FIXME notification id is calculated by hashCode() function on "es_interaction_id" string. Collisions may appear
        val notificationId: Int = notificationIdString?.hashCode() ?: NOTIFICATION_ID_DEFAULT
        /*@formatter:off*/ Logger.i(TAG, "getNotificationId(): ", "bundle = [" , bundle , "]", " notificationId = [", notificationId, "]")
        /*@formatter:on*/
        return notificationId
    }

    private fun getNotificationIcon(application: Application): Int {
        val metadata = application.getApplicationMetaData()
        val customIconResName = application.resources.getString(R.string.notification_icon)
        var icon = metadata.getInt(customIconResName)

        if (icon == 0) {
            /*@formatter:off*/ Logger.i(TAG, "getNotificationIcon(): ", "application = [" , application , "], No icon in metaData.")
            /*@formatter:on*/
            icon = getDefaultNotificationIcon(application.applicationContext)
        }

        return icon
    }

    /**
     * Gets default push notification resource id for RETENO_DEFAULT_PUSH_ICON in drawable.
     *
     * @param context Current application context.
     * @return int Resource id.
     */
    private fun getDefaultNotificationIcon(context: Context): Int {
        return try {
            /*@formatter:off*/ Logger.i(TAG, "getDefaultPushNotificationIconResourceId(): ", "context = [" , context , "]")
            /*@formatter:on*/
            val resources = context.resources
            resources.getIdentifier(RETENO_DEFAULT_PUSH_ICON, "drawable", context.packageName)
        } catch (ignored: Throwable) {
            0
        }
    }

    private fun getNotificationTitle(application: Application, bundle: Bundle): String {
        val title = bundle.getString(KEY_ES_TITLE) ?: application.getAppName()
        /*@formatter:off*/ Logger.i(TAG, "getNotificationName(): ", "application = [" , application , "], bundle = [" , bundle , "] title = [", title, "]")
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
     * @param context Current application context.
     * @param bundle Bundle with notification data.
     * @return Scaled bitmap for push notification with big image or null.
     */
    private fun getNotificationBigPictureBitmap(context: Context, bundle: Bundle): Bitmap? {
        val imageUrl = bundle.getString(KEY_ES_NOTIFICATION_IMAGE) ?: return null

        var bigPicture: Bitmap? = null
        // BigPictureStyle support requires API 16 and higher.
        if (!TextUtils.isEmpty(imageUrl)) {
            bigPicture = BitmapUtil.getScaledBitmap(context, imageUrl)
            if (bigPicture == null) {
                /*@formatter:off*/ Logger.i(TAG, "getNotificationBigPictureBitmap(): ", "Failed to download image for push notification;", " context = [" , context , "], imageUrl = [" , imageUrl , "]")
                /*@formatter:on*/
            }
        }
        return bigPicture
    }

    private fun createPendingIntent(context: Context, message: Bundle): PendingIntent {
        if (BuildUtil.shouldDisableTrampolines(context)) {
            val intent: Intent =
                createActivityIntent(context, message)
            return PendingIntent.getActivity(
                context,
                Random().nextInt(),
                intent,
                BuildUtil.createIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            val intent: Intent =
                createBroadcastIntent(context, message)
            return PendingIntent.getBroadcast(
                context,
                Random().nextInt(),
                intent,
                BuildUtil.createIntentFlags(0)
            )
        }
    }

    private fun createActivityIntent(context: Context, bundle: Bundle): Intent {
        val intent = Intent(context, RetenoNotificationClickedActivity::class.java)

        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun createBroadcastIntent(context: Context, message: Bundle): Intent {
        val intent = Intent(context, RetenoNotificationClickedReceiver::class.java)
        intent.addCategory("retenoAction")
        intent.putExtras(message)
        return intent
    }
}