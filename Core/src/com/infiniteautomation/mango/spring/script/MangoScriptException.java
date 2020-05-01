/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.serotonin.m2m2.module.SourceLocation;

public class MangoScriptException extends RuntimeException {
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

    public static class EngineNotCompilableException extends MangoScriptException {
        private static final long serialVersionUID = 1L;

        EngineNotCompilableException(ScriptEngine engine) {
            super("Script engine " + engine.getFactory().getEngineName() + " does not implement Compilable interface");
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

        private final SourceLocation sourceLocation;

        ScriptEvalException(ScriptException cause, SourceLocation sourceLocation) {
            super(cause);
            this.sourceLocation = sourceLocation;
        }

        public SourceLocation getSourceLocation() {
            return sourceLocation;
        }
    }

    public static class EngineNotFoundException extends MangoScriptException {
        private static final long serialVersionUID = 1L;
        private final Set<String> availableEngines;

        EngineNotFoundException(String engineName, Set<String> availableEngines) {
            super("Script engine not found: " + engineName);
            this.availableEngines = availableEngines;
        }

        public Set<String> getAvailableEngines() {
            return availableEngines;
        }
    }

    public static class NoEngineForFileException extends MangoScriptException {
        private static final long serialVersionUID = 1L;

        NoEngineForFileException(String extension, String mime) {
            super(String.format("No engine found for extension %s or mime type %s", extension, mime));
        }
    }

    public static class NoDefinitionForFactory extends MangoScriptException {
        private static final long serialVersionUID = 1L;

        NoDefinitionForFactory(String factoryName) {
            super("No definition for engine found: " + factoryName);
        }
    }
}