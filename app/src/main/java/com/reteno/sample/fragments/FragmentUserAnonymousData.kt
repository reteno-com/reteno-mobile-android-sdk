package com.reteno.sample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.reteno.core.domain.model.user.Address
import com.reteno.core.domain.model.user.UserAttributesAnonymous
import com.reteno.core.domain.model.user.UserCustomField
import com.reteno.core.util.Logger.e
import com.reteno.sample.BaseFragment
import com.reteno.sample.R
import com.reteno.sample.databinding.FragmentUserAnonymousDataBinding
import com.reteno.sample.util.Util

class FragmentUserAnonymousData : BaseFragment() {

    private var binding: FragmentUserAnonymousDataBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserAnonymousDataBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun setListeners() {
        binding!!.btnSend.setOnClickListener {
            try {
                initUserData()
                Toast.makeText(this.context, "Sent", Toast.LENGTH_SHORT).show()
                NavHostFragment.findNavController(this).popBackStack()
            } catch (e: Exception) {
                Toast.makeText(this.context, e.message, Toast.LENGTH_LONG).show()
                e(TAG, "FragmentUserData", e)
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

    private fun initUserData() {
        val userCustomData = getUserCustomData()
        val address = Address(
            Util.getTextOrNull(binding!!.etRegion),
            Util.getTextOrNull(binding!!.etTown),
            Util.getTextOrNull(binding!!.etAddress),
            Util.getTextOrNull(binding!!.etPostcode)
        )
        val userAttributes = UserAttributesAnonymous(
            Util.getTextOrNull(binding!!.etFirstName),
            Util.getTextOrNull(binding!!.etLastName),
            Util.getTextOrNull(binding!!.etLanguageCode),
            Util.getTextOrNull(binding!!.etTimeZone),
            address,
            userCustomData
        )
        sendAnonymousUserAttributes(userAttributes)
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

    private fun sendAnonymousUserAttributes(attributes: UserAttributesAnonymous?) {
        reteno.setAnonymousUserAttributes(attributes!!)
    }

    private fun createNewFields(): View {
        return LayoutInflater
            .from(binding!!.getRoot().context)
            .inflate(R.layout.view_user_custom_fields_horizontal, binding!!.llCustomData, false)
    }

    companion object {
        private val TAG = FragmentUserAnonymousData::class.java.getSimpleName()
    }
}