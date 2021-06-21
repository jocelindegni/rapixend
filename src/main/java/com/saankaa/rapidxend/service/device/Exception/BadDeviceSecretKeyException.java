package com.saankaa.rapidxend.service.device.Exception;

import lombok.Getter;

public class BadDeviceSecretKeyException extends Exception {

    @Getter
    private final String message;

    public BadDeviceSecretKeyException(String message) {
        this.message = message;
    }
}
