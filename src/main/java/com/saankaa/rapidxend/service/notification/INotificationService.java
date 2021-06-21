package com.saankaa.rapidxend.service.notification;

import com.saankaa.rapidxend.model.Notification;

public interface INotificationService {

    /**
     * Notify other device for an event which occurred.
     * Each event is listed in {@link com.saankaa.rapidxend.model.NotificationType}
     *
     * @param notification Notification object
     */
    void notifyDevice(Notification notification);

    /**
     * This method is called by spring when incoming message from broke like (kafka, redis, rabbitmq...)
     *
     * @param notification
     */
    void onMessage(Notification notification);

}
