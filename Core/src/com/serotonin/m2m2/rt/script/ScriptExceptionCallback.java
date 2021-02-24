/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.rt.script;

import javax.script.ScriptException;

@FunctionalInterface
public interface ScriptExceptionCallback {
    Object exception(Exception e) throws ScriptException;
}
