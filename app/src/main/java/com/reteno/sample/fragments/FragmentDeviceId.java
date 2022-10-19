package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.RetenoImpl;
import com.reteno.core.data.local.config.DeviceIdMode;
import com.reteno.core.data.local.ds.ConfigRepository;
import com.reteno.core.di.ServiceLocator;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentDeviceIdBinding;
import com.reteno.sample.util.AppSharedPreferencesManager;

import java.lang.reflect.Field;

import kotlin.Unit;

public class FragmentDeviceId extends BaseFragment {

    private FragmentDeviceIdBinding binding;
    private ServiceLocator serviceLocator;
    private ConfigRepository configRepository;

    public FragmentDeviceId() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Field field = RetenoImpl.class.getDeclaredField("serviceLocator");
            field.setAccessible(true);
            serviceLocator = (ServiceLocator) field.get(getReteno());
            field.setAccessible(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        configRepository = serviceLocator.getConfigRepositoryProvider().get();
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

        setupSpinner(view);
        initExternalDeviceId(view);
        initListeners(view);

        refreshUi();
    }

    private void setupSpinner(@NonNull View view) {
        binding.spModesSelection.setAdapter(new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, DeviceIdMode.values()));
        binding.spModesSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DeviceIdMode mode = DeviceIdMode.values()[position];
                getReteno().setDeviceIdMode(mode, () -> {
                    refreshUi();
                    return Unit.INSTANCE;
                });
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
        binding.tvCurrentDeviceIdMode.setText(configRepository.getDeviceId().getMode$RetenoSdkCore_debug().toString());
        binding.tvCurrentDeviceId.setText(configRepository.getDeviceId().getId$RetenoSdkCore_debug());
        binding.spModesSelection.setSelection(configRepository.getDeviceId().getMode$RetenoSdkCore_debug().ordinal());
        binding.tvExternalId.setText(configRepository.getDeviceId().getExternalId$RetenoSdkCore_debug());
        binding.etFcmToken.setText(configRepository.getFcmToken());
    }
}