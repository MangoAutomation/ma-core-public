/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.permission.DataPointAccess;
import com.serotonin.web.taglib.Functions;

public class UserDao extends BaseDao {
    private static final Log LOG = LogFactory.getLog(UserDao.class);

    private static final String USER_SELECT = //
    "select id, username, password, email, phone, admin, disabled, homeUrl, "
            + "lastLogin, receiveAlarmEmails, receiveOwnAuditEvents, timezone from users ";

    public User getUser(int id) {
        User user = queryForObject(USER_SELECT + "where id=?", new Object[] { id }, new UserRowMapper(), null);
        populateUserPermissions(user);
        return user;
    }

    public User getUser(String username) {
        User user = queryForObject(USER_SELECT + "where lower(username)=lower(?)", new Object[] { username },
                new UserRowMapper(), null);
        populateUserPermissions(user);
        return user;
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
            user.setAdmin(charToBool(rs.getString(++i)));
            user.setDisabled(charToBool(rs.getString(++i)));
            user.setHomeUrl(rs.getString(++i));
            user.setLastLogin(rs.getLong(++i));
            user.setReceiveAlarmEmails(rs.getInt(++i));
            user.setReceiveOwnAuditEvents(charToBool(rs.getString(++i)));
            user.setTimezone(rs.getString(++i));
            return user;
        }
    }

    public List<User> getUsers() {
        List<User> users = query(USER_SELECT + "order by username", new Object[0], new UserRowMapper());
        populateUserPermissions(users);
        return users;
    }

    public List<User> getActiveUsers() {
        List<User> users = query(USER_SELECT + "where disabled=?", new Object[] { boolToChar(false) },
                new UserRowMapper());
        populateUserPermissions(users);
        return users;
    }

    private void populateUserPermissions(List<User> users) {
        for (User user : users)
            populateUserPermissions(user);
    }

    private static final String SELECT_DATA_SOURCE_PERMISSIONS = "select dataSourceId from dataSourceUsers where userId=?";
    private static final String SELECT_DATA_POINT_PERMISSIONS = "select dataPointId, permission from dataPointUsers where userId=?";

    public void populateUserPermissions(User user) {
        if (user == null)
            return;

        user.setDataSourcePermissions(queryForList(SELECT_DATA_SOURCE_PERMISSIONS, new Object[] { user.getId() },
                Integer.class));
        user.setDataPointPermissions(query(SELECT_DATA_POINT_PERMISSIONS, new Object[] { user.getId() },
                new RowMapper<DataPointAccess>() {
                    @Override
                    public DataPointAccess mapRow(ResultSet rs, int rowNum) throws SQLException {
                        DataPointAccess a = new DataPointAccess();
                        a.setDataPointId(rs.getInt(1));
                        a.setPermission(rs.getInt(2));
                        return a;
                    }
                }));
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

    private static final String USER_INSERT = "insert into users (username, password, email, phone, admin, " //
            + "disabled, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents, timezone) " //
            + "values (?,?,?,?,?,?,?,?,?,?)";

    void insertUser(User user) {
        int id = doInsert(
                USER_INSERT,
                new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                        boolToChar(user.isAdmin()), boolToChar(user.isDisabled()), user.getHomeUrl(),
                        user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()), user.getTimezone() },
                new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR });
        user.setId(id);
        saveRelationalData(user);
    }

    private static final String USER_UPDATE = "update users set " //
            + "  username=?, password=?, email=?, phone=?, admin=?, disabled=?, homeUrl=?, receiveAlarmEmails=?, " //
            + "  receiveOwnAuditEvents=?, timezone=? " //
            + "where id=?";

    void updateUser(User user) {
        // Potential fix for "An attempt was made to get a data value of type 'VARCHAR' from a data value of type 'null'"
        if (user.getPhone() == null)
            user.setPhone("");
        if (user.getHomeUrl() == null)
            user.setHomeUrl("");
        if (user.getTimezone() == null)
            user.setTimezone("");

        try {
            ejt.update(
                    USER_UPDATE,
                    new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                            boolToChar(user.isAdmin()), boolToChar(user.isDisabled()), user.getHomeUrl(),
                            user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()),
                            user.getTimezone(), user.getId() }, new int[] { Types.VARCHAR, Types.VARCHAR,
                            Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                            Types.VARCHAR, Types.VARCHAR, Types.INTEGER });
            saveRelationalData(user);
        }
        catch (DataIntegrityViolationException e) {
            // Log some information about the user object.
            LOG.error("Error updating user: " + user, e);
            throw e;
        }
    }

    private void saveRelationalData(final User user) {
        // Delete existing permissions.
        ejt.update("delete from dataSourceUsers where userId=?", new Object[] { user.getId() });
        ejt.update("delete from dataPointUsers where userId=?", new Object[] { user.getId() });

        // Save the new ones.
        ejt.batchUpdate("insert into dataSourceUsers (dataSourceId, userId) values (?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public int getBatchSize() {
                        return user.getDataSourcePermissions().size();
                    }

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setInt(1, user.getDataSourcePermissions().get(i));
                        ps.setInt(2, user.getId());
                    }
                });
        ejt.batchUpdate("insert into dataPointUsers (dataPointId, userId, permission) values (?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public int getBatchSize() {
                        return user.getDataPointPermissions().size();
                    }

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setInt(1, user.getDataPointPermissions().get(i).getDataPointId());
                        ps.setInt(2, user.getId());
                        ps.setInt(3, user.getDataPointPermissions().get(i).getPermission());
                    }
                });
    }

    public void deleteUser(final int userId) {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @SuppressWarnings("synthetic-access")
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                Object[] args = new Object[] { userId };
                ejt.update("update userComments set userId=null where userId=?", args);
                ejt.update("delete from mailingListMembers where userId=?", args);
                ejt.update("delete from userEvents where userId=?", args);
                ejt.update("update events set ackUserId=null, alternateAckSource=? where ackUserId=?", new Object[] {
                        new TranslatableMessage("events.ackedByDeletedUser").serialize(), userId });
                ejt.update("delete from users where id=?", args);
            }
        });
    }

    public void recordLogin(int userId) {
        ejt.update("update users set lastLogin=? where id=?", new Object[] { System.currentTimeMillis(), userId });
    }

    public void saveHomeUrl(int userId, String homeUrl) {
        ejt.update("update users set homeUrl=? where id=?", new Object[] { homeUrl, userId });
    }

    //
    //
    // User comments
    //
    private static final String USER_COMMENT_INSERT = "insert into userComments (userId, commentType, typeKey, ts, commentText) "
            + "values (?,?,?,?,?)";

    public void insertUserComment(int typeId, int referenceId, UserComment comment) {
        comment.setComment(Functions.truncate(comment.getComment(), 1024));
        ejt.update(USER_COMMENT_INSERT, new Object[] { comment.getUserId(), typeId, referenceId, comment.getTs(),
                comment.getComment() });
    }
}
