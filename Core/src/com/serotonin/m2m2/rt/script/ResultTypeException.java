/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * @author Matthew Lohbihler
 */
public class ResultTypeException extends TranslatableException {
    static final long serialVersionUID = -1;

    public ResultTypeException(TranslatableMessage message) {
        super(message);
    }
}
