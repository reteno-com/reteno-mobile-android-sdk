package com.reteno.core.data.local.database.manager

import com.reteno.core.base.robolectric.BaseRobolectricTest
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertFalse
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
            inAppInteractionManager
        )
    }

    @Test
    fun givenAllTablesEmpty_whenNoDataForSync_thenFalseReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertFalse(hasDataForSync)
    }

    @Test
    fun givenDevicesPresent_whenNoDataForSync_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 3
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertTrue(hasDataForSync)
    }

    @Test
    fun givenUsersPresent_whenNoDataForSync_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 4
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertTrue(hasDataForSync)
    }

    @Test
    fun givenInteractionsPresent_whenNoDataForSync_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 4
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertTrue(hasDataForSync)
    }

    @Test
    fun givenEventPresent_whenNoDataForSync_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 5
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertTrue(hasDataForSync)
    }

    @Test
    fun givenAppInboxPresent_whenNoDataForSync_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 6
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertTrue(hasDataForSync)
    }

    @Test
    fun givenRecomEventPresent_whenNoDataForSync_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 9
        every { wrappedLinksManager.getWrappedLinksCount() } returns 0

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertTrue(hasDataForSync)
    }

    @Test
    fun givenWrappedLinksPresent_whenNoDataForSync_thenTrueReturned() {
        // Given
        every { deviceManager.getUnSyncedDeviceCount() } returns 0
        every { userManager.getUnSyncedUserCount() } returns 0
        every { interactionManager.getInteractionCount() } returns 0
        every { eventsManager.getEventsCount() } returns 0
        every { appInboxManager.getAppInboxMessagesCount() } returns 0
        every { recomEventsManager.getRecomEventsCount() } returns 0
        every { wrappedLinksManager.getWrappedLinksCount() } returns 11

        // When
        val hasDataForSync = SUT.hasDataForSync()

        // Then
        assertTrue(hasDataForSync)
    }
}