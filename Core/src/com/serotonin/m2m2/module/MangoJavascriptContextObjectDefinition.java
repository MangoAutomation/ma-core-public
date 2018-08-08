/**
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.module;

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
    
    /**
     * Get the object to use in the context
     * @return
     */
    abstract public Object getContextObject(PermissionHolder holder);
}
