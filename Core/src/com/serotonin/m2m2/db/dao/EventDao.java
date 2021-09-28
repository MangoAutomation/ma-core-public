/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.UpdateConditionStep;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.Events;
import com.infiniteautomation.mango.db.tables.MintermsRoles;
import com.infiniteautomation.mango.db.tables.PermissionsMinterms;
import com.infiniteautomation.mango.db.tables.UserComments;
import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.db.tables.records.EventsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.rt.event.type.MissingEventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.JsonSerializableUtility;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * This class is used for runtime management of events sort an eventRT but could
 *  be merged with the EventInstanceDao which is used for historical access and querying.
 *
 *  NOTE the queries from the class do NOT use permission enforcement
 *
 * @author Terry Packer
 */
@Repository
public class EventDao extends BaseDao {
    private static final Logger LOG = LoggerFactory.getLogger(EventDao.class);

    private static final LazyInitSupplier<EventDao> instance = new LazyInitSupplier<>(() -> Common.getBean(EventDao.class));

    private final AuditEventDao auditEventDao;
    private final UserCommentDao userCommentDao;
    private final Events table;
    private final Users userTable;
    private final UserComments userCommentTable;
    private final PermissionService permissionService;

    @Autowired
    private EventDao(AuditEventDao auditEventDao,
            UserCommentDao userCommentDao,
            PermissionService permissionService, DatabaseProxy databaseProxy) {
        super(databaseProxy);
        this.auditEventDao = auditEventDao;
        this.userCommentDao = userCommentDao;
        this.table = Events.EVENTS;
        this.userTable = Users.USERS;
        this.userCommentTable = UserComments.USER_COMMENTS;
        this.permissionService = permissionService;
    }

    public static EventDao getInstance() {
        return instance.get();
    }

    public void saveEvent(EventInstance event) {
        if (event.getEventType().getEventType().equals(EventTypeNames.AUDIT)) {
            AuditEventInstanceVO vo = new AuditEventInstanceVO();
            AuditEventType type = (AuditEventType) event.getEventType();
            vo.setTypeName(type.getEventSubtype());
            vo.setAlarmLevel(event.getAlarmLevel());
            if (type.getRaisingUser() != null)
                vo.setUserId(type.getRaisingUser().getId());
            else
                vo.setUserId(Common.NEW_ID);
            vo.setChangeType(type.getChangeType());
            vo.setObjectId(type.getReferenceId1());
            vo.setTimestamp(event.getActiveTimestamp());
            try {
                vo.setContext(JsonSerializableUtility.convertMapToJsonObject(event.getContext()));
            } catch (JsonException e) {
                LOG.error(e.getMessage(), e);
            }
            vo.setMessage(event.getMessage());
            auditEventDao.insert(vo);
            // Save for use in the cache
            type.setReferenceId2(vo.getId());
        } else {
            if (event.getId() == Common.NEW_ID)
                insertEvent(event);
            else
                updateEvent(event);
        }
    }

    private void insertEvent(EventInstance event) {
        savePreRelationalData(event);
        int id = -1;
        InsertSetMoreStep<EventsRecord> insert = this.create.insertInto(table).set(voToObjectArray(event));
        String sql = insert.getSQL();
        List<Object> args = insert.getBindValues();
        id = ejt.doInsert(sql, args.toArray(new Object[0]));
        event.setId(id);
    }

