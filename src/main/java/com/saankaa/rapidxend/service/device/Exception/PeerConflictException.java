package com.saankaa.rapidxend.service.device.Exception;

import lombok.Getter;

public class PeerConflictException extends Exception {

    @Getter
    private final String message;

    public PeerConflictException(String message) {
        this.message = message;
    }
}
