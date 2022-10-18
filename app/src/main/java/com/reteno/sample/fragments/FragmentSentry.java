package com.reteno.sample.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.Reteno;
import com.reteno.sample.BaseFragment;
import com.reteno.sample.databinding.FragmentSentryBinding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FragmentSentry extends BaseFragment {

    private FragmentSentryBinding binding;

    public FragmentSentry() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSentryBinding.inflate(getLayoutInflater(), container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initListeners();
    }

    private void initListeners() {
        binding.btnCrashApp.setOnClickListener(v -> {
            throw new IllegalArgumentException("This is a test crash from app scope");
        });
        binding.btnCrashSdk.setOnClickListener(v -> {
            try {
                Method method = Reteno.class.getDeclaredMethod("testCrash");
                method.setAccessible(true);
                method.invoke(getReteno());
                method.setAccessible(false);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
    }

}
