/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.i18n;

/**
 * @author Matthew Lohbihler
 */
public class TranslatableException extends Exception {
    private static final long serialVersionUID = 1L;

    private TranslatableMessage translatableMessage = null;

    public TranslatableException() {
        super();
    }

    public TranslatableException(TranslatableMessage message, Throwable cause) {
        super(cause);
        this.translatableMessage = message;
    }

    public TranslatableException(TranslatableMessage message) {
        this.translatableMessage = message;
    }

    public TranslatableException(Throwable cause) {
        super(cause);
    }

    public TranslatableMessage getTranslatableMessage() {
        return translatableMessage;
    }
}
