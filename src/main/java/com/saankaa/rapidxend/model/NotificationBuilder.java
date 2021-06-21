package com.saankaa.rapidxend.model;

public class NotificationBuilder {

    private String senderDeviceId;
    private String receiverDeviceId;
    private String transferId;
    private Integer dataBlockNumber;
    private int notificationType;

    private Notification notification;

    public NotificationBuilder senderDeviceId(String senderDeviceId) {
        this.senderDeviceId = senderDeviceId;
        return this;
    }

    public NotificationBuilder receiverDeviceId(String receiverDeviceId) {
        this.receiverDeviceId = receiverDeviceId;
        return this;
    }

    public NotificationBuilder transferId(String transferId) {
        this.transferId = transferId;
        return this;
    }

    public NotificationBuilder dataBlockNumber(Integer dataBlockNumber) {
        this.dataBlockNumber = dataBlockNumber;
        return this;
    }

    public NotificationBuilder notificationType(int notificationType) {
        this.notificationType = notificationType;
        return this;
    }

    public Notification build() {
        Notification notification = new Notification();
        notification.setSenderDeviceId(this.senderDeviceId);
        notification.setReceiverDeviceId(receiverDeviceId);
        notification.setDataBlockNumber(dataBlockNumber);
        notification.setTransferId(this.transferId);
        notification.setNotificationType(this.notificationType);

        return notification;
    }
}
