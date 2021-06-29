/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.dataSource;

import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public class NoMatchException extends TranslatableException {
    private static final long serialVersionUID = 1L;

    public NoMatchException(TranslatableMessage message) {
        super(message);
    }
}
