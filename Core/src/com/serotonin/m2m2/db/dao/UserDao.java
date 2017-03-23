/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSecurityConfiguration;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoUserDetailsService;

public class UserDao extends AbstractDao<User> {
	
	public static final UserDao instance = new UserDao();
	
    /**
	 * @param typeName
	 * @param tablePrefix
	 * @param extraProperties
	 * @param extraSQL
	 */
	private UserDao() {
		super(ModuleRegistry.getWebSocketHandlerDefinition("USER"), AuditEventType.TYPE_USER, new TranslatableMessage("internal.monitor.USER_COUNT"));
	}

	private static final Log LOG = LogFactory.getLog(UserDao.class);

    private static final String USER_SELECT = //
    "SELECT id, username, password, email, phone, disabled, homeUrl, " //
            + "lastLogin, receiveAlarmEmails, receiveOwnAuditEvents, timezone, muted, permissions, name, locale FROM users ";

    public User getUser(int id) {
        return queryForObject(USER_SELECT + "WHERE id=?", new Object[] { id }, new UserRowMapper(), null);
    }

    public User getUser(String username) {
        return queryForObject(USER_SELECT + "WHERE LOWER(username)=LOWER(?)", new Object[] { username },
                new UserRowMapper(), null);
    }
    
    /**
     * For use in Spring Security Layer to Get a User with its authorities pre-set
     * @param username
     * @return
     */
    public User getUserWithAuthorities(String username){
    	return queryForObject(USER_SELECT + "WHERE LOWER(username)=LOWER(?)", new Object[] { username },
                new UserAuthoritiesRowMapper(), null);
    }
    
    public boolean userExists(int id) {
    	return ejt.queryForInt("SELECT count(id) FROM users WHERE id="+id, new Object[0], 0) == 1;
    }

