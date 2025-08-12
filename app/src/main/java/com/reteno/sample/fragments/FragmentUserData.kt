package com.reteno.sample.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.User
import com.reteno.core.domain.model.user.UserAttributes
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentUserDataBinding
import com.reteno.sample.util.Util

class FragmentUserData : BaseFragment() {
    private var binding: FragmentUserDataBinding? = null
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
            try {
                createUserData()
                Toast.makeText(this.context, "Sent", Toast.LENGTH_SHORT).show()
                NavHostFragment.findNavController(this).popBackStack()
            } catch (e: Exception) {
                Toast.makeText(this.context, e.message, Toast.LENGTH_LONG).show()
                Log.e(TAG, "FragmentUserData", e)
            }
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

    private fun createUserData() {
        var externalId: String? = binding!!.etExternalId.getText().toString()
        if (externalId!!.contains("null")) {
            externalId = null
        }
        val isMultiAccount = binding!!.cbMultiAccount.isChecked
        val userCustomData = getUserCustomData()
        val address = Address(
            Util.getTextOrNull(binding!!.etRegion),
            Util.getTextOrNull(binding!!.etTown),
            Util.getTextOrNull(binding!!.etAddress),
            Util.getTextOrNull(binding!!.etPostcode)
        )
        val userAttributes = UserAttributes(
            Util.getTextOrNull(binding!!.etPhone),
            Util.getTextOrNull(binding!!.etEmail),
            Util.getTextOrNull(binding!!.etFirstName),
            Util.getTextOrNull(binding!!.etLastName),
            Util.getTextOrNull(binding!!.etLanguageCode),
            Util.getTextOrNull(binding!!.etTimeZone),
            address,
            userCustomData
        )
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
        sendUserData(externalId, isMultiAccount, user)
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

    protected fun sendUserData(externalId: String?, isMultiAccount: Boolean, user: User?) {
        if (isMultiAccount) {
            reteno.setMultiAccountUserAttributes(externalId!!, user)
        } else {
            reteno.setUserAttributes(externalId!!, user)
        }
    }

    private fun createNewFields(): View {
        return LayoutInflater
            .from(binding!!.getRoot().context)
            .inflate(R.layout.view_user_custom_fields_horizontal, binding!!.llCustomData, false)
    }

    companion object {
        private val TAG = FragmentUserData::class.java.getSimpleName()
    }
}