/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.engines;

import javax.script.ScriptEngineFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import com.infiniteautomation.mango.permission.MangoPermission;
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
    public MangoPermission requiredPermission() {
        return permission.getPermission();
    }

}
