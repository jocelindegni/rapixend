package com.saankaa.rapidxend.service.notification;

import com.saankaa.rapidxend.config.AppEnvVariable;
import com.saankaa.rapidxend.model.Notification;
import com.saankaa.rapidxend.service.websocket.IWebsocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService implements INotificationService {

    private final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final IWebsocketSessionService websocketSessionService;
    private final SimpMessagingTemplate simpMessagingTemplate; // Websocket messaging
    private final RedisTemplate<String, Notification> redisTemplate;


    public NotificationService(@Autowired IWebsocketSessionService websocketSessionService, @Autowired SimpMessagingTemplate simpMessagingTemplate, @Autowired RedisTemplate<String, Notification> redisTemplate) {
        this.websocketSessionService = websocketSessionService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void notifyDevice(Notification notification) {

        // Check if receiver to notify is connected to current instance
        if (websocketSessionService.isConnected(notification.getReceiverDeviceId())) {
            simpMessagingTemplate.convertAndSendToUser(notification.getReceiverDeviceId(), "/user", notification);
        } else {
            // Publish notification via redis to others instances
            redisTemplate.convertAndSend(AppEnvVariable.REDIS_CHANNEL, notification);
        }

    }

    @Override
    public void onMessage(Notification notification) {
        // Message from redis
        LOGGER.info("On receive notification from redis... {}", notification);

        // Check if the receiver of this current notification
        if (websocketSessionService.isConnected(notification.getReceiverDeviceId())) {
            LOGGER.info("Receiver is connected to current instance");
            simpMessagingTemplate.convertAndSendToUser(notification.getReceiverDeviceId(), "/user", notification);
        }
    }
}
