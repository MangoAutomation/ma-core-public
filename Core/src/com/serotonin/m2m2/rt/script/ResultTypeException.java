/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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
