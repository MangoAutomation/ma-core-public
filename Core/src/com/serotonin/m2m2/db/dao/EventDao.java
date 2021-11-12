/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.serotonin.ShouldNeverHappenException;
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
        EventsRecord result = create.insertInto(table)
                .set(voToObjectArray(event))
                .returning(table.id)
                .fetchOne();

        int id = result == null ? -1 : result.get(table.id);
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
     * Set rtnTs and rtnCause for this event
     * @param event
     */
    private void updateEvent(EventInstance event) {
        if (event.isRtnApplicable()) {
            create.update(table)
                    .set(table.rtnTs, event.getRtnTimestamp())
                    .set(table.rtnCause, event.getRtnCause().value())
                    .where(table.id.eq(event.getId()))
                    .execute();
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

        for(List<Integer> batch : partitionInParameters(eventIds.stream())) {
            create.update(table)
                    .set(table.rtnTs, timestamp)
                    .set(table.rtnCause, cause.value())
                    .where(table.rtnApplicable.eq(boolToChar(true)))
                    .and(table.id.in(batch))
                    .execute();
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
        return create.update(table)
                .set(table.ackTs, time)
                .set(table.ackUserId, userId)
                .set(table.alternateAckSource, writeTranslatableMessage(alternateAckSource))
                .where(table.id.eq(eventId))
                .and(table.ackTs.isNull())
                .execute() > 0;
    }

    /**
     * Get all active events
     * @return
     */
    public List<EventInstance> getActiveEvents() {
        List<EventInstance> events = new ArrayList<>();
        getJoinedSelectQuery()
                .where(table.rtnApplicable.eq(boolToChar(true)))
                .and(table.rtnTs.isNull())
                .fetch()
                .forEach(record -> {
                    EventInstance item = mapRecord(record);
                    loadRelationalData(item);
                    events.add(item);
                });
        return events;
    }

    /**
     * Get a specfic event instance
     * @param id
     * @return
     */
    public EventInstance get(int id) {
        EventInstance item = getJoinedSelectQuery()
                .where(table.id.eq(id))
                .limit(1)
                .fetchOne(this::mapRecord);

        if (item != null) {
            loadRelationalData(item);
        }
        return item;
    }


    public EventInstance mapRecord(Record record) {
        EventType type = createEventType(record);
        EventInstance event = new EventInstance(
                type,
                record.get(table.activeTs),
                charToBool(record.get(table.rtnApplicable)),
                AlarmLevels.fromValue(record.get(table.alarmLevel)),
                BaseDao.readTranslatableMessage(record.get(table.message)),
                null
        );
        event.setId(record.get(table.id));
        Long rtnTs = record.get(table.rtnTs);
        if (rtnTs != null)
            event.returnToNormal(rtnTs, ReturnCause.fromValue(record.get(table.rtnCause)));

        MangoPermission read = new MangoPermission(record.get(table.readPermissionId));
        event.supplyReadPermission(() -> read);

        Long ackTs = record.get(table.ackTs);
        if (ackTs != null) {
            event.setAcknowledgedTimestamp(ackTs);
            Integer ackUserId = record.get(table.ackUserId);
            if (ackUserId != null)
                event.setAcknowledgedByUsername(record.get(userTable.username));

            event.setAlternateAckSource(BaseDao.readTranslatableMessage(record.get(table.alternateAckSource)));
        }

        Integer cnt = (Integer) record.get("cnt");
        event.setHasComments(cnt != null && cnt > 0);
        return event;
    }

    public EventType createEventType(Record record) {
        String typeName = record.get(table.typeName);
        String subTypeName = record.get(table.subTypeName);
        Integer typeRef1 = record.get(table.typeRef1);
        Integer typeRef2 = record.get(table.typeRef2);
        return createEventType(typeName, subTypeName, typeRef1, typeRef2);
    }

    public EventType createEventType(String typeName, String subTypeName, Integer typeRef1, Integer typeRef2) {
        EventType type;
        switch (typeName) {
            case EventType.EventTypeNames.DATA_POINT:
                type = new DataPointEventType(typeRef1, typeRef2);
                break;
            case EventType.EventTypeNames.DATA_SOURCE:
                type = new DataSourceEventType(typeRef1, typeRef2);
                break;
            case EventType.EventTypeNames.SYSTEM:
                type = new SystemEventType(subTypeName, typeRef1);
                break;
            case EventType.EventTypeNames.PUBLISHER:
                type = new PublisherEventType(typeRef1, typeRef2);
                break;
            case EventType.EventTypeNames.AUDIT:
                type = new AuditEventType(subTypeName, -1, typeRef2); //TODO allow tracking the various of audit events...
                break;
            default:
                EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(typeName);
                if (def == null) {
                    //Create Missing Event Type
                    type = new MissingEventType(typeName, null, typeRef1, typeRef2);
                } else {
                    type = def.createEventType(subTypeName, typeRef1, typeRef2);
                    if (type == null) {
                        //Create Missing Event type
                        type = new MissingEventType(typeName, subTypeName, typeRef1, typeRef2);
                    }
                }
                break;
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
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                int total = create.deleteFrom(table).execute(); //UserEvents table will be deleted on cascade
                create.deleteFrom(userCommentTable)
                        .where(userCommentTable.commentType.eq(UserCommentVO.TYPE_EVENT))
                        .execute();
                //TODO Mango 4.0 do we really want to clean the permissions table?
                return total;
            }
        });
        return count;
    }

    /**
     * Purge Events Before a given time with a given alarmLevel
     * @param time
     * @param alarmLevel
     * @return
     */
    public int purgeEventsBefore(final long time, final AlarmLevels alarmLevel) {
        // Find a list of event ids with no remaining acknowledgments pending.
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                int count = create.deleteFrom(table)
                        .where(table.activeTs.lessThan(time))
                        .and(table.alarmLevel.eq(alarmLevel.value()))
                        .execute();
                deleteOrphanedUserComments();
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
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                int count = create.deleteFrom(table)
                        .where(table.activeTs.lessThan(time))
                        .and(table.typeName.eq(typeName))
                        .execute();

                deleteOrphanedUserComments();
                //TODO Mango 4.0 do we really want to clean the permissions table?
                return count;
            }
        });

        return count;
    }

    /**
     * Purge Events Before a given time
     * @param time
     * @return
     */
    public int purgeEventsBefore(final long time) {
        // Find a list of event ids with no remaining acknowledgments pending.
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
                int count = create.deleteFrom(table).where(table.activeTs.lessThan(time)).execute();
                deleteOrphanedUserComments();
                //TODO Mango 4.0 do we really want to clean the permissions table?
                return count;
            }
        });

        return count;
    }

    private void deleteOrphanedUserComments() {
        // Delete orphaned user comments.
        create.deleteFrom(userCommentTable)
                .where(userCommentTable.commentType.eq(UserCommentVO.TYPE_EVENT))
                .and(userCommentTable.typeKey.notIn(
                        create.select(table.id).from(table)
                )).execute();
    }

    public int getEventCount() {
        return create.select(DSL.count(table.id))
                .from(table)
                .fetchSingle()
                .value1();
    }
}
