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
    @RelaxedMockK
    private lateinit var wrappedLinksManager: RetenoDatabaseManagerWrappedLink
    @RelaxedMockK
    private lateinit var logEventManager: RetenoDatabaseManagerLogEvent
    @RelaxedMockK
    private lateinit var inAppMessagesManager: RetenoDatabaseManagerInAppMessages
    @RelaxedMockK
    private lateinit var inAppInteractionManager: RetenoDatabaseManagerInAppInteraction

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
            recomEventsManager,
            wrappedLinksManager,
            logEventManager,
            inAppMessagesManager,
            inAppInteractionManager
        )
    }

    @Test
    fun givenAllTablesEmpty_whenIsDatabaseEmpty_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertTrue(isDatabaseEmpty)
    }

    @Test
    fun givenDevicesPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 3
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenUsersPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 4
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenInteractionsPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 4
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenEventPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 5
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenAppInboxPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 6
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenRecomEventPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 9
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertFalse(isDatabaseEmpty)
    }

    @Test
    fun givenWrappedLinksPresent_whenIsDatabaseEmpty_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 11

        // When
        val isDatabaseEmpty = SUT.hasDataForSync()

        // Then
        assertFalse(isDatabaseEmpty)
    }
}