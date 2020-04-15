/*
 * Copyright (C) 2020 Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
public interface MangoScript extends PermissionHolder {
    Map<String, Object> getBindings();
    String getEngineName();
    String getScriptName();
    Reader readScript() throws IOException;

    @Override
    default String getPermissionHolderName() {
        return this.getScriptName();
    }

    @Override
    default boolean isPermissionHolderDisabled() {
        return false;
    }
}
