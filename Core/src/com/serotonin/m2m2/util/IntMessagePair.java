/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util;

import java.io.Serializable;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class IntMessagePair implements Serializable {
    private static final long serialVersionUID = -1;

    private int key;
    private TranslatableMessage message;

    public IntMessagePair() {
        // no op
    }

    public IntMessagePair(int key, TranslatableMessage message) {
        this.key = key;
        this.message = message;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getKey() {
        return key;
    }

    public TranslatableMessage getMessage() {
        return message;
    }

    public void setMessage(TranslatableMessage message) {
        this.message = message;
    }
}
