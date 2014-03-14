/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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
