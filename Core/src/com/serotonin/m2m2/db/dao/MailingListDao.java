/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.tables.MailingLists;
import com.infiniteautomation.mango.db.tables.records.MailingListsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.service.PermissionService;
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

    private final PermissionService permissionService;

    @Autowired
    private MailingListDao(PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher){
        super(AuditEventType.TYPE_MAILING_LIST,
                MailingLists.MAILING_LISTS,
                new TranslatableMessage("internal.monitor.MAILING_LIST_COUNT"),
                mapper, publisher);
        this.permissionService = permissionService;
    }

    public static MailingListDao getInstance() {
        return springInstance.get();
    }

    private static final String MAILING_LIST_INACTIVE_INSERT = "insert into mailingListInactive (mailingListId, inactiveInterval) values (?,?)";
    private static final String MAILING_LIST_ENTRY_INSERT = "insert into mailingListMembers (mailingListId, typeId, userId, address) values (?,?,?,?)";

    @Override
    public void savePreRelationalData(MailingList existing, MailingList vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission());
        vo.setEditPermission(editPermission);
    }

    @Override
    public void saveRelationalData(MailingList existing, MailingList ml) {

        // Save the inactive intervals.
        if(existing != null)
            ejt.update("delete from mailingListInactive where mailingListId=?", new Object[] { ml.getId() });

        // Save what is in the mailing list object.
        final List<Integer> intervalIds = new ArrayList<Integer>(ml.getInactiveIntervals());
        ejt.batchUpdate(MAILING_LIST_INACTIVE_INSERT, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return intervalIds.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, ml.getId());
                ps.setInt(2, intervalIds.get(i));
            }
        });

        // Delete existing entries
        if(existing != null)
            ejt.update("delete from mailingListMembers where mailingListId=?", new Object[] { ml.getId() });

        // Save what is in the mailing list object.
        final List<MailingListRecipient> entries = ml.getEntries();
        ejt.batchUpdate(MAILING_LIST_ENTRY_INSERT, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return entries.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MailingListRecipient e = entries.get(i);
                ps.setInt(1, ml.getId());
                ps.setInt(2, e.getRecipientType().value());
                ps.setInt(3, e.getReferenceId());
                ps.setString(4, e.getReferenceAddress());
            }
        });

        if(existing != null) {
            if(!existing.getReadPermission().equals(ml.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(ml.getEditPermission())) {
                permissionService.deletePermissions(existing.getEditPermission());
            }
        }
    }

    private static final String MAILING_LIST_INACTIVE_SELECT = "select inactiveInterval from mailingListInactive where mailingListId=?";
    private static final String MAILING_LIST_ENTRIES_SELECT = "select typeId, userId, address, '' from mailingListMembers where mailingListId=?";

    @Override
    public void loadRelationalData(MailingList ml) {
        ml.getInactiveIntervals().addAll(
                query(MAILING_LIST_INACTIVE_SELECT, new Object[] { ml.getId() },
                        new MailingListScheduleInactiveMapper()));

        ml.setEntries(query(MAILING_LIST_ENTRIES_SELECT, new Object[] { ml.getId() }, new EmailRecipientRowMapper()));

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

    class EmailRecipientRowMapper implements RowMapper<MailingListRecipient> {
        @Override
        public MailingListRecipient mapRow(ResultSet rs, int rowNum) throws SQLException {
            int intType = rs.getInt(1);
            RecipientListEntryType type = RecipientListEntryType.fromValue(intType);
            switch (type) {
                case ADDRESS:
                    AddressEntry ae = new AddressEntry();
                    ae.setAddress(rs.getString(3));
                    return ae;
                case MAILING_LIST:
                    MailingListEntry ml = new MailingListEntry();
                    ml.setMailingListId(rs.getInt(2));
                    return ml;
                case PHONE_NUMBER:
                    PhoneEntry pe = new PhoneEntry();
                    pe.setPhone(rs.getString(3));
                    return pe;
                case USER:
                case USER_PHONE_NUMBER:
                    UserEntry ue = new UserEntry();
                    ue.setUserId(rs.getInt(2));
                    return ue;
                default:
                    throw new ShouldNeverHappenException("Unknown mailing list entry type: " + intType);
            }
        }
    }

    class MailingListScheduleInactiveMapper implements RowMapper<Integer> {
        @Override
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt(1);
        }
    }
}
