package com.reteno.push.channel

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
internal class NotificationChannelJson(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("importance") val importance: Int? = null,
    @SerializedName("enable_lights") val enableLights: Boolean? = null,
    @SerializedName("light_color") val lightColor: Int? = null,
    @SerializedName("enable_vibration") val enableVibration: Boolean? = null,
    @SerializedName("vibration_pattern") val vibrationPattern: List<Long>? = null,
    @SerializedName("lockscreen_visibility") val lockscreenVisibility: Int? = null,
    @SerializedName("bypass_dnd") val bypassDnd: Boolean? = null,
    @SerializedName("show_badge") val showBadge: Boolean? = null
) {

    fun toChannelData(): NotificationChannelData? {
        if (id == null || name == null || description == null) return null
        val defaults = NotificationChannelData(id, name, description)
        return defaults.copy(
            importance = importance ?: defaults.importance,
            enableLights = enableLights ?: defaults.enableLights,
            lightColor = lightColor ?: defaults.lightColor,
            enableVibration = enableVibration ?: defaults.enableVibration,
            vibrationPattern = vibrationPattern ?: defaults.vibrationPattern,
            lockscreenVisibility = lockscreenVisibility ?: defaults.lockscreenVisibility,
            bypassDnd = bypassDnd ?: defaults.bypassDnd,
            showBadge = showBadge ?: defaults.showBadge
        )
    }
}
