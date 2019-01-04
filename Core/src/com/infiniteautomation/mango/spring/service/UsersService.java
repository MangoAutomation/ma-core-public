/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */ 
@Service
public class UsersService extends AbstractVOService<User, UserDao> {

    @Autowired
    public UsersService(UserDao dao) {
        super(dao);
    }

    /*
     * Nice little hack since Users don't have an XID.
     */
    @Override
    protected User get(String username, PermissionHolder user, boolean full)
            throws NotFoundException, PermissionException {
        User vo = dao.getUser(username);
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(user, vo);
        return vo;
    }
    
    @Override
    protected User insert(User vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
        //Ensure they can create
        ensureCreatePermission(user);
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        dao.saveUser(vo);
        return vo;
    }
    
    @Override
    protected User update(User existing, User vo, PermissionHolder user, boolean full)
            throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        
        String newPassword = vo.getPassword();
        if (StringUtils.isBlank(newPassword)) {
            // just use the old password
            vo.setPassword(existing.getPassword());
        }
        
        ensureValid(existing, vo, user);
        dao.saveUser(vo);
        return vo;
    }
    
    @Override
    public User delete(String xid, PermissionHolder user)
            throws PermissionException, NotFoundException {
        User vo = get(xid, user);
        
        //You cannot delete yourself
        if(user.getPermissionHolderId() == vo.getId())
            throw new PermissionException(new TranslatableMessage("users.validate.badDelete"), user);

        //Only admin can delete
        user.ensureHasAdminPermission();

        dao.deleteUser(vo.getId());
        return vo;
    }
    
    @Override
    public ProcessResult validate(User vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        vo.validate(result);
        return result;
    }
    
    @Override
    public ProcessResult validate(User existing, User vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        
        //Things we cannot do to ourselves
        if(existing.getId() == user.getPermissionHolderId()) {
            
            //Cannot remove admin permission
            if(existing.hasAdminPermission())
                if(!vo.hasAdminPermission())
                    result.addContextualMessage("permissions", "users.validate.adminInvalid");
            
            //Cannot disable
            if(vo.isDisabled())
                result.addContextualMessage("permissions", "users.validate.adminDisable");
                
        }
        
        //Things we cannot do as non-admin
        if(!user.hasAdminPermission()) {
            //We cannot modify our own privs
            if(!StringUtils.equals(existing.getPermissions(), vo.getPermissions()))
                result.addContextualMessage("permissions", "users.validate.cannotChangePermissions");
        }

        //Cannot Rename a User to an existing Username
        if(!StringUtils.equals(vo.getUsername(), existing.getUsername())){
            User existingUser = UserDao.getInstance().getUser(vo.getUsername());
            if(existingUser != null){
                result.addContextualMessage("username", "users.validate.usernameInUse");
            }
        }
        
        vo.validate(result);
        return result;
    }
    
    @Override
    public boolean hasCreatePermission(PermissionHolder user) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, User vo) {
        if(user.hasAdminPermission())
            return true;
        else if(user.getPermissionHolderId() == vo.getId())
            return true;
        else
            return false;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, User vo) {
        if(user.hasAdminPermission())
            return true;
        else if(user.getPermissionHolderId() == vo.getId())
            return true;
        else
            return false;
    }
}
