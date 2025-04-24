package com.reteno.sample.fragments.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser
import com.reteno.core.data.local.model.user.UserDb
import com.reteno.core.data.remote.mapper.toJsonOrNull
import com.reteno.sample.R
import com.reteno.sample.databinding.ItemDbUserBinding
import com.reteno.sample.databinding.ViewUserCustomFieldsVerticalBinding
import com.reteno.sample.fragments.database.UserReadDialogFragment.UserAdapter
import com.reteno.sample.fragments.database.UserReadDialogFragment.UserViewHolder
import java.util.Locale

internal class UserReadDialogFragment :
    BaseReadDialogFragment<UserDb, ItemDbUserBinding, UserViewHolder, UserAdapter>() {
    private var databaseManager: RetenoDatabaseManagerUser? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databaseManager = serviceLocator.retenoDatabaseManagerUserProvider.get()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseManager = null
    }

    override fun initAdapter() {
        adapter = UserAdapter {
            TransitionManager.beginDelayedTransition(
                bindingMain!!.rvList, AutoTransition()
            )
        }
    }

    override fun initCount() {
        val userEventsCount = databaseManager!!.getUnSyncedUserCount()
        bindingMain!!.tvCount.text = String.format(Locale.US, "Count: %d", userEventsCount)
    }

    override fun initItems() {
        val newItems = databaseManager!!.getUsers(null)
        adapter!!.setItems(newItems)
    }

    override fun deleteItems(count: Int) {
        val users = databaseManager!!.getUsers(count)
        for (user in users) {
            databaseManager!!.deleteUser(user)
        }
    }

    //==============================================================================================
    internal class UserAdapter(listener: ViewHolderListener) :
        BaseReadAdapter<UserDb, ItemDbUserBinding, UserViewHolder>(listener) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
            val binding =
                ItemDbUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserViewHolder(binding)
        }

        override fun initListeners(binding: ItemDbUserBinding) {
            binding.ivExpand.setOnClickListener {
                if (binding.llContent.visibility == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.GONE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more)
                } else {
                    onExpandCollapseClickListener.onExpandCollapse()
                    binding.llContent.visibility = View.VISIBLE
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less)
                }
            }
        }
    }

    //==============================================================================================
    class UserViewHolder(binding: ItemDbUserBinding) :
        BaseReadViewHolder<UserDb, ItemDbUserBinding>(binding) {
        override fun bind(user: UserDb) {
            bindingHolder.tvDeviceId.setTextOrHide(user.deviceId)
            bindingHolder.tvExternalUserId.setTextOrHide(user.externalUserId)
            bindingHolder.tvSubscriptionKeys.setTextOrHide(user.subscriptionKeys.toJsonOrNull())
            bindingHolder.tvGroupNamesInclude.setTextOrHide(user.groupNamesInclude.toJsonOrNull())
            bindingHolder.tvGroupNamesInclude.setTextOrHide(user.groupNamesExclude.toJsonOrNull())
            val userAttributes = user.userAttributes
            if (userAttributes != null) {
                bindingHolder.llUserAttributes.visibility = View.VISIBLE
                bindingHolder.tvPhone.setTextOrHide(userAttributes.phone)
                bindingHolder.tvEmail.setTextOrHide(userAttributes.email)
                bindingHolder.tvFirstName.setTextOrHide(userAttributes.firstName)
                bindingHolder.tvLastName.setTextOrHide(userAttributes.lastName)
                bindingHolder.tvLanguageCode.setTextOrHide(userAttributes.languageCode)
                bindingHolder.tvTimeZone.setTextOrHide(userAttributes.timeZone)
                val address = userAttributes.address
                if (address != null) {
                    bindingHolder.llAddress.visibility = View.VISIBLE
                    bindingHolder.tvRegion.setTextOrHide(address.region)
                    bindingHolder.tvTown.setTextOrHide(address.town)
                    bindingHolder.tvAddress.setTextOrHide(address.address)
                    bindingHolder.tvPostCode.setTextOrHide(address.postcode)
                } else {
                    bindingHolder.llAddress.visibility = View.GONE
                }
                val customFields = userAttributes.fields
                if (!customFields.isNullOrEmpty()) {
                    bindingHolder.llCustomFields.visibility = View.VISIBLE
                    for ((key, value) in customFields) {
                        val customFieldView = createNewFields(key, value)
                        bindingHolder.llCustomFields.addView(customFieldView)
                    }
                } else {
                    bindingHolder.llCustomFields.visibility = View.GONE
                }
            } else {
                bindingHolder.llUserAttributes.visibility = View.GONE
            }
        }

        private fun createNewFields(key: String, value: String?): View {
            val binding = ViewUserCustomFieldsVerticalBinding.inflate(
                LayoutInflater
                    .from(bindingHolder.getRoot().context), bindingHolder.llCustomFields, false
            )
            binding.etKey.setText(key)
            binding.etValue.setText(value)
            return binding.getRoot()
        }
    }
}
