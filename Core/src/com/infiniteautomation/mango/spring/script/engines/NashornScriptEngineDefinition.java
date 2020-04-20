/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.engines;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.permissions.NashornPermission;
import com.serotonin.m2m2.module.ScriptEngineDefinition;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * @author Jared Wiltshire
 */
@SuppressWarnings("restriction")
public class NashornScriptEngineDefinition extends ScriptEngineDefinition {

    @Autowired
    NashornPermission permission;

    @Override
    public boolean supports(ScriptEngineFactory engineFactory) {
        return "jdk.nashorn.api.scripting.NashornScriptEngineFactory".equals(engineFactory.getClass().getName());
    }

    @Override
    public ScriptEngine createEngine(ScriptEngineFactory engineFactory, MangoScript script) {
        if (permissionService.hasAdminRole(script)) {
            return engineFactory.getScriptEngine();
        }

        return ((NashornScriptEngineFactory) engineFactory).getScriptEngine(name -> false);
    }

    @Override
    public MangoPermission accessPermission() {
        return permission.getPermission();
    }

}
