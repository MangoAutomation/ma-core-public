/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
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
 *
 */
@Service
public class RoleService extends AbstractVOService<RoleVO, RoleTableDefinition, RoleDao> {

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
        return permissionService.isValidPermissionHolder(user);
    }


    @Override
    public RoleVO delete(RoleVO vo)
            throws PermissionException, NotFoundException {
        //Cannot delete the 'user' or 'superadmin' roles
        if(StringUtils.equalsIgnoreCase(vo.getXid(), getSuperadminRole().getXid())) {
            PermissionHolder user = Common.getUser();
            throw new PermissionException(new TranslatableMessage("roles.cannotAlterSuperadminRole"), user);
        }else if(StringUtils.equalsIgnoreCase(vo.getXid(), getUserRole().getXid())) {
            PermissionHolder user = Common.getUser();
            throw new PermissionException(new TranslatableMessage("roles.cannotAlterUserRole"), user);
        }else if(StringUtils.equalsIgnoreCase(vo.getXid(), getAnonymousRole().getXid())) {
            PermissionHolder user = Common.getUser();
            throw new PermissionException(new TranslatableMessage("roles.cannotAlterAnonymousRole"), user);
        }
        return super.delete(vo);
    }

    @Override
    public ProcessResult validate(RoleVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);

        //Don't allow the use of role 'user' or 'superadmin'
        if(StringUtils.equalsIgnoreCase(vo.getXid(), getSuperadminRole().getXid())) {
            result.addContextualMessage("xid", "roles.cannotAlterSuperadminRole");
        }
        if(StringUtils.equalsIgnoreCase(vo.getXid(), getUserRole().getXid())) {
            result.addContextualMessage("xid", "roles.cannotAlterUserRole");
        }
        if(StringUtils.equalsIgnoreCase(vo.getXid(), getAnonymousRole().getXid())) {
            result.addContextualMessage("xid", "roles.cannotAlterAnonymousRole");
        }

        //Don't allow spaces in the XID
        Matcher matcher = Functions.WHITESPACE_PATTERN.matcher(vo.getXid());
        if(matcher.find()) {
            result.addContextualMessage("xid", "validate.role.noSpaceAllowed");
        }


        return result;
    }

    @Override
    public ProcessResult validate(RoleVO existing, RoleVO vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        if(!StringUtils.equals(existing.getXid(), vo.getXid())) {
            result.addContextualMessage("xid", "validate.role.cannotChangeXid");
        }
        return result;
    }

    public ProcessResult commonValidation(RoleVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        //Ensure inherited roles exist and they are not us
        if(vo.getInherited() != null) {
            for(Role role : vo.getInherited()) {
                if(dao.getXidById(role.getId()) == null) {
                    result.addContextualMessage("inherited", "validate.role.notFound", role.getXid());
                }else if(role.getId() == vo.getId()){
                    result.addContextualMessage("inherited", "validate.role.cannotInheritSelf", role.getXid());
                }
            }
        }
        return result;
    }
    /**
     * Get the superadmin role
     * @return
     */
    public Role getSuperadminRole() {
        return PermissionHolder.SUPERADMIN_ROLE;
    }

    /**
     * Get the default user role
     * @return
     */
    public Role getUserRole() {
        return PermissionHolder.USER_ROLE;
    }

    /**
     * Get the default anonymous role
     * @return
     */
    public Role getAnonymousRole() {
        return PermissionHolder.ANONYMOUS_ROLE;
    }
}
