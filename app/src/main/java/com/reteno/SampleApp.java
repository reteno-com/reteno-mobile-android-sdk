package com.reteno;


import android.app.Application;

import com.reteno.config.DeviceIdMode;
import com.reteno.util.SharedPreferencesManager;

public class SampleApp extends Application {

    private Reteno retenoInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        retenoInstance = new Reteno(this);
        DeviceIdMode deviceIdMode = SharedPreferencesManager.getDeviceIdMode(this);
        retenoInstance.changeDeviceIdMode(deviceIdMode);
    }

    public Reteno getRetenoInstance() {
        return retenoInstance;
    }
}
