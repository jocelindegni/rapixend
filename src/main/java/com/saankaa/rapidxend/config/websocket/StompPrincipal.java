package com.saankaa.rapidxend.config.websocket;

import java.security.Principal;

public class StompPrincipal implements Principal {

    private final String deviceId;

    public StompPrincipal(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String getName() {
        return deviceId;
    }
}
