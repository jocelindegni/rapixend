package com.saankaa.rapidxend.service.websocket;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WebsocketSessionService implements IWebsocketSessionService {

    List<String> connectedDeviceId = new ArrayList<>();

    @Override
    synchronized public boolean isConnected(String deviceId) {
        return connectedDeviceId.contains(deviceId);
    }

    @Override
    synchronized public void addConnectedDevice(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return;
        connectedDeviceId.add(deviceId);
    }

    @Override
    synchronized public void removeConnectedDevice(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) return;
        connectedDeviceId.remove(deviceId);
    }
}
