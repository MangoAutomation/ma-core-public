/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.io.messaging;

import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Placeholder for message sending exceptions
 * @author Terry Packer
 */
public abstract class MessageSendException extends TranslatableException {

    private static final long serialVersionUID = 1L;

    public MessageSendException(TranslatableMessage message, Throwable cause) {
        super(message, cause);
    }

}
