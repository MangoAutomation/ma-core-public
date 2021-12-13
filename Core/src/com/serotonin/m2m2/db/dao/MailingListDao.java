/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.tables.MailingListInactive;
import com.infiniteautomation.mango.db.tables.MailingListMembers;
import com.infiniteautomation.mango.db.tables.MailingLists;
import com.infiniteautomation.mango.db.tables.records.MailingListsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.MailingListEntry;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.mailingList.PhoneEntry;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryType;
import com.serotonin.m2m2.vo.mailingList.UserEntry;

/**
 * @author Matthew Lohbihler
 */
@Repository
public class MailingListDao extends AbstractVoDao<MailingList, MailingListsRecord, MailingLists> {

    private static final LazyInitSupplier<MailingListDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(MailingListDao.class);
    });

    private final MailingListInactive mailingListInactiveTable;
    private final MailingListMembers mailingListMembersTable;
    private final UserDao userDao;

    @Autowired
    private MailingListDao(DaoDependencies dependencies, UserDao userDao){
        super(dependencies, AuditEventType.TYPE_MAILING_LIST,
                MailingLists.MAILING_LISTS,
                new TranslatableMessage("internal.monitor.MAILING_LIST_COUNT"));
        this.mailingListInactiveTable = MailingListInactive.MAILING_LIST_INACTIVE;
        this.mailingListMembersTable = MailingListMembers.MAILING_LIST_MEMBERS;
        this.userDao = userDao;
    }

    public static MailingListDao getInstance() {
        return springInstance.get();
    }

    @Override
    public void savePreRelationalData(MailingList existing, MailingList vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission());
        vo.setEditPermission(editPermission);
    }

    @Override
    public void saveRelationalData(MailingList existing, MailingList ml) {
        // Delete existing entries
        if(existing != null) {
            create.deleteFrom(mailingListInactiveTable)
                    .where(mailingListInactiveTable.mailingListId.eq(ml.getId())).execute();
            create.deleteFrom(mailingListMembersTable)
                    .where(mailingListMembersTable.mailingListId.eq(ml.getId())).execute();
        }

        // Save relational data
        Set<Integer> inactiveIntervals = ml.getInactiveIntervals();
        if (inactiveIntervals.size() > 0) {
            BatchBindStep batchInactive = create.batch(create.insertInto(mailingListInactiveTable)
                    .columns(mailingListInactiveTable.mailingListId, mailingListInactiveTable.inactiveInterval)
                    .values((Integer) null, null));
            for (Integer interval : inactiveIntervals)
                batchInactive.bind(ml.getId(), interval);
            batchInactive.execute();
        }

        List<MailingListRecipient> entries = ml.getEntries();
        if (entries.size() > 0) {
            BatchBindStep batchMembers = create.batch(create.insertInto(mailingListMembersTable)
                    .columns(mailingListMembersTable.mailingListId, mailingListMembersTable.typeId,
                            mailingListMembersTable.userId, mailingListMembersTable.address)
                    .values((Integer) null, null, null, null));
            for (MailingListRecipient r : entries)
                batchMembers.bind(ml.getId(), r.getRecipientType().value(), r.getReferenceId(), r.getReferenceAddress());
            batchMembers.execute();
        }

        if(existing != null) {
            if(!existing.getReadPermission().equals(ml.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(ml.getEditPermission())) {
                permissionService.deletePermissions(existing.getEditPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(MailingList ml) {
        ml.getInactiveIntervals().addAll(
                create.select().from(mailingListInactiveTable)
                        .where(mailingListInactiveTable.mailingListId.equal(ml.getId()))
                        .fetch(mailingListInactiveTable.inactiveInterval, Integer.class)
        );

        List<MailingListRecipient> recipients = new ArrayList<>();
        create.select()
                .from(mailingListMembersTable)
                .where(mailingListMembersTable.mailingListId.eq(ml.getId()))
                .fetch()
                .forEach(record -> {
                    recipients.add(getRecipientType(record));
                });
        ml.setEntries(recipients);

        //Populate permissions
        ml.setReadPermission(permissionService.get(ml.getReadPermission().getId()));
        ml.setEditPermission(permissionService.get(ml.getEditPermission().getId()));
    }

    @Override
    public void deletePostRelationalData(MailingList vo) {
        //Clean permissions
        permissionService.deletePermissions(vo.getReadPermission(), vo.getEditPermission());
    }

    @Override
    protected String getXidPrefix() {
        return MailingList.XID_PREFIX;
    }

    @Override
    protected Record toRecord(MailingList vo) {
        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.name, vo.getName());
        record.set(table.receiveAlarmEmails, vo.getReceiveAlarmEmails().value());
        record.set(table.readPermissionId, vo.getReadPermission().getId());
        record.set(table.editPermissionId, vo.getEditPermission().getId());
        return record;
    }

    @Override
    public MailingList mapRecord(Record record) {
        MailingList ml = new MailingList();
        ml.setId(record.get(table.id));
        ml.setXid(record.get(table.xid));
        ml.setName(record.get(table.name));
        ml.setReceiveAlarmEmails(AlarmLevels.fromValue(record.get(table.receiveAlarmEmails)));
        ml.setReadPermission(new MangoPermission(record.get(table.readPermissionId)));
        ml.setEditPermission(new MangoPermission(record.get(table.editPermissionId)));
        return ml;
    }

    private MailingListRecipient getRecipientType(Record record) {
        int intType = record.get(mailingListMembersTable.typeId);
        RecipientListEntryType type = RecipientListEntryType.fromValue(intType);
        Integer userId = record.get(mailingListMembersTable.userId);
        switch (type) {
            case ADDRESS:
                AddressEntry ae = new AddressEntry();
                ae.setAddress(record.get(mailingListMembersTable.address));
                return ae;
            case MAILING_LIST:
                MailingListEntry ml = new MailingListEntry();
                ml.setMailingListId(userId != null ? userId : 0);
                return ml;
            case PHONE_NUMBER:
                PhoneEntry pe = new PhoneEntry();
                pe.setPhone(record.get(mailingListMembersTable.address));
                return pe;
            case USER:
            case USER_PHONE_NUMBER:
                UserEntry ue = new UserEntry();
                ue.setUserId(userId != null ? userId : 0);
                return ue;
            default:
                throw new ShouldNeverHappenException("Unknown mailing list entry type: " + intType);
        }
    }

    /**
     * Clean a list of recipients by removing any entries with dead references,
     *  i.e. a user was deleted while this list was serialized in the database
     * @param list
     */
    public void cleanRecipientList(List<MailingListRecipient> list) {
        if(list == null)
            return;

        ListIterator<MailingListRecipient> it = list.listIterator();
        while(it.hasNext()) {
            MailingListRecipient recipient = it.next();
            switch(recipient.getRecipientType()){
                case ADDRESS:
                case PHONE_NUMBER:
                    if(StringUtils.isEmpty(recipient.getReferenceAddress())) {
                        it.remove();
                    }
                    break;
                case MAILING_LIST:
                    if(getXidById(recipient.getReferenceId()) == null) {
                        it.remove();
                    }
                    break;
                case USER:
                case USER_PHONE_NUMBER:
                    if(userDao.getXidById(recipient.getReferenceId()) == null) {
                        it.remove();
                    }
                    break;
                default:
                    break;

            }
        }
    }
}
