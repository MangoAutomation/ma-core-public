/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.permission;

import java.util.Collections;
import java.util.Set;

/**
 * Container for a Mango Permission such as 'permissionDatasource' or  'permissions.user.editSelf'
 *   not to be confused with a role which are defined for these permissions.
 *
 * This class is sorted/ordered and equaled on the typeName only
 *
 * @author Terry Packer
 *
 */
public class Permission {

    private String typeName;
    private Set<String> roles;

    public Permission(String typeName, Set<String> roles) {
        this.typeName = typeName;
        this.roles = Collections.unmodifiableSet(roles);
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = Collections.unmodifiableSet(roles);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
        Permission other = (Permission) obj;
        if (typeName == null) {
            if (other.typeName != null)
                return false;
        } else if (!typeName.equals(other.typeName))
            return false;
        return true;
    }

}
