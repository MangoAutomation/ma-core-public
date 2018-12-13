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
        ensureValid(vo, user);
        dao.saveUser(vo);
        return vo;
    }
    
    @Override
    public User delete(String xid, PermissionHolder user)
            throws PermissionException, NotFoundException {
        User vo = get(xid, user);
        ensureEditPermission(user, vo);
        dao.deleteUser(vo.getId());
        return vo;
    }
    
    @Override
    public boolean hasCreatePermission(PermissionHolder user) {
        return user.hasAdminPermission();
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, User vo) {
        if(user.hasAdminPermission())
            return true;
        if(user.getPermissionHolderId() != vo.getId())
            return false;
        if(!StringUtils.equals(user.getPermissions(), vo.getPermissions()))
            return false;
        return true;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, User vo) {
        return hasEditPermission(user, vo);
    }

}
