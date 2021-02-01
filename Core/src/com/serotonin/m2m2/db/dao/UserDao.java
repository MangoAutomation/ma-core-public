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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.RQLOperation;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition.RQLVisitException;
import com.infiniteautomation.mango.db.tables.UserRoleMappings;
import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.db.tables.records.UsersRecord;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao.RoleSetResultSetExtractor;
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
public class UserDao extends AbstractVoDao<User, UsersRecord, Users> {
    private static final Log LOG = LogFactory.getLog(UserDao.class);

    private static final LazyInitSupplier<UserDao> springInstance = new LazyInitSupplier<>(() -> Common.getRuntimeContext().getBean(UserDao.class));

    private final PermissionService permissionService;
    UserRoleMappings userRoleMappings = UserRoleMappings.USER_ROLE_MAPPINGS;

    @Autowired
    private UserDao(PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(AuditEventType.TYPE_USER,
                Users.USERS,
                new TranslatableMessage("internal.monitor.USER_COUNT"),
                mapper, publisher);
        this.permissionService = permissionService;
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static UserDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected Map<String, Field<?>> createAliasMap() {
        Map<String, Field<?>> aliases = super.createAliasMap();
        Map<String, Field<?>> myAliases = new HashMap<>();
        myAliases.put("lastPasswordChange", table.passwordChangeTimestamp);
        myAliases.put("created", table.createdTs);
        myAliases.put("emailVerified", table.emailVerifiedTs);
        return combine(aliases, myAliases);
    }

    @Override
    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        Map<String, Function<Object, Object>> converters = super.createValueConverterMap();
        Map<String, Function<Object, Object>> myConverters = new HashMap<>();
        myConverters.put("receiveAlarmEmails", v -> {
            if (v instanceof String) {
                return AlarmLevels.fromName((String) v).value();
            }
            return v;
        });
        return combine(converters, myConverters);
    }

    @Override
    protected Map<String, RQLSubSelectCondition> createSubSelectMap() {
        Map<String, RQLSubSelectCondition> subSelectMap = super.createSubSelectMap();
        Map<String, RQLSubSelectCondition> mySubSelects = new HashMap<>();
        mySubSelects.put("roles", createUserRoleCondition());
        mySubSelects.put("inheritedRoles", createUserInheritedRoleCondition());
        return combine(subSelectMap, mySubSelects);
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
            return this.getCountQuery().from(table).where(
                    table.username.eq(username),
                    table.id.notEqual(excludeId)).fetchOneInto(Integer.class) == 0;
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
            return this.getCountQuery().from(table).where(
                    table.email.eq(email),
                    table.id.notEqual(excludeId)).fetchOneInto(Integer.class) == 0;
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

        Select<Record> query = getJoinedSelectQuery().where(table.username.equalIgnoreCase(username));
        List<Object> args = query.getBindValues();
        User user = ejt.query(query.getSQL(), args.toArray(new Object[0]),
                getObjectResultSetExtractor());
        if(user != null) {
            loadRelationalData(user);
        }
        return user;
    }

    /**
     * Get the roles for a user from the database mapping table
     * @param vo
     * @return
     */
    public Set<Role> getUserRoles(User vo) {
        return query(USER_ROLES_SELECT, new Object[] {vo.getId()}, new RoleSetResultSetExtractor());
    }

    @Override
    public void loadRelationalData(User vo) {
        vo.setRoles(getUserRoles(vo));
    }

    @Override
    public void saveRelationalData(User existing, User vo) {
        if(existing != null) {
            //delete role mappings
            ejt.update(USER_ROLES_DELETE, vo.getId());
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
        Select<Record> query = getJoinedSelectQuery().where(table.email.eq(emailAddress));
        List<Object> args = query.getBindValues();
        return ejt.query(query.getSQL(), args.toArray(new Object[0]), getObjectResultSetExtractor());
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
            user.setEmailVerifiedDate(emailVerified);
            user.setData(extractData(rs.getClob(++i)));
            return user;
        }
    }

