/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */

package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.DataPointDao;
import com.serotonin.m2m2.db.dao.EventInstanceDao;
import com.serotonin.m2m2.db.dao.JsonDataDao;
import com.serotonin.m2m2.db.dao.UserCommentDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.vo.json.JsonDataVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.validation.StringValidation;

/**
 *
 * @author Terry Packer
 */
@Service
public class UserCommentService extends AbstractVOService<UserCommentVO, UserCommentDao>  {

    private final UserDao userDao;
    private final DataPointDao dataPointDao;
    private final JsonDataDao jsonDataDao;
    private final EventInstanceDao eventInstanceDao;

    @Autowired
    public UserCommentService(UserCommentDao dao,
                              ServiceDependencies dependencies,
                              UserDao userDao,
                              DataPointDao dataPointDao,
                              JsonDataDao jsonDataDao,
                              EventInstanceDao eventInstanceDao) {
        super(dao, dependencies);
        this.userDao = userDao;
        this.dataPointDao = dataPointDao;
        this.jsonDataDao = jsonDataDao;
        this.eventInstanceDao = eventInstanceDao;
    }

    @Override
    public boolean hasCreatePermission(PermissionHolder user, UserCommentVO vo) {
        switch(vo.getCommentType()) {
            case UserCommentVO.TYPE_EVENT:
                EventInstanceVO event = eventInstanceDao.get(vo.getReferenceId());
                if(event != null) {
                    return permissionService.hasPermission(user, event.getReadPermission());
                }else {
                    return permissionService.hasAdminRole(user);
                }
            case UserCommentVO.TYPE_POINT:
                return permissionService.hasDataPointReadPermission(user, vo.getReferenceId());
            case UserCommentVO.TYPE_JSON_DATA:
                JsonDataVO json = jsonDataDao.get(vo.getReferenceId());
                if(json != null) {
                    return permissionService.hasPermission(user, json.getReadPermission());
                }else {
                    return permissionService.hasAdminRole(user);
                }
            default:
                return permissionService.hasAdminRole(user);
        }
    }

    @Override
    public boolean hasEditPermission(PermissionHolder user, UserCommentVO vo) {
        if (permissionService.hasAdminRole(user))
            return true;
        else
            return user.getUser() != null && vo.getUserId() == user.getUser().getId();
    }

    @Override
    public boolean hasReadPermission(PermissionHolder user, UserCommentVO vo) {
        return true;
    }

    @Override
    public ProcessResult validate(UserCommentVO vo) {
        //Don't use super, we don't have a 'name' field
        ProcessResult result = commonValidation(vo);

        PermissionHolder user = Common.getUser();
        if (!permissionService.hasAdminRole(user)) {
            if (user.getUser() != null) {
                if (user.getUser().getId() != vo.getUserId()) {
                    result.addContextualMessage("userId", "validate.cannotChangeOwner");
                }
            } else {
                result.addContextualMessage("userId", "validate.onlyUserCanCreate");
            }
        }
        return result;
    }

    @Override
    public ProcessResult validate(UserCommentVO existing, UserCommentVO vo) {
        //Don't use super, we don't have a 'name' field
        ProcessResult result = commonValidation(vo);

        PermissionHolder user = Common.getUser();
        if(!permissionService.hasAdminRole(user) && (existing.getUserId() != vo.getUserId())) {
            result.addContextualMessage("userId", "validate.cannotChangeOwner");
        }

        return result;
    }

    protected ProcessResult commonValidation(UserCommentVO vo) {
        ProcessResult result = new ProcessResult();
        if (StringUtils.isBlank(vo.getXid()))
            result.addContextualMessage("xid", "validate.required");
        else if (StringUtils.isBlank(vo.getComment()))
            result.addContextualMessage("commentText", "validate.required");
        else if (StringValidation.isLengthGreaterThan(vo.getXid(), 100))
            result.addMessage("xid", new TranslatableMessage("validate.notLongerThan", 100));
        else if (!isXidUnique(vo.getXid(), vo.getId()))
            result.addContextualMessage("xid", "validate.xidUsed");

        User owner = userDao.get(vo.getUserId());
        if(owner == null) {
            result.addContextualMessage("userId", "validate.userMissing");
        }

        if(!UserCommentVO.COMMENT_TYPE_CODES.isValidId(vo.getCommentType())) {
            result.addContextualMessage("commentType", "validate.invalidValue");
        }else {
            switch(vo.getCommentType()) {
                case UserCommentVO.TYPE_EVENT:
                    if(eventInstanceDao.get(vo.getReferenceId()) == null) {
                        result.addContextualMessage("referenceId", "validate.invalidValue");
                    }
                    break;
                case UserCommentVO.TYPE_POINT:
                    if(dataPointDao.getXidById(vo.getReferenceId()) == null) {
                        result.addContextualMessage("referenceId", "validate.invalidValue");
                    }
                    break;
                case UserCommentVO.TYPE_JSON_DATA:
                    if(jsonDataDao.getXidById(vo.getReferenceId()) == null) {
                        result.addContextualMessage("referenceId", "validate.invalidValue");
                    }
                    break;
            }
        }
        return result;
    }

}
