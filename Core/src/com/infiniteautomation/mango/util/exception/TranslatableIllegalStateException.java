/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Something is in the wrong state, translatable message says what and why
 * @author Terry Packer
 */
public class TranslatableIllegalStateException extends IllegalStateException implements TranslatableExceptionI {

    private static final long serialVersionUID = 1L;

    private TranslatableMessage translatableMessage;

    public TranslatableIllegalStateException(TranslatableMessage message) {
        this.translatableMessage = message;
    }

    @Override
    public TranslatableMessage getTranslatableMessage() {
        return this.translatableMessage;
    }

    @Override
    public String getMessage() {
        return this.translatableMessage.translate(Common.getTranslations());
    }
}
