/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Throwables;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.service.PermissionService;

/**
 * Customizes a ScriptEngine and applies security restrictions based on roles.
 *
 * @author Jared Wiltshire
 */
public abstract class ScriptEngineDefinition extends ModuleElementDefinition {

    @Autowired
    protected PermissionService permissionService;

    public abstract MangoPermission requiredPermission();
    public abstract boolean supports(ScriptEngineFactory engineFactory);

    public ScriptEngine createEngine(ScriptEngineFactory engineFactory, MangoScript script) {
        return engineFactory.getScriptEngine();
    }

    public Object toScriptNative(Object value) {
        return value;
    }

    public SourceLocation extractSourceLocation(ScriptException e) {
        int lineNumber = e.getLineNumber();
        int columnNumber = e.getColumnNumber();
        Throwable rootCause = Throwables.getRootCause(e);
        return new SourceLocation(e.getFileName(), lineNumber >= 0 ? lineNumber : null, columnNumber >= 0 ? columnNumber : null, Throwables.getStackTraceAsString(rootCause));
    }
}
