/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

import com.infiniteautomation.mango.util.script.ScriptUtility;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
public abstract class MangoJavascriptContextObjectDefinition extends ModuleElementDefinition {

    /**
     * Get the context key for the Object in the Context
     * 
     * @return
     */
    abstract public String getContextKey();
    
    abstract protected Class<? extends ScriptUtility> getUtilityClass();
    /**
     * Get the object to use in the context
     * @return
     */
    public ScriptUtility initializeContextObject(PermissionHolder holder) {
        Class<? extends ScriptUtility> utilityClass = getUtilityClass();
        //Auto wire this guy
        ScriptUtility utility = Common.getRuntimeContext().getAutowireCapableBeanFactory().createBean(utilityClass);
        utility.setPermissions(holder);
        return utility;
    }
}
