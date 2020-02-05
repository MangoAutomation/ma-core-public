/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
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
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryBean;
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

    @Autowired
    private MailingListDao(MailingListTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher){
        super(AuditEventType.TYPE_MAILING_LIST,
                table,
                new TranslatableMessage("internal.monitor.MAILING_LIST_COUNT"),
                mapper, publisher);
    }

    public static MailingListDao getInstance() {
        return springInstance.get();
    }

    /**
     * Get any mailing list that is mailed on alarm level up to and including 'alarmLevel'
     * @param alarmLevel
     * @return
     */
    public List<MailingList> getAlarmMailingLists(AlarmLevels alarmLevel) {
        return getTransactionTemplate().execute(status -> {
            List<MailingList> result = new ArrayList<>();
            query(getJoinedSelectQuery().getSQL() + " where receiveAlarmEmails>=0 and receiveAlarmEmails<=?", new Object[] {alarmLevel.value()}, new MailingListRowMapper(), (list, index) -> {
                try{
                    loadRelationalData(list);
                    result.add(list);
                }catch(Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            });
            return result;
        });
    }

    /**
     * Get addresses based on lists being inactive
     * @param beans
     * @param sendTime
     * @return
     */
    public Set<String> getRecipientAddresses(List<RecipientListEntryBean> beans, DateTime sendTime) {
        List<EmailRecipient> entries = new ArrayList<EmailRecipient>(beans.size());
        for (RecipientListEntryBean bean : beans)
            entries.add(bean.createEmailRecipient());
        populateEntrySubclasses(entries);
        Set<String> addresses = new HashSet<String>();
        for (EmailRecipient entry : entries)
            entry.appendAddresses(addresses, sendTime);
        return addresses;
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
        final List<EmailRecipient> entries = ml.getEntries();
        ejt.batchUpdate(MAILING_LIST_ENTRY_INSERT, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return entries.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EmailRecipient e = entries.get(i);
                ps.setInt(1, ml.getId());
                ps.setInt(2, e.getRecipientType());
                ps.setInt(3, e.getReferenceId());
                ps.setString(4, e.getReferenceAddress());
            }
        });

        //Replace the role mappings
        RoleDao.getInstance().replaceRolesOnVoPermission(ml.getReadRoles(), ml, PermissionService.READ, insert);
        RoleDao.getInstance().replaceRolesOnVoPermission(ml.getEditRoles(), ml, PermissionService.EDIT, insert);

    }

    private static final String MAILING_LIST_INACTIVE_SELECT = "select inactiveInterval from mailingListInactive where mailingListId=?";
    private static final String MAILING_LIST_ENTRIES_SELECT = "select typeId, userId, address, '' from mailingListMembers where mailingListId=?";

    @Override
    public void loadRelationalData(MailingList ml) {
        ml.getInactiveIntervals().addAll(
                query(MAILING_LIST_INACTIVE_SELECT, new Object[] { ml.getId() },
                        new MailingListScheduleInactiveMapper()));

        ml.setEntries(query(MAILING_LIST_ENTRIES_SELECT, new Object[] { ml.getId() }, new EmailRecipientRowMapper()));

        // Update the user type entries with their respective user objects.
        populateEntrySubclasses(ml.getEntries());

        //Populate permissions
        ml.setReadRoles(RoleDao.getInstance().getRoles(ml, PermissionService.READ));
        ml.setEditRoles(RoleDao.getInstance().getRoles(ml, PermissionService.EDIT));
    }

    public void populateEntrySubclasses(List<EmailRecipient> entries) {
        // Update the user type entries with their respective user objects.
        UserDao userDao = UserDao.getInstance();
        for (EmailRecipient e : entries) {
            if (e instanceof MailingList)
                loadRelationalData((MailingList) e);
            else if (e instanceof UserEntry) {
                UserEntry ue = (UserEntry) e;
                ue.setUser(userDao.get(ue.getUserId()));
            }
        }
    }

    @Override
    public void deleteRelationalData(MailingList vo) {
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.READ);
        RoleDao.getInstance().deleteRolesForVoPermission(vo, PermissionService.EDIT);
    }

    @Override
    public Condition hasPermission(PermissionHolder user) {
        List<Integer> roleIds = user.getRoles().stream().map(r -> r.getId()).collect(Collectors.toList());
        return RoleTableDefinition.roleIdFieldAlias.in(roleIds);
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinRoles(SelectJoinStep<R> select, String permissionType) {

        if(PermissionService.EDIT.equals(permissionType)) {

            Condition editJoinCondition = DSL.and(
                    RoleTableDefinition.voTypeFieldAlias.eq(MailingList.class.getSimpleName()),
                    RoleTableDefinition.voIdFieldAlias.eq(this.table.getIdAlias()),
                    RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.EDIT)
                    );
            return select.join(RoleTableDefinition.roleMappingTableAsAlias).on(editJoinCondition);

        }else if(PermissionService.READ.equals(permissionType)) {

            Condition readJoinCondition = DSL.or(
                    DSL.and(
                            RoleTableDefinition.voTypeFieldAlias.eq(MailingList.class.getSimpleName()),
                            RoleTableDefinition.voIdFieldAlias.eq(this.table.getIdAlias()),
                            RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.READ)
                            ),
                    DSL.and(
                            RoleTableDefinition.voTypeFieldAlias.eq(MailingList.class.getSimpleName()),
                            RoleTableDefinition.voIdFieldAlias.eq(this.table.getIdAlias()),
                            RoleTableDefinition.permissionTypeFieldAlias.eq(PermissionService.EDIT)
                            )
                    );

            return select.join(RoleTableDefinition.roleMappingTableAsAlias).on(readJoinCondition);
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

    class EmailRecipientRowMapper implements RowMapper<EmailRecipient> {
        @Override
        public EmailRecipient mapRow(ResultSet rs, int rowNum) throws SQLException {
            int type = rs.getInt(1);
            switch (type) {
                case EmailRecipient.TYPE_MAILING_LIST:
                    MailingList ml = new MailingList();
                    ml.setId(rs.getInt(2));
                    ml.setName(rs.getString(4));
                    return ml;
                case EmailRecipient.TYPE_USER:
                    UserEntry ue = new UserEntry();
                    ue.setUserId(rs.getInt(2));
                    return ue;
                case EmailRecipient.TYPE_ADDRESS:
                    AddressEntry ae = new AddressEntry();
                    ae.setAddress(rs.getString(3));
                    return ae;
            }
            throw new ShouldNeverHappenException("Unknown mailing list entry type: " + type);
        }
    }

    class MailingListScheduleInactiveMapper implements RowMapper<Integer> {
        @Override
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt(1);
        }
    }
}
