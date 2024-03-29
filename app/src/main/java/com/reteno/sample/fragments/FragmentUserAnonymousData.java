package com.reteno.sample.fragments;

import android.os.Bundle;
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
import com.reteno.core.domain.model.user.UserAttributesAnonymous;
import com.reteno.core.domain.model.user.UserCustomField;
import com.reteno.core.util.Logger;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.R;
import com.reteno.sample.databinding.FragmentUserAnonymousDataBinding;
import com.reteno.sample.util.Util;

import java.util.ArrayList;
import java.util.List;

public class FragmentUserAnonymousData extends BaseFragment {

    private static final String TAG = FragmentUserAnonymousData.class.getSimpleName();
    protected FragmentUserAnonymousDataBinding binding;

    public FragmentUserAnonymousData() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserAnonymousDataBinding.inflate(inflater, container, false);
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
                Logger.e(TAG, "FragmentUserData", e);
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
        List<UserCustomField> userCustomData = getUserCustomData();

        Address address = new Address(
                Util.getTextOrNull(binding.etRegion),
                Util.getTextOrNull(binding.etTown),
                Util.getTextOrNull(binding.etAddress),
                Util.getTextOrNull(binding.etPostcode)
        );
        UserAttributesAnonymous userAttributes = new UserAttributesAnonymous(
                Util.getTextOrNull(binding.etFirstName),
                Util.getTextOrNull(binding.etLastName),
                Util.getTextOrNull(binding.etLanguageCode),
                Util.getTextOrNull(binding.etTimeZone),
                address,
                userCustomData
        );

        sendAnonymousUserAttributes(userAttributes);
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

    protected void sendAnonymousUserAttributes(UserAttributesAnonymous attributes) {
        getReteno().setAnonymousUserAttributes(attributes);
    }

    private View createNewFields() {
        return LayoutInflater
                .from(binding.getRoot().getContext())
                .inflate(R.layout.view_user_custom_fields_horizontal, binding.llCustomData, false);
    }

}