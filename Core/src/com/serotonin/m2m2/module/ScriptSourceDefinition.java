package com.serotonin.m2m2.module;

import java.util.List;

/**
 * Used by modules that wish to declare scripts to be globally available to script engines. Currently supports
 * ECMAScript (Javascript).
 */
abstract public class ScriptSourceDefinition extends ModuleElementDefinition {
    abstract public List<String> getScripts();
}
