/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class PathMangoScript implements MangoScript {

    String engineName;
    Path scriptPath;
    Charset charset = StandardCharsets.UTF_8;
    Map<String, Object> bindings = Collections.emptyMap();
    Set<Role> roles = Collections.emptySet();

    public PathMangoScript(String engineName, Path scriptPath) {
        this.engineName = engineName;
        this.scriptPath = scriptPath;
    }

    @Override
    public Reader readScript() throws IOException {
        return Files.newBufferedReader(scriptPath, charset);
    }

    @Override
    public String getScriptName() {
        return scriptPath.toAbsolutePath().normalize().toString();
    }

    @Override
    public String getPermissionHolderName() {
        return scriptPath.getFileName().toString();
    }

    @Override
    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public Path getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(Path scriptPath) {
        this.scriptPath = scriptPath;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
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
