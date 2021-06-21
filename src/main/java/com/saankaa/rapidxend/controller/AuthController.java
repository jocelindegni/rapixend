package com.saankaa.rapidxend.controller;

import com.saankaa.rapidxend.service.Authentication.AuthenticationService;
import com.saankaa.rapidxend.service.Authentication.InvalidCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(@Autowired AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("")
    public String authentication(@RequestBody Map<String, String> credentials) {

        try {
            return authenticationService.authenticate(credentials.get("deviceId"), credentials.get("totp"));
        } catch (InvalidCredentials ic) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ic.getMessage());
        } catch (Exception e) {
            // Todo notify admin
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }
}
