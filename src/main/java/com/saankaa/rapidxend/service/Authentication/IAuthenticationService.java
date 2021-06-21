package com.saankaa.rapidxend.service.Authentication;


public interface IAuthenticationService {

    String authenticate(String deviceId, String totp) throws InvalidCredentials;

    String getCurrentUserId();

}
