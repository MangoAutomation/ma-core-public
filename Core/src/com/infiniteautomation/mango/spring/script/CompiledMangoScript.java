/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import javax.script.CompiledScript;

import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class CompiledMangoScript implements MangoScript {

    final CompiledScript compiled;
    final String scriptName;
    final String scriptFilename;
    final Set<Role> roles;

    CompiledMangoScript(CompiledScript compiled, MangoScript source) {
        this.compiled = compiled;
        this.scriptName = source.getScriptName();
        this.scriptFilename = source.getScriptFilename();
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

    @Override
    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public String getScriptFilename() {
        return scriptFilename;
    }

}
