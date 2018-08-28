/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;

@Repository
public class UserDao extends AbstractDao<User> {
    private static final Log LOG = LogFactory.getLog(UserDao.class);

    private static final LazyInitSupplier<UserDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(UserDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (UserDao)o;
    });

    public static enum UpdatedFields {
        AUTH_TOKEN, PASSWORD, PERMISSIONS, LAST_LOGIN, HOME_URL, MUTED
    }

    private final ConcurrentMap<String, User> userCache = new ConcurrentHashMap<>();

    /**
     * @param typeName
     * @param tablePrefix
     * @param extraProperties
     * @param extraSQL
     */
    private UserDao() {
        super(AuditEventType.TYPE_USER, "u",
                new String[0], false,
                new TranslatableMessage("internal.monitor.USER_COUNT"));
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static UserDao getInstance() {
        return springInstance.get();
    }

    public User getUser(int id) {
        return this.get(id);
    }

    public User getUser(String username) {
        if (username == null) return null;

        return userCache.computeIfAbsent(username.toLowerCase(Locale.ROOT), u -> {
            return queryForObject(SELECT_ALL + " WHERE LOWER(username)=LOWER(?)", new Object[] { u },
                    new UserRowMapper(), null);
        });
    }

    public boolean userExists(int id) {
        return ejt.queryForInt("SELECT count(id) FROM users WHERE id=?", new Object[] {id}, 0) == 1;
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
            user.setPasswordChangeTimestamp(rs.getLong(++i));
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
        // ensure passwords prefixed with {PLAINTEXT} are always hashed before database insertion/update
        // we hash plain text passwords after validation has taken place so we can check complexity etc
        user.hashPlainText();

        if (user.getId() == Common.NEW_ID)
            insertUser(user);
        else
            updateUser(user);
    }

    private static final String USER_INSERT = "INSERT INTO users (username, password, email, phone, " //
            + "disabled, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents, timezone, muted, permissions, name, locale, tokenVersion, passwordVersion, passwordChangeTimestamp) " //
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    void insertUser(User user) {
        int id = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                return ejt.doInsert(
                        USER_INSERT,
                        new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                                boolToChar(user.isDisabled()), user.getHomeUrl(),
                                user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()), user.getTimezone(),
                                boolToChar(user.isMuted()), user.getPermissions(), user.getName(), user.getLocale(), user.getTokenVersion(),
                                user.getPasswordVersion(), Common.timer.currentTimeMillis()},
                        new int[] { Types.VARCHAR, Types.VARCHAR,
                                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                                Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BIGINT}
                        );
            }
        });

        user.setId(id);
        AuditEventType.raiseAddedEvent(AuditEventType.TYPE_USER, user);
        this.countMonitor.increment();

        this.eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.CREATE, user, null, null));
    }

    private static final String USER_UPDATE = "UPDATE users SET " //
            + "  username=?, password=?, email=?, phone=?, disabled=?, homeUrl=?, receiveAlarmEmails=?, " //
            + "  receiveOwnAuditEvents=?, timezone=?, muted=?, permissions=?, name=?, locale=?, passwordVersion=?, passwordChangeTimestamp=?" //
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
                        user.setPasswordChangeTimestamp(Common.timer.currentTimeMillis());
                        user.setPasswordVersion(old.getPasswordVersion() + 1);
                    } else {
                        user.setPasswordChangeTimestamp(old.getPasswordChangeTimestamp());
                        user.setPasswordVersion(old.getPasswordVersion());
                    }

                    ejt.update(
                            USER_UPDATE,
                            new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                                    boolToChar(user.isDisabled()), user.getHomeUrl(),
                                    user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()),
                                    user.getTimezone(), boolToChar(user.isMuted()), user.getPermissions(), user.getName(), user.getLocale(),
                                    user.getPasswordVersion(), user.getPasswordChangeTimestamp(), user.getId() },
                            new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                                    Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                                    Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.BIGINT, Types.INTEGER }
                            );

                    return old;
                }
            });

            if (old == null) {
                throw new NotFoundException();
            }

            AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);

            boolean permissionsChanged = !old.getPermissions().equals(user.getPermissions());
            boolean passwordChanged = user.getPasswordVersion() > originalPwVersion;

            EnumSet<UpdatedFields> fields = EnumSet.noneOf(UpdatedFields.class);
            if (passwordChanged) {
                fields.add(UpdatedFields.PASSWORD);
            }
            if (permissionsChanged) {
                fields.add(UpdatedFields.PERMISSIONS);
            }

            if (passwordChanged || permissionsChanged || user.isDisabled()) {
                exireSessionsForUser(old);
            }

            userCache.remove(old.getUsername().toLowerCase(Locale.ROOT));
            eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, null, old.getUsername(), fields));

        } catch (DataIntegrityViolationException e) {
            // Log some information about the user object.
            LOG.error("Error updating user: " + user, e);
            throw e;
        }
    }

    private void exireSessionsForUser(User user) {
        // web context may not be initialized, can't inject this context
        ApplicationContext context = Common.getRootWebContext();
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

        // expire the user's sessions
        exireSessionsForUser(user);

        userCache.remove(user.getUsername().toLowerCase(Locale.ROOT));
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.DELETE, user, null, null));
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

        userCache.remove(user.getUsername().toLowerCase(Locale.ROOT));
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, null, username, EnumSet.of(UpdatedFields.AUTH_TOKEN)));
    }

    public static final String LOCKED_PASSWORD = "{" + User.LOCKED_ALGORITHM + "}";

    public void lockPassword(User user) {
        updatePasswordHash(user, LOCKED_PASSWORD);
    }

    /**
     * @param user
     * @param newPassword plain text password
     * @throws ValidationException if password is not valid
     */
    public void updatePassword(User user, String newPassword) throws ValidationException {
        // don't want to change the passed in user in case it comes from the cache (in which case another thread might use it)
        User copy = this.get(user.getId());
        copy.setPlainTextPassword(newPassword);
        copy.ensureValid();
        copy.hashPlainText();

        this.updatePasswordHash(user, copy.getPassword());
    }

    private void updatePasswordHash(User user, String newPasswordHash) {
        int userId = user.getId();
        int currentPasswordVersion = user.getPasswordVersion();
        int newPasswordVersion = currentPasswordVersion + 1;
        long passwordChangeTimestamp = Common.timer.currentTimeMillis();
        String username = user.getUsername();

        int count = ejt.update("UPDATE users SET password = ?, passwordVersion = ?, passwordChangeTimestamp = ? WHERE id = ? AND passwordVersion = ? AND username = ?",
                new Object[] { newPasswordHash, newPasswordVersion, passwordChangeTimestamp, userId, currentPasswordVersion, username });
        if (count == 0) {
            throw new EmptyResultDataAccessException("Updated no rows", 1);
        }

        user.setPassword(newPasswordHash);
        user.setPasswordVersion(newPasswordVersion);
        user.setPasswordChangeTimestamp(passwordChangeTimestamp);

        // expire the user's sessions
        exireSessionsForUser(user);
        userCache.remove(user.getUsername().toLowerCase(Locale.ROOT));
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, null, username, EnumSet.of(UpdatedFields.PASSWORD)));
    }

    public void recordLogin(User user) {
        long loginTime = Common.timer.currentTimeMillis();
        user.setLastLogin(loginTime);
        ejt.update("UPDATE users SET lastLogin=? WHERE id=?", new Object[] { loginTime, user.getId() });
        userCache.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, null, user.getUsername(), EnumSet.of(UpdatedFields.LAST_LOGIN)));
    }

    public void saveHomeUrl(int userId, String homeUrl) {
        User old = getUser(userId);
        ejt.update("UPDATE users SET homeUrl=? WHERE id=?", new Object[] { homeUrl, userId });
        User user = getUser(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        userCache.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, null, user.getUsername(), EnumSet.of(UpdatedFields.LAST_LOGIN)));
    }

    public void saveMuted(int userId, boolean muted) {
        User old = getUser(userId);
        ejt.update("UPDATE users SET muted=? WHERE id=?", new Object[] { boolToChar(muted), userId });
        User user = getUser(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        userCache.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, null, user.getUsername(), EnumSet.of(UpdatedFields.MUTED)));
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
                vo.getPasswordVersion(),
                vo.getPasswordChangeTimestamp()
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
        map.put("passwordChangeTimestamp", Types.BIGINT);

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
