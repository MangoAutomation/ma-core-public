/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.Functions;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 * @author Terry Packer
 */
@Service
public class RoleService extends AbstractVOService<RoleVO, RoleDao> {

    @Autowired
    public RoleService(RoleDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, RoleVO vo) {
        return permissionService.hasAdminRole(user);
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, RoleVO vo) {
        if (!permissionService.isValidPermissionHolder(user)) return false;
        Set<Role> heldRoles = permissionService.getAllInheritedRoles(user);
        return heldRoles.contains(PermissionHolder.SUPERADMIN_ROLE) || heldRoles.contains(vo.getRole());
    }

    @Override
    public RoleVO delete(RoleVO vo) throws PermissionException, NotFoundException {
        //Cannot delete the 'user' or 'superadmin' roles
        if (StringUtils.equalsIgnoreCase(vo.getXid(), getSuperadminRole().getXid())) {
            PermissionHolder user = Common.getUser();
            throw new PermissionException(new TranslatableMessage("roles.cannotAlterSuperadminRole"), user);
        } else if (StringUtils.equalsIgnoreCase(vo.getXid(), getUserRole().getXid())) {
            PermissionHolder user = Common.getUser();
            throw new PermissionException(new TranslatableMessage("roles.cannotAlterUserRole"), user);
        } else if (StringUtils.equalsIgnoreCase(vo.getXid(), getAnonymousRole().getXid())) {
            PermissionHolder user = Common.getUser();
            throw new PermissionException(new TranslatableMessage("roles.cannotAlterAnonymousRole"), user);
        }
        return super.delete(vo);
    }

    @Override
    public ProcessResult validate(RoleVO vo, PermissionHolder user) {
        return commonValidation(vo, user);
    }

    @Override
    public ProcessResult validate(RoleVO existing, RoleVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        if (!StringUtils.equals(existing.getXid(), vo.getXid())) {
            result.addContextualMessage("xid", "validate.role.cannotChangeXid");
        }
        return result;
    }

    public @NonNull RoleVO getOrInsert(@NonNull String xid) {
        return getOrInsert(xid, xid);
    }

    public @NonNull RoleVO getOrInsert(@NonNull String xid, @NonNull String name) {
        permissionService.ensureAdminRole(Common.getUser());
        return dao.doInTransaction(tx -> {
            RoleVO role;
            try {
                role = get(xid);
            } catch (NotFoundException e) {
                role = insert(new RoleVO(Common.NEW_ID, xid, name));
            }
            return role;
        });
    }

    public ProcessResult commonValidation(RoleVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);

        //Don't allow the use of role 'user' or 'superadmin'
        if (StringUtils.equalsIgnoreCase(vo.getXid(), getSuperadminRole().getXid())) {
            result.addContextualMessage("xid", "roles.cannotAlterSuperadminRole");
        }
        if (StringUtils.equalsIgnoreCase(vo.getXid(), getUserRole().getXid())) {
            result.addContextualMessage("xid", "roles.cannotAlterUserRole");
        }
        if (StringUtils.equalsIgnoreCase(vo.getXid(), getAnonymousRole().getXid())) {
            result.addContextualMessage("xid", "roles.cannotAlterAnonymousRole");
        }

        //Don't allow spaces in the XID
        Matcher matcher = Functions.WHITESPACE_PATTERN.matcher(vo.getXid());
        if (matcher.find()) {
            result.addContextualMessage("xid", "validate.role.noSpaceAllowed");
        }

        //Ensure inherited roles exist and they are not us and there are no loops
        if (vo.getInherited() != null) {
            Set<Role> used = new HashSet<>();
            used.add(vo.getRole());
            for (Role role : vo.getInherited()) {
                if (dao.getXidById(role.getId()) == null) {
                    result.addContextualMessage("inherited", "validate.role.notFound", role.getXid());
                }
                if (recursivelyCheckForUsedRoles(role, used)) {
                    result.addContextualMessage("inherited", "validate.role.inheritanceLoop", role.getXid());
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Make sure no role is re-used in its hierarchy
     *
     * @param role role to search for
     * @param used seen roles
     * @return - true is role was already used somewhere in its inheritance
     */
    private boolean recursivelyCheckForUsedRoles(Role role, Set<Role> used) {
        if (!used.add(role)) {
            return true;
        }
        Set<Role> inheritance = dao.getFlatInheritance(role);
        for (Role inherited : inheritance) {
            if (!used.add(inherited)) {
                return true;
            }
            recursivelyCheckForUsedRoles(role, used);
        }
        return false;
    }

    /**
     * @return the superadmin role
     */
    public Role getSuperadminRole() {
        return PermissionHolder.SUPERADMIN_ROLE;
    }

    /**
     * @return the user role
     */
    public Role getUserRole() {
        return PermissionHolder.USER_ROLE;
    }

    /**
     * @return the anonymous role
     */
    public Role getAnonymousRole() {
        return PermissionHolder.ANONYMOUS_ROLE;
    }
}
