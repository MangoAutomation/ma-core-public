/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Service
public class PermissionService {

    public static final String READ = "READ";
    public static final String EDIT = "EDIT";
    public static final String DELETE = "DELETE";
    public static final String SET = "SET";
    
    private final RoleDao roleDao;
    
    @Autowired
    public PermissionService(RoleDao roleDao) {
        this.roleDao = roleDao;
    }
    
    /**
     * Does this permission holder have any role defined in this permission?
     * @param holder
     * @param createPermissionDefinition
     * @return
     */
    public boolean hasPermission(PermissionHolder holder, PermissionDefinition permission) {
        List<RoleVO> roles = roleDao.getRoles(permission.getPermissionTypeName());
        return hasAnyRole(holder, roles);
    }
    
    /**
     * Does this permission holder have any role for the permission 
     *  type on this vo?
     *  
     * @param holder
     * @param vo
     * @param permissionType
     * @return
     */
    public boolean hasPermission(PermissionHolder holder, AbstractVO<?> vo, String permissionType) {
        List<RoleVO> roles = roleDao.getRoles(vo, permissionType);
        return hasAnyRole(holder, roles);
    }
    
    /**
     * Does this permission holder have at least one of the required roles
     * @param user
     * @param requiredRoles
     * @return
     */
    public boolean hasAnyRole(PermissionHolder user, List<RoleVO> requiredRoles) {
        if (!isValidPermissionHolder(user)) return false;

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsAnyRole(heldPermissions, requiredRoles);
    }
    
    /**
     * Does this permission holder have this exact role
     * @param user
     * @param requiredRole
     * @return
     */
    public boolean hasSingleRole(PermissionHolder user, RoleVO requiredRole) {
        if (!isValidPermissionHolder(user)) return false;

        Set<String> heldPermissions = user.getPermissionsSet();
        return containsSingleRole(heldPermissions, requiredRole);
    }
    
    /**
     * Is this permission holder valid
     * - must be non null
     * - must not disabled
     * 
     * @param user
     * @return
     */
    public boolean isValidPermissionHolder(PermissionHolder user) {
        return !(user == null || user.isPermissionHolderDisabled());
    }
    
    //TODO Add permission validation method from Permissions
    
    /**
     * Is this required role in the held roles? 
     * @param heldRoles
     * @param requiredRole
     * @return
     */
    private boolean containsSingleRole(Set<String> heldRoles, RoleVO requiredRole) {
        if (heldRoles.contains(SuperadminPermissionDefinition.GROUP_NAME)) {
            return true;
        }

        // empty permissions string indicates that only superadmins are allowed access
        if (requiredRole == null) {
            return false;
        }

        return heldRoles.contains(requiredRole.getXid());
    }

    /**
     * Is every required role in the held roles?
     * @param heldRoles
     * @param requiredRoles
     * @return
     */
    private static boolean containsAll(Set<String> heldRoles, List<RoleVO> requiredRoles) {

        if (heldRoles.contains(SuperadminPermissionDefinition.GROUP_NAME)) {
            return true;
        }

        // empty permissions string indicates that only superadmins are allowed access
        if (requiredRoles.isEmpty()) {
            return false;
        }
        
        for(RoleVO role : requiredRoles) {
            if(!heldRoles.contains(role.getXid())) {
               return false; 
            }
        }
        return true;
    }
    
    /**
     * Is any required role in the held roles?
     * @param heldRoles
     * @param requiredRoles
     * @return
     */
    private boolean containsAnyRole(Set<String> heldRoles, List<RoleVO> requiredRoles) {

        if (heldRoles.contains(SuperadminPermissionDefinition.GROUP_NAME)) {
            return true;
        }

        // empty roles indicates that only superadmins are allowed access
        if (requiredRoles.isEmpty()) {
            return false;
        }

        for (RoleVO requiredRole : requiredRoles) {
            if (heldRoles.contains(requiredRole.getXid())) {
                return true;
            }
        }

        return false;
    }
}
