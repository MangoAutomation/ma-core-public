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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.exception.NotFoundException;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;

public class UserDao extends AbstractDao<User> {

    public static final UserDao instance = new UserDao();
    private final ConcurrentMap<String, User> userCache = new ConcurrentHashMap<>();

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

    public User getUser(int id) {
        return this.get(id);
    }

    public User getUser(String username) {
        if (username == null) return null;

        return userCache.computeIfAbsent(username, u -> {
            return queryForObject(SELECT_ALL + " WHERE LOWER(username)=LOWER(?)", new Object[] { u },
                    new UserRowMapper(), null);
        });
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
            user.setTokenVersion(rs.getInt(++i));
            user.setPasswordVersion(rs.getInt(++i));
            return user;
        }
    }

    public List<User> getUsers() {
        return query(SELECT_ALL + " ORDER BY username", new Object[0], new UserRowMapper());
    }

    public List<User> getActiveUsers() {
        return query(SELECT_ALL + " WHERE disabled=? ORDER BY username", new Object[] { boolToChar(false) },
                new UserRowMapper());
    }

    public void saveUser(final User user) {
        if (user.getId() == Common.NEW_ID)
            insertUser(user);
        else
            updateUser(user);
    }

    private static final String USER_INSERT = "INSERT INTO users (username, password, email, phone, " //
            + "disabled, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents, timezone, muted, permissions, name, locale, tokenVersion, passwordVersion) " //
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    void insertUser(User user) {
        int id = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                return ejt.doInsert(
                        USER_INSERT,
                        new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                                boolToChar(user.isDisabled()), user.getHomeUrl(),
                                user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()), user.getTimezone(),
                                boolToChar(user.isMuted()), user.getPermissions(), user.getName(), user.getLocale(), user.getTokenVersion(), user.getPasswordVersion() },
                        new int[] { Types.VARCHAR, Types.VARCHAR,
                                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER }
                        );
            }
        });

        user.setId(id);
        AuditEventType.raiseAddedEvent(AuditEventType.TYPE_USER, user);
        this.countMonitor.increment();

        if (handler != null)
            handler.notify("add", user);
    }

    private static final String USER_UPDATE = "UPDATE users SET " //
            + "  username=?, password=?, email=?, phone=?, disabled=?, homeUrl=?, receiveAlarmEmails=?, " //
            + "  receiveOwnAuditEvents=?, timezone=?, muted=?, permissions=?, name=?, locale=?, passwordVersion=?" //
            + " WHERE id=?";

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

        int originalPwVersion = user.getPasswordVersion();

        try {
            User old = getTransactionTemplate().execute(new TransactionCallback<User>() {
                @Override
                public User doInTransaction(TransactionStatus status) {
                    User old = getUser(user.getId());
                    if (old == null) {
                        return null;
                    }

                    boolean passwordChanged = !old.getPassword().equals(user.getPassword());
                    if (passwordChanged) {
                        user.setPasswordVersion(old.getPasswordVersion() + 1);
                    } else {
                        user.setPasswordVersion(old.getPasswordVersion());
                    }

                    ejt.update(
                            USER_UPDATE,
                            new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                                    boolToChar(user.isDisabled()), user.getHomeUrl(),
                                    user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()),
                                    user.getTimezone(), boolToChar(user.isMuted()), user.getPermissions(), user.getName(), user.getLocale(),
                                    user.getPasswordVersion(), user.getId() },
                            new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                                    Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                                    Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER }
                            );

                    return old;
                }
            });

            if (old == null) {
                throw new NotFoundException();
            }

            AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);

            boolean permissionsChanged = !old.getPermissions().equals(user.getPermissions());
            if (user.getPasswordVersion() > originalPwVersion || permissionsChanged || user.isDisabled()) {
                exireSessionsForUser(old);
            }

            userCache.remove(old.getUsername());

            if (handler != null)
                handler.notify("update", user);

        } catch (DataIntegrityViolationException e) {
            // Log some information about the user object.
            LOG.error("Error updating user: " + user, e);
            throw e;
        }
    }

    private void exireSessionsForUser(User user) {
        AnnotationConfigWebApplicationContext context = Common.getRootContext();
        if (context != null) {
            MangoSessionRegistry sessionRegistry = context.getBean(MangoSessionRegistry.class);
            sessionRegistry.exireSessionsForUser(user);
        }
    }

    public void deleteUser(final int userId) {
        User user = getTransactionTemplate().execute(new TransactionCallback<User>() {
            @Override
            public User doInTransaction(TransactionStatus status) {
                User user = get(userId);

                Object[] args = new Object[] { userId };
                ejt.update("UPDATE userComments SET userId=null WHERE userId=?", args);
                ejt.update("DELETE FROM mailingListMembers WHERE userId=?", args);
                ejt.update("DELETE FROM userEvents WHERE userId=?", args);
                ejt.update("UPDATE events SET ackUserId=null, alternateAckSource=? WHERE ackUserId=?", new Object[] {
                        new TranslatableMessage("events.ackedByDeletedUser").serialize(), userId });
                ejt.update("DELETE FROM users WHERE id=?", args);

                return user;
            }
        });

        AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_USER, user);
        countMonitor.decrement();
        if (handler != null)
            handler.notify("delete", user);

        // expire the user's sessions
        exireSessionsForUser(user);
        userCache.remove(user.getUsername());
    }

    public void revokeTokens(User user) {
        int userId = user.getId();
        int currentTokenVersion = user.getTokenVersion();
        int newTokenVersion = currentTokenVersion + 1;
        String username = user.getUsername();

        int count = ejt.update("UPDATE users SET tokenVersion = ? WHERE id = ? AND tokenVersion = ? AND username = ?", new Object[] { newTokenVersion, userId, currentTokenVersion, username });
        if (count == 0) {
            throw new EmptyResultDataAccessException("Updated no rows", 1);
        }

        user.setTokenVersion(newTokenVersion);

        userCache.remove(user.getUsername());
    }

    public static final String LOCKED_PASSWORD = "{LOCKED}";

    public void lockPassword(User user) {
        int userId = user.getId();
        int currentPasswordVersion = user.getPasswordVersion();
        int newPasswordVersion = currentPasswordVersion + 1;
        String username = user.getUsername();

        int count = ejt.update("UPDATE users SET password = ?, passwordVersion = ? WHERE id = ? AND passwordVersion = ? AND username = ?",
                new Object[] { LOCKED_PASSWORD, newPasswordVersion, userId, currentPasswordVersion, username });
        if (count == 0) {
            throw new EmptyResultDataAccessException("Updated no rows", 1);
        }

        user.setPassword(LOCKED_PASSWORD);
        user.setPasswordVersion(newPasswordVersion);

        // expire the user's sessions
        exireSessionsForUser(user);
        userCache.remove(user.getUsername());
    }

    public void recordLogin(User user) {
        long loginTime = Common.timer.currentTimeMillis();
        user.setLastLogin(loginTime);
        ejt.update("UPDATE users SET lastLogin=? WHERE id=?", new Object[] { loginTime, user.getId() });
    }

    public void saveHomeUrl(int userId, String homeUrl) {
        User old = getUser(userId);
        ejt.update("UPDATE users SET homeUrl=? WHERE id=?", new Object[] { homeUrl, userId });
        User user = getUser(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        userCache.put(user.getUsername(), user);
    }

    public void saveMuted(int userId, boolean muted) {
        User old = getUser(userId);
        ejt.update("UPDATE users SET muted=? WHERE id=?", new Object[] { boolToChar(muted), userId });
        User user = getUser(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        userCache.put(user.getUsername(), user);
    }

    @Override
    public void save(User user, String initiatorId) {
        throw new UnsupportedOperationException("Use saveUser()");
    }

    @Override
    public void delete(User user, String initiatorId) {
        throw new UnsupportedOperationException("Use deleteUser()");
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
                vo.getLocale(),
                vo.getTokenVersion(),
                vo.getPasswordVersion()
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
        map.put("tokenVersion", Types.INTEGER);
        map.put("passwordVersion", Types.INTEGER);

        return map;
    }

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
     */
    @Override
    protected String getTableName() {
        return SchemaDefinition.USERS_TABLE;
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
