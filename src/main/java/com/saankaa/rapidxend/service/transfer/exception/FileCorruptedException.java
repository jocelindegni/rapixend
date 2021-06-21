package com.saankaa.rapidxend.service.transfer.exception;

import lombok.Getter;

public class FileCorruptedException extends Exception {
    @Getter
    private final String message;

    public FileCorruptedException(String message) {
        this.message = message;
    }
}
