package com.saankaa.rapidxend.service.transfer.exception;

import lombok.Getter;

public class FileTooLargeException extends RuntimeException {

    @Getter
    private final String message;

    public FileTooLargeException(String message) {
        this.message = message;
    }
}
