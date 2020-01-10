/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.infiniteautomation.mango.spring.db.UserCommentTableDefinition;
import com.serotonin.m2m2.db.dao.UserCommentDao;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
public class UserCommentService extends AbstractVOService<UserCommentVO, UserCommentTableDefinition, UserCommentDao>  {

    @Autowired
    public UserCommentService(UserCommentDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, UserCommentVO vo) {
        if(user.hasAdminRole())
            return true;
        else if((user instanceof User) && vo.getUserId() == ((User)user).getId())
            return true;
        return false;
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, UserCommentVO vo) {
        return true;
    }

}
