package com.saankaa.rapidxend.service.Authentication;

import lombok.Getter;

public class InvalidCredentials extends Exception {

    @Getter
    private final String message;

    public InvalidCredentials(String message) {
        this.message = message;
    }

}
