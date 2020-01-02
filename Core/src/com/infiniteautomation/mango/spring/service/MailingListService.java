/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.db.dao.UserDao;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.MailingListCreatePermission;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.UserEntry;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;

/**
 * Mailing list service
 * 
 * NOTE: This VO has a join table for the entries so if you desire 
 *    the entries with the VO you must use the getFull and insertFull etc 
 *    endpoints.
 *    
 * @author Terry Packer
 *
 */
@Service
public class MailingListService extends AbstractVOService<MailingList, MailingListDao> {
    
    @Autowired 
    public MailingListService(MailingListDao dao, PermissionService permissionService) {
        super(dao, permissionService);
    }
    
    @Override
    public Set<Role> getCreatePermissionRoles() {
        return ModuleRegistry.getPermissionDefinition(MailingListCreatePermission.PERMISSION).getRoles();
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
        return permissionService.hasAnyRole(user, item.getEditRoles());
    }
    
    /**
     * All users can read mailing lists, however you must have READ permission to view the addresses
     * 
     * @param user
     * @param item
     * @return
     */
    @Override
    public boolean hasReadPermission(PermissionHolder user, MailingList item) {
        return permissionService.hasAnyRole(user, item.getReadRoles());
    }
    
    /**
     * Can this user view the recipients on this list?
     * @param user
     * @param item
     * @return
     */
    public boolean hasRecipientViewPermission(PermissionHolder user, MailingList item) {
        if(permissionService.hasAnyRole(user, item.getReadRoles())) {
            return true;
        }else if(permissionService.hasAnyRole(user, item.getEditRoles())) {
            return true;
        }else {
            return false;
        }
    }
    
    @Override
    public ProcessResult validate(MailingList vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        permissionService.validateVoRoles(result, "readRoles", user, false, null, vo.getReadRoles());
        permissionService.validateVoRoles(result, "editRoles", user, false, null, vo.getEditRoles());
        return result;
    }
    
    @Override
    public ProcessResult validate(MailingList existing, MailingList vo, PermissionHolder user) {
        ProcessResult result = commonValidation(vo, user);
        
        //Additional checks for existing list
        permissionService.validateVoRoles(result, "readRoles", user, false, existing.getReadRoles(), vo.getReadRoles());
        permissionService.validateVoRoles(result, "editRoles", user, false, existing.getEditRoles(), vo.getEditRoles());
        return result;
    }
    
    /**
     * Common validation logic for insert/update of Mailing lists
     * @param vo
     * @param user
     * @return
     */
    protected ProcessResult commonValidation(MailingList vo, PermissionHolder user) {
        ProcessResult result = super.validate(vo, user);
        
        if(vo.getReceiveAlarmEmails() == null) {
            result.addContextualMessage("receiveAlarmEmails", "validate.invalidValue");
        }

        if(vo.getEntries() == null || vo.getEntries().size() == 0) {
            result.addContextualMessage("recipients", "mailingLists.validate.entries");
        }else {
            int index = 0;
            for(EmailRecipient recipient : vo.getEntries()) {
                switch(recipient.getRecipientType()) {
                    case EmailRecipient.TYPE_ADDRESS:
                        //TODO Ensure valid email format...
                        AddressEntry ee = (AddressEntry)recipient;
                        if (StringUtils.isBlank(ee.getAddress()))
                            result.addContextualMessage("recipients[" + index + "]", "validate.required");
                        break;
                    case EmailRecipient.TYPE_MAILING_LIST:
                        result.addContextualMessage("recipients[" + index + "]", "validate.invalidValue");
                        break;
                    case EmailRecipient.TYPE_USER:
                        //Ensure the user exists
                        UserEntry ue = (UserEntry)recipient;
                        if(UserDao.getInstance().get(ue.getUserId()) == null)
                            result.addContextualMessage("recipients[" + index + "]", "validate.invalidValue");
                        break;
                }
                index++;
            }
        }
        
        if(vo.getInactiveIntervals() != null) {
            if(vo.getInactiveIntervals().size() > 672)
                result.addContextualMessage("inactiveSchedule", "validate.invalidValue");
        }
        return result;
    }
}
