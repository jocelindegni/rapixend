package com.saankaa.rapidxend.config.websocket;

import com.saankaa.rapidxend.config.security.IJwtUtils;
import com.saankaa.rapidxend.service.Authentication.InvalidCredentials;
import com.saankaa.rapidxend.service.websocket.IWebsocketSessionService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class AuthChannelInterceptorAdapter implements ChannelInterceptor {

    private final IJwtUtils jwtUtils;
    private final IWebsocketSessionService websocketSessionService;

    public AuthChannelInterceptorAdapter(@Autowired IJwtUtils jwtUtils, @Autowired IWebsocketSessionService websocketSessionService) {
        this.jwtUtils = jwtUtils;
        this.websocketSessionService = websocketSessionService;
    }

    @SneakyThrows
    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT == accessor.getCommand()) {

            // Get authorization header
            final String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

            // Get jwt token
            if (authorization == null || authorization.isEmpty()) throw new InvalidCredentials("invalid token");
            if (authorization.split(" ").length != 2) throw new InvalidCredentials("invalid token");
            String jwt = authorization.split(" ")[1];

            // Save user as principal
            StompPrincipal user = new StompPrincipal(jwtUtils.getUserId(jwt));
            accessor.setUser(user);

            // Save to connected device id list
            websocketSessionService.addConnectedDevice(user.getName());
        }

        if (StompCommand.DISCONNECT == accessor.getCommand()) {
            // Get device id
            String deviceId = accessor.getUser().getName();
            // Remove to connected device id list
            websocketSessionService.removeConnectedDevice(deviceId);
        }

        return message;
    }
}
