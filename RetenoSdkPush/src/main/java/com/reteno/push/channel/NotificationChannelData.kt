package com.reteno.push.channel

import android.app.Notification
import android.app.NotificationManager
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName


/**
 * JSON data format
 *   {
 *       "id":"id", 					// required
 *       "name":"name",					// required
 *       "description":"description",	// required
 *       "importance":4,
 *       "enable_lights":false,
 *       "light_color":0,
 *       "enable_vibration":false,
 *       "vibration_pattern":[122,222,333],
 *       "lockscreen_visibility":1,
 *       "bypass_dnd":false,
 *       "show_badge":false
 *   }
 */
@Keep
internal data class NotificationChannelData(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,

    @SerializedName("importance") val importance: Int = NotificationManager.IMPORTANCE_HIGH,
    @SerializedName("enable_lights") val enableLights: Boolean = false,
    @SerializedName("light_color") val lightColor: Int = 0,
    @SerializedName("enable_vibration") val enableVibration: Boolean = false,
    @SerializedName("vibration_pattern") val vibrationPattern: List<Long>? = null,
    @SerializedName("lockscreen_visibility") val lockscreenVisibility: Int = Notification.VISIBILITY_PUBLIC,
    @SerializedName("bypass_dnd") val bypassDnd: Boolean = false,
    @SerializedName("show_badge") val showBadge: Boolean = false
)