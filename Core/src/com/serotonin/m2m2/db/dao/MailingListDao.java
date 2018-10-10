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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.infiniteautomation.mango.monitor.AtomicIntegerMonitor;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.UserEntry;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;

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


    private static final String MAILING_LIST_SELECT = "select id, xid, name, receiveAlarmEmails from mailingLists ";
    private static final String COUNT = "SELECT COUNT(DISTINCT id) FROM mailingLists";
    
    public int count(){
    	return ejt.queryForInt(COUNT, new Object[0], 0);
    }

    public List<MailingList> getMailingLists() {
        List<MailingList> result = query(MAILING_LIST_SELECT + "order by name", new MailingListRowMapper());
        setRelationalData(result);
        return result;
    }
    
    public List<MailingList> getAlarmMailingLists(int alarmLevel) {
    	List<MailingList> result = query(MAILING_LIST_SELECT + "where receiveAlarmEmails>=0 and receiveAlarmEmails<=?", new Object[] {alarmLevel}, new MailingListRowMapper());
    	setRelationalData(result);
    	return result;
    }

    public MailingList getMailingList(int id) {
        MailingList ml = queryForObject(MAILING_LIST_SELECT + "where id=?", new Object[] { id }, new MailingListRowMapper(), null);
        if(ml != null)
        	setRelationalData(ml);
        return ml;
    }

    public MailingList getMailingList(String xid) {
        MailingList ml = queryForObject(MAILING_LIST_SELECT + "where xid=?", new Object[] { xid },
                new MailingListRowMapper(), null);
        if (ml != null)
            setRelationalData(ml);
        return ml;
    }

    class MailingListRowMapper implements RowMapper<MailingList> {
        public MailingList mapRow(ResultSet rs, int rowNum) throws SQLException {
            MailingList ml = new MailingList();
            ml.setId(rs.getInt(1));
            ml.setXid(rs.getString(2));
            ml.setName(rs.getString(3));
            ml.setReceiveAlarmEmails(rs.getInt(4));
            return ml;
        }
    }

    private void setRelationalData(List<MailingList> mls) {
        for (MailingList ml : mls)
            setRelationalData(ml);
    }

    private static final String MAILING_LIST_INACTIVE_SELECT = "select inactiveInterval from mailingListInactive where mailingListId=?";
    private static final String MAILING_LIST_ENTRIES_SELECT = "select typeId, userId, address, '' from mailingListMembers where mailingListId=?";

    private void setRelationalData(MailingList ml) {
        ml.getInactiveIntervals().addAll(
                query(MAILING_LIST_INACTIVE_SELECT, new Object[] { ml.getId() },
                        new MailingListScheduleInactiveMapper()));

        ml.setEntries(query(MAILING_LIST_ENTRIES_SELECT, new Object[] { ml.getId() }, new EmailRecipientRowMapper()));

        // Update the user type entries with their respective user objects.
        populateEntrySubclasses(ml.getEntries());
    }

    class MailingListScheduleInactiveMapper implements RowMapper<Integer> {
        public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return rs.getInt(1);
        }
    }

    class EmailRecipientRowMapper implements RowMapper<EmailRecipient> {
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

    /**
     * Generate the recipient addresses
     * @param entries
     * @param sendTime
     * @return
     */
    public Set<String> generateRecipientAddresses(List<EmailRecipient> entries, DateTime sendTime) {
 
        populateEntrySubclasses(entries);
        Set<String> addresses = new HashSet<String>();
        for (EmailRecipient entry : entries)
            entry.appendAddresses(addresses, sendTime);
        return addresses;
    }
    
    public void populateEntrySubclasses(List<EmailRecipient> entries) {
        // Update the user type entries with their respective user objects.
        UserDao userDao = UserDao.getInstance();
        for (EmailRecipient e : entries) {
            if (e instanceof MailingList)
                // NOTE: this does not set the mailing list name.
                setRelationalData((MailingList) e);
            else if (e instanceof UserEntry) {
                UserEntry ue = (UserEntry) e;
                ue.setUser(userDao.getUser(ue.getUserId()));
            }
        }
    }

    private static final String MAILING_LIST_INSERT = "insert into mailingLists (xid, name, receiveAlarmEmails) values (?,?,?)";
    private static final String MAILING_LIST_UPDATE = "update mailingLists set xid=?, name=?, receiveAlarmEmails=? where id=?";

    public void saveMailingList(final MailingList ml) {
        final ExtendedJdbcTemplate ejt2 = ejt;
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @SuppressWarnings("synthetic-access")
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                if (ml.getId() == Common.NEW_ID){
                    ml.setId(doInsert(MAILING_LIST_INSERT, new Object[] { ml.getXid(), ml.getName(), ml.getReceiveAlarmEmails() }));
                    countMonitor.increment();
                }else
                    ejt2.update(MAILING_LIST_UPDATE, new Object[] { ml.getXid(), ml.getName(), ml.getReceiveAlarmEmails(), ml.getId() });
                saveRelationalData(ml);
            }
        });
    }

    private static final String MAILING_LIST_INACTIVE_INSERT = "insert into mailingListInactive (mailingListId, inactiveInterval) values (?,?)";
    private static final String MAILING_LIST_ENTRY_INSERT = "insert into mailingListMembers (mailingListId, typeId, userId, address) values (?,?,?,?)";

    void saveRelationalData(final MailingList ml) {
        // Save the inactive intervals.
        ejt.update("delete from mailingListInactive where mailingListId=?", new Object[] { ml.getId() });

        // Save what is in the mailing list object.
        final List<Integer> intervalIds = new ArrayList<Integer>(ml.getInactiveIntervals());
        ejt.batchUpdate(MAILING_LIST_INACTIVE_INSERT, new BatchPreparedStatementSetter() {
            public int getBatchSize() {
                return intervalIds.size();
            }

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, ml.getId());
                ps.setInt(2, intervalIds.get(i));
            }
        });

        // Delete existing entries
        ejt.update("delete from mailingListMembers where mailingListId=?", new Object[] { ml.getId() });

        // Save what is in the mailing list object.
        final List<EmailRecipient> entries = ml.getEntries();
        ejt.batchUpdate(MAILING_LIST_ENTRY_INSERT, new BatchPreparedStatementSetter() {
            public int getBatchSize() {
                return entries.size();
            }

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EmailRecipient e = entries.get(i);
                ps.setInt(1, ml.getId());
                ps.setInt(2, e.getRecipientType());
                ps.setInt(3, e.getReferenceId());
                ps.setString(4, e.getReferenceAddress());
            }
        });

    }

    public void deleteMailingList(final int mailingListId) {
        ejt.update("delete from mailingLists where id=?", new Object[] { mailingListId });
        this.countMonitor.decrement();
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("id", Types.INTEGER);
        map.put("xid", Types.VARCHAR);
        map.put("name", Types.VARCHAR);
        return map;
    }

    @Override
    protected Map<String, IntStringPair> getPropertiesMap() {
        HashMap<String, IntStringPair> map = new HashMap<String, IntStringPair>();
        return map;
    }

    @Override
    public RowMapper<MailingList> getRowMapper() {
        // TODO Auto-generated method stub
        return null;
    }
    
    //TODO Mango 3.6 Remove this and fix Internal Module to use correct method
    public AtomicIntegerMonitor getMonitor() {
        return super.getCountMonitor();
    }
}
