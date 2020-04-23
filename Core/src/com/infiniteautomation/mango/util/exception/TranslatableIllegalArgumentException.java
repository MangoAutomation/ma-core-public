/**
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.util.exception;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Jared Wiltshire
 */
public class TranslatableIllegalArgumentException extends IllegalArgumentException implements TranslatableExceptionI {

    private static final long serialVersionUID = 1L;

    private TranslatableMessage translatableMessage;

    public TranslatableIllegalArgumentException(TranslatableMessage message) {
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
