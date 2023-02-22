package com.reteno.core.data.remote.mapper

import com.reteno.core.base.BaseUnitTest
import com.reteno.core.data.local.model.user.AddressDb
import com.reteno.core.data.local.model.user.UserAttributesDb
import com.reteno.core.data.local.model.user.UserCustomFieldDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.model.user.AddressRemote
import com.reteno.core.data.remote.model.user.UserAttributesRemote
import com.reteno.core.data.remote.model.user.UserCustomFieldRemote
import com.reteno.core.data.remote.model.user.UserRemote
import junit.framework.TestCase.assertEquals
import org.junit.Test


class UserMappersKtTest : BaseUnitTest() {

    // region constants ----------------------------------------------------------------------------
    companion object {
        private const val DEVICE_ID = "DEVICE_ID"
        private const val EXTERNAL_USER_ID = "EXTERNAL_USER_ID"
        private val SUBSCRIPTION_KEYS = listOf("SUBSCRIPTION_KEYS")
        private val GROUP_NAMES_INCLUDE = listOf("GROUP_NAMES_INCLUDE")
        private val GROUP_NAMES_EXCLUDE = listOf("GROUP_NAMES_EXCLUDE")

        private const val PHONE = "PHONE"
        private const val EMAIL = "EMAIL"
        private const val FIRST_NAME = "FIRST_NAME"
        private const val LAST_NAME = "LAST_NAME"
        private const val LANGUAGE_CODE = "LANGUAGE_CODE"
        private const val TIME_ZONE = "TIME_ZONE"
        private const val CUSTOM_FIELD_KEY_1 = "CUSTOM_FIELD_KEY_1"
        private const val CUSTOM_FIELD_VALUE_1 = "CUSTOM_FIELD_VALUE_1"
        private const val CUSTOM_FIELD_KEY_2 = "CUSTOM_FIELD_KEY_2"
        private const val CUSTOM_FIELD_VALUE_2 = "CUSTOM_FIELD_VALUE_2"

        private const val REGION = "REGION"
        private const val TOWN = "TOWN"
        private const val ADDRESS = "ADDRESS"
        private const val POSTCODE = "POSTCODE"
    }
    // endregion constants -------------------------------------------------------------------------

    @Test
    fun givenAddressDb_whenToRemote_thenAddressRemoteReturned() {
        // Given
        val addressDb = getAddressDb()
        val expectedAddressRemote = getAddressRemote()

        // When
        val actualAddressRemote = addressDb.toRemote()

        // Then
        assertEquals(expectedAddressRemote, actualAddressRemote)
    }

    @Test
    fun givenUserCustomFieldDb_whenToRemote_thenUserCustomFieldRemoteReturned() {
        // Given
        val userCustomFieldDb = getUserCustomFieldDb1()
        val expectedUserCustomFieldRemote = getUserCustomFieldRemote1()

        // When
        val actualUserCustomFieldRemote = userCustomFieldDb.toRemote()

        // Then
        assertEquals(expectedUserCustomFieldRemote, actualUserCustomFieldRemote)
    }

    @Test
    fun givenUserAttributesDb_whenToRemote_thenUserAttributesRemoteReturned() {
        // Given
        val userAttributesDb = getUserAttributesDb()
        val expectedUserAttributesRemote = getUserAttributesRemote()

        // When
        val actualUserAttributesRemote = userAttributesDb.toRemote()

        // Then
        assertEquals(expectedUserAttributesRemote, actualUserAttributesRemote)
    }

    @Test
    fun givenUserDb_whenToRemote_thenUserRemoteReturned() {
        // Given
        val userDb = getUserDb()
        val expectedUserRemote = getUserRemote()

        // When
        val actualUserRemote = userDb.toRemote()

        // Then
        assertEquals(expectedUserRemote, actualUserRemote)
    }

    // region helper methods -----------------------------------------------------------------------
    private fun getAddressDb() = AddressDb(
        region = REGION, town = TOWN, address = ADDRESS, postcode = POSTCODE
    )

    private fun getAddressRemote() = AddressRemote(
        region = REGION, town = TOWN, address = ADDRESS, postcode = POSTCODE
    )

    private fun getUserCustomFieldDb1() =
        UserCustomFieldDb(key = CUSTOM_FIELD_KEY_1, value = CUSTOM_FIELD_VALUE_1)

    private fun getUserCustomFieldDb2() =
        UserCustomFieldDb(key = CUSTOM_FIELD_KEY_2, value = CUSTOM_FIELD_VALUE_2)

    private fun getUserCustomFieldRemote1() =
        UserCustomFieldRemote(key = CUSTOM_FIELD_KEY_1, value = CUSTOM_FIELD_VALUE_1)

    private fun getUserCustomFieldRemote2() =
        UserCustomFieldRemote(key = CUSTOM_FIELD_KEY_2, value = CUSTOM_FIELD_VALUE_2)

    private fun getUserAttributesDb() = UserAttributesDb(
        phone = PHONE,
        email = EMAIL,
        firstName = FIRST_NAME,
        lastName = LAST_NAME,
        languageCode = LANGUAGE_CODE,
        timeZone = TIME_ZONE,
        address = getAddressDb(),
        fields = listOf(getUserCustomFieldDb1(), getUserCustomFieldDb2())
    )

    private fun getUserAttributesRemote() = UserAttributesRemote(
        phone = PHONE,
        email = EMAIL,
        firstName = FIRST_NAME,
        lastName = LAST_NAME,
        languageCode = LANGUAGE_CODE,
        timeZone = TIME_ZONE,
        address = getAddressRemote(),
        fields = listOf(getUserCustomFieldRemote1(), getUserCustomFieldRemote2())
    )

    private fun getUserDb() = UserDb(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_USER_ID,
        userAttributes = getUserAttributesDb(),
        subscriptionKeys = SUBSCRIPTION_KEYS,
        groupNamesInclude = GROUP_NAMES_INCLUDE,
        groupNamesExclude = GROUP_NAMES_EXCLUDE
    )

    private fun getUserRemote() = UserRemote(
        deviceId = DEVICE_ID,
        externalUserId = EXTERNAL_USER_ID,
        userAttributes = getUserAttributesRemote(),
        subscriptionKeys = SUBSCRIPTION_KEYS,
        groupNamesInclude = GROUP_NAMES_INCLUDE,
        groupNamesExclude = GROUP_NAMES_EXCLUDE
    )
    // endregion helper methods --------------------------------------------------------------------
}