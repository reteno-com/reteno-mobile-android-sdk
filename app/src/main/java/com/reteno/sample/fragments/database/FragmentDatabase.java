package com.reteno.sample.fragments.database;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

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
        binding.btnWriteUser.setOnClickListener(v -> showDialogWriteUser());
        binding.btnReadUser.setOnClickListener(v -> showDialogReadUser());
        binding.btnWriteInteraction.setOnClickListener(v -> showDialogWriteInteraction());
        binding.btnReadInteraction.setOnClickListener(v -> showDialogReadInteraction());
        binding.btnWriteEvent.setOnClickListener(v -> showDialogWriteEvent());
        binding.btnReadEvent.setOnClickListener(v -> showDialogReadEvent());
    }

    private void showDialogWriteDevice() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToWriteDevice());
    }

    private void showDialogReadDevice() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToReadDevice());
    }

    private void showDialogWriteUser() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToWriteUser());
    }

    private void showDialogReadUser() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToReadUser());
    }

    private void showDialogWriteInteraction() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToWriteInteraction());
    }

    private void showDialogReadInteraction() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToReadInteraction());
    }

    private void showDialogWriteEvent() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToWriteEvent());
    }

    private void showDialogReadEvent() {
        NavHostFragment.findNavController(this).navigate(FragmentDatabaseDirections.databaseToReadEvent());
    }
}