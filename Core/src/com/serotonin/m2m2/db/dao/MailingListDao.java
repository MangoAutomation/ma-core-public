/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.RecipientListEntryBean;
import com.serotonin.m2m2.vo.mailingList.UserEntry;
import com.serotonin.m2m2.vo.permission.Permissions;

/**
 * @author Matthew Lohbihler
 */
@Repository
public class MailingListDao extends AbstractDao<MailingList> {

    private static final LazyInitSupplier<MailingListDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(MailingListDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (MailingListDao)o;
    });


    private MailingListDao(){
        super(AuditEventType.TYPE_MAILING_LIST, "ml", new String[] {}, false, new TranslatableMessage("internal.monitor.MAILING_LIST_COUNT"));
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
            query(SELECT_ALL + " where receiveAlarmEmails>=0 and receiveAlarmEmails<=?", new Object[] {alarmLevel.value()}, new MailingListRowMapper(), (list, index) -> {
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
    }

    public void populateEntrySubclasses(List<EmailRecipient> entries) {
        // Update the user type entries with their respective user objects.
        UserDao userDao = UserDao.getInstance();
        for (EmailRecipient e : entries) {
            if (e instanceof MailingList)
                loadRelationalData((MailingList) e);
            else if (e instanceof UserEntry) {
                UserEntry ue = (UserEntry) e;
                ue.setUser(userDao.getUser(ue.getUserId()));
            }
        }
    }

    @Override
    protected String getXidPrefix() {
        return MailingList.XID_PREFIX;
    }

    @Override
    public MailingList getNewVo() {
        return new MailingList();
    }

    @Override
    protected String getTableName() {
        return SchemaDefinition.MAILING_LISTS_TABLE;
    }

    @Override
    protected Object[] voToObjectArray(MailingList vo) {
        return new Object[] {
                vo.getXid(),
                vo.getName(),
                vo.getReceiveAlarmEmails().value(),
                vo.getReadPermissions() == null ? null : Permissions.implodePermissionGroups(vo.getReadPermissions()),
                        vo.getEditPermissions() == null ? null : Permissions.implodePermissionGroups(vo.getEditPermissions())
        };
    }

    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("id", Types.INTEGER);
        map.put("xid", Types.VARCHAR);
        map.put("name", Types.VARCHAR);
        map.put("receiveAlarmEmails", Types.INTEGER);
        map.put("readPermission", Types.VARCHAR);
        map.put("editPermission", Types.VARCHAR);
        return map;
    }

    @Override
    protected Map<String, IntStringPair> getPropertiesMap() {
        HashMap<String, IntStringPair> map = new HashMap<String, IntStringPair>();
        return map;
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
            ml.setReadPermissions(Permissions.explodePermissionGroups(rs.getString(++i)));
            ml.setEditPermissions(Permissions.explodePermissionGroups(rs.getString(++i)));
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

    /*
     *
     * BELOW HERE TO BE REMOVED
     *
     *
    public Set<String> generateRecipientAddresses(List<EmailRecipient> entries, DateTime sendTime) {

        populateEntrySubclasses(entries);
        Set<String> addresses = new HashSet<String>();
        for (EmailRecipient entry : entries)
            entry.appendAddresses(addresses, sendTime);
        return addresses;
    }
     */


}
