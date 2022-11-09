package com.reteno.sample.fragments.database;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoImpl;
import com.reteno.core.data.local.database.RetenoDatabaseManager;
import com.reteno.core.di.ServiceLocator;
import com.reteno.sample.SampleApp;

import java.lang.reflect.Field;

public class BaseDatabaseDialogFragment extends DialogFragment {

    private ServiceLocator serviceLocator;
    protected RetenoDatabaseManager databaseManager;

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        databaseManager = serviceLocator.getDatabaseManagerProvider().get();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        databaseManager = serviceLocator.getDatabaseManagerProvider().get();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        databaseManager = null;
    }

    private Reteno getReteno() {
        return ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
    }
}
