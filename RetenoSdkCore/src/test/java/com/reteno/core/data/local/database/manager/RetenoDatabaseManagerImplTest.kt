package com.reteno.core.data.local.database.manager

import com.reteno.core.base.robolectric.BaseRobolectricTest
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test


class RetenoDatabaseManagerImplTest : BaseRobolectricTest() {


    // region helper fields ------------------------------------------------------------------------
    @RelaxedMockK
    private lateinit var deviceManager: RetenoDatabaseManagerDevice
    @RelaxedMockK
    private lateinit var userManager: RetenoDatabaseManagerUser
    @RelaxedMockK
    private lateinit var interactionManager: RetenoDatabaseManagerInteraction
    @RelaxedMockK
    private lateinit var eventsManager: RetenoDatabaseManagerEvents
    @RelaxedMockK
    private lateinit var appInboxManager: RetenoDatabaseManagerAppInbox
    @RelaxedMockK
    private lateinit var recomEventsManager: RetenoDatabaseManagerRecomEvents

    private lateinit var SUT: RetenoDatabaseManager
    // endregion helper fields ---------------------------------------------------------------------

    override fun before() {
        super.before()
        SUT = RetenoDatabaseManagerImpl(
            deviceManager,
            userManager,
            interactionManager,
            eventsManager,
            appInboxManager,
            recomEventsManager
        )
    }

    @Test
    fun givenAllTablesEmpty_whenIsDatabaseEmpty_thenTrueReturned() {
        // Given
        every { deviceManager.getDeviceCount() } returns 0
        every { userManager.getUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.isDatabaseEmpty()

        // Then
        assertTrue(isDatabaseEmpty)
    }

    @Test
    fun givenDevicesPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getDeviceCount() } returns 3
        every { userManager.getUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenUsersPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getDeviceCount() } returns 0
        every { userManager.getUserCount() } returns 4
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenInteractionsPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getDeviceCount() } returns 0
        every { userManager.getUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 4
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenEventPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getDeviceCount() } returns 0
        every { userManager.getUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 5
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenAppInboxPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getDeviceCount() } returns 0
        every { userManager.getUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 6
        every { recomEventsManager.getRecomEventsCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenRecomEventPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getDeviceCount() } returns 0
        every { userManager.getUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 9

        // When
        val isDatabaseEmpty = SUT.isDatabaseEmpty()

        // Then
        assertFalse(isDatabaseEmpty)
    }
}