/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.UserCommentTableDefinition;
import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.db.MappedRowCallback;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;

/**
 * We don't use XIDs for comments yet.
 *
 * @author Terry Packer
 *
 */
@Repository()
public class UserCommentDao  extends AbstractVoDao<UserCommentVO, UserCommentTableDefinition>{

    private static final LazyInitSupplier<UserCommentDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(UserCommentDao.class);
    });

    private final UserTableDefinition userTable;

    @Autowired
    private UserCommentDao(UserCommentTableDefinition table,
            UserTableDefinition userTable,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher){
        super(AuditEventType.TYPE_USER_COMMENT, table, null, mapper, publisher);
        this.userTable = userTable;
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static UserCommentDao getInstance() {
        return springInstance.get();
    }

    public static final String USER_COMMENT_SELECT = "select uc.id, uc.xid, uc.userId, uc.ts, uc.commentText, uc.commentType, uc.typeKey, u.username "
            + "from userComments uc left join users u on uc.userId = u.id ";

    private static final String POINT_COMMENT_SELECT = USER_COMMENT_SELECT
            + "where uc.commentType= " + UserCommentVO.TYPE_POINT + " and uc.typeKey=? " + "order by uc.ts";

    private static final String EVENT_COMMENT_SELECT = USER_COMMENT_SELECT //
            + "where uc.commentType= " + UserCommentVO.TYPE_EVENT //
            + " and uc.typeKey=? " //
            + "order by uc.ts";

    private static final String JSON_DATA_COMMENT_SELECT = USER_COMMENT_SELECT
            + "where uc.commentType=" + UserCommentVO.TYPE_JSON_DATA
            + " and uc.typeKey=?"
            + "order by uc.ts";
    /**
     * Return all comments for a given event
     * @param id
     * @return
     */
    public void getEventComments(int id, MappedRowCallback<UserCommentVO> callback) {
        query(EVENT_COMMENT_SELECT, new Object[] { id }, new UserCommentVORowMapper(), callback);
    }

    /**
     * Return all comments for a given point
     * @param dpId
     * @return
     */
    public void getPointComments(int dpId, MappedRowCallback<UserCommentVO> callback){
        query(POINT_COMMENT_SELECT, new Object[] { dpId }, new UserCommentVORowMapper(), callback);
    }

    /**
     * Return all comments for a given JsonData Store Entry
     * @param jsonDataId
     * @param callback
     */
    public void getJsonDataComments(int jsonDataId, MappedRowCallback<UserCommentVO> callback){
        query(JSON_DATA_COMMENT_SELECT, new Object[] { jsonDataId }, new UserCommentVORowMapper(), callback);
    }

    public  class UserCommentVORowMapper implements RowMapper<UserCommentVO> {

        @Override
        public UserCommentVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserCommentVO c = new UserCommentVO();
            int i=0;
            c.setId(rs.getInt(++i));
            c.setXid(rs.getString(++i));
            c.setUserId(rs.getInt(++i));
            c.setTs(rs.getLong(++i));
            c.setComment(rs.getString(++i));
            c.setCommentType(rs.getInt(++i));
            c.setReferenceId(rs.getInt(++i));
            c.setUsername(rs.getString(++i));
            return c;
        }
    }

    @Override
    protected String getXidPrefix() {
        return "UC_";
    }

    @Override
    protected Object[] voToObjectArray(UserCommentVO vo) {
        return new Object[]{
                vo.getXid(),
                vo.getUserId(),
                vo.getTs(),
                vo.getComment(),
                vo.getCommentType(),
                vo.getReferenceId()
        };
    }

    @Override
    public RowMapper<UserCommentVO> getRowMapper() {
        return new UserCommentVORowMapper();
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select,
            ConditionSortLimit conditions) {
        return select.join(this.userTable.getTableAsAlias()).on(DSL.field(userTable.getAlias("id")).eq(this.table.getAlias("userId")));
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(userTable.getAlias("username"));
        return fields;
    }
}
