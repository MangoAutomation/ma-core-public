/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.i18n;

import java.util.Objects;

import com.infiniteautomation.mango.util.exception.TranslatableExceptionI;
import com.serotonin.m2m2.Common;

/**
 * @author Matthew Lohbihler
 */
public class TranslatableException extends Exception implements TranslatableExceptionI {
    private static final long serialVersionUID = 1L;

    private TranslatableMessage translatableMessage = null;

    public TranslatableException(TranslatableMessage message, Throwable cause) {
        super(cause);
        this.translatableMessage = Objects.requireNonNull(message);
    }

    public TranslatableException(TranslatableMessage message) {
        this.translatableMessage = Objects.requireNonNull(message);
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
