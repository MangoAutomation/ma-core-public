/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.script.CompiledScript;

import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class CompiledMangoScript implements MangoScript {

    final CompiledScript compiled;
    String scriptName;
    Map<String, Object> bindings = Collections.emptyMap();
    Set<Role> roles = Collections.emptySet();

    CompiledMangoScript(CompiledScript compiled, MangoScript source) {
        this.compiled = compiled;
        this.scriptName = source.getScriptName();
        this.bindings = source.getBindings();
        this.roles = source.getRoles();
    }

    @Override
    public String getEngineName() {
        return compiled.getEngine().getFactory().getEngineName();
    }

    @Override
    public Reader readScript() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    public Map<String, Object> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    @Override
    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

}
