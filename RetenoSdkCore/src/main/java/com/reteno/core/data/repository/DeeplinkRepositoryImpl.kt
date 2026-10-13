package com.reteno.core.data.repository

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerWrappedLink
import com.reteno.core.data.remote.OperationQueue
import com.reteno.core.util.Logger

internal class DeeplinkRepositoryImpl(
    private val databaseManager: RetenoDatabaseManagerWrappedLink,
    private val requestSync: () -> Unit
) : DeeplinkRepository {

    override fun saveWrappedLink(wrappedLink: String) {
        /*@formatter:off*/ Logger.i(TAG, "saveWrappedLink(): ", "wrappedLink = [", wrappedLink, "]")
        /*@formatter:on*/
        if (wrappedLink.isBlank()) return

        OperationQueue.addParallelOperation {
            databaseManager.insertWrappedLink(wrappedLink)
            requestSync()
        }
    }

    companion object {
        private val TAG: String = DeeplinkRepositoryImpl::class.java.simpleName

        private const val REMOVE_WRAPPED_LINKS = "Removed wrapped links"
    }
}