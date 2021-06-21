package com.saankaa.rapidxend.service.device.Exception;

import lombok.Getter;

public class InvalidFileType extends Exception {

    @Getter
    private final String message;

    public InvalidFileType(String message) {
        this.message = message;
    }
}
