package com.reteno.sample.fragments.database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoImpl;
import com.reteno.core._interop.DeviceIdInternal;
import com.reteno.core.data.local.config.DeviceId;
import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl;
import com.reteno.core.model.Event;
import com.reteno.core.model.Events;
import com.reteno.core.model.Parameter;
import com.reteno.sample.R;
import com.reteno.sample.SampleApp;
import com.reteno.sample.databinding.DialogDbWriteEventBinding;
import com.reteno.sample.databinding.ViewEventWriteBinding;
import com.reteno.sample.databinding.ViewUserCustomFieldsHorizontalBinding;
import com.reteno.sample.util.Util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventWriteDialogFragment extends DialogFragment {

    private DialogDbWriteEventBinding binding;
    private RetenoDatabaseManagerImpl databaseManager;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogDbWriteEventBinding.inflate(getLayoutInflater());
        return new AlertDialog.Builder(requireActivity())
                .setView(binding.getRoot())
                .create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        databaseManager = new RetenoDatabaseManagerImpl();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        databaseManager = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        initListeners();
    }

    private void initListeners() {
        binding.btnSubmit.setOnClickListener(v -> {
            Events events = getEventData();
            databaseManager.insertEvents(events);
            Toast.makeText(this.getContext(), "Sent", Toast.LENGTH_SHORT).show();
        });

        binding.btnEventPlus.setOnClickListener(v -> {
            View view = createEvent();
            binding.llEvents.addView(view);
        });

        binding.btnEventMinus.setOnClickListener(v -> {
            int countView = binding.llEvents.getChildCount();
            if (countView > 0) {
                binding.llEvents.removeViewAt(countView - 1);
            }
        });
    }

    private Events getEventData() {
        Reteno reteno = ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
        RetenoImpl retenoImpl = ((RetenoImpl) reteno);
        DeviceId deviceIdModel = retenoImpl.getServiceLocator().getConfigRepositoryProvider().get().getDeviceId();
        String deviceId = DeviceIdInternal.INSTANCE.getIdInternal(deviceIdModel);
        String externalUserId = Util.getTextOrNull(binding.etExternalUserId);

        List<Event> eventList = getEventList();

        return new Events(deviceId, externalUserId, eventList);
    }

    private List<Event> getEventList() {
        List<Event> list = new ArrayList<>();
        int countView = binding.llEvents.getChildCount();
        if (countView == 0) return list;

        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) binding.llEvents.getChildAt(i);

            EditText etEventTypeKey = (EditText) parent.getChildAt(0);

            String eventTypeKey = Util.getTextOrNull(etEventTypeKey);
            LocalDateTime occurred = LocalDateTime.now();

            if (eventTypeKey != null) {
                List<Parameter> params = getParamsList(parent);
                list.add(new Event(eventTypeKey, occurred, params));
            }
        }
        return list;
    }

    private List<Parameter> getParamsList(ViewGroup container) {
        List<Parameter> list = new ArrayList<>();
        LinearLayout paramsView = container.findViewById(R.id.llParams);

        int countView = paramsView.getChildCount();
        if (countView == 0) return list;

        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) paramsView.getChildAt(i);

            EditText etKey = (EditText) parent.getChildAt(0);
            EditText etValue = (EditText) parent.getChildAt(1);

            String key = Util.getTextOrNull(etKey);
            String value = Util.getTextOrNull(etValue);

            if (key != null && value != null) {
                list.add(new Parameter(key, value));
            }
        }
        return list;
    }

    private View createEvent() {
        ViewEventWriteBinding viewEventBinding = ViewEventWriteBinding.inflate(getLayoutInflater(), binding.llEvents, false);

        viewEventBinding.btnParamPlus.setOnClickListener(v -> {
            View view = createParam(viewEventBinding);
            viewEventBinding.llParams.addView(view);
        });
        viewEventBinding.btnParamMinus.setOnClickListener(v -> {
            int countView = viewEventBinding.llParams.getChildCount();
            if (countView > 0) {
                viewEventBinding.llParams.removeViewAt(countView - 1);
            }
        });

        return viewEventBinding.getRoot();
    }

    private View createParam(ViewEventWriteBinding viewEventBinding) {
        ViewUserCustomFieldsHorizontalBinding viewParamsBinding = ViewUserCustomFieldsHorizontalBinding.inflate(getLayoutInflater(), viewEventBinding.llParams, false);
        return viewParamsBinding.getRoot();
    }
}
