/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public class TranslatableRuntimeException extends RuntimeException implements TranslatableExceptionI {
    private static final long serialVersionUID = 1L;

    private TranslatableMessage translatableMessage = null;

    public TranslatableRuntimeException(TranslatableMessage message, Throwable cause) {
        super(cause);
        this.translatableMessage = message;
    }

    public TranslatableRuntimeException(TranslatableMessage message) {
        super();
        this.translatableMessage = message;
    }

    @Override
    public TranslatableMessage getTranslatableMessage() {
        return translatableMessage;
    }

    @Override
    public String getMessage() {
        return this.translatableMessage.translate(Common.getTranslations());
    }
}
