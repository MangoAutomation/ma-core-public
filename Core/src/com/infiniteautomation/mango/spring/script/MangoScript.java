/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.script;

import java.io.IOException;
import java.io.Reader;

import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Jared Wiltshire
 */
public interface MangoScript extends PermissionHolder {

    /**
     * @return name of the JSR223 script engine to use to execute the script
     */
    String getEngineName();

    /**
     * @return the name of the script, used for logging and for the permission holder name (by default)
     */
    String getScriptName();

    /**
     * @return the full path of the script being executed, can be null if the script is not from a file
     */
    String getScriptFilename();


    /**
     * @return Reader for the script, can be from a file or from a string
     * @throws IOException
     */
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
