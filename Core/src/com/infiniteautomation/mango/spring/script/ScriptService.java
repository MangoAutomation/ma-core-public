/*
 * Copyright (C) 2019 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.script.MangoScriptException.EngineNotCompilableException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.EngineNotFoundException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.EngineNotInvocableException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.NoDefinitionForFactory;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptEvalException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptIOException;
import com.infiniteautomation.mango.spring.script.MangoScriptException.ScriptInterfaceException;
import com.infiniteautomation.mango.spring.script.permissions.ScriptServicesPermissionDefinition;
import com.infiniteautomation.mango.spring.service.AbstractVOService;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ScriptEngineDefinition;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
@Service
public class ScriptService {

    final ScriptEngineManager manager;
    final PermissionService permissionService;
    final ApplicationContext context;
    final ScriptServicesPermissionDefinition scriptServicesPermission;
    final List<ScriptEngineDefinition> engineDefinitions;

    @Autowired
    public ScriptService(ScriptEngineManager manager, PermissionService permissionService, ApplicationContext context,
            ScriptServicesPermissionDefinition scriptServicesPermission, List<ScriptEngineDefinition> engineDefinitions) {
        this.manager = manager;
        this.permissionService = permissionService;
        this.context = context;
        this.scriptServicesPermission = scriptServicesPermission;
        this.engineDefinitions = engineDefinitions;
    }

    private ScriptEngine getEngineByName(String engineName) {
        ScriptEngine engine = manager.getEngineByName(engineName);
        if (engine == null) {
            List<String> names = new ArrayList<>();
            manager.getEngineFactories().stream().forEach(f -> names.addAll(f.getNames()));
            throw new EngineNotFoundException(engineName, names);
        }
        return engine;
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
            MangoPermission permission = definitionForFactory(f).accessPermission();
            return permissionService.hasPermission(user, permission);
        }).collect(Collectors.toList());
    }

    public CompiledMangoScript compile(MangoScript script) {
        if (script instanceof CompiledMangoScript) {
            return (CompiledMangoScript) script;
        }

        ScriptEngine engine = getScriptEngine(script);

        if (!(engine instanceof Compilable)) {
            throw new EngineNotCompilableException(engine);
        }
        Compilable compilableEngine = (Compilable) engine;

        try (Reader reader = script.readScript()) {
            return new CompiledMangoScript(compilableEngine.compile(reader), script);
        } catch (ScriptException e) {
            throw new ScriptEvalException(e);
        } catch (IOException e) {
            throw new ScriptIOException(e);
        }
    }

    public ScriptEngineDefinition definitionForScript(MangoScript script) {
        ScriptEngine engine;
        if (script instanceof CompiledMangoScript) {
            engine = ((CompiledMangoScript) script).compiled.getEngine();
        } else {
            engine = getEngineByName(script.getEngineName());
        }

        return definitionForFactory(engine.getFactory());
    }

    private ScriptEngine getScriptEngine(MangoScript script) {
        ScriptEngine engine;
        if (script instanceof CompiledMangoScript) {
            engine = ((CompiledMangoScript) script).compiled.getEngine();
        } else {
            engine = getEngineByName(script.getEngineName());
        }

        ScriptEngineDefinition definition = definitionForFactory(engine.getFactory());

        PermissionHolder user = Common.getUser();
        permissionService.ensurePermission(user, definition.accessPermission());
        permissionService.ensureHasAllRoles(user, script.getRoles());

        return engine;
    }

    private Object evalScript(MangoScript script, ScriptEngine engine) {
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

    private void configureBindings(MangoScript script, ScriptEngine engine, Map<String, Object> bindings) {
        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        ScriptEngineDefinition definition = definitionForFactory(engine.getFactory());
        definition.applyRestrictions(engine, script);

        Logger log = LoggerFactory.getLogger("script." + script.getScriptName());
        engineBindings.put("log", log);

        if (permissionService.hasPermission(script, scriptServicesPermission.getPermission())) {
            @SuppressWarnings("rawtypes")
            Map<String, AbstractVOService> services = context.getBeansOfType(AbstractVOService.class);
            engineBindings.put("services", services);
        }

        String scriptName = script.getScriptName();
        if (scriptName != null) {
            engineBindings.put(ScriptEngine.FILENAME, scriptName);
        }
        engineBindings.putAll(bindings);
    }

    public Object eval(MangoScript script) {
        return eval(script, Collections.emptyMap());
    }

    public Object eval(MangoScript script, Map<String, Object> bindings) {
        ScriptEngine engine = getScriptEngine(script);
        configureBindings(script, engine, bindings);
        return evalScript(script, engine);
    }

    public <T> T getInterface(MangoScript script, Class<T> clazz) {
        return getInterface(script, clazz, Collections.emptyMap());
    }

    public <T> T getInterface(MangoScript script, Class<T> clazz, Map<String, Object> bindings) {
        ScriptEngine engine = getScriptEngine(script);
        if (!(engine instanceof Invocable)) {
            throw new EngineNotInvocableException(engine);
        }
        Invocable invocableEngine = (Invocable) engine;

        configureBindings(script, engine, bindings);
        evalScript(script, engine);

        T instance = invocableEngine.getInterface(clazz);
        if (instance == null) {
            throw new ScriptInterfaceException(clazz);
        }
        return instance;
    }

}
