/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public class TranslatableRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private TranslatableMessage translatableMessage = null;

    public TranslatableRuntimeException() {
        super();
    }

    public TranslatableRuntimeException(TranslatableMessage message, Throwable cause) {
        super(cause);
        this.translatableMessage = message;
    }

    public TranslatableRuntimeException(TranslatableMessage message) {
        this.translatableMessage = message;
    }

    public TranslatableRuntimeException(Throwable cause) {
        super(cause);
    }

    public TranslatableMessage getTranslatableMessage() {
        return translatableMessage;
    }

    @Override
    public String getMessage() {
        return this.translatableMessage.translate(Common.getTranslations());
    }
}
