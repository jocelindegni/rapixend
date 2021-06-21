package com.saankaa.rapidxend.service.websocket;


public interface IWebsocketSessionService {

    /**
     * Check if device with deviceId is connected
     *
     * @param deviceId device id
     * @return true if current device id is connected
     */
    boolean isConnected(String deviceId);

    /**
     * Add connected user
     *
     * @param deviceId Device id
     */
    void addConnectedDevice(String deviceId);

    /**
     * Remove device from connected device
     *
     * @param deviceId Device id
     */
    void removeConnectedDevice(String deviceId);

}
