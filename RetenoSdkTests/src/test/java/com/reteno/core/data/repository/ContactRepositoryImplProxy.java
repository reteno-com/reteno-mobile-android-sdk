package com.reteno.core.data.repository;

import com.reteno.core.data.remote.api.ApiClient;
import com.reteno.core.data.remote.ds.ContactRepositoryImpl;
import com.reteno.core.domain.ResponseCallback;
import com.reteno.core.model.device.Device;

class ContactRepositoryImplProxy {

    private ContactRepositoryImpl contactRepositoryImpl;

    ContactRepositoryImplProxy(ApiClient apiClient) {
        contactRepositoryImpl = new ContactRepositoryImpl(apiClient);
    }

    void sendDeviceProperties(Device device, ResponseCallback responseHandler) {
        contactRepositoryImpl.sendDeviceProperties(device, responseHandler);
    }
}
