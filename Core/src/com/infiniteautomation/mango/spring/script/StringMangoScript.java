/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class StringMangoScript implements MangoScript {

    final String engineName;
    final String scriptName;
    final Set<Role> roles;
    final String script;

    public StringMangoScript(String engineName, String scriptName, Set<Role> roles, String script) {
        this.engineName = engineName;
        this.scriptName = scriptName;
        this.roles = Collections.unmodifiableSet(roles);
        this.script = script;
    }

    @Override
    public Reader readScript() throws IOException {
        return new StringReader(script);
    }

    @Override
    public String getEngineName() {
        return engineName;
    }

    @Override
    public String getScriptName() {
        return scriptName;
    }

    public String getScript() {
        return script;
    }

    @Override
    public Set<Role> getAllInheritedRoles() {
        return roles;
    }

    @Override
    public String getScriptFilename() {
        return null;
    }

}