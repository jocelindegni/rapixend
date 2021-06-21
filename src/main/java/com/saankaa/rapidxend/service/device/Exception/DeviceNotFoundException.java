package com.saankaa.rapidxend.service.device.Exception;

import lombok.Getter;

public class DeviceNotFoundException extends Exception {

    @Getter
    private final String message;

    public DeviceNotFoundException(String message) {
        this.message = message;
    }
}
