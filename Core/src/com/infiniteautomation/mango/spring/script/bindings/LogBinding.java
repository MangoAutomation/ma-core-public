/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.bindings;

import javax.script.Bindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.permissions.LogBindingPermission;
import com.serotonin.m2m2.module.ScriptBindingsDefinition;

/**
 * @author Jared Wiltshire
 */
public class LogBinding extends ScriptBindingsDefinition {

    @Autowired
    LogBindingPermission logBindingPermission;

    @Override
    public void addBindings(MangoScript script, Bindings engineBindings, ScriptNativeConverter converter) {
        Logger log = LoggerFactory.getLogger("script." + script.getScriptName());
        engineBindings.put("log", log);
    }

    @Override
    public MangoPermission requiredPermission() {
        return logBindingPermission.getPermission();
    }

}