    public List<User> getActiveUsers() {
        Select<Record> query = getJoinedSelectQuery().where(table.disabled.eq("N"));
        List<Object> args = query.getBindValues();
        return query(query.getSQL(), args.toArray(new Object[0]), getListResultSetExtractor());
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
            User old = getTransactionTemplate().execute(status -> {
                User old1 = get(vo.getId());
                if (old1 == null) {
                    return null;
                }
                enforceUserRole(vo);
                boolean passwordChanged = !old1.getPassword().equals(vo.getPassword());
                if (passwordChanged) {
                    vo.setPasswordChangeTimestamp(Common.timer.currentTimeMillis());
                    vo.setPasswordVersion(old1.getPasswordVersion() + 1);
                } else {
                    vo.setPasswordChangeTimestamp(old1.getPasswordChangeTimestamp());
                    vo.setPasswordVersion(old1.getPasswordVersion());
                }
                UserDao.super.update(old1, vo);
                //Set the last login time so it is available on the saved user
                vo.setLastLogin(old1.getLastLogin());

                if (passwordChanged || vo.isDisabled()) {
                    expireSessionsForUser(old1);
                }

                return old1;
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

    // TODO Mango 4.0 add event listener to MangoSessionRegistry
    private void expireSessionsForUser(User user) {
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
        if (deleted) {
            // expire the user's sessions
            expireSessionsForUser(vo);
        }
        return deleted;
    }

    @Override
    public void deleteRelationalData(User vo) {
        Object[] args = new Object[] { vo.getId() };
        ejt.update("UPDATE userComments SET userId=null WHERE userId=?", args);
        ejt.update("DELETE FROM mailingListMembers WHERE userId=?", args);
        ejt.update("UPDATE events SET ackUserId=null, alternateAckSource=? WHERE ackUserId=?", new TranslatableMessage("events.ackedByDeletedUser").serialize(), vo.getId());
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


        int count = ejt.update("UPDATE users SET tokenVersion = ? WHERE id = ? AND tokenVersion = ? AND username = ?", newTokenVersion, userId, currentTokenVersion, username);
        if (count == 0) {
            throw new EmptyResultDataAccessException("Updated no rows", 1);
        }

        user.setTokenVersion(newTokenVersion);
        eventPublisher.publishEvent(new DaoEvent<>(this, DaoEventType.UPDATE, user, old));
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
                newPasswordHash, newPasswordVersion, passwordChangeTimestamp, userId, currentPasswordVersion, username);
        if (count == 0) {
            throw new EmptyResultDataAccessException("Updated no rows", 1);
        }

        user.setPassword(newPasswordHash);
        user.setPasswordVersion(newPasswordVersion);
        user.setPasswordChangeTimestamp(passwordChangeTimestamp);

        // expire the user's sessions
        expireSessionsForUser(user);
        eventPublisher.publishEvent(new DaoEvent<>(this, DaoEventType.UPDATE, user, old));
    }

    public void recordLogin(User user) {
        User old = get(user.getId());

        Objects.requireNonNull(old);
        Objects.requireNonNull(user);

        long loginTime = Common.timer.currentTimeMillis();
        user.setLastLogin(loginTime);
        ejt.update("UPDATE users SET lastLogin=? WHERE id=?", loginTime, user.getId());
        eventPublisher.publishEvent(new DaoEvent<>(this, DaoEventType.UPDATE, user, old));
    }

    public void saveHomeUrl(int userId, String homeUrl) {
        User old = get(userId);
        ejt.update("UPDATE users SET homeUrl=? WHERE id=?", homeUrl, userId);
        User user = get(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        eventPublisher.publishEvent(new DaoEvent<>(this, DaoEventType.UPDATE, user, old));
    }

    public void saveMuted(int userId, boolean muted) {
        User old = get(userId);
        ejt.update("UPDATE users SET muted=? WHERE id=?", boolToChar(muted), userId);
        User user = get(userId);
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_USER, old, user);
        eventPublisher.publishEvent(new DaoEvent<>(this, DaoEventType.UPDATE, user, old));
    }

    @Override
    protected Record voToObjectArray(User vo) {
        Record record = table.newRecord();
        record.set(table.username, vo.getUsername());
        record.set(table.name, vo.getName());
        record.set(table.password, vo.getPassword());
        record.set(table.email, vo.getEmail());
        record.set(table.phone, vo.getPhone());
        record.set(table.disabled, boolToChar(vo.isDisabled()));
        record.set(table.lastLogin, vo.getLastLogin());
        record.set(table.homeUrl, vo.getHomeUrl());
        record.set(table.receiveAlarmEmails, vo.getReceiveAlarmEmails().value());
        record.set(table.receiveOwnAuditEvents, boolToChar(vo.isReceiveOwnAuditEvents()));
        record.set(table.timezone, vo.getTimezone());
        record.set(table.muted, boolToChar(vo.isMuted()));
        record.set(table.locale, vo.getLocale());
        record.set(table.tokenVersion, vo.getTokenVersion());
        record.set(table.passwordVersion, vo.getPasswordVersion());
        record.set(table.passwordChangeTimestamp, vo.getPasswordChangeTimestamp());
        record.set(table.sessionExpirationOverride, boolToChar(vo.isSessionExpirationOverride()));
        record.set(table.sessionExpirationPeriods, vo.getSessionExpirationPeriods());
        record.set(table.sessionExpirationPeriodType, vo.getSessionExpirationPeriodType());
        record.set(table.organization, vo.getOrganization());
        record.set(table.organizationalRole, vo.getOrganizationalRole());
        record.set(table.createdTs, vo.getCreatedTs());
        record.set(table.emailVerifiedTs, vo.getEmailVerifiedTimestamp());
        record.set(table.data, convertData(vo.getData()));
        return record;
    }

    @Override
    public RowMapper<User> getRowMapper() {
        return new UserRowMapper();
    }

    @Override
    public User mapRecord(Record record) {
        return null;
    }

    @Override
    protected String getXidPrefix() {
        return "";
    }

    /**
     * Create an RQL mapping to allow querying on user.roles
     * @return
     */
    public RQLSubSelectCondition createUserRoleCondition() {
        return (operation, node) -> {
            List<Object> arguments = node.getArguments();

            //Check the role Xid input
            if (arguments.size() > 2) {
                throw new RQLVisitException(String.format("Only single arguments supported for node type '%s'", node.getName()));
            }

            Object roleXid = arguments.get(1);
            Integer roleId = null;

            Role role = permissionService.getRole((String)roleXid);
            if(role != null) {
                roleId = role.getId();
            }

            SelectConditionStep<Record1<Integer>> afterWhere = create.select(userRoleMappings.userId)
                    .from(userRoleMappings)
                    .where(userRoleMappings.roleId.eq(roleId));

            if (operation == RQLOperation.CONTAINS) {
                return table.id.in(afterWhere.asField());
            }
            throw new RQLVisitException(String.format("Unsupported node type '%s' for field '%s'", node.getName(), arguments.get(0)));
        };
    }

    /**
     * Create an RQL mapping to allow querying on user.inheritedRoles
     * @return
     */
    public RQLSubSelectCondition createUserInheritedRoleCondition() {
        return (operation, node) -> {
            List<Object> arguments = node.getArguments();

            //Check the role Xid input
            if (arguments.size() > 2) {
                throw new RQLVisitException(String.format("Only single arguments supported for node type '%s'", node.getName()));
            }

            Object roleXid = arguments.get(1);
            Set<Integer> roleIds = new HashSet<>();

            Set<Role> inherited = permissionService.getRolesThatInherit((String) roleXid);
            for (Role r : inherited) {
                roleIds.add(r.getId());
            }

            SelectConditionStep<Record1<Integer>> afterWhere = UserDao.this.create.select(userRoleMappings.userId)
                    .from(userRoleMappings)
                    .where(userRoleMappings.roleId.in(roleIds));

            if (operation == RQLOperation.CONTAINS) {
                return table.id.in(afterWhere.asField());
            }
            throw new RQLVisitException(String.format("Unsupported node type '%s' for field '%s'", node.getName(), arguments.get(0)));
        };
    }
}
