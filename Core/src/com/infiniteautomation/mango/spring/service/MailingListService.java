/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.SystemSettingsDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.definitions.permissions.MailingListCreatePermission;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.UserEntry;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * 
 * @author Terry Packer
 *
 */
@Service
public class MailingListService extends AbstractVOService<MailingList> {
    
    public MailingListService(@Autowired MailingListDao dao) {
        super(dao);
    }
    
    @Override
    protected void validateImpl(MailingList vo, User user, ProcessResult result) throws ValidationException {
        
        if(!AlarmLevels.CODES.isValidId(vo.getReceiveAlarmEmails()))
            result.addContextualMessage("receiveAlarmEmails", "validate.invalidValue");
        
        if(vo.getEntries() == null || vo.getEntries().size() == 0)
            result.addContextualMessage("recipients", "mailingLists.validate.entries");
        else {
            int index = 0;
            for(EmailRecipient recipient : vo.getEntries()) {
                switch(recipient.getRecipientType()) {
                    case EmailRecipient.TYPE_ADDRESS:
                        //TODO Ensure valid email format...
                        AddressEntry ee = (AddressEntry)recipient;
                        if (StringUtils.isBlank(ee.getAddress()))
                            result.addContextualMessage("recipients-" + index, "validate.required");
                        break;
                    case EmailRecipient.TYPE_MAILING_LIST:
                        result.addContextualMessage("recipients-" + index, "validate.invalidValue");
                        break;
                    case EmailRecipient.TYPE_USER:
                        //Ensure the user exists
                        UserEntry ue = (UserEntry)recipient;
                        if(UserDao.getInstance().get(ue.getUserId()) == null)
                            result.addContextualMessage("recipients-" + index, "validate.invalidValue");
                        break;
                }
            }
        }
        
        Permissions.validateAddedPermissions(vo.getReadPermissions(), user, result, "readPermissions");
        Permissions.validateAddedPermissions(vo.getEditPermissions(), user, result, "editPermissions");

        if(vo.getInactiveIntervals() != null) {
            if(vo.getInactiveIntervals().size() > 672)
                result.addContextualMessage("inactiveSchedule", "validate.invalidValue");
        }
        if(result.getHasMessages())
            throw new ValidationException(result);
    }
 
    
    /**
     * Can this user create a mailing list
     * 
     * @param user
     * @param item
     * @return
     */
    @Override
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
    @Override
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
    @Override
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
}
