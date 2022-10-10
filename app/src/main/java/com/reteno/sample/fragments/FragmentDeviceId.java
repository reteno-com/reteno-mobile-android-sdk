package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.Reteno;
import com.reteno.config.DeviceId;
import com.reteno.config.DeviceIdMode;
import com.reteno.config.RestConfig;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentDeviceIdBinding;
import com.reteno.sample.util.SharedPreferencesManager;
import com.reteno.util.SharedPrefsManager;

import java.lang.reflect.Field;

public class FragmentDeviceId extends BaseFragment {

    private FragmentDeviceIdBinding binding;
    private DeviceId deviceId;
    private SharedPrefsManager sharedPrefsManager;

    public FragmentDeviceId() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceIdBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readDeviceId();
        readSharedPrefsManager();

        setupSpinner(view);
        initExternalDeviceId(view);
        initListeners(view);

        refreshUi();
    }

    private void readDeviceId() {
        try {
            Field field = Reteno.class.getDeclaredField("restConfig");
            field.setAccessible(true);
            RestConfig restConfig = (RestConfig) field.get(getReteno());
            deviceId = restConfig.getDeviceId();
            field.setAccessible(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void readSharedPrefsManager() {
        try {
            Field field = Reteno.class.getDeclaredField("sharedPrefsManager");
            field.setAccessible(true);
            sharedPrefsManager = (SharedPrefsManager) field.get(getReteno());
            field.setAccessible(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private void setupSpinner(@NonNull View view) {
        binding.spModesSelection.setAdapter(new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, DeviceIdMode.values()));
        binding.spModesSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DeviceIdMode mode = DeviceIdMode.values()[position];
                getReteno().changeDeviceIdMode(mode);
                SharedPreferencesManager.saveDeviceIdMode(view.getContext(), mode);
                refreshUi();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                binding.tvCurrentDeviceIdMode.setText("NOTHING SELECTED!!! ERROR!");
            }
        });
    }

    private void initExternalDeviceId(@NonNull View view) {
        String externalSavedId = SharedPreferencesManager.getExternalId(view.getContext());
        getReteno().setExternalDeviceId(externalSavedId);
    }

    private void initListeners(@NonNull View view) {
        binding.rootView.setOnRefreshListener(() -> {
            refreshUi();
            binding.rootView.setRefreshing(false);
        });
        binding.tilExternalId.setStartIconOnClickListener(v -> {
            String externalId = binding.etExternalId.getText().toString();
            SharedPreferencesManager.saveExternalId(view.getContext(), externalId);
            getReteno().setExternalDeviceId(externalId);
            refreshUi();
        });
        binding.tilExternalId.setEndIconOnClickListener(v -> {
            SharedPreferencesManager.saveExternalId(view.getContext(), "");
            getReteno().setExternalDeviceId("");
            binding.etExternalId.setText("");
            refreshUi();
        });
    }

    private void refreshUi() {
        binding.tvCurrentDeviceIdMode.setText(deviceId.getMode().toString());
        binding.tvCurrentDeviceId.setText(deviceId.getId());
        binding.spModesSelection.setSelection(deviceId.getMode().ordinal());
        binding.tvExternalId.setText(deviceId.getExternalId());
        binding.etFcmToken.setText(sharedPrefsManager.getFcmToken());
    }
}