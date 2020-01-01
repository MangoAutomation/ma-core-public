/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.module.definitions.permissions.MailingListCreatePermission;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class MailingListServiceTest extends AbstractVOServiceWithPermissionsTest<MailingList, MailingListDao, MailingListService> {

    @Override
    MailingListService getService() {
        return Common.getBean(MailingListService.class);
    }
    
    @Override
    MailingListDao getDao() {
        return MailingListDao.getInstance();
    }
    
    @Test(expected = PermissionException.class)
    public void testInvalidPermission() {
        runTest(() -> {
            MailingList vo = newVO();
            Set<Role> editRoles = Collections.singleton(editRole); 
            vo.setEditRoles(editRoles);
            service.insert(vo, true, readUser);            
        });
    }

    @Test(expected = PermissionException.class)
    public void testAddOverprivledgedEditRole() {
        runTest(() -> {
            MailingList vo = newVO();
            Set<Role> editRoles = Collections.singleton(editRole); 
            vo.setEditRoles(editRoles);
            service.insert(vo, true, systemSuperadmin);
            MailingList fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            service.update(vo.getXid(), vo, true, readUser);
        });
    }

    @Test(expected = PermissionException.class)
    public void testRemoveOverprivledgedEditRole() {
        runTest(() -> {
            MailingList vo = newVO();
            Set<Role> editRoles = Collections.singleton(editRole); 
            vo.setEditRoles(editRoles);
            service.insert(vo, true, systemSuperadmin);
            MailingList fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);
            vo.setEditRoles(Collections.emptySet());
            service.update(vo.getXid(), vo, true, readUser);            
        });
    }
    
    @Test(expected = ValidationException.class)
    public void testAddMissingEditRole() {
        MailingList vo = newVO();
        Role role = new Role(10000, "new-role");
        Set<Role> editRoles = Collections.singleton(role); 
        vo.setEditRoles(editRoles);
        service.insert(vo, true, systemSuperadmin);   
    }
    
    @Test
    public void testRemoveRole() {
        runTest(() -> {
            MailingList vo = newVO();
            Set<Role> editRoles = Collections.singleton(editRole); 
            vo.setEditRoles(editRoles);
            service.insert(vo, true, systemSuperadmin);
            roleService.delete(editRole.getXid(), systemSuperadmin);
            vo.setEditRoles(Collections.emptySet());
            MailingList fromDb = service.get(vo.getId(), true, systemSuperadmin);
            assertVoEqual(vo, fromDb);            
        });
    }
    
    @Override
    void assertVoEqual(MailingList expected, MailingList actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getReceiveAlarmEmails(), actual.getReceiveAlarmEmails());
        assertEquals(expected.getEntries().size(), actual.getEntries().size());
        for(int i=0; i<expected.getEntries().size(); i++) {
            EmailRecipient expectedRecipient = expected.getEntries().get(i);
            EmailRecipient actualRecipient = actual.getEntries().get(i);
            switch(expectedRecipient.getRecipientType()) {
                case EmailRecipient.TYPE_ADDRESS:
                    assertEquals(expectedRecipient.getReferenceAddress(), actualRecipient.getReferenceAddress());
                    break;
                case EmailRecipient.TYPE_MAILING_LIST:
                    assertEquals(expectedRecipient.getReferenceId(), actualRecipient.getReferenceId());
                    break;
                case EmailRecipient.TYPE_USER:
                    assertEquals(expectedRecipient.getReferenceId(), actualRecipient.getReferenceId());
                    break;
                default:
                    fail("unknown recipient type");
            }

        }
        assertRoles(expected.getReadRoles(), actual.getReadRoles());
        assertRoles(expected.getEditRoles(), actual.getEditRoles());
    }
    
    @Override
    String getCreatePermissionType() {
        return MailingListCreatePermission.PERMISSION;
    }
    
    @Override
    void setReadRoles(Set<Role> roles, MailingList vo) {
        vo.setReadRoles(roles);
    }
    
    @Override
    void setEditRoles(Set<Role> roles, MailingList vo) {
        vo.setEditRoles(roles);
    }
    
    @Override
    MailingList updateVO(MailingList existing) {
        MailingList updated = existing.copy();
        updated.setName("updated");
        updated.setXid("NEW_XID");
        updated.setReadRoles(Collections.singleton(readRole));
        return updated;
    }
    
    @Override
    MailingList newVO() {
        MailingList vo = new MailingList();
        vo.setXid(MailingListDao.getInstance().generateUniqueXid());
        vo.setName("MailingList");
        vo.setReceiveAlarmEmails(AlarmLevels.NONE);
        
        List<EmailRecipient> entries = new ArrayList<>();
        AddressEntry entry = new AddressEntry();
        entry.setAddress("entry1@example.com");
        entries.add(entry);
        vo.setEntries(entries);
        
        return vo;
    }
    
}
