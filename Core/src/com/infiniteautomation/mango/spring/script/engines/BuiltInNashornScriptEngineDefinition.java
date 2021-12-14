/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.script.engines;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import com.serotonin.m2m2.module.ConditionalDefinition;

/**
 * @author Jared Wiltshire
 */
@ConditionalDefinition(requireClasses = {"jdk.nashorn.api.scripting.NashornScriptEngineFactory"})
public class BuiltInNashornScriptEngineDefinition extends NashornScriptEngineDefinition {

    @Override
    public boolean supports(ScriptEngineFactory engineFactory) {
        return "jdk.nashorn.api.scripting.NashornScriptEngineFactory".equals(engineFactory.getClass().getName());
    }

    @Override
    protected Object callFunction(Object function, Object thiz, Object... args) {
        try {
            Method call = Class.forName("jdk.nashorn.api.scripting.JSObject")
                    .getDeclaredMethod("call", Object.class, Object[].class);
            return call.invoke(thiz, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ScriptEngine createScriptEngine(ScriptEngineFactory engineFactory, Function<String, Boolean> filter) {
        if (filter == null) {
            return engineFactory.getScriptEngine();
        }

        try {
            Class<?> classFilterType = Class.forName("jdk.nashorn.api.scripting.ClassFilter");
            Method getScriptEngine = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory")
                    .getDeclaredMethod("getScriptEngine", classFilterType);

            Object classFilter = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { classFilterType }, (instance, method, args) -> {
                if ("exposeToScripts".equals(method.getName()) && args.length == 1 && args[0] instanceof String) {
                    return filter.apply((String) args[0]);
                }
                throw new IllegalStateException("Unknown method");
            });

            //noinspection JavaReflectionInvocation
            return (ScriptEngine) getScriptEngine.invoke(engineFactory, classFilter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
