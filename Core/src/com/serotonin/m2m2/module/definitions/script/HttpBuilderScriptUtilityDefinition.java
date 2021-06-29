/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
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
    protected Class<? extends ScriptUtility> getUtilityClass() {
        return HttpBuilderScriptUtility.class;
    }

    @Override
    protected Class<? extends ScriptUtility> getTestUtilityClass() {
        return HttpBuilderScriptUtility.class;
    }

}
