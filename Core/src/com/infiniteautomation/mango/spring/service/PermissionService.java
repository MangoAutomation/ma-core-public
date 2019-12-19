/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.module.definitions.permissions.SuperadminPermissionDefinition;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Terry Packer
 *
 */
@Service
public class PermissionService {
    
    //Permission Types
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
     * Does this user have the superadmin role?
     * @param holder
     * @return
     */
    public boolean hasAdminRole(PermissionHolder holder) {
        return hasSingleRole(holder, roleDao.getSuperadminRole());
    }
    
    /**
     * Does this permission holder have any role defined in this permission?
     * @param holder
     * @param createPermissionDefinition
     * @return
     */
    public boolean hasPermission(PermissionHolder holder, PermissionDefinition permission) {
        Set<RoleVO> roles = roleDao.getRoles(permission.getPermissionTypeName());
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
        Set<RoleVO> roles = roleDao.getRoles(vo, permissionType);
        return hasAnyRole(holder, roles);
    }
    
    /**
     * Does this permission holder have at least one of the required roles
     * @param user
     * @param requiredRoles
     * @return
     */
    public boolean hasAnyRole(PermissionHolder user, Set<RoleVO> requiredRoles) {
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
    private static boolean containsAll(Set<String> heldRoles, Set<RoleVO> requiredRoles) {

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
    private boolean containsAnyRole(Set<String> heldRoles, Set<RoleVO> requiredRoles) {

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

    /**
     * Validate roles.  This will validate that:
     *
     *   1. the new permissions are non null
     *   2. all new permissions are not empty
     *   3. the new permissions do not contain spaces
     *   (then for non admin/owners)
     *   4. the saving user will at least retain one permission
     *   5. the user cannot not remove an existing permission they do not have
     *   6. the user has all of the new permissions being added
     *
     *   If the saving user is also the owner, then the new permissions need not contain
     *   one of the user's roles
     *
     * @param result - the result of the validation
     * @param contextKey - the key to apply the messages to
     * @param holder - the saving permission holder
     * @param savedByOwner - is the saving user the owner of this item (use false if no owner is possible)
     * @param existingRoles - the currently saved permissions
     * @param newRoles - the new permissions to validate
     */
    public void validateVoRoles(ProcessResult result, String contextKey, PermissionHolder holder,
            boolean savedByOwner, Set<RoleVO> existingRoles, Set<RoleVO> newRoles) {
        if (holder == null) {
            result.addContextualMessage(contextKey, "validate.userRequired");
            return;
        }

        if(newRoles == null) {
            result.addContextualMessage(contextKey, "validate.invalidValue");
            return;
        }

        for (RoleVO role : newRoles) {
            if (role == null) {
                result.addContextualMessage(contextKey, "validate.role.empty");
                return;
            }else if(RoleDao.getInstance().getIdByXid(role.getXid()) == null) {
                result.addContextualMessage(contextKey, "validate.role.notFound", role.getXid());
            }
        }
        
        if(holder.hasAdminPermission())
            return;

        //Ensure the holder has at least one of the new permissions
        if(!savedByOwner && Collections.disjoint(holder.getPermissionsSet(), newRoles)) {
            result.addContextualMessage(contextKey, "validate.mustRetainPermission");
        }

        if(existingRoles != null) {
            //Check for permissions being added that the user does not have
            Set<RoleVO> added = new HashSet<>(newRoles);
            added.removeAll(existingRoles);
            added.removeAll(holder.getRoles());
            if(added.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(holder.getRoles()));
            }
            //Check for permissions being removed that the user does not have
            Set<RoleVO> removed = new HashSet<>(existingRoles);
            removed.removeAll(newRoles);
            removed.removeAll(holder.getRoles());
            if(removed.size() > 0) {
                result.addContextualMessage(contextKey, "validate.role.invalidModification", implodeRoles(holder.getRoles()));
            }
        }
        return;
    }
    
    /**
     * Turn a set of roles into a comma separated list for display in a message
     * @param roles
     * @return
     */
    public String implodeRoles(Set<RoleVO> roles) {
        return String.join(",", roles.stream().map(role -> role.getXid()).collect(Collectors.toSet()));
    }
}
