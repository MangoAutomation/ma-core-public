/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.spring.db.UserCommentTableDefinition;
import com.serotonin.m2m2.db.dao.UserCommentDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 *
 * @author Terry Packer
 */
@Service
public class UserCommentService extends AbstractVOService<UserCommentVO, UserCommentTableDefinition, UserCommentDao>  {

    private final UserDao userDao;

    @Autowired
    public UserCommentService(UserCommentDao dao, UserDao userDao, PermissionService permissionService) {
        super(dao, permissionService);
        this.userDao = userDao;
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

    @Override
    public ProcessResult validate(UserCommentVO vo, PermissionHolder user) {
        //Don't use super, we don't have a 'name' field
        ProcessResult result = commonValidation(vo, user);
        if(!user.hasAdminRole()) {
            if(user instanceof User) {
                if(((User)user).getId() !=  vo.getUserId()) {
                    result.addContextualMessage("userId", "validate.cannotChangeOwner");
                }
            }else {
                result.addContextualMessage("userId","validate.onlyUserCanCreate");
            }
        }
        return result;
    }

    @Override
    public ProcessResult validate(UserCommentVO existing, UserCommentVO vo, PermissionHolder user) {
        //Don't use super, we don't have a 'name' field
        ProcessResult result = commonValidation(vo, user);

        if(!user.hasAdminRole() && (existing.getUserId() != vo.getUserId())) {
            result.addContextualMessage("userId", "validate.cannotChangeOwner");
        }

        return result;
    }

    protected ProcessResult commonValidation(UserCommentVO vo, PermissionHolder user) {
        ProcessResult result = new ProcessResult();
        if (StringUtils.isBlank(vo.getXid()))
            result.addContextualMessage("xid", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getXid(), 100))
            result.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!isXidUnique(vo.getXid(), vo.getId()))
            result.addContextualMessage("xid", "validate.xidUsed");

        User owner = userDao.get(vo.getUserId());
        if(owner == null) {
            result.addContextualMessage("userId", "validate.userMissing");
        }

        return result;
    }

}
