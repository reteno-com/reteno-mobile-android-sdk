package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.reteno.core.RetenoInternalImpl
import com.reteno.core._interop.DeviceIdInternal.getIdInternal
import com.reteno.core._interop.DeviceIdInternal.getModeInternal
import com.reteno.core.data.local.config.DeviceId
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.mappers.toDb
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.core.util.allElementsNull
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentUserDataBinding
import com.reteno.sample.util.Util

class UserWriteFragment : BaseDatabaseDialogFragment() {
    private var binding: FragmentUserDataBinding? = null
    private var databaseManager: RetenoDatabaseManagerUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerUserProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserDataBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun setListeners() {
        binding!!.btnSend.setOnClickListener {
            initUserData()
            Toast.makeText(this.context, "Sent", Toast.LENGTH_SHORT).show()
        }
        binding!!.btnCustomFieldsAdd.setOnClickListener {
            val view = createNewFields()
            binding!!.llCustomData.addView(view)
        }
        binding!!.btnCustomFieldsMinus.setOnClickListener {
            val countView = binding!!.llCustomData.childCount
            if (countView > 0) {
                binding!!.llCustomData.removeViewAt(countView - 1)
            }
        }
    }

    private fun initUserData() {
        val externalId = Util.getTextOrNull(binding!!.etExternalId)
        val userCustomData = getUserCustomData()
        val address: Address? = if (allElementsNull(
                Util.getTextOrNull(binding!!.etRegion),
                Util.getTextOrNull(binding!!.etTown),
                Util.getTextOrNull(binding!!.etAddress),
                Util.getTextOrNull(binding!!.etPostcode)
            )
        ) {
            null
        } else {
            Address(
                Util.getTextOrNull(binding!!.etRegion),
                Util.getTextOrNull(binding!!.etTown),
                Util.getTextOrNull(binding!!.etAddress),
                Util.getTextOrNull(binding!!.etPostcode)
            )
        }
        val userAttributes: UserAttributes? = if (allElementsNull(
                Util.getTextOrNull(binding!!.etPhone),
                Util.getTextOrNull(binding!!.etEmail),
                Util.getTextOrNull(binding!!.etFirstName),
                Util.getTextOrNull(binding!!.etLastName),
                Util.getTextOrNull(binding!!.etLanguageCode),
                Util.getTextOrNull(binding!!.etTimeZone),
                address,
                userCustomData
            )
        ) {
            null
        } else {
            UserAttributes(
                Util.getTextOrNull(binding!!.etPhone),
                Util.getTextOrNull(binding!!.etEmail),
                Util.getTextOrNull(binding!!.etFirstName),
                Util.getTextOrNull(binding!!.etLastName),
                Util.getTextOrNull(binding!!.etLanguageCode),
                Util.getTextOrNull(binding!!.etTimeZone),
                address,
                userCustomData
            )
        }
        val subscriptionKeys = Util.getListFromEditText(
            binding!!.etSubscriptionKeys
        )
        val groupNamesInclude = Util.getListFromEditText(
            binding!!.etGroupNamesInclude
        )
        val groupNamesExclude = Util.getListFromEditText(
            binding!!.etGroupNamesExclude
        )
        val user = User(userAttributes, subscriptionKeys, groupNamesInclude, groupNamesExclude)
        sendUserData(externalId.orEmpty(), user)
    }

    private fun getUserCustomData(): List<UserCustomField>? {
        val countView = binding!!.llCustomData.childCount
        if (countView == 0) return null
        val list: MutableList<UserCustomField> = ArrayList()
        for (i in 0 until countView) {
            val parent = binding!!.llCustomData.getChildAt(i) as LinearLayout
            val etKey = parent.getChildAt(0) as EditText
            val etValue = parent.getChildAt(1) as EditText
            val key = Util.getTextOrNull(etKey)
            val value = Util.getTextOrNull(etValue)
            if (key != null) {
                list.add(UserCustomField(key, value))
            }
        }
        return list
    }

    private fun createNewFields(): View {
        return LayoutInflater
            .from(binding!!.getRoot().context)
            .inflate(R.layout.view_user_custom_fields_horizontal, binding!!.llCustomData, false)
    }

    private fun sendUserData(externalId: String, user: User) {
        var deviceId = getDeviceId()
        deviceId = deviceId.copy(
            deviceId.getIdInternal(),
            externalId,
            deviceId.getModeInternal(),
            user.userAttributes!!.email,
            user.userAttributes!!.phone
        )
        val userDb: UserDb = user.toDb(deviceId)
        databaseManager!!.insertUser(userDb)
    }

    private fun getDeviceId(): DeviceId {
        val retenoImpl = RetenoInternalImpl.instance
        return retenoImpl.serviceLocator.configRepositoryProvider.get().getDeviceId()
    }
}
