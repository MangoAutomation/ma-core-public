/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Jared Wiltshire
 */
public interface MangoScript {

    Map<String, Object> getBindings();
    Set<Role> getRoles();
    String getEngineName();
    String getScriptName();
    Reader readScript() throws IOException;

}