    class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            int i = 0;
            user.setId(rs.getInt(++i));
            user.setUsername(rs.getString(++i));
            user.setPassword(rs.getString(++i));
            user.setEmail(rs.getString(++i));
            user.setPhone(rs.getString(++i));
            //user.setAdmin(charToBool(rs.getString(++i)));
            user.setDisabled(charToBool(rs.getString(++i)));
            user.setHomeUrl(rs.getString(++i));
            user.setLastLogin(rs.getLong(++i));
            user.setReceiveAlarmEmails(rs.getInt(++i));
            user.setReceiveOwnAuditEvents(charToBool(rs.getString(++i)));
            user.setTimezone(rs.getString(++i));
            user.setMuted(charToBool(rs.getString(++i)));
            user.setPermissions(rs.getString(++i));
            user.setName(rs.getString(++i));
            user.setLocale(rs.getString(++i));
            return user;
        }
    }
    
    /**
     * Map a User with its Authorities pre-built for Spring Security
     * 
     * @author Terry Packer
     */
    class UserAuthoritiesRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            String permissions = rs.getString(13);
            User user = new User(MangoUserDetailsService.getGrantedAuthorities(permissions));
            
            user.setId(rs.getInt(1));
            user.setUsername(rs.getString(2));
            user.setPassword(rs.getString(3));
            user.setEmail(rs.getString(4));
            user.setPhone(rs.getString(5));
            user.setDisabled(charToBool(rs.getString(6)));
            user.setHomeUrl(rs.getString(7));
            user.setLastLogin(rs.getLong(8));
            user.setReceiveAlarmEmails(rs.getInt(9));
            user.setReceiveOwnAuditEvents(charToBool(rs.getString(10)));
            user.setTimezone(rs.getString(11));
            user.setMuted(charToBool(rs.getString(12)));
            user.setPermissions(permissions);
            user.setName(rs.getString(14));
            user.setLocale(rs.getString(15));
            return user;
        }
    }
    
    public List<User> getUsers() {
        return query(USER_SELECT + "ORDER BY username", new Object[0], new UserRowMapper());
    }

    public List<User> getActiveUsers() {
        return query(USER_SELECT + "WHERE disabled=? ORDER BY username", new Object[] { boolToChar(false) },
                new UserRowMapper());
    }

    public void saveUser(final User user) {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                if (user.getId() == Common.NEW_ID)
                    insertUser(user);
                else
                    updateUser(user);
            }
        });
    }

    private static final String USER_INSERT = "INSERT INTO users (username, password, email, phone, " //
            + "disabled, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents, timezone, muted, permissions, name, locale) " //
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    void insertUser(User user) {
        int id = ejt.doInsert(
                USER_INSERT,
                new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                        boolToChar(user.isDisabled()), user.getHomeUrl(),
                        user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()), user.getTimezone(),
                        boolToChar(user.isMuted()), user.getPermissions(), user.getName(), user.getLocale() }, new int[] { Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });
        user.setId(id);
        AuditEventType.raiseAddedEvent(AuditEventType.TYPE_USER, user);
        this.countMonitor.increment();
        
        if (handler != null)
            handler.notify("add", user);
        
    }

    private static final String USER_UPDATE = "UPDATE users SET " //
            + "  username=?, password=?, email=?, phone=?, disabled=?, homeUrl=?, receiveAlarmEmails=?, " //
            + "  receiveOwnAuditEvents=?, timezone=?, muted=?, permissions=?, name=?, locale=? " //
            + "WHERE id=?";

    void updateUser(User user) {
        // Potential fix for "An attempt was made to get a data value of type 'VARCHAR' from a data value of type 'null'"
        if (user.getPhone() == null)
            user.setPhone("");
        if (user.getHomeUrl() == null)
            user.setHomeUrl("");
        if (user.getTimezone() == null)
            user.setTimezone("");
        if (user.getName() == null)
            user.setName("");
        if (user.getLocale() == null)
            user.setLocale("");
        User old = getUser(user.getId());
        try {
            ejt.update(
                    USER_UPDATE,
                    new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                            boolToChar(user.isDisabled()), user.getHomeUrl(),
                            user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()),
                            user.getTimezone(), boolToChar(user.isMuted()), user.getPermissions(), user.getName(), user.getLocale(), user.getId() },
                    new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                            Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER });
            AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
            if (handler != null)
                handler.notify("update", user);
            //Update User In Session
            MangoSecurityConfiguration.replaceUserInSessions(old, user);
        }
        catch (DataIntegrityViolationException e) {
            // Log some information about the user object.
            LOG.error("Error updating user: " + user, e);
            throw e;
        }
    }

    public void deleteUser(final int userId) {
    	User user = getUser(userId);
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @SuppressWarnings("synthetic-access")
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Object[] args = new Object[] { userId };
                ejt.update("UPDATE userComments SET userId=null WHERE userId=?", args);
                ejt.update("DELETE FROM mailingListMembers WHERE userId=?", args);
                ejt.update("DELETE FROM userEvents WHERE userId=?", args);
                ejt.update("UPDATE events SET ackUserId=null, alternateAckSource=? WHERE ackUserId=?", new Object[] {
                        new TranslatableMessage("events.ackedByDeletedUser").serialize(), userId });
                ejt.update("DELETE FROM users WHERE id=?", args);
                AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_USER, user);
                countMonitor.decrement();
                if (handler != null)
                    handler.notify("delete", user);
                //Update User In Session
                MangoSecurityConfiguration.replaceUserInSessions(user, null);
            }
        });
        
    }

    public void recordLogin(int userId) {
        ejt.update("UPDATE users SET lastLogin=? WHERE id=?", new Object[] { Common.backgroundProcessing.currentTimeMillis(), userId });
    }

    public void saveHomeUrl(int userId, String homeUrl) {
    	User old = getUser(userId);
        ejt.update("UPDATE users SET homeUrl=? WHERE id=?", new Object[] { homeUrl, userId });
        User user = getUser(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        //Update User In Session
        MangoSecurityConfiguration.replaceUserInSessions(old, user);
    }

    public void saveMuted(int userId, boolean muted) {
    	User old = getUser(userId);
        ejt.update("UPDATE users SET muted=? WHERE id=?", new Object[] { boolToChar(muted), userId });
        User user = getUser(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        //Update User In Session
        MangoSecurityConfiguration.replaceUserInSessions(old, user);
    }
    
    //Overrides for use in AbstractBasicDao
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractVO)
	 */
	@Override
	protected Object[] voToObjectArray(User vo) {
		return new Object[]{
				vo.getUsername(),
				vo.getPassword(),
				vo.getEmail(),
				vo.getPhone(),
				vo.isDisabled(),
				vo.getHomeUrl(),
				vo.getLastLogin(),
				vo.getReceiveAlarmEmails(),
				vo.isReceiveOwnAuditEvents(),
				vo.getTimezone(),
				vo.isMuted(),
				vo.getPermissions(),
                vo.getName(),
                vo.getLocale()
		};
	}
    
	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<User> getRowMapper() {
		return new UserRowMapper();
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("id", Types.INTEGER);
		map.put("username", Types.VARCHAR);
		map.put("password", Types.VARCHAR);
		map.put("email", Types.VARCHAR);
		map.put("phone", Types.VARCHAR);
		map.put("disabled", Types.CHAR);
		map.put("homeUrl", Types.VARCHAR);
		map.put("lastLogin", Types.BIGINT);
		map.put("receiveAlarmEmails", Types.INTEGER);
		map.put("receiveOwnAuditEvents", Types.CHAR);
		map.put("timezone", Types.VARCHAR);
		map.put("muted", Types.CHAR);
		map.put("permissions", Types.VARCHAR);
        map.put("name", Types.VARCHAR);
        map.put("locale", Types.VARCHAR);

		return map;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return "users";
	}


	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		return new HashMap<>();
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return "";
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public User getNewVo() {
		return new User();
	}

}
