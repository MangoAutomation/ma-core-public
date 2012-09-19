/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.serotonin.m2m2.vo.UserComment;

public class UserCommentRowMapper implements RowMapper<UserComment> {
    public static final String USER_COMMENT_SELECT = "select uc.userId, u.username, uc.ts, uc.commentText "
            + "from userComments uc left join users u on uc.userId = u.id ";

    public UserComment mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserComment c = new UserComment();
        c.setUserId(rs.getInt(1));
        c.setUsername(rs.getString(2));
        c.setTs(rs.getLong(3));
        c.setComment(rs.getString(4));
        return c;
    }
}
