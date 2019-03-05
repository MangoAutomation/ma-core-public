/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.permissions.UserEditSelfPermission;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionDetails;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
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
        ensureCreatePermission(user, vo);
        
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
    
    /**
     * Lock a user's password
     * @param username
     * @param user
     * @throws PermissionException
     * @throws NotFoundException
     */
    public void lockPassword(String username, PermissionHolder user)
            throws PermissionException, NotFoundException {
        user.ensureHasAdminPermission();
        User toLock = get(username, user);
        if(toLock.getId() == user.getPermissionHolderId()) 
            throw new PermissionException(new TranslatableMessage("users.validate.cannotLockOwnPassword"), user);
        dao.lockPassword(toLock);
    }
    

    /**
     * Get User Permissions Information for all users, exclude provided groups in query
     * @param query
     * @param user
     * @return
     */
    public Set<PermissionDetails> getPermissionDetails(String query, PermissionHolder user) {
        Set<PermissionDetails> details = new TreeSet<>();
        for (User u : dao.getActiveUsers()){
            PermissionDetails deets = Permissions.getPermissionDetails(user, query, u);
            if(deets != null)
                details.add(deets);
        }
        return details;
    }
    
    /**
     * Get All User Groups that a user can 'see', exclude any groups listed
     * @param exclude
     * @param user
     * @return
     */
    public Set<String> getUserGroups(List<String> exclude, PermissionHolder user) {
        Set<String> groups = new TreeSet<>();
        //All users have this role
        groups.add(Permissions.USER_DEFAULT);
        if(user.hasAdminPermission()) {
            for (User u : UserDao.getInstance().getActiveUsers())
                groups.addAll(Permissions.explodePermissionGroups(u.getPermissions()));
        }else {
            groups.addAll(user.getPermissionsSet());
        }
        
        if (exclude != null) {
            for (String part : exclude)
                groups.remove(part);
        }
        
        return groups;
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
    public boolean hasCreatePermission(PermissionHolder user, User vo) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, User vo) {
        if(user.hasAdminPermission())
            return true;
        else if(user.getPermissionHolderId() == vo.getId() && Permissions.hasAnyPermission(user, Permissions.explodePermissionGroups(SystemSettingsDao.instance.getValue(UserEditSelfPermission.PERMISSION))))
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
