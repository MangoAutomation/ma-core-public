/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.serotonin.m2m2.web.mvc.spring.security.permissions;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.infiniteautomation.mango.spring.service.PermissionService;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.DataSourceDao;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PermissionDefinition;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.dataSource.DataSourceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * Class to define Custom Spring EL Expressions for use in @PreAuthorize and @PostAuthorize annotations
 *
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

    /**
     * @return true if the user has the superadmin permission and is not disabled
     */
    public boolean hasAdminPermission() {
        Object principal = this.getPrincipal();

        if (principal instanceof User) {
            User user = (User) this.getPrincipal();
            return permissionService.hasAdminRole(user);
        }

        // principal is probably a string "anonymousUser"

        return false;
    }

    /**
     * Is this User an admin?
     * @return
     */
    @Deprecated
    public boolean isAdmin() {
        return this.hasAdminPermission();
    }

    /**
     * Does this User have data source permission?
     * @return
     */
    public boolean hasDataSourcePermission(){
        PermissionHolder user =  (PermissionHolder) this.getPrincipal();
        return permissionService.hasDataSourcePermission(user);
    }

    /**
     * Does this User have edit access for this data source
     * @param xid
     * @return
     */
    public boolean hasDataSourceXidPermission(String xid){
        PermissionHolder user =  (PermissionHolder) this.getPrincipal();
        if(permissionService.hasAdminRole(user))
            return true;
        DataSourceVO dsvo = DataSourceDao.getInstance().getByXid(xid);
        if((dsvo == null)||(!permissionService.hasDataSourceEditPermission(user, dsvo)))
            return false;
        return true;
    }

    /**
     * Checks if a user is granted a permission
     * @param permissionName System setting key for the granted permission
     * @return
     */
    public boolean isGrantedPermission(String permissionName) {
        PermissionDefinition def = ModuleRegistry.getPermissionDefinition(permissionName);
        if(def == null) {
            return false;
        }else {
            return permissionService.hasPermission((PermissionHolder) this.getPrincipal(), def.getPermission());
        }
    }

    /**
     * Does a user have data point read permissions?
     * @param vo
     * @return
     */
    public boolean hasDataPointXidReadPermission(String xid){
        PermissionHolder user =  (PermissionHolder) this.getPrincipal();
        DataPointVO vo = DataPointDao.getInstance().getByXid(xid);

        return (vo != null) && permissionService.hasPermission(user, vo.getReadPermission());
    }

    /**
     * Does the user have read permissions to every data point in the list?
     * @param xids
     * @return
     */
    public boolean hasDataPointXidsReadPermission(String[] xids){
        User user =  (User) this.getPrincipal();
        for(String xid : xids){
            DataPointVO vo = DataPointDao.getInstance().getByXid(xid);
            if((vo == null)||(!permissionService.hasPermission(user, vo.getReadPermission())))
                return false;

        }
        return true;
    }

    /**
     * Does a user have data point set permissions?
     * @param vo
     * @return
     */
    public boolean hasDataPointXidSetPermission(String xid){
        PermissionHolder user =  (PermissionHolder) this.getPrincipal();
        DataPointVO vo = DataPointDao.getInstance().getByXid(xid);

        return (vo != null) && permissionService.hasPermission(user, vo.getSetPermission());
    }

    /**
     * TODO Throw NotFoundRestException instead?
     * Does the user have read permissions to every data point in the list?
     * @param xids
     * @return
     */
    public boolean hasDataPointXidsSetPermission(String[] xids){
        PermissionHolder user =  (PermissionHolder) this.getPrincipal();
        for(String xid : xids){
            DataPointVO vo = DataPointDao.getInstance().getByXid(xid);
            if((vo == null)||(!permissionService.hasPermission(user, vo.getSetPermission())))
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
