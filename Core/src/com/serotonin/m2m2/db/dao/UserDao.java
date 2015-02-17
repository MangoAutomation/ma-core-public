/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.web.taglib.Functions;

public class UserDao extends BaseDao {
    private static final Log LOG = LogFactory.getLog(UserDao.class);

    private static final String USER_SELECT = //
    "SELECT id, username, password, email, phone, admin, disabled, homeUrl, " //
            + "lastLogin, receiveAlarmEmails, receiveOwnAuditEvents, timezone, muted, permissions FROM users ";

    public User getUser(int id) {
        return queryForObject(USER_SELECT + "WHERE id=?", new Object[] { id }, new UserRowMapper(), null);
    }

    public User getUser(String username) {
        return queryForObject(USER_SELECT + "WHERE LOWER(username)=LOWER(?)", new Object[] { username },
                new UserRowMapper(), null);
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
            user.setMuted(charToBool(rs.getString(++i)));
            user.setPermissions(rs.getString(++i));
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

    private static final String USER_INSERT = "INSERT INTO users (username, password, email, phone, admin, " //
            + "disabled, homeUrl, receiveAlarmEmails, receiveOwnAuditEvents, timezone, muted, permissions) " //
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

    void insertUser(User user) {
        int id = ejt.doInsert(
                USER_INSERT,
                new Object[] { user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
                        boolToChar(user.isAdmin()), boolToChar(user.isDisabled()), user.getHomeUrl(),
                        user.getReceiveAlarmEmails(), boolToChar(user.isReceiveOwnAuditEvents()), user.getTimezone(),
                        boolToChar(user.isMuted()), user.getPermissions() }, new int[] { Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });
        user.setId(id);
    }

    private static final String USER_UPDATE = "UPDATE users SET " //
            + "  username=?, password=?, email=?, phone=?, admin=?, disabled=?, homeUrl=?, receiveAlarmEmails=?, " //
            + "  receiveOwnAuditEvents=?, timezone=?, muted=?, permissions=? " //
            + "WHERE id=?";

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
                            user.getTimezone(), boolToChar(user.isMuted()), user.getPermissions(), user.getId() },
                    new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                            Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                            Types.VARCHAR, Types.INTEGER });
        }
        catch (DataIntegrityViolationException e) {
            // Log some information about the user object.
            LOG.error("Error updating user: " + user, e);
            throw e;
        }
    }

    public void deleteUser(final int userId) {
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
            }
        });
    }

    public void recordLogin(int userId) {
        ejt.update("UPDATE users SET lastLogin=? WHERE id=?", new Object[] { System.currentTimeMillis(), userId });
    }

    public void saveHomeUrl(int userId, String homeUrl) {
        ejt.update("UPDATE users SET homeUrl=? WHERE id=?", new Object[] { homeUrl, userId });
    }

    public void saveMuted(int userId, boolean muted) {
        ejt.update("UPDATE users SET muted=? WHERE id=?", new Object[] { boolToChar(muted), userId });
    }

    //
    //
    // User comments
    //
    private static final String USER_COMMENT_INSERT = //
    "INSERT INTO userComments (userId, commentType, typeKey, ts, commentText) VALUES (?,?,?,?,?)";

    public void insertUserComment(int typeId, int referenceId, UserComment comment) {
        comment.setComment(Functions.truncate(comment.getComment(), 1024));
        ejt.update(USER_COMMENT_INSERT, new Object[] { comment.getUserId(), typeId, referenceId, comment.getTs(),
                comment.getComment() });
    }
}
