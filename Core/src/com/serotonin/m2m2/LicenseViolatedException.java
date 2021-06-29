/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2;

import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

public class LicenseViolatedException extends TranslatableRuntimeException {
    private static final long serialVersionUID = 1L;

    public LicenseViolatedException(TranslatableMessage errorMessage) {
        super(errorMessage);
    }

    public TranslatableMessage getErrorMessage() {
        return this.getTranslatableMessage();
    }
}
