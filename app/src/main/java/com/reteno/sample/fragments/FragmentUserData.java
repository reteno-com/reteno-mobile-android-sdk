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

import com.reteno.core.model.user.Address;
import com.reteno.core.model.user.User;
import com.reteno.core.model.user.UserAttributes;
import com.reteno.core.model.user.UserCustomField;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentUserDataBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FragmentUserData extends BaseFragment {

    private FragmentUserDataBinding binding;

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
        String externalId = getTextOrNull(binding.etExternalId);

        List<UserCustomField> userCustomData = getUserCustomData();

        Address address = new Address(
                getTextOrNull(binding.etRegion),
                getTextOrNull(binding.etTown),
                getTextOrNull(binding.etAddress),
                getTextOrNull(binding.etPostcode)
        );
        UserAttributes userAttributes = new UserAttributes(
                getTextOrNull(binding.etPhone),
                getTextOrNull(binding.etEmail),
                getTextOrNull(binding.etFirstName),
                getTextOrNull(binding.etLastName),
                getTextOrNull(binding.etLanguageCode),
                getTextOrNull(binding.etTimeZone),
                address,
                userCustomData
        );

        List<String> subscriptionKeys = getListFromEditText(binding.etSubscriptionKeys);
        List<String> groupNamesInclude = getListFromEditText(binding.etGroupNamesInclude);
        List<String> groupNamesExclude = getListFromEditText(binding.etGroupNamesExclude);

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

            String key = getTextOrNull(etKey);
            String value = getTextOrNull(etValue);

            if (key != null) {
                list.add(new UserCustomField(key, value));
            }
        }
        return list;
    }

    private void sendUserData(String externalId, User user) {
        getReteno().setUserAttributes(externalId, user);
    }

    private String getTextOrNull(EditText editText) {
        String rawText = editText.getText().toString().trim();
        if (rawText.isEmpty()) return null;
        return rawText;
    }

    private List<String> getListFromEditText(EditText editText) {
        String rawText = editText.getText().toString().trim();
        if (rawText.isEmpty()) return null;
        return Arrays.asList(rawText.split(","));
    }

    private View createNewFields() {
        return LayoutInflater
                .from(binding.getRoot().getContext())
                .inflate(R.layout.view_user_custom_fields, binding.llCustomData, false);
    }

}