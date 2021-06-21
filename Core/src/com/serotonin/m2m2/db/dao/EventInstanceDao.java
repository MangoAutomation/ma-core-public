/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectSelectStep;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.query.RQLOperation;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.db.query.RQLToConditionWithTagKeys;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.Events;
import com.infiniteautomation.mango.db.tables.UserComments;
import com.infiniteautomation.mango.db.tables.Users;
import com.infiniteautomation.mango.db.tables.records.EventsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.service.EventInstanceService.AlarmPointTagCount;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.permissions.EventsSuperadminViewPermissionDefinition;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.rt.event.type.MissingEventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.event.EventInstanceVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import net.jazdw.rql.parser.ASTNode;

/**
 * This is used for querying events from the database
 *
 * @author Terry Packer
 *
 */
@Repository()
public class EventInstanceDao extends AbstractVoDao<EventInstanceVO, EventsRecord, Events> {

    private static final LazyInitSupplier<EventInstanceDao> springInstance = new LazyInitSupplier<>(
            () -> Common.getRuntimeContext().getBean(EventInstanceDao.class));

    private final Users users;
    private final DataPointTagsDao dataPointTagsDao;
    private final UserCommentDao userCommentDao;
    private final Field<Integer> commentCount;
    private final EventsSuperadminViewPermissionDefinition eventsSuperadminViewPermission;

    private final DataPoints dataPoints = DataPoints.DATA_POINTS;

    private final Field<Integer> count = DSL.count(table.id).as("count");
    private final Field<Long> latestActive = DSL.max(table.activeTs).as("latestActive");
    private final Field<Long> latestRtn = DSL.max(table.rtnTs).as("latestRtn");
    private final Name eventCounts = DSL.name("eventCounts");

    private final Map<String, Field<?>> eventCountsFields;

    @Autowired
    private EventInstanceDao(DataPointTagsDao dataPointTagsDao,
                             PermissionService permissionService,
                             @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper,
                             ApplicationEventPublisher publisher, UserCommentDao userCommentDao,
                             @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") EventsSuperadminViewPermissionDefinition eventsSuperadminViewPermission) {
        super(null, Events.EVENTS, null, mapper, publisher, permissionService);
        this.users = Users.USERS;
        this.dataPointTagsDao = dataPointTagsDao;
        this.userCommentDao = userCommentDao;
        this.eventsSuperadminViewPermission = eventsSuperadminViewPermission;

        UserComments userComments = UserComments.USER_COMMENTS;
        this.commentCount = this.create.selectCount()
                .from(userComments)
                .where(userComments.commentType.eq(UserCommentVO.TYPE_EVENT),
                        userComments.typeKey.eq(table.id))
                .asField("commentCount");


        // pseudo-table with alias so we can grab fields from with correct qualified name
        Table<Record6<Integer, String, Integer, Integer, Long, Long>> eventCountsTable = DSL.select(
                table.typeRef1,
                table.message,
                table.alarmLevel,
                count,
                latestActive,
                latestRtn
        ).from(table).asTable(eventCounts);

        Map<String, Field<?>> eventCountsFields = new HashMap<>();
        eventCountsFields.put("xid", dataPoints.xid);
        eventCountsFields.put("name", dataPoints.name);
        eventCountsFields.put("deviceName", dataPoints.deviceName);
        for (Field<?> field : eventCountsTable.fields()) {
            eventCountsFields.put(field.getName(), field);
        }
        this.eventCountsFields = Collections.unmodifiableMap(eventCountsFields);
    }

    /**
     * Get cached instance from Spring Context
     */
    public static EventInstanceDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected String getXidPrefix() {
        return null; //No XIDs
    }

    @Override
    protected Record toRecord(EventInstanceVO event) {
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

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        select = select.leftJoin(users).on(users.id.eq(table.ackUserId));

        if (conditions instanceof ConditionSortLimitWithTagKeys) {
            Map<String, Field<String>> tagFields = ((ConditionSortLimitWithTagKeys) conditions).getTagFields();
            select = dataPointTagsDao.joinTags(select, table.typeRef1, tagFields,
                        table.typeName.eq(EventType.EventTypeNames.DATA_POINT),
                        table.subTypeName.isNull());
        }

        return select;
    }

