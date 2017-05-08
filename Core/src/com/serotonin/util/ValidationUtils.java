/*
    Copyright (C) 2006-2007 Serotonin Software Technologies Inc.
 	@author Matthew Lohbihler
 */
package com.serotonin.util;

import org.springframework.validation.BindException;

import com.serotonin.web.dwr.DwrMessage;
import com.serotonin.web.dwr.DwrMessageI18n;
import com.serotonin.web.dwr.DwrResponse;
import com.serotonin.web.dwr.DwrResponseI18n;

/**
 * @author Matthew Lohbihler
 */
public class ValidationUtils {
    public static void reject(BindException errors, String errorCode, Object... args) {
        errors.reject(errorCode, args, "???" + errorCode + "(10)???");
    }

    public static void rejectValue(BindException errors, String field, String errorCode, Object... args) {
        errors.rejectValue(field, errorCode, args, "???" + errorCode + "(11)???");
    }

    public static void reject(BindException errors, String fieldPrefix, DwrResponse response) {
        for (DwrMessage m : response.getMessages()) {
            if (m.getContextKey() != null)
                ValidationUtils.rejectValue(errors, fieldPrefix + m.getContextKey(), m.getContextualMessage());
            else
                ValidationUtils.reject(errors, m.getGenericMessage());
        }
    }

    public static void reject(BindException errors, String fieldPrefix, DwrResponseI18n response) {
        for (DwrMessageI18n m : response.getMessages()) {
            if (m.getContextKey() != null)
                ValidationUtils.rejectValue(errors, fieldPrefix + m.getContextKey(), m.getContextualMessage().getKey(),
                        m.getContextualMessage().getArgs());
            else
                ValidationUtils.reject(errors, m.getGenericMessage().getKey(), m.getGenericMessage().getArgs());
        }
    }
}
