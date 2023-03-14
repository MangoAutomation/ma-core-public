/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.infiniteautomation.mango.spring.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

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
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryType;
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
        MailingList recursiveMailingList = service.insert(newVO(readUser));

        List<MailingListRecipient> entries = new ArrayList<>();
        recursiveMailingList.setEntries(entries);

        MailingListEntry ml = new MailingListEntry();
        ml.setMailingListId(recursiveMailingList.getId());
        entries.add(ml);

        service.update(recursiveMailingList.getId(), recursiveMailingList);
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

    /**
     * ensure Active interval access works
     */
    @Test
    public void testActiveRecipients() {

        //First create a list with 2 entries
        //Create an inactive list
        MailingList vo = new MailingList();
        vo.setXid(MailingListDao.getInstance().generateUniqueXid());
        vo.setName("MailingList");
        vo.setReceiveAlarmEmails(AlarmLevels.NONE);

        List<MailingListRecipient> entries = new ArrayList<>();
        AddressEntry entry1 = new AddressEntry();
        entry1.setAddress("entry1List1@example.com");
        entries.add(entry1);

        AddressEntry entry2 = new AddressEntry();
        entry2.setAddress("entry2List1@example.com");
        entries.add(entry2);

        vo.setEntries(entries);

        service.insert(vo);
        MailingListEntry mlEntry = new MailingListEntry();
        mlEntry.setMailingListId(vo.getId());
        List<MailingListRecipient> mlRecipients = Collections.singletonList(mlEntry);

        //Set our time to the start of this week, the test a full week's intervals
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime startOfWeek = now.with(ChronoField.DAY_OF_WEEK, 1).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfNextWeek = startOfWeek.plus(2, ChronoUnit.WEEKS);
        while(startOfWeek.isBefore(endOfNextWeek)) {

            //Setup our interval
            vo.setInactiveIntervals(
                    Collections.singleton(
                            MailingListService.MailingListUtility.getIntervalIdAt(startOfWeek.toInstant().toEpochMilli())));
            vo = service.update(vo.getId(), vo);

            Set<String> recipients = service.getActiveRecipients(mlRecipients,
                    startOfWeek.toInstant().toEpochMilli(),
                    RecipientListEntryType.MAILING_LIST,
                    RecipientListEntryType.ADDRESS,
                    RecipientListEntryType.USER);

            assertEquals(0, recipients.size());

            //Update the list to be active and re-test
            ZonedDateTime previousIntervalStartTime = startOfWeek.minus(15, ChronoUnit.MINUTES);
            vo.setInactiveIntervals(
                    Collections.singleton(
                            MailingListService.MailingListUtility
                                    .getIntervalIdAt(previousIntervalStartTime.toInstant().toEpochMilli())));
            vo = service.update(vo.getId(), vo);
            recipients = service.getActiveRecipients(mlRecipients,
                    startOfWeek.toInstant().toEpochMilli(),
                    RecipientListEntryType.MAILING_LIST,
                    RecipientListEntryType.ADDRESS,
                    RecipientListEntryType.USER);

            assertEquals(2, recipients.size());
            for(MailingListRecipient e : entries) {
                assertTrue(recipients.contains(e.toString()));
            }

            //Advance 1 minute
            startOfWeek = startOfWeek.plus(1, ChronoUnit.MINUTES);
        }
    }

    @Test
    public void testInactiveIntervalComputation() {
        //Set our time to the start of this week, the test a full week's intervals
        ZonedDateTime date = ZonedDateTime.now();
        ZonedDateTime startOfWeek = date.with(ChronoField.DAY_OF_WEEK, 1).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfWeek = startOfWeek.plus(1, ChronoUnit.WEEKS);
        inactiveIntervalComputation(startOfWeek, endOfWeek);

        // Set timezone to test daylight saving for static dates
        TimeZone.setDefault(TimeZone.getTimeZone("MST7MDT"));

        //Set our time to the start of Daylight saving switchover week, the test a full week's intervals
        date = ZonedDateTime.of(2023, 3, 12, 0, 0, 0, 0, ZoneId.systemDefault());
        startOfWeek = date.with(ChronoField.DAY_OF_WEEK, 1).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        endOfWeek = startOfWeek.plus(1, ChronoUnit.WEEKS);
        inactiveIntervalComputation(startOfWeek, endOfWeek);

        //Set our time to the start of Daylight saving switchover week, the test a full week's intervals
        date = ZonedDateTime.of(2023, 11, 5, 0, 0, 0, 0, ZoneId.systemDefault());
        startOfWeek = date.with(ChronoField.DAY_OF_WEEK, 1).toLocalDate().atStartOfDay(ZoneId.systemDefault());
        endOfWeek = startOfWeek.plus(1, ChronoUnit.WEEKS);
        inactiveIntervalComputation(startOfWeek, endOfWeek);
    }

    private void inactiveIntervalComputation(ZonedDateTime startOfWeek, ZonedDateTime endOfWeek) {
        //Count of 15 minute intervals in week starting at [00:00 to 00:15) on Monday. Thus, there are 4 * 24 * 7
        //     * ~= 672 individual periods. (+-4) when daylight saving switchover
        int interval = 0;
        while(startOfWeek.isBefore(endOfWeek)) {

            //Test interval
            assertEquals(interval, MailingListService.MailingListUtility.getIntervalIdAt(startOfWeek.toInstant().toEpochMilli()));

            //Advance 15 minute
            var previousDaylightSaving = ZoneId.systemDefault().getRules().getDaylightSavings(startOfWeek.toInstant());
            startOfWeek = startOfWeek.plus(15, ChronoUnit.MINUTES);
            var currentDaylightSaving = ZoneId.systemDefault().getRules().getDaylightSavings(startOfWeek.toInstant());
            var diffMinutes = currentDaylightSaving.toMinutes() - previousDaylightSaving.toMinutes();
            var shiftedInterval = diffMinutes / 15;
            interval += 1 + shiftedInterval;
        }
    }
}
