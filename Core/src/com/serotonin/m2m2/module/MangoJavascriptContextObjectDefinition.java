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
    
    abstract protected Class<? extends ScriptUtility> getUtilityClass();
    
    abstract protected Class<? extends ScriptUtility> getTestUtilityClass();
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
    
    public ScriptUtility initializeTestContextObject(PermissionHolder holder) {
        Class<? extends ScriptUtility> utilityClass = getTestUtilityClass();
        //Auto wire this guy
        ScriptUtility utility = Common.getRuntimeContext().getAutowireCapableBeanFactory().createBean(utilityClass);
        utility.setPermissions(holder);
        return utility;
    }
}
