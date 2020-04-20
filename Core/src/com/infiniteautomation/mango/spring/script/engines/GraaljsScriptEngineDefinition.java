/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script.engines;

import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.graalvm.polyglot.proxy.ProxyObject;
import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScript;
import com.infiniteautomation.mango.spring.script.permissions.GraaljsPermission;
import com.serotonin.m2m2.module.ScriptEngineDefinition;

/**
 * @author Jared Wiltshire
 */
public class GraaljsScriptEngineDefinition extends ScriptEngineDefinition {

    public static final String SUPPORTED_ENGINE_NAME = "Graal.js";

    @Autowired
    GraaljsPermission permission;

    @Override
    public boolean supports(ScriptEngineFactory engineFactory) {
        return SUPPORTED_ENGINE_NAME.equals(engineFactory.getEngineName());
    }

    @Override
    public MangoPermission accessPermission() {
        return permission.getPermission();
    }

    @Override
    public void applyRestrictions(ScriptEngine engine, MangoScript script) {
        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        if (permissionService.hasAdminRole(script)) {
            engineBindings.put("polyglot.js.allowAllAccess", true);
        }

        engineBindings.put("polyglot.js.allowHostAccess", true);
        engineBindings.put("polyglot.js.nashorn-compat", true);

    }

    @SuppressWarnings("unchecked")
    @Override
    public void addToBindings(Bindings bindings, String name, Object value) {
        if (value instanceof Map) {
            value = ProxyObject.fromMap((Map<String, Object>) value);
        }
        super.addToBindings(bindings, name, value);
    }
}
