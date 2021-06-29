/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.i18n;

/**
 * @author Matthew Lohbihler
 */
public class TranslatableMessageParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public TranslatableMessageParseException() {
        super();
    }

    public TranslatableMessageParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public TranslatableMessageParseException(String message) {
        super(message);
    }

    public TranslatableMessageParseException(Throwable cause) {
        super(cause);
    }
}
