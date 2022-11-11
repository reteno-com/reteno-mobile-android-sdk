package com.reteno.core.data.local.database

import com.reteno.core.data.local.model.interaction.InteractionDb
import com.reteno.core.data.remote.model.event.EventsRemote
import com.reteno.core.data.remote.model.user.UserRemote
import com.reteno.core.domain.model.device.Device

interface RetenoDatabaseManager {

    fun insertDevice(device: Device)
    fun getDevices(limit: Int? = null): List<Device>
    fun getDeviceCount(): Long
    fun deleteDevices(count: Int, oldest: Boolean = true)

    fun insertUser(user: UserRemote)
    fun getUser(limit: Int? = null): List<UserRemote>
    fun getUserCount(): Long
    fun deleteUsers(count: Int, oldest: Boolean = true)

    fun insertInteraction(interaction: InteractionDb)
    fun getInteractions(limit: Int? = null): List<InteractionDb>
    fun getInteractionCount(): Long
    fun deleteInteractions(count: Int, oldest: Boolean = true)

    fun insertEvents(events: EventsRemote)
    fun getEvents(limit: Int? = null): List<EventsRemote>
    fun getEventsCount(): Long
    fun deleteEvents(count: Int, oldest: Boolean = true)

    fun isDatabaseEmpty(): Boolean
}