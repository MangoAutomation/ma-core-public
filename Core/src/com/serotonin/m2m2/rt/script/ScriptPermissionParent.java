/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.rt.script;

import java.util.Set;

import com.infiniteautomation.mango.util.LazyInitializer;
import com.serotonin.m2m2.vo.RoleVO;

/**
 * Work around for combinedPermissions being null after deserialization.
 * Works as the default constructor of the first parent that is not serializable is called during deserialization.
 *
 * @author Jared Wiltshire
 */
public class ScriptPermissionParent {
    protected final LazyInitializer<Set<String>> combinedPermissions;
    protected final LazyInitializer<Set<RoleVO>> combinedRoles;
    
    public ScriptPermissionParent() {
        combinedPermissions = new LazyInitializer<>();
        combinedRoles = new LazyInitializer<>();
    }
}
