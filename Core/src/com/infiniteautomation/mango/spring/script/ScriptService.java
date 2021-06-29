/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScriptException.EngineNotFoundException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.EngineNotInvocableException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.NoDefinitionForFactory;
import com.infiniteautomation.mango.spring.script.MangoScriptException.NoEngineForFileException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptEvalException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptIOException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptInterfaceException;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ScriptBindingsDefinition;
import com.serotonin.m2m2.module.ScriptEngineDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.infiniteautomation.mango.spring.components.RunAs;

/**
 * @author Jared Wiltshire
 */
@Service
public class ScriptService {

    final ScriptEngineManager manager;
    final PermissionService permissionService;
    final List<ScriptEngineDefinition> engineDefinitions;
    final List<ScriptBindingsDefinition> bindingsDefinitions;
    final Map<String, ScriptEngineFactory> factories;
    final RunAs runAs;

    @Autowired
    public ScriptService(ScriptEngineManager manager, PermissionService permissionService, List<ScriptEngineDefinition> engineDefinitions,
                         List<ScriptBindingsDefinition> bindingsDefinitions, RunAs runAs) {
        this.manager = manager;
        this.permissionService = permissionService;
        this.engineDefinitions = engineDefinitions;
        this.bindingsDefinitions = bindingsDefinitions;

        this.factories = manager.getEngineFactories().stream()
                .collect(Collectors.toMap(ScriptEngineFactory::getEngineName, Function.identity()));
        this.runAs = runAs;
    }

    private ScriptEngineFactory getFactoryByName(String engineName) {
        return Optional.ofNullable(factories.get(engineName)).orElseThrow(() -> {
            return new EngineNotFoundException(engineName, factories.keySet());
        });
    }

    private ScriptEngineDefinition definitionForFactory(ScriptEngineFactory factory) {
        return engineDefinitions.stream()
                .filter(def -> def.supports(factory))
                .findFirst()
                .orElseThrow(() -> new NoDefinitionForFactory(factory.getEngineName()));
    }

    public Stream<ScriptEngineFactory> getEngineFactories() {
        PermissionHolder user = Common.getUser();
        return manager.getEngineFactories().stream().filter(f -> {
            MangoPermission permission = definitionForFactory(f).requiredPermission();
            return permissionService.hasPermission(user, permission);
        });
    }

    public CompiledMangoScript compile(MangoScript script) {
        if (script instanceof CompiledMangoScript) {
            return (CompiledMangoScript) script;
        }

        ScriptAndEngine scriptAndEngine = getScriptEngine(script);
        ScriptEngine engine = scriptAndEngine.engine;

        try (Reader reader = script.readScript()) {
            return new CompiledMangoScript(((Compilable) engine).compile(reader), script);
        } catch (ScriptException e) {
            throw new ScriptEvalException(e, scriptAndEngine.engineDefinition.extractSourceLocation(e));
        } catch (IOException e) {
            throw new ScriptIOException(e);
        }
    }

    private static class ScriptAndEngine {
        final MangoScript script;
        final ScriptEngineDefinition engineDefinition;
        final ScriptEngine engine;
        final Object synchronizationObject;

        private ScriptAndEngine(MangoScript script, ScriptEngineDefinition engineDefinition, ScriptEngine engine) {
            this.script = script;
            this.engineDefinition = engineDefinition;
            this.engine = engine;
            this.synchronizationObject = new Object();
        }
    }

    private ScriptAndEngine getScriptEngine(MangoScript script) {
        ScriptEngine engine;
        ScriptEngineDefinition definition;

        if (script instanceof CompiledMangoScript) {
            engine = ((CompiledMangoScript) script).compiled.getEngine();
            definition = definitionForFactory(engine.getFactory());
        } else {
            ScriptEngineFactory factory = getFactoryByName(script.getEngineName());
            definition = definitionForFactory(factory);
            engine = definition.createEngine(factory, script);
        }

        PermissionHolder user = Common.getUser();
        permissionService.ensurePermission(user, definition.requiredPermission());
        permissionService.hasSupersetOfRoles(user, script);

        return new ScriptAndEngine(script, definition, engine);
    }

