package com.reteno.sample.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.reteno.sample.databinding.FragmentDatabaseBinding;


public class FragmentDatabase extends Fragment {


    private FragmentDatabaseBinding binding;

    public FragmentDatabase() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDatabaseBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
    }

    private void initListeners() {
        binding.btnWriteDevice.setOnClickListener(v -> showDialogWriteDevice());
        binding.btnReadDevice.setOnClickListener(v -> showDialogReadDevice());
    }

    private void showDialogWriteDevice() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToWriteDevice());
    }

    private void showDialogReadDevice() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToReadDevice());
    }
}