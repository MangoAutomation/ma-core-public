/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.permission;

import java.util.Set;

/**
 * Something that holds permissions, typically a user. Could however in the future be groups, data source scripts etc.
 *
 * @author Jared Wiltshire
 */
public interface PermissionHolder {
    /**
     * Use getPermissionsSet() instead
     *
     * @return Comma separated list of permissions
     */
    @Deprecated
    String getPermissions();

    /**
     * @return a name for the permission holder, typically the username
     */
    String getPermissionHolderName();
    
    /**
     * @return the id of the permission holder, useful if created items have an 'owner'
     */
    default Integer getPermissionHolderId() {
        return null;
    }
    
    /**
     * @return true if permission holder is disabled (i.e. all permission checks should fail)
     */
    boolean isPermissionHolderDisabled();

    default Set<String> getPermissionsSet() {
        return Permissions.explodePermissionGroups(this.getPermissions());
    }

    default boolean hasAdminPermission() {
        return Permissions.hasAdminPermission(this);
    }

    default boolean hasSinglePermission(String requiredPermission) {
        return Permissions.hasSinglePermission(this, requiredPermission);
    }

    default boolean hasAnyPermission(Set<String> requiredPermissions) {
        return Permissions.hasAnyPermission(this, requiredPermissions);
    }

    default boolean hasAllPermissions(Set<String> requiredPermissions) {
        return Permissions.hasAllPermissions(this, requiredPermissions);
    }

    default void ensureHasAdminPermission() {
        Permissions.ensureHasAdminPermission(this);
    }

    default void ensureHasSinglePermission(String requiredPermission) {
        Permissions.ensureHasSinglePermission(this, requiredPermission);
    }

    default void ensureHasAnyPermission(Set<String> requiredPermissions) {
        Permissions.ensureHasAnyPermission(this, requiredPermissions);
    }

    default void ensureHasAllPermissions(Set<String> requiredPermissions) {
        Permissions.ensureHasAllPermissions(this, requiredPermissions);
    }
}