    @Override
    protected <R extends Record> SelectJoinStep<R> joinPermissionsOnField(SelectJoinStep<R> select, PermissionHolder user, Field<Integer> permissionIdField){
        if (this.permissionService.hasPermission(user, eventsSuperadminViewPermission.getPermission())) {
            return select;
        } else {
            return super.joinPermissionsOnField(select ,user ,permissionIdField);
        }
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(users.username);
        fields.add(commentCount);
        return fields;
    }

    @Override
    public @NonNull EventInstanceVO mapRecord(@NonNull Record record) {
        EventInstanceVO event = new EventInstanceVO();
        event.setId(record.get(table.id));

        EventType type = createEventType(record);
        event.setEventType(type);
        event.setActiveTimestamp(record.get(table.activeTs));
        event.setRtnApplicable(charToBool(record.get(table.rtnApplicable)));
        event.setAlarmLevel(AlarmLevels.fromValue(record.get(table.alarmLevel)));
        TranslatableMessage message = BaseDao.readTranslatableMessage(record.get(table.message));
        if(message == null)
            event.setMessage(new TranslatableMessage("common.noMessage"));
        else
            event.setMessage(message);

        //Set the Return to normal
        Long rtnTs = record.get(table.rtnTs);
        if (rtnTs != null) {
            //if(event.isActive()){ Probably don't need this
            event.setRtnTimestamp(rtnTs);
            event.setRtnCause(ReturnCause.fromValue(record.get(table.rtnCause)));
            //}
        }

        MangoPermission read = new MangoPermission(record.get(table.readPermissionId));
        event.supplyReadPermission(() -> read);

        Long ackTs = record.get(table.ackTs);
        if (ackTs != null) {
            //Compute total time
            event.setAcknowledgedTimestamp(ackTs);
            Integer ackUserId = record.get(table.ackUserId);
            if (ackUserId != null) {
                event.setAcknowledgedByUserId(ackUserId);
                event.setAcknowledgedByUsername(record.get(users.username));
            }
            event.setAlternateAckSource(BaseDao.readTranslatableMessage(record.get(table.alternateAckSource)));
        }
        event.setHasComments(record.get(commentCount) > 0);
        return event;
    }

