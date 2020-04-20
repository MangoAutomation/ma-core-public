/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.engines;

import javax.script.ScriptEngineFactory;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.permissions.GroovyPermission;
import com.serotonin.m2m2.module.ScriptEngineDefinition;

/**
 * @author Jared Wiltshire
 */
public class GroovyScriptingEngineDefinition extends ScriptEngineDefinition {

    @Autowired
    GroovyPermission permission;

    @Override
    public boolean supports(ScriptEngineFactory engineFactory) {
        return engineFactory instanceof GroovyScriptEngineFactory;
    }

    @Override
    public MangoPermission requiredPermission() {
        return permission.getPermission();
    }

}
