package com.serotonin.m2m2.vo;

import com.serotonin.m2m2.i18n.ProcessResult;

/**
 * @author Jared Wiltshire
 */
public interface Validatable {
    public void validate(ProcessResult response);
}
