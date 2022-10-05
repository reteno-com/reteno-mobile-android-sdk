package com.reteno;


import android.app.Application;

import com.reteno.config.DeviceIdMode;

public class SampleApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Reteno instance = new Reteno(this);
        instance.changeDeviceIdMode(DeviceIdMode.ANDROID_ID);
    }
}
