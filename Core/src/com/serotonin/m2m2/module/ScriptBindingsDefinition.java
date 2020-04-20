/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import javax.script.Bindings;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.service.PermissionService;

/**
 * Adds objects (typically utilities, services etc) to a script engine's bindings
 *
 * @author Jared Wiltshire
 */
public abstract class ScriptBindingsDefinition extends ModuleElementDefinition {

    @Autowired
    protected PermissionService permissionService;

    public abstract MangoPermission requiredPermission();
    public abstract void addBindings(MangoScript script, Bindings engineBindings, ScriptNativeConverter engineDefinition);

    /**
     * Converts a Java object to its native representation in the scripting environment
     * @author Jared Wiltshire
     */
    @FunctionalInterface
    public static interface ScriptNativeConverter {
        Object convert(Object value);
    }
}
