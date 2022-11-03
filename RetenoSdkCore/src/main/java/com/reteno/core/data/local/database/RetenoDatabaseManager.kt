package com.reteno.core.data.local.database

import com.reteno.core.data.local.model.InteractionModelDb
import com.reteno.core.data.remote.model.user.UserDTO
import com.reteno.core.model.Events
import com.reteno.core.model.device.Device

interface RetenoDatabaseManager {

    fun insertDevice(device: Device)
    fun getDevices(limit: Int? = null): List<Device>
    fun getDeviceCount(): Long
    fun deleteDevices(count: Int, oldest: Boolean = true)

    fun insertUser(user: UserDTO)
    fun getUser(limit: Int? = null): List<UserDTO>
    fun getUserCount(): Long
    fun deleteUsers(count: Int, oldest: Boolean = true)

    fun insertInteraction(interaction: InteractionModelDb)
    fun getInteractions(limit: Int? = null): List<InteractionModelDb>
    fun getInteractionCount(): Long
    fun deleteInteractions(count: Int, oldest: Boolean = true)

    fun insertEvents(events: Events)
    fun getEvents(limit: Int? = null): List<Events>
    fun getEventsCount(): Long
    fun deleteEvents(count: Int, oldest: Boolean = true)
}