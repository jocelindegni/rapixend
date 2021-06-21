package com.saankaa.rapidxend.service.transfer.exception;

import lombok.Getter;

public class InvalidDataBlockException extends Exception {

    @Getter
    private final String message;

    public InvalidDataBlockException(String message) {
        this.message = message;
    }
}
