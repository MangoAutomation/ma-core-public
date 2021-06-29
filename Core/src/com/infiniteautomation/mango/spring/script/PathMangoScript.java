/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public class PathMangoScript implements MangoScript {

    final String engineName;
    final Set<Role> roles;
    final Path scriptPath;
    final Charset charset;

    public PathMangoScript(String engineName, Set<Role> roles, Path scriptPath) {
        this(engineName, roles, scriptPath, StandardCharsets.UTF_8);
    }

    public PathMangoScript(String engineName, Set<Role> roles, Path scriptPath, Charset charset) {
        this.engineName = engineName;
        this.roles = Collections.unmodifiableSet(roles);
        this.scriptPath = scriptPath;
        this.charset = charset;
    }

    @Override
    public Reader readScript() throws IOException {
        return Files.newBufferedReader(scriptPath, charset);
    }

    @Override
    public String getScriptName() {
        return scriptPath.getFileName().toString();
    }

    @Override
    public String getEngineName() {
        return engineName;
    }

    public Path getScriptPath() {
        return scriptPath;
    }

    public Charset getCharset() {
        return charset;
    }

    @Override
    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public String getScriptFilename() {
        return scriptPath.toAbsolutePath().normalize().toString();
    }

}
