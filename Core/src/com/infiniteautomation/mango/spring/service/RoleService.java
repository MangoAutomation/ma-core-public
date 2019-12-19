/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.vo.AbstractVO;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Service
public class RoleService extends AbstractVOService<RoleVO, RoleDao> {
    
    //To check role for spaces
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s");
    
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
    public ProcessResult validate(RoleVO vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        
        //Don't allow the use of role 'user' or 'superadmin'
        if(StringUtils.equalsIgnoreCase(vo.getXid(), RoleDao.SUPERADMIN_ROLE_NAME)) {
            result.addContextualMessage("xid", "roles.cannotAlterSuperadminRole");
        }
        if(StringUtils.equalsIgnoreCase(vo.getXid(), RoleDao.USER_ROLE_NAME)) {
            result.addContextualMessage("xid", "roles.cannotAlterUserRole");
        }
        
        //Don't allow spaces in the XID
        Matcher matcher = SPACE_PATTERN.matcher(vo.getXid());
        if(matcher.find()) {
            result.addContextualMessage("xid", "validate.role.noSpaceAllowed");
        }
        return result;
    }
    
    /**
     * Add a role to a permission
     * @param voId
     * @param role
     * @param permissionType
     * @param user
     */
    public void addRoleToVoPermission(RoleVO role, AbstractVO<?> vo, String permissionType, PermissionHolder user) {
        //TODO PermissionHolder check?
        // Superadmin ok
        // holder must contain the role already?
        this.dao.addRoleToVoPermission(role, vo, permissionType);
    }    
    
}
