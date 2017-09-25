/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 * @author Phillip Dunlap
 */
package com.serotonin.m2m2.rt.script;

import java.util.Map;

public interface ScriptHttpCallback {
    public Object invoke(int status, Map<String, String> headers, String content);
}
