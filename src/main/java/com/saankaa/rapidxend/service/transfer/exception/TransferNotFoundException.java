package com.saankaa.rapidxend.service.transfer.exception;

import lombok.Getter;

public class TransferNotFoundException extends Exception {

    @Getter
    private final String message;

    public TransferNotFoundException(String message) {
        this.message = message;
    }
}
