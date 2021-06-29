/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.util.script;

import com.serotonin.m2m2.i18n.TranslatableMessage;

/**
 * Container for Mango Java Script Errors (Compilation/Eval)
 * 
 * @author Terry Packer
 *
 */
public class MangoJavaScriptError {

    private final TranslatableMessage message;
    private final Integer lineNumber;
    private final Integer columnNumber;
    
    public MangoJavaScriptError(String message) {
        this(new TranslatableMessage("common.default", message));
    }
    
    public MangoJavaScriptError(TranslatableMessage message) {
        this(message, null, null);
    }
    
    public MangoJavaScriptError(TranslatableMessage message, Integer lineNumber, Integer columnNumber) {
        this.message = message;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }
    
    public TranslatableMessage getMessage() {
        return message;
    }
    
    public Integer getLineNumber() {
        return lineNumber;
    }

    public Integer getColumnNumber() {
        return columnNumber;
    }
}