    private Record voToObjectArray(EventInstance event) {
        EventType type = event.getEventType();
        Record record = table.newRecord();
        record.set(table.typeName, type.getEventType());
        record.set(table.subTypeName, type.getEventSubtype());
        record.set(table.typeRef1, type.getReferenceId1());
        record.set(table.typeRef2, type.getReferenceId2());
        record.set(table.activeTs, event.getActiveTimestamp());
        record.set(table.rtnApplicable, boolToChar(event.isRtnApplicable()));
        if (event.isRtnApplicable() && !event.isActive()) {
            record.set(table.rtnTs, event.getRtnTimestamp());
            record.set(table.rtnCause, event.getRtnCause().value());
        }else {
            record.set(table.rtnTs, null);
            record.set(table.rtnCause, null);
        }
        record.set(table.alarmLevel, event.getAlarmLevel().value());
        record.set(table.message, writeTranslatableMessage(event.getMessage()));
        record.set(table.ackTs, null);
        record.set(table.ackUserId, null);
        record.set(table.alternateAckSource, null);
        record.set(table.readPermissionId, event.getReadPermission().getId());
        return record;
    }

    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(Arrays.asList(table.fields()));
        fields.add(userTable.username);
        Field<?> hasComments = this.create.selectCount().from(userCommentTable)
                .where(userCommentTable.commentType.eq(UserCommentVO.TYPE_EVENT), userCommentTable.typeKey.eq(table.id)).asField("cnt");
        fields.add(hasComments);
        return fields;
    }

    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        select = select.leftJoin(userTable).on(userTable.id.eq(table.ackUserId));
        return select;
    }

    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select,
            ConditionSortLimit conditions, PermissionHolder user) {

        if(!permissionService.hasAdminRole(user)) {

            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = MintermsRoles.MINTERMS_ROLES.roleId.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermsRoles.MINTERMS_ROLES.mintermId)
                    .from(MintermsRoles.MINTERMS_ROLES)
                    .groupBy(MintermsRoles.MINTERMS_ROLES.mintermId)
                    .having(DSL.count().eq(DSL.count(
                            DSL.case_().when(roleIdsIn, DSL.inline(1))
                            .else_(DSL.inline((Integer)null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.selectDistinct(PermissionsMinterms.PERMISSIONS_MINTERMS.permissionId)
                    .from(PermissionsMinterms.PERMISSIONS_MINTERMS)
                    .join(mintermsGranted).on(mintermsGranted.field(MintermsRoles.MINTERMS_ROLES.mintermId).eq(PermissionsMinterms.PERMISSIONS_MINTERMS.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(
                    permissionsGranted.field(PermissionsMinterms.PERMISSIONS_MINTERMS.permissionId).in(
                            table.readPermissionId));

        }
        return select;

    }

    public void savePreRelationalData(EventInstance vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);
    }

    public void loadRelationalData(EventInstance vo) {
        if (vo.isHasComments()) {
            List<UserCommentVO> comments = new ArrayList<>();
            userCommentDao.getEventComments(vo.getId(), comments::add);
            vo.setEventComments(comments);
        }

        MangoPermission read = vo.getReadPermission();
        vo.supplyReadPermission(() -> permissionService.get(read.getId()));
    }

    public void deletePostRelationalData(EventInstance vo) {
        MangoPermission readPermission = vo.getReadPermission();
        permissionService.deletePermissions(readPermission);
    }

    public SelectJoinStep<Record> getSelectQuery(List<Field<?>> fields) {
        return this.create.select(fields)
                .from(table);
    }

    public SelectJoinStep<Record> getJoinedSelectQuery() {
        SelectJoinStep<Record> query = getSelectQuery(getSelectFields());
        return joinTables(query, null);
    }

    /**
     * Available to overload the result set extractor for callback queries
     *  to customize error handling
     * @param callback
     * @return
     */
    protected ResultSetExtractor<Void> getCallbackResultSetExtractor(Consumer<EventInstance> callback) {
        return getCallbackResultSetExtractor(callback, (e, rs) -> {
            if(e.getCause() instanceof ModuleNotLoadedException) {
                LOG.error(e.getCause().getMessage(), e.getCause());
            }else {
                LOG.error(e.getMessage(), e);
                //TODO Mango 4.0 What shall we do here? most likely this caused by a bug in the code and we
                // want to see the 500 error in the API etc.
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    protected ResultSetExtractor<Void> getCallbackResultSetExtractor(Consumer<EventInstance> callback, BiConsumer<Exception, ResultSet> error) {
        return new ResultSetExtractor<Void>() {

            @Override
            public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
                RowMapper<EventInstance> rowMapper = getRowMapper();
                int rowNum = 0;
                while (rs.next()) {
                    try {
                        EventInstance row = rowMapper.mapRow(rs, rowNum);
                        loadRelationalData(row);
                        callback.accept(row);
                    }catch (Exception e) {
                        error.accept(e, rs);
                    }finally {
                        rowNum++;
                    }
                }
                return null;
            }
        };
    }

    /**
     * Available to overload the result set extractor for list queries
     * @param callback
     * @return
     */
    protected ResultSetExtractor<EventInstance> getObjectResultSetExtractor() {
        return getObjectResultSetExtractor((e,rs) -> {
            if(e.getCause() instanceof ModuleNotLoadedException) {
                //We will log and continue as to not prevent someone from loading module based VOs for
                // which the modules are actually installed.
                LOG.error(e.getCause().getMessage(), e.getCause());
            }else {
                LOG.error(e.getMessage(), e);
                //TODO Mango 4.0 What shall we do here? most likely this caused by a bug in the code and we
                // want to see the 500 error in the API etc.
                throw new ShouldNeverHappenException(e);
            }
        });
    }

    /**
     *
     * @param error
     * @return
     */
    protected ResultSetExtractor<EventInstance> getObjectResultSetExtractor(BiConsumer<Exception, ResultSet> error) {
        return new ResultSetExtractor<EventInstance>() {

            @Override
            public EventInstance extractData(ResultSet rs) throws SQLException, DataAccessException {
                RowMapper<EventInstance> rowMapper = getRowMapper();
                List<EventInstance> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    try {
                        EventInstance row = rowMapper.mapRow(rs, rowNum);
                        loadRelationalData(row);
                        results.add(row);
                    } catch (Exception e) {
                        error.accept(e, rs);
                    } finally {
                        rowNum++;
                    }
                    return DataAccessUtils.uniqueResult(results);
                }
                return null;
            }
        };
    }

    public EventInstanceRowMapper getRowMapper() {
        return new EventInstanceRowMapper();
    }

    /**
     * Set rtnTs and rtnCause for this event
     * @param event
     */
    private void updateEvent(EventInstance event) {
        if (event.isRtnApplicable()) {
            Map<Field<?>, Object> values = new LinkedHashMap<>();
            values.put(table.rtnTs, event.getRtnTimestamp());
            values.put(table.rtnCause, event.getRtnCause().value());
            UpdateConditionStep<?> update = this.create.update(table).set(values).where(table.id.eq(event.getId()));
            String sql = update.getSQL();
            List<Object> args = update.getBindValues();
            ejt.update(sql, args.toArray(new Object[args.size()]));
        }
    }

    /**
     * Bulk return events to normal in batches based on the env property
     *
     * @param eventIds
     * @param timestamp
     * @param cause
     */
    public void returnEventsToNormal(List<Integer> eventIds, long timestamp, ReturnCause cause){
        if(eventIds.size() == 0) {
            throw new ShouldNeverHappenException("Not enough Ids!");
        }

        Map<Field<?>, Object> values = new LinkedHashMap<>();
        values.put(table.rtnTs, timestamp);
        values.put(table.rtnCause, cause.value());

        for(List<Integer> batch : partitionInParameters(eventIds.stream())) {
            UpdateConditionStep<?> update = this.create.update(table).set(values).where(table.rtnApplicable.eq(boolToChar(true)).and(table.id.in(batch)));
            String sql = update.getSQL();
            List<Object> args = update.getBindValues();
            ejt.update(sql, args.toArray(new Object[0]));
        }
    }

    /**
     * Acknowledge an event
     * @param eventId
     * @param time
     * @param userId the ID of the user that acknowledged the event, or null if acknowledged by system (e.g. EventManagerListenerDefinition)
     * @param alternateAckSource
     * @return
     */
    public boolean ackEvent(int eventId, long time, Integer userId, TranslatableMessage alternateAckSource) {
        Map<Field<?>, Object> values = new LinkedHashMap<>();
        values.put(table.ackTs, time);
        values.put(table.ackUserId, userId);
        values.put(table.alternateAckSource, writeTranslatableMessage(alternateAckSource));
        UpdateConditionStep<?> update = this.create.update(table).set(values).where(table.id.eq(eventId).and(table.ackTs.isNull()));
        String sql = update.getSQL();
        List<Object> args = update.getBindValues();
        return ejt.update(sql, args.toArray(new Object[0])) > 0;
    }

    private static final String BASIC_EVENT_SELECT = //
            "select e.id, e.typeName, e.subtypeName, e.typeRef1, e.typeRef2, e.activeTs, e.rtnApplicable, e.rtnTs, " //
            + "  e.rtnCause, e.alarmLevel, e.message, e.ackTs, e.ackUserId, u.username, e.alternateAckSource, " //
            + "  (select count(1) from userComments where commentType=" + UserCommentVO.TYPE_EVENT //
            + "     and typeKey=e.id) as cnt " //
            + "from events e " //
            + "  left join users u on e.ackUserId=u.id ";

    /**
     * Get all active events
     * @return
     */
    public List<EventInstance> getActiveEvents() {
        List<EventInstance> events = new ArrayList<>();
        SelectJoinStep<Record> query = this.getJoinedSelectQuery();
        SelectConditionStep<Record> where = query.where(table.rtnApplicable.eq(boolToChar(true)).and(table.rtnTs.isNull()));
        String sql = where.getSQL();
        List<Object> args = where.getBindValues();
        ejt.query(sql, getCallbackResultSetExtractor((item) -> {
            loadRelationalData(item);
            events.add(item);
        }), args.toArray());
        return events;
    }

    /**
     * Get a specfic event instance
     * @param id
     * @return
     */
    public EventInstance get(int id){
        Select<Record> query = this.getJoinedSelectQuery()
                .where(table.id.eq(id))
                .limit(1);
        String sql = query.getSQL();
        List<Object> args = query.getBindValues();
        EventInstance item = ejt.query(sql, args.toArray(new Object[0]), getObjectResultSetExtractor());
        if (item != null) {
            loadRelationalData(item);
        }
        return item;
    }


    public static class EventInstanceRowMapper implements RowMapper<EventInstance> {
        @Override
        public EventInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventType type = createEventType(rs, 2);
            EventInstance event = new EventInstance(type, rs.getLong(6), charToBool(rs.getString(7)), AlarmLevels.fromValue(rs.getInt(10)),
                    BaseDao.readTranslatableMessage(rs, 11), null);
            event.setId(rs.getInt(1));
            long rtnTs = rs.getLong(8);
            if (!rs.wasNull())
                event.returnToNormal(rtnTs, ReturnCause.fromValue(rs.getInt(9)));

            MangoPermission read = new MangoPermission(rs.getInt(15));
            event.supplyReadPermission(() -> read);

            long ackTs = rs.getLong(12);
            if (!rs.wasNull()) {
                event.setAcknowledgedTimestamp(ackTs);
                event.setAcknowledgedByUserId(rs.getInt(13));
                if (!rs.wasNull())
                    event.setAcknowledgedByUsername(rs.getString(16));
                event.setAlternateAckSource(BaseDao.readTranslatableMessage(rs, 14));
            }
            event.setHasComments(rs.getInt(17) > 0);

            return event;
        }
    }

    /**
     * Get an event type from a result set
     *
     * eventTypeName = offset
     * eventSubtypeName = offset + 1
     * eventTypeRef1 = offset + 2
     * eventTypeRef2 = offset + 3
     *
     * @param rs
     * @param offset
     * @return
     * @throws SQLException
     */
    public static EventType createEventType(ResultSet rs, int offset) throws SQLException {
        String typeName = rs.getString(offset);
        String subtypeName = rs.getString(offset + 1);
        EventType type;
        if (typeName.equals(EventTypeNames.DATA_POINT))
            type = new DataPointEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventTypeNames.DATA_SOURCE))
            type = new DataSourceEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventTypeNames.SYSTEM))
            type = new SystemEventType(subtypeName, rs.getInt(offset + 2));
        else if (typeName.equals(EventTypeNames.PUBLISHER))
            type = new PublisherEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventTypeNames.AUDIT))
            type = new AuditEventType(subtypeName, -1, rs.getInt(offset + 3)); //TODO allow tracking the various types of audit events...
        else {
            EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(typeName);
            if (def == null) {
                //Create Missing Event Type
                type = new MissingEventType(typeName, null, rs.getInt(offset + 2), rs.getInt(offset + 3));
            }else {
                type = def.createEventType(subtypeName, rs.getInt(offset + 2), rs.getInt(offset + 3));
                if (type == null) {
                    //Create Missing Event type
                    type = new MissingEventType(typeName, subtypeName, rs.getInt(offset + 2), rs.getInt(offset + 3));
                }
            }
        }
        return type;
    }

    /**
     * Insert a comment and return the event commented on
     * @param comment
     * @return
     */
    public EventInstance insertEventComment(UserCommentVO comment) {
        userCommentDao.insert(comment);
        return get(comment.getReferenceId());
    }

    /**
     * Purge all events by truncating the table
     * @return
     */
    public int purgeAllEvents(){
        final ExtendedJdbcTemplate ejt2 = ejt;
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                int tot = ejt2.update("delete from events"); //UserEvents table will be deleted on cascade
                ejt2.update("delete from userComments where commentType=" + UserCommentVO.TYPE_EVENT);
                //TODO Mango 4.0 do we really want to clean the permissions table?
                return tot;
            }
        });
        return count;
    }
    /**
     * Purge Events Before a given time with a given alarmLevel
     * @param time
     * @param typeName
     * @return
     */
    public int purgeEventsBefore(final long time, final AlarmLevels alarmLevel) {
        // Find a list of event ids with no remaining acknowledgments pending.
        final ExtendedJdbcTemplate ejt2 = ejt;
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {

                int count = ejt2.update("delete from events where activeTs<? and alarmLevel=?", new Object[] { time, alarmLevel.value()});

                // Delete orphaned user comments.
                ejt2.update("delete from userComments where commentType=" + UserCommentVO.TYPE_EVENT
                        + "  and typeKey not in (select id from events)");

                //TODO Mango 4.0 do we really want to clean the permissions table?
                return count;
            }
        });


        return count;
    }

    /**
     * Purge Events Before a given time with a given typeName
     * @param time
     * @param typeName
     * @return
     */
    public int purgeEventsBefore(final long time, final String typeName) {
        // Find a list of event ids with no remaining acknowledgments pending.
        final ExtendedJdbcTemplate ejt2 = ejt;
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {

                int count = ejt2.update("delete from events where activeTs<? and typeName=?", new Object[] { time, typeName});

                // Delete orphaned user comments.
                ejt2.update("delete from userComments where commentType=" + UserCommentVO.TYPE_EVENT
                        + "  and typeKey not in (select id from events)");

                //TODO Mango 4.0 do we really want to clean the permissions table?
                return count;
            }
        });


        return count;
    }


    public int purgeEventsBefore(final long time) {
        // Find a list of event ids with no remaining acknowledgments pending.
        final ExtendedJdbcTemplate ejt2 = ejt;
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                int count = ejt2.update("delete from events where activeTs<?", new Object[] { time });

                // Delete orphaned user comments.
                ejt2.update("delete from userComments where commentType=" + UserCommentVO.TYPE_EVENT
                        + "  and typeKey not in (select id from events)");
                //TODO Mango 4.0 do we really want to clean the permissions table?

                return count;
            }
        });


        return count;
    }

    public int getEventCount() {
        return ejt.queryForInt("select count(*) from events", null, 0);
    }
}
