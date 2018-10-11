/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.definitions.permissions.MailingListCreatePermission;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * @author Terry Packer
 *
 */
@Service
public class MailingListService extends AbstractVOMangoService<MailingList> {
    
    public MailingListService(@Autowired MailingListDao dao) {
        super(dao);
    }
    
    @Override
    protected void ensureValidImpl(MailingList vo, PermissionHolder user, ProcessResult result) throws ValidationException {
        //TODO Don't use this method, do all work here
        vo.validate(result);
        
        if(!AlarmLevels.CODES.isValidId(vo.getReceiveAlarmEmails()))
            result.addContextualMessage("receiveAlarmEmails", "validate.invalidValue");
        
        if(vo.getEntries().size() == 0)
            result.addGenericMessage("mailingLists.validate.entries");
        //TODO Validate each entry
        
        if(result.getHasMessages())
            throw new ValidationException(result);
    }


    /**
     * 
     * @param xid
     * @param user
     * @return
     * @throws NotFoundException
     * @throws PermissionException
     * @throws ValidationException
     */
    public MailingList get(String xid, User user) throws NotFoundException, PermissionException, ValidationException {
        MailingList vo = dao.getFullByXid(xid);
        if(vo == null)
            throw new NotFoundException();
        ensureReadPermission(user, vo);
        return vo;
    }


    /**
     * 
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public MailingList insert(MailingList vo, User user) throws PermissionException, ValidationException {
        //Ensure they can create a list
        ensureCreatePermission(user);
        
        //Generate an Xid if necessary
        if(StringUtils.isEmpty(vo.getXid()))
            vo.setXid(dao.generateUniqueXid());
        
        ensureValid(vo, user);
        dao.saveFull(vo);
        return vo;
    }

    /**
     * 
     * @param existingXid
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public MailingList update(String existingXid, MailingList vo, User user) throws PermissionException, ValidationException {
        return update(get(existingXid, user), vo, user);
    }


    /**
     * 
     * @param existing
     * @param vo
     * @param user
     * @return
     * @throws PermissionException
     * @throws ValidationException
     */
    public MailingList update(MailingList existing, MailingList vo, PermissionHolder user) throws PermissionException, ValidationException {
        ensureEditPermission(user, existing);
        vo.setId(existing.getId());
        ensureValid(vo, user);
        dao.save(vo);
        return vo;
    }
    
    /**
     * 
     * @param xid
     * @param user
     * @return
     * @throws PermissionException
     */
    public MailingList delete(String xid, User user) throws PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Can this user create a mailing list
     * 
     * @param user
     * @param item
     * @return
     */
    public boolean hasCreatePermission(PermissionHolder user) {
        if(user.hasAdminPermission())
            return true;
        else if(Permissions.hasAnyPermission(user, Permissions.explodePermissionGroups(SystemSettingsDao.instance.getValue(MailingListCreatePermission.PERMISSION))))
            return true;
        else
            return false;
    }
    
    /**
     * Can this user edit this mailing list
     * 
     * @param user
     * @param item
     * @return
     */
    public boolean hasEditPermission(PermissionHolder user, MailingList item) {
        if(user.hasAdminPermission())
            return true;
        else if(Permissions.hasAnyPermission(user, item.getEditPermissions()))
            return true;
        else
            return false;
    }
    
    /**
     * Can this user read this mailing list
     * 
     * @param user
     * @param item
     * @return
     */
    public boolean hasReadPermission(PermissionHolder user, MailingList item) {
        if(user.hasAdminPermission())
            return true;
        else if(Permissions.hasAnyPermission(user, item.getReadPermissions()))
            return true;
        else if(Permissions.hasAnyPermission(user, item.getEditPermissions()))
            return true;
        else
            return false;
    }

    /**
     * Ensure this user can create a mailing list
     * 
     * @param user
     * @throws PermissionException
     */
    public void ensureCreatePermission(PermissionHolder user) throws PermissionException {
        if(user.hasAdminPermission())
            return;
        else if(Permissions.hasAnyPermission(user, Permissions.explodePermissionGroups(SystemSettingsDao.instance.getValue(MailingListCreatePermission.PERMISSION))))
            return;
        else
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
    }
    
    /**
     * Ensure this user can edit this mailing list
     * 
     * @param user
     * @param item
     */
    public void ensureEditPermission(PermissionHolder user, MailingList item) throws PermissionException {
        if(user.hasAdminPermission())
            return;
        else if(Permissions.hasAnyPermission(user, item.getEditPermissions()))
            return;
        else
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
    }
    
    /**
     * Ensure this user can read this mailing list
     * 
     * @param user
     * @param item
     * @throws PermissionException
     */
    public void ensureReadPermission(PermissionHolder user, MailingList item) throws PermissionException {
        if(user.hasAdminPermission())
            return;
        else if(Permissions.hasAnyPermission(user, item.getReadPermissions()))
            return;
        else if(Permissions.hasAnyPermission(user, item.getEditPermissions()))
            return;
        else
            throw new PermissionException(new TranslatableMessage("permission.exception.doesNotHaveRequiredPermission", user.getPermissionHolderName()), user);
    }
}
