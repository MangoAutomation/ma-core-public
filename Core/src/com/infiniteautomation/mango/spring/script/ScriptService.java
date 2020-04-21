/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptEvalException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptIOException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptInterfaceException;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ScriptBindingsDefinition;
import com.serotonin.m2m2.module.ScriptEngineDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

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

    @Autowired
    public ScriptService(ScriptEngineManager manager, PermissionService permissionService, List<ScriptEngineDefinition> engineDefinitions,
            List<ScriptBindingsDefinition> bindingsDefinitions) {
        this.manager = manager;
        this.permissionService = permissionService;
        this.engineDefinitions = engineDefinitions;
        this.bindingsDefinitions = bindingsDefinitions;

        this.factories = manager.getEngineFactories().stream()
                .collect(Collectors.toMap(ScriptEngineFactory::getEngineName, Function.identity()));
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

    public List<ScriptEngineFactory> getEngineFactories() {
        PermissionHolder user = Common.getUser();
        return manager.getEngineFactories().stream().filter(f -> {
            MangoPermission permission = definitionForFactory(f).requiredPermission();
            return permissionService.hasPermission(user, permission);
        }).collect(Collectors.toList());
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
            throw new ScriptEvalException(e);
        } catch (IOException e) {
            throw new ScriptIOException(e);
        }
    }

    private static class ScriptAndEngine {
        final MangoScript script;
        final ScriptEngineDefinition engineDefinition;
        final ScriptEngine engine;

        private ScriptAndEngine(MangoScript script, ScriptEngineDefinition engineDefinition, ScriptEngine engine) {
            this.script = script;
            this.engineDefinition = engineDefinition;
            this.engine = engine;
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
        permissionService.ensureHasAllRoles(user, script.getAllInheritedRoles());

        return new ScriptAndEngine(script, definition, engine);
    }

    private Object evalScript(ScriptAndEngine scriptAndEngine, Map<String, Object> bindings) {
        MangoScript script = scriptAndEngine.script;
        ScriptEngine engine = scriptAndEngine.engine;
        ScriptEngineDefinition engineDefinition = scriptAndEngine.engineDefinition;

        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        String scriptName = script.getScriptName();
        if (scriptName != null) {
            engineBindings.put(ScriptEngine.FILENAME, scriptName);
        }

        for (ScriptBindingsDefinition bindingsDef : bindingsDefinitions) {
            MangoPermission permission = bindingsDef.requiredPermission();
            if (permissionService.hasPermission(script, permission)) {
                bindingsDef.addBindings(script, engineBindings, engineDefinition::toScriptNative);
            }
        }

        engineBindings.putAll(bindings);

        return this.permissionService.runAs(script, () -> {
            try {
                if (script instanceof CompiledMangoScript) {
                    return ((CompiledMangoScript) script).compiled.eval();
                }

                try (Reader reader = script.readScript()) {
                    return engine.eval(reader);
                }
            } catch (ScriptException e) {
                throw new ScriptEvalException(e);
            } catch (IOException e) {
                throw new ScriptIOException(e);
            }
        });
    }

    public Object eval(MangoScript script) {
        return eval(script, Collections.emptyMap());
    }

    public Object eval(MangoScript script, Map<String, Object> bindings) {
        ScriptAndEngine scriptAndEngine = getScriptEngine(script);
        return evalScript(scriptAndEngine, bindings);
    }

    public <T> T getInterface(MangoScript script, Class<T> clazz) {
        return getInterface(script, clazz, Collections.emptyMap());
    }

    public <T> T getInterface(MangoScript script, Class<T> clazz, Map<String, Object> bindings) {
        ScriptAndEngine scriptAndEngine = getScriptEngine(script);
        ScriptEngine engine = scriptAndEngine.engine;

        if (!(engine instanceof Invocable)) {
            throw new EngineNotInvocableException(engine);
        }

        evalScript(scriptAndEngine, bindings);

        T instance = ((Invocable) engine).getInterface(clazz);
        if (instance == null) {
            throw new ScriptInterfaceException(clazz);
        }

        return permissionService.runAsProxy(script, instance);
    }

}
