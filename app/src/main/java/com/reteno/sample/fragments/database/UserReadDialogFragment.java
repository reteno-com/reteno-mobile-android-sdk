package com.reteno.sample.fragments.database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl;
import com.reteno.core.data.remote.mapper.JsonMappersKt;
import com.reteno.core.data.remote.model.user.AddressDTO;
import com.reteno.core.data.remote.model.user.UserAttributesDTO;
import com.reteno.core.data.remote.model.user.UserCustomFieldDTO;
import com.reteno.core.data.remote.model.user.UserDTO;
import com.reteno.sample.R;
import com.reteno.sample.databinding.DialogDbReadBinding;
import com.reteno.sample.databinding.ItemDbUserBinding;
import com.reteno.sample.databinding.ViewUserCustomFieldsVerticalBinding;

import java.util.List;
import java.util.Locale;

import kotlin.Pair;


public class UserReadDialogFragment extends DialogFragment {

    private DialogDbReadBinding bindingMain;
    private RetenoDatabaseManagerImpl databaseManager;
    private UserAdapter adapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        bindingMain = DialogDbReadBinding.inflate(getLayoutInflater());
        return new AlertDialog.Builder(requireActivity())
                .setView(bindingMain.getRoot())
                .create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        databaseManager = new RetenoDatabaseManagerImpl();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        initUi();
    }

    private void initUi() {
        initRecycler();
        initRemoveSection();
        initCount();
        initUserItems();
    }

    private void initRecycler() {
        adapter = new UserAdapter(() ->
                TransitionManager.beginDelayedTransition(bindingMain.rvList, new AutoTransition())
        );
        bindingMain.rvList.setAdapter(adapter);
    }

    private void initRemoveSection() {
        bindingMain.npRemoveEntries.setWrapSelectorWheel(false);
        bindingMain.npRemoveEntries.setMinValue(1);
        bindingMain.npRemoveEntries.setMaxValue(Integer.MAX_VALUE);
        bindingMain.btnRemoveEntries.setOnClickListener(v -> {
            int count = bindingMain.npRemoveEntries.getValue();
            databaseManager.deleteUserEvents(count, true);

            initCount();
            initUserItems();
        });
    }

    private void initCount() {
        long userEventsCount = databaseManager.getUserEventsCount();
        bindingMain.tvCount.setText(String.format(Locale.US, "Count: %d", userEventsCount));
    }

    private void initUserItems() {
        List<Pair<String, UserDTO>> newItems = databaseManager.getUserEvents(null);
        adapter.setItems(newItems);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bindingMain = null;
        databaseManager = null;
    }

    //==============================================================================================
    private static class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {

        private List<Pair<String, UserDTO>> items;
        private final ViewHolderListener onExpandCollapseClickListener;

        private UserAdapter(ViewHolderListener listener) {
            this.onExpandCollapseClickListener = listener;
        }

        private void setItems(List<Pair<String, UserDTO>> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemDbUserBinding binding = ItemDbUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new UserViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            Pair<String, UserDTO> item = items.get(position);
            initListeners(holder.bindingHolder);
            holder.bind(item.component1(), item.component2());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private void initListeners(ItemDbUserBinding binding) {
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

    private static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemDbUserBinding bindingHolder;

        private UserViewHolder(ItemDbUserBinding binding) {
            super(binding.getRoot());
            this.bindingHolder = binding;
        }

        private void bind(String timestamp, UserDTO user) {
            bindingHolder.tvValueTimestamp.setText(timestamp);

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
            binding.etCustomFieldKey.setText(key);
            binding.etCustomFieldValue.setText(value);
            return binding.getRoot();
        }
    }

    private interface ViewHolderListener {
        void onExpandCollapse();
    }
}
