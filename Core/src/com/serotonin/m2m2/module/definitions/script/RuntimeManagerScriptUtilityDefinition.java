/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module.definitions.script;

import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition;
import com.serotonin.m2m2.rt.script.RuntimeManagerScriptTestUtility;
import com.serotonin.m2m2.rt.script.RuntimeManagerScriptUtility;

/**
 *
 * @author Phillip Dunlap
 */
public class RuntimeManagerScriptUtilityDefinition extends MangoJavascriptContextObjectDefinition{
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition#getUtilityClass()
     */
    @Override
    protected Class<? extends ScriptUtility> getUtilityClass() {
        return RuntimeManagerScriptUtility.class;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.module.MangoJavascriptContextObjectDefinition#getTestUtilityClass()
     */
    @Override
    protected Class<? extends ScriptUtility> getTestUtilityClass() {
        return RuntimeManagerScriptTestUtility.class;
    }

}
