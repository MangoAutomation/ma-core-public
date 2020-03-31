/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.permission;

import java.util.Set;

import com.serotonin.m2m2.vo.role.Role;

/**
 * Container for a set of roles that apply to a permission such as 'permissionDatasource' or  'permissions.user.editSelf'
 *   or a specific VO permission such as data point set permission.
 *
 * This class is sorted/ordered and equaled on the permissionType only
 *
 * @author Terry Packer
 *
 */
public class MangoPermission {

    private final String permissionType;
    private final String voType;
    private final Integer voId;
    private final Set<Role> roles;

    /**
     *
     * @param permissionType
     * @param roles
     */
    public MangoPermission(String permissionType, Set<Role> roles) {
        super();
        this.permissionType = permissionType;
        this.voType = null;
        this.voId = null;
        this.roles = roles;
    }

    /**
     * @param permissionType
     * @param voType
     * @param voId
     * @param roles
     */
    public MangoPermission(String permissionType, String voType, Integer voId, Set<Role> roles) {
        super();
        this.permissionType = permissionType;
        this.voType = voType;
        this.voId = voId;
        this.roles = roles;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public String getVoType() {
        return voType;
    }

    public Integer getVoId() {
        return voId;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((permissionType == null) ? 0 : permissionType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MangoPermission other = (MangoPermission) obj;
        if (permissionType == null) {
            if (other.permissionType != null)
                return false;
        } else if (!permissionType.equals(other.permissionType))
            return false;
        return true;
    }
}
