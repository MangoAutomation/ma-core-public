/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved. 
 *
 */
package com.serotonin.m2m2.module.definitions.script;

import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.rt.script.HttpBuilderScriptUtility;

/**
 *
 * @author Phillip Dunlap
 */
public class HttpBuilderScriptUtilityDefinition extends MangoJavascriptContextObjectDefinition {

    @Override
    public String getContextKey() {
        return HttpBuilderScriptUtility.CONTEXT_KEY;
    }

    @Override
    protected Class<? extends ScriptUtility> getUtilityClass() {
        return HttpBuilderScriptUtility.class;
    }

    @Override
    protected Class<? extends ScriptUtility> getTestUtilityClass() {
        return HttpBuilderScriptUtility.class;
    }

}
