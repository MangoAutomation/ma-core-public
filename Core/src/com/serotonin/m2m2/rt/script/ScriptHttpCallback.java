package com.serotonin.m2m2.rt.script;

import java.util.Map;

public interface ScriptHttpCallback {
    public void invoke(int status, Map<String, String> headers, String content);
}
