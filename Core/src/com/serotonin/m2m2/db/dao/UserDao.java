/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Record;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao.RoleDeletedDaoEvent;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.web.mvc.spring.security.MangoSessionRegistry;

/**
 * TODO Mango 4.0 Move userCache into service?
 * @author Terry Packer
 *
 */
@Repository
public class UserDao extends AbstractDao<User, UserTableDefinition> {
    private static final Log LOG = LogFactory.getLog(UserDao.class);

    private static final LazyInitSupplier<UserDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(UserDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (UserDao)o;
    });

    private final RoleDao roleDao;
    private final ConcurrentMap<String, User> userCache = new ConcurrentHashMap<>();

    @Autowired
    private UserDao(RoleDao roleDao,
            UserTableDefinition table,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(AuditEventType.TYPE_USER,
                table,
                new TranslatableMessage("internal.monitor.USER_COUNT"),
                mapper, publisher);
        this.roleDao = roleDao;
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static UserDao getInstance() {
        return springInstance.get();
    }

    /**
     * Confirm that this username is not used
     * @param username
     * @param excludeId
     * @return
     */
    public boolean isUsernameUnique(String username, int excludeId) {
        if(username == null) {
            return false;
        }else {
            return this.getCountQuery().from(this.table.getTableAsAlias()).where(
                    this.table.getField("username").eq(username),
                    this.table.getField("id").notEqual(excludeId)).fetchOneInto(Integer.class) == 0;
        }
    }

    /**
     * Confirm that this email address is not used
     * @param email
     * @param excludeId
     * @return
     */
    public boolean isEmailUnique(String email, int excludeId) {
        if(email == null) {
            return false;
        }else {
            return this.getCountQuery().from(this.table.getTableAsAlias()).where(
                    this.table.getField("email").eq(email),
                    this.table.getField("id").notEqual(excludeId)).fetchOneInto(Integer.class) == 0;
        }
    }

    /**
     * Get a user from the cache, load from database first if necessary
     * @param username
     * @return
     */
    @Override
    public User getByXid(String username) {
        if (username == null) return null;

        return userCache.computeIfAbsent(username.toLowerCase(Locale.ROOT), u -> {
            Select<Record> query = getJoinedSelectQuery().where(this.table.getField("username").equalIgnoreCase(username));
            List<Object> args = query.getBindValues();
            User user = ejt.query(query.getSQL(), args.toArray(new Object[args.size()]),
                    getObjectResultSetExtractor());
            if(user != null) {
                loadRelationalData(user);
            }
            return user;
        });
    }

    /**
     * Get the roles for a user from the database mapping table
     * @param vo
     * @return
     */
    public Set<Role> getUserRoles(User vo) {
        return query(USER_ROLES_SELECT, new Object[] {vo.getId()}, roleDao.getRoleSetResultSetExtractor());
    }

    @Override
    public void loadRelationalData(User vo) {
        vo.setRoles(getUserRoles(vo));
    }

    @Override
    public void saveRelationalData(User vo, boolean insert) {
        if(!insert) {
            //delete role mappings
            ejt.update(USER_ROLES_DELETE, new Object[] {vo.getId()});
        }
        //insert role mappings
        List<Role> entries = new ArrayList<>(vo.getRoles());
        ejt.batchUpdate(USER_ROLE_INSERT, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return entries.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Role role = entries.get(i);
                ps.setInt(1, role.getId());
                ps.setInt(2, vo.getId());
            }
        });
    }

    /**
     * Get a user by their email address
     * @param emailAddress
     * @return
     */
    public User getUserByEmail(String emailAddress) {
        if (emailAddress == null) return null;
        Select<Record> query = getJoinedSelectQuery().where(this.table.getField("email").eq(emailAddress));
        List<Object> args = query.getBindValues();
        return ejt.query(query.getSQL(), args.toArray(new Object[args.size()]), getObjectResultSetExtractor());
    }

    class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            int i = 0;
            user.setId(rs.getInt(++i));
            user.setUsername(rs.getString(++i));
            user.setName(rs.getString(++i));
            user.setPassword(rs.getString(++i));
            user.setEmail(rs.getString(++i));
            user.setPhone(rs.getString(++i));
            user.setDisabled(charToBool(rs.getString(++i)));
            user.setLastLogin(rs.getLong(++i));
            user.setHomeUrl(rs.getString(++i));
            user.setReceiveAlarmEmails(AlarmLevels.fromValue(rs.getInt(++i)));
            user.setReceiveOwnAuditEvents(charToBool(rs.getString(++i)));
            user.setTimezone(rs.getString(++i));
            user.setMuted(charToBool(rs.getString(++i)));
            user.setLocale(rs.getString(++i));
            user.setTokenVersion(rs.getInt(++i));
            user.setPasswordVersion(rs.getInt(++i));
            user.setPasswordChangeTimestamp(rs.getLong(++i));
            user.setSessionExpirationOverride(charToBool(rs.getString(++i)));
            user.setSessionExpirationPeriods(rs.getInt(++i));
            user.setSessionExpirationPeriodType(rs.getString(++i));
            user.setOrganization(rs.getString(++i));
            user.setOrganizationalRole(rs.getString(++i));
            user.setCreated(new Date(rs.getLong(++i)));
            Date emailVerified = new Date(rs.getLong(++i));
            if (rs.wasNull()) {
                emailVerified = null;
            }
            user.setEmailVerified(emailVerified);
            user.setData(extractData(rs.getClob(++i)));
            return user;
        }
    }

    public List<User> getActiveUsers() {
        Select<Record> query = getJoinedSelectQuery().where(this.table.getField("disabled").eq("N"));
        List<Object> args = query.getBindValues();
        return query(query.getSQL(), args.toArray(new Object[args.size()]), getListResultSetExtractor());
    }

    private static final String USER_ROLES_DELETE = "DELETE FROM userRoleMappings WHERE userId=?";
    private static final String USER_ROLE_INSERT = "INSERT INTO userRoleMappings (roleId, userId) VALUES (?,?)";
    private static final String USER_ROLES_SELECT = "SELECT r.id, r.xid, r.name FROM userRoleMappings AS ur JOIN roles r ON ur.roleId=r.id WHERE ur.userId=?";

    @Override
    public void insert(User vo) {
        // ensure passwords prefixed with {PLAINTEXT} are always hashed before database insertion/update
        // we hash plain text passwords after validation has taken place so we can check complexity etc
        vo.hashPlainText();
        if(vo.getCreatedTs() == null) {
            vo.setCreated(new Date(Common.timer.currentTimeMillis()));
        }
        enforceUserRole(vo);
        super.insert(vo);
    }

    @Override
    public void update(User existing, User vo) {
        // ensure passwords prefixed with {PLAINTEXT} are always hashed before database insertion/update
        // we hash plain text passwords after validation has taken place so we can check complexity etc
        vo.hashPlainText();
        try {
            User old = getTransactionTemplate().execute(new TransactionCallback<User>() {
                @Override
                public User doInTransaction(TransactionStatus status) {
                    User old = get(vo.getId());
                    if (old == null) {
                        return null;
                    }
                    enforceUserRole(vo);
                    boolean passwordChanged = !old.getPassword().equals(vo.getPassword());
                    if (passwordChanged) {
                        vo.setPasswordChangeTimestamp(Common.timer.currentTimeMillis());
                        vo.setPasswordVersion(old.getPasswordVersion() + 1);
                    } else {
                        vo.setPasswordChangeTimestamp(old.getPasswordChangeTimestamp());
                        vo.setPasswordVersion(old.getPasswordVersion());
                    }
                    UserDao.super.update(old, vo);
                    //Set the last login time so it is available on the saved user
                    vo.setLastLogin(old.getLastLogin());

                    boolean permissionsChanged = !old.getRoles().equals(vo.getRoles());

                    if (passwordChanged || permissionsChanged || vo.isDisabled()) {
                        exireSessionsForUser(old);
                    }

                    userCache.remove(old.getUsername().toLowerCase(Locale.ROOT));
                    return old;
                }
            });

            if (old == null) {
                throw new NotFoundException();
            }
        } catch (DataIntegrityViolationException e) {
            // Log some information about the user object.
            LOG.error("Error updating user: " + vo, e);
            throw e;
        }
    }

    private void enforceUserRole(User vo) {
        Role userRole = PermissionHolder.USER_ROLE;
        if(!vo.getRoles().contains(userRole)) {
            Set<Role> updated = new HashSet<>(vo.getRoles());
            updated.add(userRole);
            vo.setRoles(Collections.unmodifiableSet(updated));
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

    @Override
    public boolean delete(User vo) {
        boolean deleted = super.delete(vo);
        if(deleted) {
            // expire the user's sessions
            exireSessionsForUser(vo);
            userCache.remove(vo.getUsername().toLowerCase(Locale.ROOT));
        }
        return deleted;
    }

    @Override
    public void deleteRelationalData(User vo) {
        Object[] args = new Object[] { vo.getId() };
        ejt.update("UPDATE userComments SET userId=null WHERE userId=?", args);
        ejt.update("DELETE FROM mailingListMembers WHERE userId=?", args);
        ejt.update("UPDATE events SET ackUserId=null, alternateAckSource=? WHERE ackUserId=?", new Object[] {
                new TranslatableMessage("events.ackedByDeletedUser").serialize(), vo.getId() });
    }

    /**
     * Revoke all tokens for user
     * @param user
     */
    public void revokeTokens(User user) {
        int userId = user.getId();
        User old = get(userId);

        int currentTokenVersion = user.getTokenVersion();
        int newTokenVersion = currentTokenVersion + 1;
        String username = user.getUsername();


        int count = ejt.update("UPDATE users SET tokenVersion = ? WHERE id = ? AND tokenVersion = ? AND username = ?", new Object[] { newTokenVersion, userId, currentTokenVersion, username });
        if (count == 0) {
            throw new EmptyResultDataAccessException("Updated no rows", 1);
        }

        user.setTokenVersion(newTokenVersion);

        userCache.remove(user.getUsername().toLowerCase(Locale.ROOT));
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, old));
    }

    public static final String LOCKED_PASSWORD = "{" + User.LOCKED_ALGORITHM + "}";

    public void lockPassword(User user) {
        updatePasswordHash(user, LOCKED_PASSWORD);
    }

    /**
     * Update the hash for a user and remove from cache
     * @param user
     * @param newPasswordHash
     */
    public void updatePasswordHash(User user, String newPasswordHash) {
        int userId = user.getId();
        User old = get(userId);

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
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, old));
    }

    public void recordLogin(User user) {
        User old = get(user.getId());

        Objects.requireNonNull(old);
        Objects.requireNonNull(user);

        long loginTime = Common.timer.currentTimeMillis();
        user.setLastLogin(loginTime);
        ejt.update("UPDATE users SET lastLogin=? WHERE id=?", new Object[] { loginTime, user.getId() });
        userCache.put(user.getUsername().toLowerCase(Locale.ROOT), user);

        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, old));
    }

    public void saveHomeUrl(int userId, String homeUrl) {
        User old = get(userId);
        ejt.update("UPDATE users SET homeUrl=? WHERE id=?", new Object[] { homeUrl, userId });
        User user = get(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        userCache.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, old));
    }

    public void saveMuted(int userId, boolean muted) {
        User old = get(userId);
        ejt.update("UPDATE users SET muted=? WHERE id=?", new Object[] { boolToChar(muted), userId });
        User user = get(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        userCache.put(user.getUsername().toLowerCase(Locale.ROOT), user);
        eventPublisher.publishEvent(new DaoEvent<User>(this, DaoEventType.UPDATE, user, old));
    }

    @Override
    protected Object[] voToObjectArray(User vo) {
        return new Object[]{
                vo.getUsername(),
                vo.getName(),
                vo.getPassword(),
                vo.getEmail(),
                vo.getPhone(),
                boolToChar(vo.isDisabled()),
                vo.getLastLogin(),
                vo.getHomeUrl(),
                vo.getReceiveAlarmEmails().value(),
                boolToChar(vo.isReceiveOwnAuditEvents()),
                vo.getTimezone(),
                boolToChar(vo.isMuted()),
                vo.getLocale(),
                vo.getTokenVersion(),
                vo.getPasswordVersion(),
                vo.getPasswordChangeTimestamp(),
                boolToChar(vo.isSessionExpirationOverride()),
                vo.getSessionExpirationPeriods(),
                vo.getSessionExpirationPeriodType(),
                vo.getOrganization(),
                vo.getOrganizationalRole(),
                vo.getCreatedTs(),
                vo.getEmailVerifiedTs(),
                convertData(vo.getData())
        };
    }

    @Override
    public RowMapper<User> getRowMapper() {
        return new UserRowMapper();
    }

    @Override
    protected String getXidPrefix() {
        return "";
    }

    /**
     * When a permission was modified, require the reload of granted permissions
     */
    public void permissionChanged() {
        this.userCache.values().stream().forEach((user) -> {
            user.resetGrantedPermissions();
        });
    }

    /**
     * Make sure roles are removed from cached users, the mappings are ON CASCADE DELETE so this only matters for the cache
     * @param event
     */
    public void handleRoleDeletedEvent(RoleDeletedDaoEvent event) {
        this.userCache.values().stream().forEach((user) -> {
            if(user.getRoles().contains(event.getRole().getRole())) {
                Set<Role> updated = new HashSet<>(user.getRoles());
                updated.remove(event.getRole().getRole());
                user.setRoles(updated);
            }
        });
    }
}
