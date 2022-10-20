package com.reteno.robolectric.core.data.local.config;

import com.reteno.core.data.local.config.DeviceId;
import com.reteno.core.data.local.config.DeviceIdHelper;
import com.reteno.core.data.local.config.DeviceIdMode;
import com.reteno.core.data.local.sharedpref.SharedPrefsManager;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

class DeviceIdHelperProxy {

    static String getId(DeviceId deviceId) {
        return deviceId.getId$RetenoSdkCore_debug();
    }

    static DeviceIdMode getMode(DeviceId deviceId) {
        return deviceId.getMode$RetenoSdkCore_debug();
    }

    static String getExternalId(DeviceId deviceId) {
        return deviceId.getExternalId$RetenoSdkCore_debug();
    }

    private DeviceIdHelper deviceIdHelper;

    public DeviceIdHelperProxy(SharedPrefsManager sharedPrefsManager) {
        deviceIdHelper = new DeviceIdHelper(sharedPrefsManager);
    }

    void withDeviceIdMode(
            DeviceId currentDeviceId,
            DeviceIdMode deviceIdMode,
            Function1<DeviceId, Unit> onDeviceIdChanged) {
        deviceIdHelper.withDeviceIdMode$RetenoSdkCore_debug(currentDeviceId, deviceIdMode, onDeviceIdChanged);
    }

    DeviceId withExternalDeviceId(DeviceId currentDeviceId, String externalDeviceId) {
        return deviceIdHelper.withExternalDeviceId$RetenoSdkCore_debug(currentDeviceId, externalDeviceId);
    }
}
