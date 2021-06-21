package com.saankaa.rapidxend.service.Authentication;

import com.saankaa.rapidxend.config.security.JwtUtils;
import com.saankaa.rapidxend.config.security.UserDetailsImpl;
import com.saankaa.rapidxend.model.Device;
import com.saankaa.rapidxend.repository.IDeviceRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements IAuthenticationService {

    private final JwtUtils jwtUtils;
    private final IDeviceRepository deviceRepository;

    public AuthenticationService(@Autowired JwtUtils jwtUtils, @Autowired IDeviceRepository deviceRepository) {
        this.jwtUtils = jwtUtils;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public String authenticate(String deviceId, String totp) throws InvalidCredentials {

        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new InvalidCredentials("Invalid deviceId or password."));

        CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        if (!verifier.isValidCode(device.getSecretKey(), totp))
            throw new InvalidCredentials("Invalid deviceId or password.");

        return jwtUtils.generateToken(device);
    }

    @Override
    public String getCurrentUserId() {
        return ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getDetails()).getUsername();
    }
}
