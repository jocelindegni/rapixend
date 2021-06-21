package com.saankaa.rapidxend.model;

import lombok.Getter;

public enum NotificationType {
    // DEVICE PEERING EVENTS
    PEERING_REQUEST(1),
    PEERING_ACCEPTED(2),
    PEERING_DENIED(3),
    DISSOCIATED(4),

    // Transfer event
    TRANSFER_CREATED(21),
    TRANSFER_CANCELLED(22),
    TRANSFER_DATA_AVAILABLE(23),
    TRANSFER_FILE_CORRUPTED(24),
    TRANSFER_FINISHED(25);


    @Getter
    int value;

    NotificationType(int value) {
        this.value = value;
    }
}
