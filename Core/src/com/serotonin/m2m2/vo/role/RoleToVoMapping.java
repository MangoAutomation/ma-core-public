/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.role;

import org.apache.commons.lang3.StringUtils;

/**
 * Container for role to vo permission mappings
 * @author Terry Packer
 *
 */
public class RoleToVoMapping {

    private final int roleId;
    private final int voId;
    private final String voType;
    private final String permissionType;
    
    /**
     * @param roleId
     * @param voId
     * @param voType
     * @param permissionType
     */
    public RoleToVoMapping(int roleId, int voId, String voType, String permissionType) {
        super();
        this.roleId = roleId;
        this.voId = voId;
        this.voType = voType;
        this.permissionType = permissionType;
    }
    
    public int getRoleId() {
        return roleId;
    }
    
    public int getVoId() {
        return voId;
    }
    
    public String getVoType() {
        return voType;
    }
    
    public String getPermissionType() {
        return permissionType;
    }
    
    /**
     * Is this mapping for a given vo type?
     * 
     * @param clazz
     * @return
     */
    public boolean isForVoType(Class<?> clazz) {
        if(StringUtils.equals(clazz.getSimpleName(), voType)) {
            return true;
        }else {
            return false;
        }
    }
    
}
