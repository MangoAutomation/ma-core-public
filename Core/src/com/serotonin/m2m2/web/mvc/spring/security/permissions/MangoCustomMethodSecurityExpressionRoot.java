/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 */
package com.serotonin.m2m2.web.mvc.spring.security.permissions;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Class to define Custom Spring EL Expressions for use in @PreAuthorize and @PostAuthorize annotations
 *
 * @author Terry Packer
 */
public class MangoCustomMethodSecurityExpressionRoot extends SecurityExpressionRoot
        implements MethodSecurityExpressionOperations {

    private final PermissionService permissionService;

    public MangoCustomMethodSecurityExpressionRoot(Authentication authentication, PermissionService permissionService) {
        super(authentication);
        this.permissionService = permissionService;
    }

    private <T> T checkNull(T item) {
        if (item == null) {
            throw new NotFoundException();
        }
        return item;
    }

    /**
     * Is this User an admin?
     *
     * @return true if user is superadmin
     */
    public boolean isAdmin() {
        if (!(this.getPrincipal() instanceof PermissionHolder)) {
            return false;
        }

        PermissionHolder user = (PermissionHolder) this.getPrincipal();
        return permissionService.hasAdminRole(user);
    }

    /**
     * Does this User have edit access for this data source
     *
     * @param xid
     * @return
     */
    public boolean hasDataSourceXidPermission(String xid) {
        if (!(this.getPrincipal() instanceof PermissionHolder)) {
            return false;
        }

        PermissionHolder user = (PermissionHolder) this.getPrincipal();
        DataSourceVO dsvo = checkNull(DataSourceDao.getInstance().getByXid(xid));
        return permissionService.hasPermission(user, dsvo.getEditPermission());
    }

    /**
     * Checks if a user is granted a permission
     *
     * @param permissionName System setting key for the granted permission
     * @return
     */
    public boolean isGrantedPermission(String permissionName) {
        if (!(this.getPrincipal() instanceof PermissionHolder)) {
            return false;
        }

        PermissionDefinition def = checkNull(ModuleRegistry.getPermissionDefinition(permissionName));
        return permissionService.hasPermission((PermissionHolder) this.getPrincipal(), def.getPermission());
    }

    /**
     * Does a user have data point read permissions?
     *
     * @param xid
     * @return
     */
    public boolean hasDataPointXidReadPermission(String xid) {
        if (!(this.getPrincipal() instanceof PermissionHolder)) {
            return false;
        }

        PermissionHolder user = (PermissionHolder) this.getPrincipal();
        DataPointVO vo = checkNull(DataPointDao.getInstance().getByXid(xid));
        return permissionService.hasPermission(user, vo.getReadPermission());
    }

    /**
     * Does the user have read permissions to every data point in the list?
     *
     * @param xids
     * @return
     */
    public boolean hasDataPointXidsReadPermission(String[] xids) {
        if (!(this.getPrincipal() instanceof PermissionHolder)) {
            return false;
        }

        PermissionHolder user = (PermissionHolder) this.getPrincipal();
        for (String xid : xids) {
            DataPointVO vo = checkNull(DataPointDao.getInstance().getByXid(xid));
            if (!permissionService.hasPermission(user, vo.getReadPermission()))
                return false;
        }
        return true;
    }

    /**
     * Does a user have data point set permissions?
     *
     * @param xid
     * @return
     */
    public boolean hasDataPointXidSetPermission(String xid) {
        if (!(this.getPrincipal() instanceof PermissionHolder)) {
            return false;
        }

        PermissionHolder user = (PermissionHolder) this.getPrincipal();
        DataPointVO vo = checkNull(DataPointDao.getInstance().getByXid(xid));
        return permissionService.hasPermission(user, vo.getSetPermission());
    }

    /**
     * Does the user have read permissions to every data point in the list?
     *
     * @param xids
     * @return
     */
    public boolean hasDataPointXidsSetPermission(String[] xids) {
        if (!(this.getPrincipal() instanceof PermissionHolder)) {
            return false;
        }

        PermissionHolder user = (PermissionHolder) this.getPrincipal();
        for (String xid : xids) {
            DataPointVO vo = checkNull(DataPointDao.getInstance().getByXid(xid));
            if (!permissionService.hasPermission(user, vo.getSetPermission()))
                return false;
        }

        return true;
    }

    public boolean isPasswordAuthenticated() {
        Authentication auth = this.getAuthentication();
        if (!(auth instanceof UsernamePasswordAuthenticationToken)) {
            throw new AccessDeniedException(new TranslatableMessage("rest.error.usernamePasswordOnly").translate(Common.getTranslations()));
        }
        return true;
    }

    public boolean hasMangoUser() {
        Object principal = this.getPrincipal();
        return principal instanceof PermissionHolder && ((PermissionHolder) principal).getUser() != null;
    }

    private Object filterObject;

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    private Object returnObject;

    @Override
    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return this;
    }
}