    @Override
    public void savePreRelationalData(EventInstanceVO existing, EventInstanceVO vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);
    }

    @Override
    public void saveRelationalData(EventInstanceVO existing, EventInstanceVO vo) {
        if(existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(EventInstanceVO vo) {
        if (vo.isHasComments()) {
            List<UserCommentVO> comments = new ArrayList<>();
            userCommentDao.getEventComments(vo.getId(), comments::add);
            vo.setEventComments(comments);
        }

        MangoPermission read = vo.getReadPermission();
        vo.supplyReadPermission(() -> permissionService.get(read.getId()));
    }

    @Override
    public void deletePostRelationalData(EventInstanceVO vo) {
        MangoPermission readPermission = vo.getReadPermission();
        permissionService.deletePermissions(readPermission);
    }

    public EventType createEventType(Record record) {
        String typeName = record.get(table.typeName);
        String subtypeName = record.get(table.subTypeName);
        Integer typeRef1 = record.get(table.typeRef1);
        Integer typeRef2 = record.get(table.typeRef2);
        return createEventType(typeName, subtypeName, typeRef1, typeRef2);
    }

    public EventType createEventType(String typeName, String subtypeName, Integer typeRef1, Integer typeRef2) {
        EventType type;
        switch (typeName) {
            case EventType.EventTypeNames.DATA_POINT:
                type = new DataPointEventType(typeRef1, typeRef2);
                break;
            case EventType.EventTypeNames.DATA_SOURCE:
                type = new DataSourceEventType(typeRef1, typeRef2);
                break;
            case EventType.EventTypeNames.SYSTEM:
                type = new SystemEventType(subtypeName, typeRef1);
                break;
            case EventType.EventTypeNames.PUBLISHER:
                type = new PublisherEventType(typeRef1, typeRef2);
                break;
            case EventType.EventTypeNames.AUDIT:
                throw new ShouldNeverHappenException("AUDIT events should not exist here. Consider running the SQL: DELETE FROM events WHERE typeName='AUDIT';");
            default:
                EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(typeName);
                if (def == null) {
                    //Create Missing Event Type
                    type = new MissingEventType(typeName, null, typeRef1, typeRef2);
                } else {
                    type = def.createEventType(subtypeName, typeRef1, typeRef2);
                    if (type == null) {
                        //Create Missing Event type
                        type = new MissingEventType(typeName, subtypeName, typeRef1, typeRef2);
                    }
                }
                break;
        }
        return type;
    }

    @Override
    protected RQLToCondition createRqlToCondition(Map<String, RQLSubSelectCondition> subSelectMapping, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> converterMap) {
        return new RQLToEventInstanceConditions(fieldMap, converterMap);
    }

    @Override
    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        Map<String, Function<Object, Object>> converters = super.createValueConverterMap();
        converters.put("alarmLevel", value -> {
            if (value instanceof String) {
                return AlarmLevels.fromName((String)value).value();
            }else if(value instanceof AlarmLevels) {
                return ((AlarmLevels)value).value();
            }
            return value;
        });
        return converters;
    }

    public static class RQLToEventInstanceConditions extends RQLToConditionWithTagKeys {

        public RQLToEventInstanceConditions(Map<String, Field<?>> fieldMapping, Map<String, Function<Object, Object>> valueConverterMap) {
            super(fieldMapping, valueConverterMap);
        }

        @Override
        protected Condition visitConditionNode(ASTNode node) {
            String property = (String) node.getArgument(0);

            switch(property) {
                case "acknowledged": {
                    Field<Object> ackField = getField(property);
                    Function<Object, Object> ackValueConverter = getValueConverter(ackField);
                    Object ackFirstArg = ackValueConverter.apply(node.getArgument(1));
                    RQLOperation operation = RQLOperation.convertTo(node.getName().toLowerCase(Locale.ROOT));
                    switch (operation) {
                        case EQUAL_TO: {
                            if (ackFirstArg == null) {
                                return ackField.isNull();
                            } else {
                                return (Boolean) ackFirstArg ? ackField.isNotNull() : ackField.isNull();
                            }
                        }
                        case NOT_EQUAL_TO: {
                            if (ackFirstArg == null) {
                                return ackField.isNotNull();
                            } else {
                                return (Boolean) ackFirstArg ? ackField.isNull() : ackField.isNotNull();
                            }
                        }
                    }
                    break;
                }
                case "active": {
                    Field<Object> activeField = getField(property);
                    Function<Object, Object> activeValueConverter = getValueConverter(activeField);
                    Object activeFirstArg = activeValueConverter.apply(node.getArgument(1));
                    RQLOperation operation = RQLOperation.convertTo(node.getName().toLowerCase(Locale.ROOT));
                    Condition rtnApplicable = getField("rtnApplicable").eq("Y");
                    switch (operation) {
                        case EQUAL_TO: {
                            if (activeFirstArg == null) {
                                return activeField.isNull();
                            } else {
                                return (Boolean) activeFirstArg ? activeField.isNull().and(rtnApplicable) : activeField.isNotNull().and(rtnApplicable);
                            }
                        }
                        case NOT_EQUAL_TO: {
                            if (activeFirstArg == null) {
                                return activeField.isNull().and(rtnApplicable);
                            } else {
                                return (Boolean) activeFirstArg ? activeField.isNotNull().and(rtnApplicable) : activeField.isNull().and(rtnApplicable);
                            }
                        }
                    }
                    break;
                }
            }

            return super.visitConditionNode(node);
        }
    }

    /**
     * We don't have an XID
     */
    @Override
    public boolean isXidUnique(String xid, int excludeId) {
        return true;
    }

    /**
     * @param conditions supplied by {@link #createEventCountsConditions(net.jazdw.rql.parser.ASTNode)}
     * @param from from timestamp
     * @param to to timestamp
     * @param user user executing the query
     * @param callback result consumer
     */
    public void queryDataPointEventCountsByRQL(@NonNull ConditionSortLimitWithTagKeys conditions,
                                               @Nullable Long from,
                                               @Nullable Long to,
                                               @NonNull PermissionHolder user,
                                               @NonNull Consumer<AlarmPointTagCount> callback) {

        Table<Record6<Integer, String, Integer, Integer, Long, Long>> eventCountsTable = eventCountsTable(from, to, user);
        Map<String, Field<String>> tagFields = conditions.getTagFields();
        SelectJoinStep<Record> joinStep = create.select(eventCountsFields.values())
                .select(tagFields.values())
                .from(eventCountsTable)
                .leftOuterJoin(dataPoints).on(dataPoints.id.equal(eventCountsTable.field(table.typeRef1)));

        SelectConditionStep<Record> conditionStep = dataPointTagsDao.joinTags(joinStep, dataPoints.id, tagFields)
                .where(conditions.getCondition());

        Select<Record> select = applySortLimitOffset(conditionStep, conditions);
        try (Stream<Record> stream = select.stream()) {
            stream.map(record -> {
                AlarmPointTagCount result = new AlarmPointTagCount();
                result.setXid(record.get(dataPoints.xid));
                result.setName(record.get(dataPoints.name));
                result.setDeviceName(record.get(dataPoints.deviceName));
                result.setMessage(readTranslatableMessage(record.get(eventCountsTable.field(table.message))));
                result.setAlarmLevel(AlarmLevels.fromValue(record.get(eventCountsTable.field(table.alarmLevel))));
                result.setCount(record.get(eventCountsTable.field(count)));
                result.setLatestActiveTs(record.get(eventCountsTable.field(latestActive)));
                result.setLatestRtnTs(record.get(eventCountsTable.field(latestRtn)));

                Map<String, String> tags = new HashMap<>();
                for (Entry<String, Field<String>> entry : tagFields.entrySet()) {
                    String value = record.get(entry.getValue());
                    if (value != null) {
                        tags.put(entry.getKey(), value);
                    }
                }
                result.setTags(tags);

                return result;
            }).forEach(callback);
        }
    }

    /**
     * @param conditions supplied by {@link #createEventCountsConditions(net.jazdw.rql.parser.ASTNode)}
     * @param from from timestamp
     * @param to to timestamp
     * @param user user executing the query
     * @return the count of rows matching the conditions
     */
    public int countDataPointEventCountsByRQL(@NonNull ConditionSortLimitWithTagKeys conditions, @Nullable Long from, @Nullable Long to, @NonNull PermissionHolder user) {
        Table<Record6<Integer, String, Integer, Integer, Long, Long>> eventCountsTable = eventCountsTable(from, to, user);

        SelectJoinStep<Record1<Integer>> joinStep = create.selectCount().from(eventCountsTable)
                .leftOuterJoin(dataPoints).on(dataPoints.id.equal(eventCountsTable.field(table.typeRef1)));

        Map<String, Field<String>> tagFields = conditions.getTagFields();
        return dataPointTagsDao.joinTags(joinStep, dataPoints.id, tagFields)
                .where(conditions.getCondition())
                .fetchSingleInto(Integer.class);
    }

    private Table<Record6<Integer, String, Integer, Integer, Long, Long>> eventCountsTable(Long from, Long to, PermissionHolder user) {
        List<Condition> eventConditions = new ArrayList<>();
        eventConditions.add(table.typeName.equal(EventTypeNames.DATA_POINT));
        if (from != null) {
            eventConditions.add(table.activeTs.greaterOrEqual(from));
        }
        if (to != null) {
            eventConditions.add(table.activeTs.lessThan(to));
        }
        return joinPermissions(DSL.select(
                table.typeRef1,
                table.message,
                table.alarmLevel,
                count,
                latestActive,
                latestRtn
        ).from(table), user)
                .where(eventConditions)
                .groupBy(table.typeRef1, table.typeRef2, table.message, table.alarmLevel)
                .asTable(eventCounts);
    }

    public ConditionSortLimitWithTagKeys createEventCountsConditions(ASTNode rql) {
        RQLToConditionWithTagKeys rqlToCondition = new RQLToConditionWithTagKeys(this.eventCountsFields, this.valueConverterMap);
        return rqlToCondition.visit(rql);
    }

    /**
     * Count all unacknowledged alarms at this level
     */
    public int countUnacknowledgedAlarms(AlarmLevels level, PermissionHolder user) {
        SelectSelectStep<Record1<Integer>> count = getCountQuery();
        SelectJoinStep<Record1<Integer>> select = count.from(table);
        select = joinPermissions(select, user);
        Condition condition = table.ackTs.isNull().and(table.alarmLevel.eq(level.value()));

        return customizedCount(select, condition);
    }

    /**
     * Get the latest unacknowledged alarm at this level
     */
    public EventInstanceVO getLatestUnacknowledgedAlarm(AlarmLevels level, PermissionHolder user) {
        SelectJoinStep<Record> select = getSelectQuery(getSelectFields());
        //We don't care about the conditions for the joins in this query
        select = joinTables(select, null);
        select = joinPermissions(select, user);
        Condition condition = table.ackTs.isNull().and(table.alarmLevel.eq(level.value()));
        SelectConnectByStep<Record> afterWhere = select.where(condition);
        SortField<Long> orderBy = table.activeTs.sort(SortOrder.DESC);
        return afterWhere.orderBy(orderBy).limit(1).fetchOne(this::mapRecordLoadRelationalData);
    }
}
