package com.reteno.sample.fragments.database;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoImpl;
import com.reteno.core._interop.DeviceIdInternal;
import com.reteno.core.data.local.config.DeviceId;
import com.reteno.core.data.local.database.manager.RetenoDatabaseManagerUser;
import com.reteno.core.data.local.mappers.UserMappersKt;
import com.reteno.core.data.local.model.user.UserDb;
import com.reteno.core.domain.model.user.Address;
import com.reteno.core.domain.model.user.User;
import com.reteno.core.domain.model.user.UserAttributes;
import com.reteno.core.domain.model.user.UserCustomField;
import com.reteno.core.util.UtilKt;
import com.reteno.sample.R;
import com.reteno.sample.SampleApp;
import com.reteno.sample.databinding.FragmentUserDataBinding;
import com.reteno.sample.util.Util;

import java.util.ArrayList;
import java.util.List;

public class UserWriteFragment extends BaseDatabaseDialogFragment {

    private FragmentUserDataBinding binding;
    private RetenoDatabaseManagerUser databaseManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseManager = serviceLocator.getRetenoDatabaseManagerUserProvider().get();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseManager = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListeners();
    }

    private void setListeners() {
        binding.btnSend.setOnClickListener(v -> {
            getUserData();
            Toast.makeText(this.getContext(), "Sent", Toast.LENGTH_SHORT).show();
        });

        binding.btnCustomFieldsAdd.setOnClickListener(v -> {
            View view = createNewFields();
            binding.llCustomData.addView(view);
        });

        binding.btnCustomFieldsMinus.setOnClickListener(v -> {
            int countView = binding.llCustomData.getChildCount();
            if (countView > 0) {
                binding.llCustomData.removeViewAt(countView - 1);
            }
        });
    }

    private void getUserData() {
        String externalId = Util.getTextOrNull(binding.etExternalId);

        List<UserCustomField> userCustomData = getUserCustomData();

        Address address;
        if (UtilKt.allElementsNull(Util.getTextOrNull(binding.etRegion),
                Util.getTextOrNull(binding.etTown),
                Util.getTextOrNull(binding.etAddress),
                Util.getTextOrNull(binding.etPostcode))) {
            address = null;
        } else {
            address = new Address(
                    Util.getTextOrNull(binding.etRegion),
                    Util.getTextOrNull(binding.etTown),
                    Util.getTextOrNull(binding.etAddress),
                    Util.getTextOrNull(binding.etPostcode));
        }

        UserAttributes userAttributes;
        if (UtilKt.allElementsNull(
                Util.getTextOrNull(binding.etPhone),
                Util.getTextOrNull(binding.etEmail),
                Util.getTextOrNull(binding.etFirstName),
                Util.getTextOrNull(binding.etLastName),
                Util.getTextOrNull(binding.etLanguageCode),
                Util.getTextOrNull(binding.etTimeZone),
                address,
                userCustomData)) {
            userAttributes = null;
        } else {
            userAttributes = new UserAttributes(
                    Util.getTextOrNull(binding.etPhone),
                    Util.getTextOrNull(binding.etEmail),
                    Util.getTextOrNull(binding.etFirstName),
                    Util.getTextOrNull(binding.etLastName),
                    Util.getTextOrNull(binding.etLanguageCode),
                    Util.getTextOrNull(binding.etTimeZone),
                    address,
                    userCustomData
            );
        }

        List<String> subscriptionKeys = Util.getListFromEditText(binding.etSubscriptionKeys);
        List<String> groupNamesInclude = Util.getListFromEditText(binding.etGroupNamesInclude);
        List<String> groupNamesExclude = Util.getListFromEditText(binding.etGroupNamesExclude);

        User user = new User(userAttributes, subscriptionKeys, groupNamesInclude, groupNamesExclude);
        sendUserData(externalId, user);
    }

    private List<UserCustomField> getUserCustomData() {
        int countView = binding.llCustomData.getChildCount();
        if (countView == 0) return null;

        List<UserCustomField> list = new ArrayList<>();
        for (int i = 0; i < countView; i++) {
            LinearLayout parent = (LinearLayout) binding.llCustomData.getChildAt(i);

            EditText etKey = (EditText) parent.getChildAt(0);
            EditText etValue = (EditText) parent.getChildAt(1);

            String key = Util.getTextOrNull(etKey);
            String value = Util.getTextOrNull(etValue);

            if (key != null) {
                list.add(new UserCustomField(key, value));
            }
        }
        return list;
    }

    private View createNewFields() {
        return LayoutInflater
                .from(binding.getRoot().getContext())
                .inflate(R.layout.view_user_custom_fields_horizontal, binding.llCustomData, false);
    }

    private void sendUserData(String externalId, User user) {
        DeviceId deviceId = getDeviceId();
        deviceId = deviceId.copy(DeviceIdInternal.INSTANCE.getIdInternal(deviceId), externalId, DeviceIdInternal.INSTANCE.getModeInternal(deviceId));

        UserDb userDb = UserMappersKt.toDb(user, deviceId);
        databaseManager.insertUser(userDb);
    }

    private DeviceId getDeviceId() {
        Reteno reteno = ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
        RetenoImpl retenoImpl = ((RetenoImpl) reteno);
        DeviceId deviceId = retenoImpl.getServiceLocator().getConfigRepositoryProvider().get().getDeviceId();
        return deviceId;
    }
}
