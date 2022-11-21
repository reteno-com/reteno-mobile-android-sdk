package com.reteno.core.data.local.database

import com.reteno.core.data.local.model.device.DeviceDb
import com.reteno.core.data.local.model.event.EventsDb
import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.local.model.user.UserDb

interface RetenoDatabaseManager {

    fun insertDevice(device: DeviceDb)
    fun getDevices(limit: Int? = null): List<DeviceDb>
    fun getDeviceCount(): Long
    fun deleteDevices(count: Int, oldest: Boolean = true)

    fun insertUser(user: UserDb)
    fun getUser(limit: Int? = null): List<UserDb>
    fun getUserCount(): Long
    fun deleteUsers(count: Int, oldest: Boolean = true)

    fun insertInteraction(interaction: InteractionDb)
    fun getInteractions(limit: Int? = null): List<InteractionDb>
    fun getInteractionCount(): Long
    fun deleteInteractions(count: Int, oldest: Boolean = true)
    fun deleteInteractionByTime(outdatedTime: String): Int

    fun insertEvents(events: EventsDb)
    fun getEvents(limit: Int? = null): List<EventsDb>
    fun getEventsCount(): Long
    fun deleteEvents(count: Int, oldest: Boolean = true)
    fun deleteEventsByTime(outdatedTime: String): Int

    fun isDatabaseEmpty(): Boolean
}