/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.RoleVO;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;

/**
 * @author Terry Packer
 *
 */
public class MailingListServiceTest extends ServiceTestBase {

    private MailingListService service;
    
    @Before
    public void setupService() {
        this.service = Common.getBean(MailingListService.class);
    }
    
    @Test
    public void testCreate() {
        MailingList vo = newVO();
        service.insertFull(vo, systemSuperadmin);
        MailingList fromDb = service.getFull(vo.getId(), systemSuperadmin);
        assertVoEqual(vo, fromDb);
    }
    
    //Test creating with permissions you don't have
    //Test adding permissions you don't have
    //Test removing permissions you don't have
    
    protected void assertVoEqual(MailingList expected, MailingList actual) {
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
    
    protected void assertRoles(Set<RoleVO> expected, Set<RoleVO> actual) {
        assertEquals(expected.size(), actual.size());
        Set<RoleVO> missing = new HashSet<>();
        for(RoleVO expectedRole : expected) {
            boolean found = false;
            for(RoleVO actualRole : actual) {
                if(expectedRole.getId() == actualRole.getId()) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                missing.add(expectedRole);
            }
        }
        if(missing.size() > 0) {
            String missingRoles = "";
            for(RoleVO missingRole : missing) {
                missingRoles += "< " + missingRole.getId() + " - " + missingRole.getName() + "> ";
            }
            fail("Not all roles matched, missing: " + missingRoles);
        }
    }
    
    protected MailingList newVO() {
        MailingList vo = new MailingList();
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
