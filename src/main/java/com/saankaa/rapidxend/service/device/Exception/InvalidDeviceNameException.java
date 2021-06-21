package com.saankaa.rapidxend.service.device.Exception;

import lombok.Getter;

public class InvalidDeviceNameException extends Exception {

    @Getter
    private final String message;

    public InvalidDeviceNameException(String message) {
        this.message = message;
    }
}
