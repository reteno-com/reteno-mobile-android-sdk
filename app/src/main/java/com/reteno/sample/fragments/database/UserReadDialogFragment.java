package com.reteno.sample.fragments.database;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.data.remote.mapper.JsonMappersKt;
import com.reteno.core.data.remote.model.user.AddressDTO;
import com.reteno.core.data.remote.model.user.UserAttributesDTO;
import com.reteno.core.data.remote.model.user.UserCustomFieldDTO;
import com.reteno.core.data.remote.model.user.UserDTO;
import com.reteno.sample.R;
import com.reteno.sample.databinding.ItemDbUserBinding;
import com.reteno.sample.databinding.ViewUserCustomFieldsVerticalBinding;

import java.util.List;
import java.util.Locale;


public class UserReadDialogFragment extends BaseReadDialogFragment<UserDTO, ItemDbUserBinding, UserReadDialogFragment.UserViewHolder, UserReadDialogFragment.UserAdapter> {

    @Override
    protected void initAdapter() {
        adapter = new UserAdapter(() ->
                TransitionManager.beginDelayedTransition(bindingMain.rvList, new AutoTransition())
        );
    }

    @Override
    protected void initCount() {
        long userEventsCount = databaseManager.getUserCount();
        bindingMain.tvCount.setText(String.format(Locale.US, "Count: %d", userEventsCount));
    }

    @Override
    protected void initItems() {
        List<UserDTO> newItems = databaseManager.getUser(null);
        adapter.setItems(newItems);
    }

    @Override
    protected void deleteItems(int count) {
        databaseManager.deleteUsers(count, true);
    }

    //==============================================================================================
    static class UserAdapter extends BaseReadAdapter<UserDTO, ItemDbUserBinding, UserViewHolder> {

        UserAdapter(ViewHolderListener listener) {
            super(listener);
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDbUserBinding binding = ItemDbUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new UserViewHolder(binding);
        }

        @Override
        protected void initListeners(ItemDbUserBinding binding) {
            binding.ivExpand.setOnClickListener(v -> {
                if (binding.llContent.getVisibility() == View.VISIBLE) {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.GONE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_more);
                } else {
                    onExpandCollapseClickListener.onExpandCollapse();
                    binding.llContent.setVisibility(View.VISIBLE);
                    binding.ivExpand.setImageResource(R.drawable.ic_expand_less);
                }
            });
        }
    }

    //==============================================================================================
    static class UserViewHolder extends BaseReadViewHolder<UserDTO, ItemDbUserBinding> {

        UserViewHolder(ItemDbUserBinding binding) {
            super(binding);
        }

        @Override
        protected void bind(UserDTO user) {
            bindingHolder.tvDeviceId.setTextOrHide(user.getDeviceId());
            bindingHolder.tvExternalUserId.setTextOrHide(user.getExternalUserId());
            bindingHolder.tvSubscriptionKeys.setTextOrHide(JsonMappersKt.toJsonOrNull(user.getSubscriptionKeys()));
            bindingHolder.tvGroupNamesInclude.setTextOrHide(JsonMappersKt.toJsonOrNull(user.getGroupNamesInclude()));
            bindingHolder.tvGroupNamesInclude.setTextOrHide(JsonMappersKt.toJsonOrNull(user.getGroupNamesExclude()));

            UserAttributesDTO userAttributes = user.getUserAttributes();
            if (userAttributes != null) {
                bindingHolder.llUserAttributes.setVisibility(View.VISIBLE);
                bindingHolder.tvPhone.setTextOrHide(userAttributes.getPhone());
                bindingHolder.tvEmail.setTextOrHide(userAttributes.getEmail());
                bindingHolder.tvFirstName.setTextOrHide(userAttributes.getFirstName());
                bindingHolder.tvLastName.setTextOrHide(userAttributes.getLastName());
                bindingHolder.tvLanguageCode.setTextOrHide(userAttributes.getLanguageCode());
                bindingHolder.tvTimeZone.setTextOrHide(userAttributes.getTimeZone());

                AddressDTO address = userAttributes.getAddress();
                if (address != null) {
                    bindingHolder.llAddress.setVisibility(View.VISIBLE);
                    bindingHolder.tvRegion.setTextOrHide(address.getRegion());
                    bindingHolder.tvTown.setTextOrHide(address.getTown());
                    bindingHolder.tvAddress.setTextOrHide(address.getAddress());
                    bindingHolder.tvPostCode.setTextOrHide(address.getPostcode());
                } else {
                    bindingHolder.llAddress.setVisibility(View.GONE);
                }

                List<UserCustomFieldDTO> customFields = userAttributes.getFields();
                if (customFields != null && !customFields.isEmpty()) {
                    bindingHolder.llCustomFields.setVisibility(View.VISIBLE);
                    for (UserCustomFieldDTO customField : customFields) {
                        View customFieldView = createNewFields(customField.getKey(), customField.getValue());
                        bindingHolder.llCustomFields.addView(customFieldView);
                    }
                } else {
                    bindingHolder.llCustomFields.setVisibility(View.GONE);
                }
            } else {
                bindingHolder.llUserAttributes.setVisibility(View.GONE);
            }
        }

        private View createNewFields(String key, String value) {
            ViewUserCustomFieldsVerticalBinding binding = ViewUserCustomFieldsVerticalBinding.inflate(LayoutInflater
                    .from(bindingHolder.getRoot().getContext()), bindingHolder.llCustomFields, false);
            binding.etKey.setText(key);
            binding.etValue.setText(value);
            return binding.getRoot();
        }
    }
}
