/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.rt.event.handlers.EventHandlerInterface;

/**
 * @author Jared Wiltshire
 */
@Service
public class ScriptExecutor {

    private final ScriptEngineManager manager;

    @Autowired
    public ScriptExecutor(ScriptEngineManager manager) {
        this.manager = manager;
    }

    public ScriptEngine getEngineByName(String engineName) {
        ScriptEngine engine = manager.getEngineByName(engineName);
        if (engine == null) {
            List<String> names = new ArrayList<>();
            manager.getEngineFactories().stream().forEach(f -> names.addAll(f.getNames()));
            throw new EngineNotFoundException(engineName, names);
        }
        return engine;
    }

    public ScriptEngine executeScript(Path scriptFile, String engineName, Map<String, Object> bindings) {
        ScriptEngine engine = getEngineByName(engineName);

        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        engineBindings.put("polyglot.js.allowHostAccess", true);

        engineBindings.put(ScriptEngine.FILENAME, scriptFile.getFileName());
        if (engineBindings != null) {
            engineBindings.putAll(bindings);
        }

        try {
            try (Reader reader = Files.newBufferedReader(scriptFile)) {
                engine.eval(reader);
            }
        } catch (ScriptException e) {
            throw new ScriptEvalException(e);
        } catch (IOException e) {
            throw new ScriptIOException(e);
        }

        return engine;
    }

    public ScriptEngine executeScript(String scriptName, String engineName, String script, Map<String, Object> bindings) {
        ScriptEngine engine = getEngineByName(engineName);

        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        engineBindings.put("polyglot.js.allowHostAccess", true);

        engineBindings.put(ScriptEngine.FILENAME, scriptName);
        if (engineBindings != null) {
            engineBindings.putAll(bindings);
        }

        try {
            engine.eval(script);
        } catch (ScriptException e) {
            throw new ScriptEvalException(e);
        }

        return engine;
    }

    public <T> T getInterface(ScriptEngine engine, Class<T> clazz) {
        if (!(engine instanceof Invocable)) {
            throw new EngineNotInvocableException(engine);
        }

        Invocable inv = (Invocable) engine;
        T instance = inv.getInterface(clazz);
        if (instance == null) {
            throw new ScriptInterfaceException(EventHandlerInterface.class);
        }
        return instance;
    }

    public static class MangoScriptException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        MangoScriptException(String message) {
            super(message);
        }
        MangoScriptException(String message, Throwable cause) {
            super(message, cause);
        }
        MangoScriptException(Throwable cause) {
            super(cause);
        }
    }

    public static class ScriptIOException extends MangoScriptException {
        private static final long serialVersionUID = 1L;

        ScriptIOException(IOException cause) {
            super(cause);
        }
    }

    public static class EngineNotInvocableException extends MangoScriptException {
        private static final long serialVersionUID = 1L;

        EngineNotInvocableException(ScriptEngine engine) {
            super("Script engine " + engine.getFactory().getEngineName() + " does not implement Invocable interface");
        }
    }

    public static class ScriptInterfaceException extends MangoScriptException {
        private static final long serialVersionUID = 1L;
        private Class<?> interfaceClass;

        ScriptInterfaceException(Class<?> interfaceClass) {
            super("Script does not implement interface " + interfaceClass.getName());
            this.interfaceClass = interfaceClass;
        }

        public Class<?> getInterfaceClass() {
            return this.interfaceClass;
        }
    }

    public static class ScriptEvalException extends MangoScriptException {
        private static final long serialVersionUID = 1L;
        ScriptEvalException(ScriptException cause) {
            super(cause);
        }

        @Override
        public synchronized javax.script.ScriptException getCause() {
            return (javax.script.ScriptException) super.getCause();
        }
    }

    public static class EngineNotFoundException extends MangoScriptException {
        private static final long serialVersionUID = 1L;
        private final List<String> availableEngines;

        EngineNotFoundException(String engineName, List<String> availableEngines) {
            super("Script engine not found: " + engineName);
            this.availableEngines = availableEngines;
        }

        public List<String> getAvailableEngines() {
            return availableEngines;
        }
    }
}
