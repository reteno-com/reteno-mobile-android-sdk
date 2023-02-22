package com.reteno.sample.fragments.database;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser;
import com.reteno.core.data.local.model.user.AddressDb;
import com.reteno.core.data.local.model.user.UserAttributesDb;
import com.reteno.core.data.local.model.user.UserCustomFieldDb;
import com.reteno.core.data.local.model.user.UserDb;
import com.reteno.core.data.remote.mapper.JsonMappersKt;
import com.reteno.sample.R;
import com.reteno.sample.databinding.ItemDbUserBinding;
import com.reteno.sample.databinding.ViewUserCustomFieldsVerticalBinding;

import java.util.List;
import java.util.Locale;


public class UserReadDialogFragment extends BaseReadDialogFragment<UserDb, ItemDbUserBinding, UserReadDialogFragment.UserViewHolder, UserReadDialogFragment.UserAdapter> {

    private RetenoDatabaseManagerUser databaseManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseManager = serviceLocator.getRetenoDatabaseManagerUserProvider().get();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseManager = null;
    }

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
        List<UserDb> newItems = databaseManager.getUsers(null);
        adapter.setItems(newItems);
    }

    @Override
    protected void deleteItems(int count) {
        List<UserDb> users = databaseManager.getUsers(count);
        for (UserDb user : users) {
            databaseManager.deleteUser(user);
        }
    }

    //==============================================================================================
    static class UserAdapter extends BaseReadAdapter<UserDb, ItemDbUserBinding, UserViewHolder> {

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
    static class UserViewHolder extends BaseReadViewHolder<UserDb, ItemDbUserBinding> {

        UserViewHolder(ItemDbUserBinding binding) {
            super(binding);
        }

        @Override
        protected void bind(UserDb user) {
            bindingHolder.tvDeviceId.setTextOrHide(user.getDeviceId());
            bindingHolder.tvExternalUserId.setTextOrHide(user.getExternalUserId());
            bindingHolder.tvSubscriptionKeys.setTextOrHide(JsonMappersKt.toJsonOrNull(user.getSubscriptionKeys()));
            bindingHolder.tvGroupNamesInclude.setTextOrHide(JsonMappersKt.toJsonOrNull(user.getGroupNamesInclude()));
            bindingHolder.tvGroupNamesInclude.setTextOrHide(JsonMappersKt.toJsonOrNull(user.getGroupNamesExclude()));

            UserAttributesDb userAttributes = user.getUserAttributes();
            if (userAttributes != null) {
                bindingHolder.llUserAttributes.setVisibility(View.VISIBLE);
                bindingHolder.tvPhone.setTextOrHide(userAttributes.getPhone());
                bindingHolder.tvEmail.setTextOrHide(userAttributes.getEmail());
                bindingHolder.tvFirstName.setTextOrHide(userAttributes.getFirstName());
                bindingHolder.tvLastName.setTextOrHide(userAttributes.getLastName());
                bindingHolder.tvLanguageCode.setTextOrHide(userAttributes.getLanguageCode());
                bindingHolder.tvTimeZone.setTextOrHide(userAttributes.getTimeZone());

                AddressDb address = userAttributes.getAddress();
                if (address != null) {
                    bindingHolder.llAddress.setVisibility(View.VISIBLE);
                    bindingHolder.tvRegion.setTextOrHide(address.getRegion());
                    bindingHolder.tvTown.setTextOrHide(address.getTown());
                    bindingHolder.tvAddress.setTextOrHide(address.getAddress());
                    bindingHolder.tvPostCode.setTextOrHide(address.getPostcode());
                } else {
                    bindingHolder.llAddress.setVisibility(View.GONE);
                }

                List<UserCustomFieldDb> customFields = userAttributes.getFields();
                if (customFields != null && !customFields.isEmpty()) {
                    bindingHolder.llCustomFields.setVisibility(View.VISIBLE);
                    for (UserCustomFieldDb customField : customFields) {
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
