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
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.MailingListTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition.GrantedAccess;
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
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Matthew Lohbihler
 */
@Repository
public class MailingListDao extends AbstractDao<MailingList, MailingListTableDefinition> {

    private static final LazyInitSupplier<MailingListDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(MailingListDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (MailingListDao)o;
    });

    private final PermissionService permissionService;

    @Autowired
    private MailingListDao(MailingListTableDefinition table,
            PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher){
        super(AuditEventType.TYPE_MAILING_LIST,
                table,
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
    public void saveRelationalData(MailingList ml, boolean insert) {

        // Save the inactive intervals.
        if(!insert)
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
        if(!insert)
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

        //Replace the role mappings
        RoleDao.getInstance().replaceRolesOnVoPermission(ml.getReadPermission(), ml, PermissionService.READ, insert);
        RoleDao.getInstance().replaceRolesOnVoPermission(ml.getEditPermission(), ml, PermissionService.EDIT, insert);

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
        ml.setReadPermission(RoleDao.getInstance().getPermission(ml, PermissionService.READ));
        ml.setEditPermission(RoleDao.getInstance().getPermission(ml, PermissionService.EDIT));
    }

    @Override
    public void deleteRelationalData(MailingList vo) {
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.READ);
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.EDIT);
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select,
            PermissionHolder user) {
        //Join on permissions
        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = user.getAllInheritedRoles().stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);
            Field<Boolean> granted = new GrantedAccess(RoleTableDefinition.maskField, roleIdsIn);

            Table<?> permission = create.select(
                    RoleTableDefinition.voTypeField,
                    RoleTableDefinition.voIdField,
                    RoleTableDefinition.permissionTypeField).from(RoleTableDefinition.ROLE_MAPPING_TABLE)
                    .groupBy(RoleTableDefinition.voTypeField,
                            RoleTableDefinition.voIdField,
                            RoleTableDefinition.permissionTypeField)
                    .having(granted)
                    .asTable("rm");


            Condition readConditions = DSL.and(
                    RoleTableDefinition.voTypeField.eq(MailingList.class.getSimpleName()),
                    this.table.getAlias("id").eq(permission.field("voId")),
                    RoleTableDefinition.permissionTypeField.in(PermissionService.READ, PermissionService.EDIT));

            //Join on role mappings select with conditions
            select = select
                    .join(permission)
                    .on(DSL.or(readConditions));
        }
        return select;
    }

    @Override
    protected String getXidPrefix() {
        return MailingList.XID_PREFIX;
    }

    @Override
    protected Object[] voToObjectArray(MailingList vo) {
        return new Object[] {
                vo.getXid(),
                vo.getName(),
                vo.getReceiveAlarmEmails().value()
        };
    }

    @Override
    public RowMapper<MailingList> getRowMapper() {
        return new MailingListRowMapper();
    }

    class MailingListRowMapper implements RowMapper<MailingList> {
        @Override
        public MailingList mapRow(ResultSet rs, int rowNum) throws SQLException {
            MailingList ml = new MailingList();
            int i = 0;
            ml.setId(rs.getInt(++i));
            ml.setXid(rs.getString(++i));
            ml.setName(rs.getString(++i));
            ml.setReceiveAlarmEmails(AlarmLevels.fromValue(rs.getInt(++i)));
            return ml;
        }
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
