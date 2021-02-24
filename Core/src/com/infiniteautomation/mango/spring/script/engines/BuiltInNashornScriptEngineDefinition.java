/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.script.engines;

import java.util.function.Function;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import com.serotonin.m2m2.module.ConditionalDefinition;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

/**
 * @author Jared Wiltshire
 */
@SuppressWarnings({"removal", "deprecation", "restriction"})
@ConditionalDefinition(requireClasses = {"jdk.nashorn.api.scripting.NashornScriptEngineFactory"})
public class BuiltInNashornScriptEngineDefinition extends NashornScriptEngineDefinition {

    @Override
    public boolean supports(ScriptEngineFactory engineFactory) {
        return "jdk.nashorn.api.scripting.NashornScriptEngineFactory".equals(engineFactory.getClass().getName());
    }

    @Override
    protected Object callLoad(Object load, Object source) {
        JSObject loadFn = (JSObject) load;
        return loadFn.call(null, source);
    }

    @Override
    public ScriptEngine createScriptEngine(ScriptEngineFactory engineFactory, Function<String, Boolean> filter) {
        if (filter == null) {
            return engineFactory.getScriptEngine();
        }
        return ((NashornScriptEngineFactory) engineFactory).getScriptEngine(filter::apply);
    }

}
