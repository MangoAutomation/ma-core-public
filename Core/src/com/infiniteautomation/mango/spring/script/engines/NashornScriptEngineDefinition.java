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

/**
 * @author Jared Wiltshire
 */
public class NashornScriptEngineDefinition extends ScriptEngineDefinition {

    public static final String SUPPORTED_ENGINE_NAME = "Oracle Nashorn";

    @Autowired
    NashornPermission permission;

    @Override
    public boolean supports(ScriptEngineFactory engineFactory) {
        return SUPPORTED_ENGINE_NAME.equals(engineFactory.getEngineName());
    }

    @Override
    public void applyRestrictions(ScriptEngine engine, MangoScript script) {
        // TODO Auto-generated method stub

    }

    @Override
    public MangoPermission accessPermission() {
        return permission.getPermission();
    }

}