    private EvalResult evalScript(ScriptAndEngine scriptAndEngine, EvalContext evalContext) {
        MangoScript script = scriptAndEngine.script;
        ScriptEngine engine = scriptAndEngine.engine;
        ScriptEngineDefinition engineDefinition = scriptAndEngine.engineDefinition;

        ScriptContext context = engine.getContext();
        Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

        engineBindings.putAll(evalContext.bindings);
        context.setWriter(evalContext.writer);
        context.setErrorWriter(evalContext.errorWriter);
        context.setReader(evalContext.reader);

        String scriptFilename = script.getScriptFilename();
        if (scriptFilename != null) {
            engineBindings.put(ScriptEngine.FILENAME, scriptFilename);
        } else {
            String scriptName = script.getScriptName();
            String fileName = scriptName;

            List<String> extensions = engine.getFactory().getExtensions();
            boolean hasExtension = extensions.stream().anyMatch(e -> scriptName.endsWith("." + e));
            if (!hasExtension && !extensions.isEmpty()) {
                fileName += "." + extensions.get(0);
            }
            engineBindings.put(ScriptEngine.FILENAME, fileName);
        }

        for (ScriptBindingsDefinition bindingsDef : bindingsDefinitions) {
            MangoPermission permission = bindingsDef.requiredPermission();
            if (permissionService.hasPermission(script, permission)) {
                bindingsDef.addBindings(script, engineBindings, scriptAndEngine.synchronizationObject,
                        engineDefinition);
            }
        }

        return this.runAs.runAs(script, () -> {
            try {
                Object value;
                if (script instanceof CompiledMangoScript) {
                    value = ((CompiledMangoScript) script).compiled.eval();
                } else {
                    try (Reader reader = script.readScript()) {
                        value = engine.eval(reader);
                    }
                }
                return new EvalResult(value, engineBindings);
            } catch (ScriptException e) {
                throw new ScriptEvalException(e, engineDefinition.extractSourceLocation(e));
            } catch (IOException e) {
                throw new ScriptIOException(e);
            }
        });
    }

    public EvalResult eval(MangoScript script) {
        return eval(script, Collections.emptyMap());
    }

    public EvalResult eval(MangoScript script, Map<String, Object> bindings) {
        return eval(script, new EvalContext(bindings));
    }

    public EvalResult eval(MangoScript script, EvalContext evalContext) {
        ScriptAndEngine scriptAndEngine = getScriptEngine(script);
        return evalScript(scriptAndEngine, evalContext);
    }

    public <T> T getInterface(MangoScript script, Class<T> clazz) {
        return getInterface(script, clazz, Collections.emptyMap());
    }

    public <T> T getInterface(MangoScript script, Class<T> clazz, Map<String, Object> bindings) {
        return getInterface(script, clazz, new EvalContext(bindings));
    }

    public <T> T getInterface(MangoScript script, Class<T> clazz, EvalContext evalContext) {
        ScriptAndEngine scriptAndEngine = getScriptEngine(script);
        ScriptEngine engine = scriptAndEngine.engine;

        if (!(engine instanceof Invocable)) {
            throw new EngineNotInvocableException(engine);
        }

        evalScript(scriptAndEngine, evalContext);

        T instance = ((Invocable) engine).getInterface(clazz);
        if (instance == null) {
            throw new ScriptInterfaceException(clazz);
        }

        ScriptEngineDefinition engineDefinition = scriptAndEngine.engineDefinition;

        T runAsInstance = runAs.runAsProxy(script, instance);
        if (engineDefinition.singleThreadedAccess()) {
            return synchronizedProxy(scriptAndEngine.synchronizationObject, runAsInstance);
        }
        return runAsInstance;
    }

    @SuppressWarnings("unchecked")
    private <T> T synchronizedProxy(Object synchronizationObject, T instance) {
        Class<?> clazz = instance.getClass();
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), (proxy, method, args) -> {
            synchronized (synchronizationObject) {
                try {
                    return method.invoke(instance, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        });
    }

    public String findEngineForFile(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf(".");
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex + 1) : null;
        String mimeType = Files.probeContentType(filePath);

        return findEngine(extension, mimeType);
    }

    public String findEngine(String extension, String mimeType) {
        return getEngineFactories()
                .filter(factory -> {
                    return extension != null && factory.getExtensions().contains(extension) ||
                            mimeType != null && factory.getMimeTypes().contains(mimeType);
                }).map(ScriptEngineFactory::getEngineName)
                .findFirst()
                .orElseThrow(() -> new NoEngineForFileException(extension, mimeType));
    }

}
