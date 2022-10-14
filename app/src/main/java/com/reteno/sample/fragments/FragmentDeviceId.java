package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.RetenoImpl;
import com.reteno.data.local.config.DeviceIdMode;
import com.reteno.data.local.ds.ConfigRepository;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentDeviceIdBinding;
import com.reteno.sample.util.AppSharedPreferencesManager;

import java.lang.reflect.Field;

public class FragmentDeviceId extends BaseFragment {

    private FragmentDeviceIdBinding binding;
    private ConfigRepository configRepository;

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

        setupSpinner(view);
        initExternalDeviceId(view);
        initListeners(view);

        refreshUi();
    }

    private void readDeviceId() {
        try {
            Field field = RetenoImpl.class.getDeclaredField("configRepository");
            field.setAccessible(true);
            configRepository = (ConfigRepository) field.get(getReteno());
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
                AppSharedPreferencesManager.saveDeviceIdMode(getContext(), mode);
                refreshUi();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                binding.tvCurrentDeviceIdMode.setText("NOTHING SELECTED!!! ERROR!");
            }
        });
    }

    private void initExternalDeviceId(@NonNull View view) {
        String externalSavedId = AppSharedPreferencesManager.getExternalId(view.getContext());
        getReteno().setExternalDeviceId(externalSavedId);
    }

    private void initListeners(@NonNull View view) {
        binding.rootView.setOnRefreshListener(() -> {
            refreshUi();
            binding.rootView.setRefreshing(false);
        });
        binding.tilExternalId.setStartIconOnClickListener(v -> {
            String externalId = binding.etExternalId.getText().toString();
            AppSharedPreferencesManager.saveExternalId(view.getContext(), externalId);
            getReteno().setExternalDeviceId(externalId);
            refreshUi();
        });
        binding.tilExternalId.setEndIconOnClickListener(v -> {
            AppSharedPreferencesManager.saveExternalId(view.getContext(), "");
            getReteno().setExternalDeviceId("");
            binding.etExternalId.setText("");
            refreshUi();
        });
    }

    private void refreshUi() {
        binding.tvCurrentDeviceIdMode.setText(configRepository.getDeviceIdMode().toString());
        binding.tvCurrentDeviceId.setText(configRepository.getDeviceId());
        binding.spModesSelection.setSelection(configRepository.getDeviceIdMode().ordinal());
        binding.tvExternalId.setText(configRepository.getExternalId());
        binding.etFcmToken.setText(configRepository.getFcmToken());
    }
}