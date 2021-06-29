/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.infiniteautomation.mango.util.script.CompiledMangoJavaScript;
import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.Common;

/**
 *
 * @author Terry Packer
 */
public abstract class MangoJavascriptContextObjectDefinition extends ModuleElementDefinition {
    
    abstract protected Class<? extends ScriptUtility> getUtilityClass();
    
    abstract protected Class<? extends ScriptUtility> getTestUtilityClass();

    public ScriptUtility initializeContextObject(CompiledMangoJavaScript script) {
        Class<? extends ScriptUtility> utilityClass = script.isTestRun() ? getTestUtilityClass() : getUtilityClass();
        //Auto wire this guy
        ScriptUtility utility = Common.getRuntimeContext().getAutowireCapableBeanFactory().createBean(utilityClass);
        utility.setPermissions(script.getPermissionHolder());
        utility.setScriptEngine(script.getEngine());
        return utility;
    }

}
