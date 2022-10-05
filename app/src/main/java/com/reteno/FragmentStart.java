package com.reteno;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.reteno.sample.databinding.FragmentStartBinding;

public class FragmentStart extends BaseFragment {

    private FragmentStartBinding binding;

    public FragmentStart() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnDeviceId.setOnClickListener(v -> {
            NavDirections direction = FragmentStartDirections.startToDeviceId();
            NavHostFragment.findNavController(this).navigate(direction);
        });
    }
}