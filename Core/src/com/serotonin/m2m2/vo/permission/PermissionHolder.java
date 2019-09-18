/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.permission;

import java.util.Collections;
import java.util.Set;

import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;

/**
 * Something that holds permissions, typically a user. Could however in the future be groups, data source scripts etc.
 *
 * @author Jared Wiltshire
 */
public interface PermissionHolder {

    /**
     * Represents the Mango system and is a member of the superadmin role. This PermissionHolder should only be used in scenarios
     * where the code needs to use a service that requires a PermissionHolder but there is no user currently logged in. i.e. for
     * background processes or operations such as resetting a user's password.
     *
     * Note: When working in a Spring service or controller prefer injecting
     * @Qualifier(SYSTEM_SUPERADMIN_PERMISSION_HOLDER) PermissionHolder
     */
    public static final PermissionHolder SYSTEM_SUPERADMIN = new PermissionHolder() {
        private final Set<String> permissions = Collections.singleton(SuperadminPermissionDefinition.GROUP_NAME);

        @Override
        public String getPermissionHolderName() {
            return "SYSTEM_SUPERADMIN";
        }

        @Override
        public boolean isPermissionHolderDisabled() {
            return false;
        }

        @Override
        public Set<String> getPermissionsSet() {
            return permissions;
        }
    };

    /**
     * @return a name for the permission holder, typically the username
     */
    String getPermissionHolderName();

    /**
     * @return true if permission holder is disabled (i.e. all permission checks should fail)
     */
    boolean isPermissionHolderDisabled();

    Set<String> getPermissionsSet();

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
