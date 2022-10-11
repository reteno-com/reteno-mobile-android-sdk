package com.reteno.push

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.reteno.util.Logger
import com.reteno.util.getApplicationMetaData

internal object RetenoNotificationHelper {

    val TAG: String = RetenoNotificationHelper::class.java.simpleName
    private const val RETENO_DEFAULT_PUSH_ICON = "reteno_default_push_icon"
    private const val CHANNEL_DEFAULT_NAME = "default"
    private const val CHANNEL_DEFAULT_DESCRIPTION = "This is a default channel"
    const val CHANNEL_DEFAULT_ID: String = "CHANNEL_ID"

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

    internal fun getNotificationIcon(application: Application): Int {
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

}