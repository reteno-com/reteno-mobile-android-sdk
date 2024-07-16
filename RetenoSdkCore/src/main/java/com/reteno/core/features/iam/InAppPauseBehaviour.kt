package com.reteno.core.features.iam

/**
 *
 * @property SKIP_IN_APPS - All In-Apps will be skipped when In-Apps are in paused state.
 * @property POSTPONE_IN_APPS - First triggered InApp in paused state will be postponed till
 * In-App messages are going to be enabled again
 */
enum class InAppPauseBehaviour {
    SKIP_IN_APPS,
    POSTPONE_IN_APPS
}