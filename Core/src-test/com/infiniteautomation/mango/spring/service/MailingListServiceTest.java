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

import com.infiniteautomation.mango.db.tables.MailingLists;
import com.infiniteautomation.mango.db.tables.records.MailingListsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.rules.ExpectValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.MailingListDao;
import com.serotonin.m2m2.module.definitions.permissions.MailingListCreatePermission;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.MailingListEntry;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.role.Role;

/**
 * @author Terry Packer
 *
 */
public class MailingListServiceTest extends AbstractVOServiceWithPermissionsTest<MailingList, MailingListsRecord, MailingLists, MailingListDao, MailingListService> {

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
        MailingList vo = newVO(readUser);
        Set<Role> editRoles = Collections.singleton(editRole);
        vo.setEditPermission(MangoPermission.requireAnyRole(editRoles));
        runAs.runAs(readUser, () -> {
            service.insert(vo);
        });
    }

    @Test(expected = PermissionException.class)
    public void testAddOverprivledgedEditRole() {
        MailingList vo = newVO(readUser);
        Set<Role> editRoles = Collections.singleton(editRole);
        vo.setEditPermission(MangoPermission.requireAnyRole(editRoles));
        service.insert(vo);
        MailingList fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);

        runAs.runAs(readUser, () -> {
            service.update(vo.getXid(), vo);
        });
    }

    @Test(expected = PermissionException.class)
    public void testRemoveOverprivledgedEditRole() {
        MailingList vo = newVO(readUser);
        Set<Role> editRoles = Collections.singleton(editRole);
        vo.setEditPermission(MangoPermission.requireAnyRole(editRoles));
        service.insert(vo);
        MailingList fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);

        runAs.runAs(readUser, () -> {
            vo.setEditPermission(MangoPermission.superadminOnly());
            service.update(vo.getXid(), vo);
        });
    }

    @Test
    @ExpectValidationException("editPermission")
    public void testAddMissingEditRole() {
        MailingList vo = newVO(readUser);
        Role role = new Role(10000, "new-role");
        Set<Role> editRoles = Collections.singleton(role);
        vo.setEditPermission(MangoPermission.requireAnyRole(editRoles));
        service.insert(vo);
    }

    @Test
    public void testRemoveRole() {
        MailingList vo = newVO(readUser);
        Set<Role> editRoles = Collections.singleton(editRole);
        vo.setEditPermission(MangoPermission.requireAnyRole(editRoles));
        service.insert(vo);
        roleService.delete(editRole.getXid());
        vo.setEditPermission(MangoPermission.superadminOnly());
        MailingList fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
    }

    @Override
    void assertVoEqual(MailingList expected, MailingList actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getReceiveAlarmEmails(), actual.getReceiveAlarmEmails());
        assertEquals(expected.getEntries().size(), actual.getEntries().size());
        for(int i=0; i<expected.getEntries().size(); i++) {
            MailingListRecipient expectedRecipient = expected.getEntries().get(i);
            MailingListRecipient actualRecipient = actual.getEntries().get(i);
            switch(expectedRecipient.getRecipientType()) {
                case ADDRESS:
                case PHONE_NUMBER:
                    assertEquals(expectedRecipient.getReferenceAddress(), actualRecipient.getReferenceAddress());
                    break;
                case USER:
                case USER_PHONE_NUMBER:
                case MAILING_LIST:
                    assertEquals(expectedRecipient.getReferenceId(), actualRecipient.getReferenceId());
                    break;
                default:
                    fail("unknown recipient type");

            }
        }
        assertPermission(expected.getReadPermission(), actual.getReadPermission());
        assertPermission(expected.getEditPermission(), actual.getEditPermission());
    }

    @Test
    @ExpectValidationException("recipients[0]")
    public void testRecursiveMailingListsAreInvalid() {
        MailingList vo = newVO(readUser);
        service.insert(vo);
        MailingList fromDb = service.get(vo.getId());
        assertVoEqual(vo, fromDb);
        MailingList saved = fromDb;

        MailingList recursive = newVO(readUser);
        List<MailingListRecipient> entries = new ArrayList<>();
        MailingListEntry ml = new MailingListEntry();
        ml.setMailingListId(saved.getId());
        entries.add(ml);
        recursive.setEntries(entries);
        service.insert(recursive);
    }

    @Override
    String getCreatePermissionType() {
        return MailingListCreatePermission.PERMISSION;
    }

    @Override
    void setReadPermission(MangoPermission permission, MailingList vo) {
        vo.setReadPermission(permission);
    }

    @Override
    void setEditPermission(MangoPermission permission, MailingList vo) {
        vo.setEditPermission(permission);
    }

    @Override
    MailingList updateVO(MailingList existing) {
        MailingList updated = (MailingList) existing.copy();
        updated.setName("updated");
        updated.setXid("NEW_XID");
        updated.setReadPermission(MangoPermission.requireAnyRole(Collections.singleton(readRole)));
        return updated;
    }

    @Override
    MailingList newVO(User owner) {
        MailingList vo = new MailingList();
        vo.setXid(MailingListDao.getInstance().generateUniqueXid());
        vo.setName("MailingList");
        vo.setReceiveAlarmEmails(AlarmLevels.NONE);

        List<MailingListRecipient> entries = new ArrayList<>();
        AddressEntry entry = new AddressEntry();
        entry.setAddress("entry1@example.com");
        entries.add(entry);
        vo.setEntries(entries);

        return vo;
    }

    @Override
    String getReadPermissionContextKey() {
        return "readPermission";
    }

    @Override
    String getEditPermissionContextKey() {
        return "editPermission";
    }

}
