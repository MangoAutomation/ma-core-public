/*
 * Copyright (C) 2018 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.vo.permission;

import java.util.Collections;
import java.util.Set;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.LazyInitializer;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Something that holds permissions, typically a user. Could however in the future be groups, data source scripts etc.
 *
 * @author Jared Wiltshire
 */
public interface PermissionHolder {

    public static final String SUPERADMIN_ROLE_XID = "superadmin";
    public static final String USER_ROLE_XID = "user";
    
    /**
     * The superadmin role from the database upon initialization
     */
    public static final LazyInitSupplier<Role> SUPERADMIN_ROLE = new LazyInitSupplier<>(()-> {
        return RoleDao.getInstance().getByXid(SUPERADMIN_ROLE_XID).getRole();
    });

    /**
     * The user role from the database upon initialization
     */
    public static final LazyInitSupplier<Role> USER_ROLE = new LazyInitSupplier<>(()-> {
        return RoleDao.getInstance().getByXid(USER_ROLE_XID).getRole();
    });
    
    /**
     * Represents the Mango system and is a member of the superadmin role. This PermissionHolder should only be used in scenarios
     * where the code needs to use a service that requires a PermissionHolder but there is no user currently logged in. i.e. for
     * background processes or operations such as resetting a user's password.
     *
     * Note: When working in a Spring service or controller prefer injecting
     * @Qualifier(SYSTEM_SUPERADMIN_PERMISSION_HOLDER) PermissionHolder
     */
    public static final PermissionHolder SYSTEM_SUPERADMIN = new PermissionHolder() {
        private final LazyInitializer<Set<Role>> roles = new LazyInitializer<>();
        
        @Override
        public String getPermissionHolderName() {
            return "SYSTEM_SUPERADMIN";
        }

        @Override
        public boolean isPermissionHolderDisabled() {
            return false;
        }
        
        @Override
        public Set<Role> getRoles() {
            return roles.get(() -> {
                return Collections.singleton(SUPERADMIN_ROLE.get());
            });
        }
    };

    static final LazyInitSupplier<PermissionService> permissionService = new LazyInitSupplier<>(() -> {
        return Common.getBean(PermissionService.class);
    });
    
    /**
     * @return a name for the permission holder, typically the username
     */
    String getPermissionHolderName();

    /**
     * @return true if permission holder is disabled (i.e. all permission checks should fail)
     */
    boolean isPermissionHolderDisabled();
    
    /**
     * The roles for this permission holder 
     * @return
     */
    Set<Role> getRoles();

    default boolean hasAdminRole() {
        return permissionService.get().hasAdminRole(this);
    }
    
    /**
     * Is the exact required role in the permission holder's roles? 
     *  NOTE: superadmin must have this role for this to be true for them. i.e. they are not treated
     *  specially
     * 
     * @param requiredRole
     * @return
     */
    default boolean hasSingleRole(Role requiredRole) {
        return permissionService.get().hasSingleRole(this, requiredRole);
    }

    default boolean hasAnyRole(Set<Role> requiredRoles) {
        return permissionService.get().hasAnyRole(this, requiredRoles);
    }

    default boolean hasAllRoles(Set<Role> requiredRoles) {
        return permissionService.get().hasAllRoles(this, requiredRoles);
    }

    default void ensureHasAdminRole() {
        permissionService.get().ensureAdminRole(this);
    }
    
    /**
     *  Ensure the exact required role is in the permission holder's roles
     *  NOTE: superadmin must have this role for this to be true for them. i.e. they are not treated
     *  specially
     *  
     * @param requiredRole
     */
    default void ensureHasSingleRole(Role requiredRole) {
        permissionService.get().ensureSingleRole(this, requiredRole);
    }
    
    default void ensureHasAnyRole(Set<Role> requiredRoles) {
        permissionService.get().ensureHasAnyRole(this, requiredRoles);
    }

    default void ensureHasAllRoles(Set<Role> requiredRoles) {
        permissionService.get().ensureHasAllRoles(this, requiredRoles);
    }
}
