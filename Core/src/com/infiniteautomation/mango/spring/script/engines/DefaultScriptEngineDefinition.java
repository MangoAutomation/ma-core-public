/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.engines;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.permissions.UnknownEnginePermission;
import com.serotonin.m2m2.module.ScriptEngineDefinition;

/**
 * Default definition that only allows a super administrator to use the script engine.
 *
 * @author Jared Wiltshire
 */
public class DefaultScriptEngineDefinition extends ScriptEngineDefinition {

    @Autowired
    UnknownEnginePermission permission;

    @Override
    public boolean supports(ScriptEngineFactory engineFactory) {
        return true;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }


    @Override
    public void applyRestrictions(ScriptEngine engine, MangoScript script) {
        permissionService.ensureAdminRole(script);
    }

    @Override
    public MangoPermission accessPermission() {
        return permission.getPermission();
    }

}
