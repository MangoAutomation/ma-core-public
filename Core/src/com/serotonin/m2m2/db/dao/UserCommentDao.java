/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.SelectOnConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.UserComments;
import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.db.tables.records.UserCommentsRecord;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.util.LazyInitSupplier;
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
public class UserCommentDao  extends AbstractVoDao<UserCommentVO, UserCommentsRecord, UserComments>{

    private static final LazyInitSupplier<UserCommentDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(UserCommentDao.class);
    });
    private final Users userTable;

    @Autowired
    private UserCommentDao(DaoDependencies dependencies) {
        super(dependencies, AuditEventType.TYPE_USER_COMMENT, UserComments.USER_COMMENTS, null);
        this.userTable = Users.USERS;
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static UserCommentDao getInstance() {
        return springInstance.get();
    }

    private SelectOnConditionStep<Record> baseQuery() {
        return this.create.select(table.fields())
                .select(userTable.username)
                .from(table)
                .leftJoin(userTable)
                .on(table.userId.equal(userTable.id));
    }

    /**
     * Return all comments for a given event
     * @param id
     * @return
     */
    public void getEventComments(int id, Consumer<UserCommentVO> callback) {
        getComments(UserCommentVO.TYPE_EVENT, id, callback);
    }

    /**
     * Return all comments for a given point
     * @param dpId
     * @return
     */
    public void getPointComments(int dpId, Consumer<UserCommentVO> callback){
        getComments(UserCommentVO.TYPE_POINT, dpId, callback);
    }

    /**
     * Return all comments for a given JsonData Store Entry
     * @param jsonDataId
     * @param callback
     */
    public void getJsonDataComments(int jsonDataId, Consumer<UserCommentVO> callback){
        getComments(UserCommentVO.TYPE_JSON_DATA, jsonDataId, callback);
    }

    public void getComments(int commentType, int typeKey, Consumer<UserCommentVO> callback){
        try (Stream<UserCommentVO> stream = baseQuery().where(table.commentType.eq(commentType))
                .and(table.typeKey.eq(typeKey))
                .orderBy(table.ts)
                .stream()
                .map(this::mapRecord)) {
            stream.forEach(callback);
        }
    }

    @Override
    protected String getXidPrefix() {
        return "UC_";
    }

    @Override
    protected Record toRecord(UserCommentVO vo) {
        UserCommentsRecord record = table.newRecord();

        record.set(table.xid, vo.getXid());
        record.set(table.userId, vo.getUserId());
        record.set(table.ts, vo.getTs());
        record.set(table.commentText, vo.getComment());
        record.set(table.commentType, vo.getCommentType());
        record.set(table.typeKey, vo.getReferenceId());

        return record;
    }

    @Override
    public UserCommentVO mapRecord(Record record) {
        UserCommentVO c = new UserCommentVO();
        c.setId(record.get(table.id));
        c.setXid(record.get(table.xid));
        c.setUserId(record.get(table.userId));
        c.setTs(record.get(table.ts));
        c.setComment(record.get(table.commentText));
        c.setCommentType(record.get(table.commentType));
        c.setReferenceId(record.get(table.typeKey));
        c.setUsername(record.get(userTable.username));
        return c;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select,
            ConditionSortLimit conditions) {
        return select.join(userTable).on(userTable.id.eq(table.userId));
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(userTable.username);
        return fields;
    }
}
