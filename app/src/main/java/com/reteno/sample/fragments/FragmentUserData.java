package com.reteno.sample.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;

import com.reteno.core.domain.model.user.Address;
import com.reteno.core.domain.model.user.User;
import com.reteno.core.domain.model.user.UserAttributes;
import com.reteno.core.domain.model.user.UserCustomField;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentUserDataBinding;
import com.reteno.sample.util.Util;

import java.util.ArrayList;
import java.util.List;

public class FragmentUserData extends BaseFragment {

    protected FragmentUserDataBinding binding;

    public FragmentUserData() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setListeners();
    }

    private void setListeners() {
        binding.btnSend.setOnClickListener(v -> {
            try {
                getUserData();
                Toast.makeText(this.getContext(), "Sent", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } catch (Exception e) {
                Toast.makeText(this.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("FragmentUserData", e.getMessage());
            }
        });

        binding.btnCustomFieldsAdd.setOnClickListener(v -> {
            View view = createNewFields();
            binding.llCustomData.addView(view);
        });

        binding.btnCustomFieldsMinus.setOnClickListener(v -> {
            int countView = binding.llCustomData.getChildCount();
            if (countView > 0) {
                binding.llCustomData.removeViewAt(countView - 1);
            }
        });
    }

    private void getUserData() {
        String externalId = binding.etExternalId.getText().toString();
        if (externalId.contains("null")) {
            externalId = null;
        }

        List<UserCustomField> userCustomData = getUserCustomData();

        Address address = new Address(
                Util.getTextOrNull(binding.etRegion),
                Util.getTextOrNull(binding.etTown),
                Util.getTextOrNull(binding.etAddress),
                Util.getTextOrNull(binding.etPostcode)
        );
        UserAttributes userAttributes = new UserAttributes(
                Util.getTextOrNull(binding.etPhone),
                Util.getTextOrNull(binding.etEmail),
                Util.getTextOrNull(binding.etFirstName),
                Util.getTextOrNull(binding.etLastName),
                Util.getTextOrNull(binding.etLanguageCode),
                Util.getTextOrNull(binding.etTimeZone),
                address,
                userCustomData
        );

        List<String> subscriptionKeys = Util.getListFromEditText(binding.etSubscriptionKeys);
        List<String> groupNamesInclude = Util.getListFromEditText(binding.etGroupNamesInclude);
        List<String> groupNamesExclude = Util.getListFromEditText(binding.etGroupNamesExclude);

        User user = new User(userAttributes, subscriptionKeys, groupNamesInclude, groupNamesExclude);
        sendUserData(externalId, user);
    }

    private List<UserCustomField> getUserCustomData() {
        int countView = binding.llCustomData.getChildCount();
        if (countView == 0) return null;

        List<UserCustomField> list = new ArrayList<>();
        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) binding.llCustomData.getChildAt(i);

            EditText etKey = (EditText) parent.getChildAt(0);
            EditText etValue = (EditText) parent.getChildAt(1);

            String key = Util.getTextOrNull(etKey);
            String value = Util.getTextOrNull(etValue);

            if (key != null) {
                list.add(new UserCustomField(key, value));
            }
        }
        return list;
    }

    protected void sendUserData(String externalId, User user) {
        getReteno().setUserAttributes(externalId, user);
    }

    private View createNewFields() {
        return LayoutInflater
                .from(binding.getRoot().getContext())
                .inflate(R.layout.view_user_custom_fields_horizontal, binding.llCustomData, false);
    }

}