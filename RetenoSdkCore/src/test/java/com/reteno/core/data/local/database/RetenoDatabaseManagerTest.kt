package com.reteno.core.data.local.database

import com.reteno.core.base.robolectric.BaseRobolectricTest
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.*
import org.junit.Test


class RetenoDatabaseManagerTest : BaseRobolectricTest() {


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var database: RetenoDatabase

    private var SUT: RetenoDatabaseManagerImpl? = null
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = RetenoDatabaseManagerImpl(database)
        assertNotNull(SUT)
    }

    @Test
    fun givenAllTablesEmpty_whenIsDatabaseEmpty_thenTrueReturned() {
        // Given
        every { database.getRowCount(DbSchema.DeviceSchema.TABLE_NAME_DEVICE) } returns 0
        every { database.getRowCount(DbSchema.UserSchema.TABLE_NAME_USER) } returns 0
        every { database.getRowCount(DbSchema.InteractionSchema.TABLE_NAME_INTERACTION) } returns 0
        every { database.getRowCount(DbSchema.EventSchema.TABLE_NAME_EVENT) } returns 0

        // When
        val isDatabaseEmpty = SUT!!.isDatabaseEmpty()

        // Then
        assertTrue(isDatabaseEmpty)
    }

    @Test
    fun givenDevicesPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { database.getRowCount(DbSchema.DeviceSchema.TABLE_NAME_DEVICE) } returns 3
        every { database.getRowCount(DbSchema.UserSchema.TABLE_NAME_USER) } returns 0
        every { database.getRowCount(DbSchema.InteractionSchema.TABLE_NAME_INTERACTION) } returns 0
        every { database.getRowCount(DbSchema.EventSchema.TABLE_NAME_EVENT) } returns 0

        // When
        val isDatabaseEmpty = SUT!!.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenUsersPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { database.getRowCount(DbSchema.DeviceSchema.TABLE_NAME_DEVICE) } returns 0
        every { database.getRowCount(DbSchema.UserSchema.TABLE_NAME_USER) } returns 5
        every { database.getRowCount(DbSchema.InteractionSchema.TABLE_NAME_INTERACTION) } returns 0
        every { database.getRowCount(DbSchema.EventSchema.TABLE_NAME_EVENT) } returns 0

        // When
        val isDatabaseEmpty = SUT!!.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenInteractionsPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { database.getRowCount(DbSchema.DeviceSchema.TABLE_NAME_DEVICE) } returns 0
        every { database.getRowCount(DbSchema.UserSchema.TABLE_NAME_USER) } returns 0
        every { database.getRowCount(DbSchema.InteractionSchema.TABLE_NAME_INTERACTION) } returns 2
        every { database.getRowCount(DbSchema.EventSchema.TABLE_NAME_EVENT) } returns 0

        // When
        val isDatabaseEmpty = SUT!!.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenEventPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { database.getRowCount(DbSchema.DeviceSchema.TABLE_NAME_DEVICE) } returns 0
        every { database.getRowCount(DbSchema.UserSchema.TABLE_NAME_USER) } returns 0
        every { database.getRowCount(DbSchema.InteractionSchema.TABLE_NAME_INTERACTION) } returns 0
        every { database.getRowCount(DbSchema.EventSchema.TABLE_NAME_EVENT) } returns 5

        // When
        val isDatabaseEmpty = SUT!!.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }
}