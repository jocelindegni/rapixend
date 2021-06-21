package com.saankaa.rapidxend.config.security;

import com.saankaa.rapidxend.model.Device;

public interface IJwtUtils {

    boolean validate(String token);

    String generateToken(Device device);

    String getUserId(String token);
}
