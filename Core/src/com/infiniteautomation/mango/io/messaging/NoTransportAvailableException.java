/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 *
 * @author Terry Packer
 */
public class NoTransportAvailableException extends MessageSendException {

    private static final long serialVersionUID = 1L;
    private final Message failedMessage;

    public NoTransportAvailableException(Message message) {
        super(new TranslatableMessage("messaging.noTransportAvailable", message.getClass().getSimpleName()), null);
        this.failedMessage = message;
    }

    public NoTransportAvailableException(Class<Message> type) {
        super(new TranslatableMessage("messaging.noTransportAvailable", type.getSimpleName()), null);
        this.failedMessage = null;
    }

    public Message getFailedMessage() {
        return this.failedMessage;
    }

}
