package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.lifecycle.ScreenTrackingConfig;
import com.reteno.core.lifecycle.ScreenTrackingTrigger;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentScreenTrackingBinding;

import java.util.Arrays;
import java.util.List;

public class FragmentScreenTracking extends BaseFragment {

    private FragmentScreenTrackingBinding binding;

    public FragmentScreenTracking() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScreenTrackingBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
        binding.spnTrigger.setAdapter(new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_item, ScreenTrackingTrigger.values()));
        binding.spnTrigger.setSelection(0);
    }

    private void initListeners() {
        binding.btnTrackScreen.setOnClickListener(v ->
                getReteno().logScreenView(binding.etScreenName.getText().toString())
        );

        binding.btnUpdateAutoScreenTrackingConfig.setOnClickListener(v -> {
            List<String> excludeScreens = Arrays.asList(binding.etExcludeScreens.getText().toString().split("\\s*,\\s*"));
            ScreenTrackingTrigger trigger = ScreenTrackingTrigger.values()[binding.spnTrigger.getSelectedItemPosition()];
            ScreenTrackingConfig config = new ScreenTrackingConfig(binding.cbEnableScreenTracking.isChecked(), excludeScreens, trigger);
            getReteno().autoScreenTracking(config);
        });
    }
}
