/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import javax.script.ScriptException;

@FunctionalInterface
public interface ScriptExceptionCallback {
    Object exception(Exception e) throws ScriptException;
}
