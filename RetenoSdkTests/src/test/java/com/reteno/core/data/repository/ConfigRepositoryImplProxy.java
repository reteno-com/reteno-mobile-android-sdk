package com.reteno.core.data.repository;

import com.reteno.core.data.local.config.DeviceId;
import com.reteno.core.data.local.config.DeviceIdMode;
import com.reteno.core.data.local.config.RestConfig;
import com.reteno.core.data.local.sharedpref.SharedPrefsManager;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

class ConfigRepositoryImplProxy {

    private ConfigRepositoryImpl configRepository;

    ConfigRepositoryImplProxy(SharedPrefsManager sharedPrefsManager, RestConfig restConfig) {
        configRepository = new ConfigRepositoryImpl(sharedPrefsManager, restConfig);
    }

    void setExternalDeviceId(String externalId) {
        configRepository.setExternalDeviceId(externalId);
    }

    void setDeviceIdMode(DeviceIdMode mode, Function1<DeviceId, Unit> onDeviceIdChanged) {
        configRepository.setDeviceIdMode(mode, onDeviceIdChanged);
    }

    DeviceId getDeviceId() {
        return configRepository.getDeviceId();
    }

    void saveFcmToken(String token) {
        configRepository.saveFcmToken(token);
    }

    String getFcmToken() {
        return configRepository.getFcmToken();
    }

    void saveDefaultNotificationChannel(String channel) {
        configRepository.saveDefaultNotificationChannel(channel);
    }

    String getDefaultNotificationChannel() {
        return configRepository.getDefaultNotificationChannel();
    }
}
