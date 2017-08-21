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

import org.joda.time.DateTime;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.infiniteautomation.mango.monitor.AtomicIntegerMonitor;
import com.infiniteautomation.mango.monitor.ValueMonitorOwner;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.mailingList.EmailRecipient;
import com.serotonin.m2m2.vo.mailingList.MailingList;
import com.serotonin.m2m2.vo.mailingList.UserEntry;
import com.serotonin.m2m2.web.dwr.beans.RecipientListEntryBean;

/**
 * @author Matthew Lohbihler
 */
public class MailingListDao extends BaseDao implements ValueMonitorOwner{
	
	public static final MailingListDao instance = new MailingListDao();
	
    //Monitor for count of table
    protected final AtomicIntegerMonitor countMonitor;

    private MailingListDao(){
		this.countMonitor = new AtomicIntegerMonitor(this.getClass().getCanonicalName() + ".COUNT", new TranslatableMessage("internal.monitor.MAILING_LIST_COUNT"), this);
        this.countMonitor.setValue(this.count());
    	Common.MONITORED_VALUES.addIfMissingStatMonitor(this.countMonitor);
	}
    
    public String generateUniqueXid() {
        return generateUniqueXid(MailingList.XID_PREFIX, SchemaDefinition.MAILING_LISTS_TABLE);
    }

    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, SchemaDefinition.MAILING_LISTS_TABLE);
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
    	List<MailingList> result = query(MAILING_LIST_SELECT + "where receiveAlarmEmails>=0 and receiveAlarmEmails<="+alarmLevel, new MailingListRowMapper());
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
        UserDao userDao = UserDao.instance;
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
    
	/* (non-Javadoc)
	 * @see com.infiniteautomation.mango.monitor.ValueMonitorOwner#reset(java.lang.String)
	 */
	@Override
	public void reset(String monitorId) {
		//We only have one monitor so:
		this.countMonitor.setValue(this.count());
	}
	
	public AtomicIntegerMonitor getMonitor(){
		return this.countMonitor;
	}
}
