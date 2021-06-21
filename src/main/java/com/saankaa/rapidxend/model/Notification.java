package com.saankaa.rapidxend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
public class Notification {

    private String senderDeviceId;

    private String receiverDeviceId;

    private String transferId;

    private Integer dataBlockNumber;

    private int notificationType;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification n = (Notification) o;

        boolean isEqual = true;
        // Sender id comparison
        if (senderDeviceId == null) {
            if (n.senderDeviceId != null) return false;
        } else {
            isEqual = senderDeviceId.equals(n.senderDeviceId);
        }
        // Receiver id comparison
        if (receiverDeviceId == null) {
            if (n.receiverDeviceId != null) return false;
        } else {
            isEqual = receiverDeviceId.equals(n.receiverDeviceId);
        }

        // DataBlockNumber comparison
        if (dataBlockNumber == null) {
            if (n.dataBlockNumber != null) return false;
        } else {
            isEqual = dataBlockNumber.equals(n.dataBlockNumber);
        }

        // Transfer Id comparison
        if (transferId == null) {
            if (n.transferId != null) return false;
        } else {
            isEqual &= transferId.equals(n.transferId);
        }

        return isEqual && (notificationType == n.notificationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderDeviceId, receiverDeviceId, dataBlockNumber, transferId, notificationType);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "senderDeviceId='" + senderDeviceId + '\'' +
                ", receiverDeviceId='" + receiverDeviceId + '\'' +
                ", transferId='" + transferId + '\'' +
                ", dataBlockNumber=" + dataBlockNumber +
                ", notificationType=" + notificationType +
                '}';
    }
}
