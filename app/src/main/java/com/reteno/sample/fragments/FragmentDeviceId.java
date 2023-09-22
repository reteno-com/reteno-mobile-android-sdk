package com.reteno.sample.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.RetenoImpl;
import com.reteno.core._interop.DeviceIdInternal;
import com.reteno.core.data.local.config.DeviceIdMode;
import com.reteno.core.data.repository.ConfigRepository;
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

        initExternalDeviceId(view);
        refreshUi();

        initListeners(view);
    }

    private void initExternalDeviceId(@NonNull View view) {
        String externalSavedId = AppSharedPreferencesManager.getExternalId(view.getContext());
        if (!TextUtils.isEmpty(externalSavedId)) {
            getReteno().setUserAttributes(externalSavedId);
        }
    }

    private void refreshUi() {
        String id = DeviceIdInternal.INSTANCE.getIdInternal(configRepository.getDeviceId());
        String externalId = DeviceIdInternal.INSTANCE.getExternalIdInternal(configRepository.getDeviceId());
        DeviceIdMode mode = DeviceIdInternal.INSTANCE.getModeInternal(configRepository.getDeviceId());
        binding.tvCurrentDeviceIdMode.setText(mode.toString());
        binding.tvCurrentDeviceId.setText(id);
        binding.tvExternalId.setText(externalId);
        configRepository.getFcmToken(str -> {
                    binding.etFcmToken.setText(str);
                    return Unit.INSTANCE;
                }
        );
    }

    private void initListeners(@NonNull View view) {
        binding.rootView.setOnRefreshListener(() -> {
            refreshUi();
            binding.rootView.setRefreshing(false);
        });
        binding.tilExternalId.setStartIconOnClickListener(v -> {
            String externalId = binding.etExternalId.getText().toString();
            AppSharedPreferencesManager.saveExternalId(view.getContext(), externalId);
            getReteno().setUserAttributes(externalId);
            refreshUi();
        });
        binding.tilExternalId.setEndIconOnClickListener(v -> {
            AppSharedPreferencesManager.saveExternalId(view.getContext(), "");
            getReteno().setUserAttributes("");
            binding.etExternalId.setText("");
            refreshUi();
        });
    }
}