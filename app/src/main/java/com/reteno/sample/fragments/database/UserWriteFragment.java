package com.reteno.sample.fragments.database;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reteno.core.Reteno;
import com.reteno.core.RetenoImpl;
import com.reteno.core._interop.DeviceIdInternal;
import com.reteno.core.data.local.config.DeviceId;
import com.reteno.core.data.local.database.RetenoDatabaseManagerImpl;
import com.reteno.core.data.remote.mapper.UserMappersKt;
import com.reteno.core.data.remote.model.user.UserDTO;
import com.reteno.core.model.user.User;
import com.reteno.sample.SampleApp;
import com.reteno.sample.fragments.FragmentUserData;

public class UserWriteFragment extends FragmentUserData {

    private RetenoDatabaseManagerImpl databaseManager;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        databaseManager = new RetenoDatabaseManagerImpl();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        databaseManager = null;
    }

    @Override
    protected void sendUserData(String externalId, User user) {
        DeviceId deviceId = getDeviceId();
        deviceId = deviceId.copy(DeviceIdInternal.INSTANCE.getIdInternal(deviceId), externalId, DeviceIdInternal.INSTANCE.getModeInternal(deviceId));

        UserDTO userDTO = UserMappersKt.toRemote(user, deviceId);
        databaseManager.insertUser(userDTO);
    }

    private DeviceId getDeviceId() {
        Reteno reteno = ((SampleApp) requireActivity().getApplication()).getRetenoInstance();
        RetenoImpl retenoImpl = ((RetenoImpl) reteno);
        DeviceId deviceId = retenoImpl.getServiceLocator().getConfigRepositoryProvider().get().getDeviceId();
        return deviceId;
    }
}
