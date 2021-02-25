/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.query.RQLOperation;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.db.query.RQLToConditionWithTagKeys;
import com.infiniteautomation.mango.db.tables.DataPointTags;
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
import com.serotonin.log.LogStopWatch;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.ReturnCause;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
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

    @Autowired
    private EventInstanceDao(DataPointTagsDao dataPointTagsDao,
                             PermissionService permissionService,
                             @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper,
                             ApplicationEventPublisher publisher, UserCommentDao userCommentDao) {
        super(null, Events.EVENTS, null, mapper, publisher, permissionService);
        this.users = Users.USERS;
        this.dataPointTagsDao = dataPointTagsDao;
        this.userCommentDao = userCommentDao;

        UserComments userComments = UserComments.USER_COMMENTS;
        this.commentCount = this.create.selectCount()
                .from(userComments)
                .where(userComments.commentType.eq(UserCommentVO.TYPE_EVENT),
                        userComments.typeKey.eq(table.id))
                .asField("commentCount");
    }

    /**
     * Get cached instance from Spring Context
     * @return
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
            Map<String, Name> tagKeyToColumn = ((ConditionSortLimitWithTagKeys) conditions).getTagKeyToColumn();
            if (!tagKeyToColumn.isEmpty()) {
                // TODO Mango 4.0 throw exception or don't join if event type is not restricted to DATA_POINT
                Table<Record> pivotTable = dataPointTagsDao.createTagPivotSql(tagKeyToColumn).asTable().as(DATA_POINT_TAGS_PIVOT_ALIAS);
                select = select.leftJoin(pivotTable).on(DataPointTagsDao.PIVOT_ALIAS_DATA_POINT_ID.eq(table.typeRef1));
            }
        }

        return select;
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(users.username);
        fields.add(commentCount);
        return fields;
    }

    @Override
    public EventInstanceVO mapRecord(Record record) {
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

    /**
     *
     * @param userId
     * @param level
     * @return
     */
    public int countUnsilencedEvents(int userId, AlarmLevels level) {
        return ejt.queryForInt(getCountQuery().getSQL() + " where ue.silenced=? and ue.userId=? and evt.alarmLevel=?", new Object[] { boolToChar(false), userId, level.value() }, 0);
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
     *
     * @param rql
     * @param from - all events >= this time (can be null)
     * @param to - all events < this time (can be null)
     * @param user
     * @param callback
     */
    public void queryDataPointEventCountsByRQL(ASTNode rql,
            Long from,
            Long to,
            PermissionHolder user,
            Consumer<AlarmPointTagCount> callback) {

        ConditionSortLimit conditions =  rqlToDataPointEventCountCondition(rql);
        Map<String, Name> tags;
        if(conditions instanceof  ConditionSortLimitWithTagKeys) {
            ConditionSortLimitWithTagKeys tagConditionSortLimit = (ConditionSortLimitWithTagKeys)conditions;
            if(tagConditionSortLimit.getTagKeyToColumn().size() == 0) {
                tags = Collections.emptyMap();
            }else{
                tags = tagConditionSortLimit.getTagKeyToColumn();
            }
        }else{
            tags = Collections.emptyMap();
        }

        final Select<?> query = createDataPointEventCountsQuery(conditions, from, to, false, user);
        String sql = query.getSQL();
        List<Object> arguments = query.getBindValues();
        Object[] argumentsArray = arguments.toArray(new Object[arguments.size()]);
        LogStopWatch stopWatch = null;
        if (useMetrics) {
            stopWatch = new LogStopWatch();
        }
        try {
            this.query(sql, argumentsArray, new ResultSetExtractor<Void>() {

                @Override
                public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
                    int rowNum = 0;
                    while (rs.next()) {
                        String xid,name,deviceName;
                        TranslatableMessage message;
                        AlarmLevels level;
                        int count;
                        Long activeTs,rtnTs;
                        Map<String,String> tagMap = new HashMap<>();
                        int columnIndex = 1;
                        try {
                            //Find the tag this is for
                            for(String tag : tags.keySet()) {
                                String tagValue = rs.getString(columnIndex);
                                columnIndex++;
                                if(rs.wasNull()) {
                                    tagMap.put(tag, null);
                                }else {
                                    tagMap.put(tag, tagValue);
                                }
                            }

                            xid = rs.getString(columnIndex++);
                            name = rs.getString(columnIndex++);
                            deviceName = rs.getString(columnIndex++);
                            message =  BaseDao.readTranslatableMessage(rs, columnIndex++);
                            level = AlarmLevels.fromValue(rs.getInt(columnIndex++));
                            count = rs.getInt(columnIndex++);
                            activeTs = rs.getLong(columnIndex++);
                            rtnTs = rs.getLong(columnIndex++);
                            if(rs.wasNull()) {
                                rtnTs = null;
                            }
                            callback.accept(new AlarmPointTagCount(xid, name, deviceName, message, level, count, activeTs, rtnTs, tagMap));

                        }catch(Exception e) {
                            throw new SQLException(e);
                        }
                    }
                    return null;
                }
            });
        }finally {
            if (stopWatch != null) {
                stopWatch.stop(() -> "queryDataPointEventsByTag(): " + this.create.renderInlined(query), metricsThreshold);
            }
        }
    }

    /**
     * Count the event counts using the conditions
     * @param rql
     * @param from
     * @param to
     * @param user
     * @return
     */
    public int countDataPointEventCountsByRQL(ASTNode rql,
        Long from,
        Long to,
        PermissionHolder user) {
        ConditionSortLimit conditions =  rqlToDataPointEventCountCondition(rql);
        final Select<?> query = createDataPointEventCountsQuery(conditions, from, to, true, user);
        return query.fetchOneInto(Integer.class);
    }

    private ConditionSortLimit rqlToDataPointEventCountCondition(ASTNode rql) {
        //Setup Mappings
        DataPoints dataPoints = DataPoints.DATA_POINTS;
        Events events = Events.EVENTS;

        Map<String, Field<?>> fieldMap = new HashMap<>();
        fieldMap.put("xid", dataPoints.xid);
        fieldMap.put("name", dataPoints.name);
        fieldMap.put("deviceName", dataPoints.deviceName);
        fieldMap.put("message", events.message);
        fieldMap.put("alarmLevel", events.alarmLevel);
        fieldMap.put("count", DSL.field(events.getQualifiedName().append("count")));
        fieldMap.put("latestActive", events.activeTs);
        fieldMap.put("latestRtn", events.rtnTs);

        RQLToConditionWithTagKeys rqlToCondition = new RQLToConditionWithTagKeys(fieldMap, this.valueConverterMap);
        ConditionSortLimit conditions = rqlToCondition.visit(rql);
        return conditions;
    }

    /**
     * Shared logic for the event counts query building
     * @param conditions
     * @param from
     * @param to
     * @param countQuery
     * @param user
     * @return
     */
    private Select<? extends Record> createDataPointEventCountsQuery(ConditionSortLimit conditions,
                                                                        Long from,
                                                                        Long to,
                                                                        boolean countQuery,
                                                                        PermissionHolder user) {
        Events events = Events.EVENTS;
        DataPoints dataPoints = DataPoints.DATA_POINTS;
        Name tagsAlias = DSL.name(DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS);
        DataPointTags dataPointTags = DataPointTags.DATA_POINT_TAGS.as(tagsAlias);

        List<Field<?>> joinSelectFields = new ArrayList<>();
        joinSelectFields.add(dataPointTags.dataPointId);

        Map<String, Name> tags;
        if(conditions instanceof  ConditionSortLimitWithTagKeys) {
            ConditionSortLimitWithTagKeys tagConditionSortLimit = (ConditionSortLimitWithTagKeys)conditions;
            if(tagConditionSortLimit.getTagKeyToColumn().size() == 0) {
                tags = Collections.emptyMap();
            }else{
                tags = tagConditionSortLimit.getTagKeyToColumn();
            }
        }else{
            tags = Collections.emptyMap();
        }

        List<Field<String>> tagFields = new ArrayList<>();
        for(Entry<String, Name> entry : tags.entrySet()) {
            Field<String> tagField = DSL.field(tagsAlias.append(entry.getValue()), SQLDataType.VARCHAR(255).nullable(false));
            tagFields.add(tagField);
            joinSelectFields.add(DSL.max(DSL.case_().when(dataPointTags.tagKey.eq(entry.getKey()), dataPointTags.tagValue)).as(entry.getValue()));
        }

        Table<?> joinSelectTable = this.create.select(joinSelectFields).from(dataPointTags)
                .groupBy(dataPointTags.dataPointId).asTable(dataPointTags);

        //Outer Select
        List<Field<?>> outerSelectFields = new ArrayList<>();
        if(!countQuery) {
            outerSelectFields.addAll(tagFields);
            outerSelectFields.add(dataPoints.xid);
            outerSelectFields.add(dataPoints.name);
            outerSelectFields.add(dataPoints.deviceName);
            outerSelectFields.add(events.message);
            outerSelectFields.add(events.alarmLevel);
            outerSelectFields.add(DSL.field(events.getQualifiedName().append("count")));
            outerSelectFields.add(events.activeTs);
            outerSelectFields.add(events.rtnTs);
        }

        //Inner Select
        List<Field<?>> innerSelectFields = new ArrayList<>();
        innerSelectFields.add(events.typeRef1);
        innerSelectFields.add(events.message);
        innerSelectFields.add(DSL.max(events.alarmLevel).as(events.alarmLevel));
        Field<?> count = DSL.count(events.typeRef1).as("count");
        innerSelectFields.add(count);
        innerSelectFields.add(DSL.max(events.activeTs).as(events.activeTs));
        innerSelectFields.add(DSL.max(events.rtnTs).as(events.rtnTs));

        Condition eventsConditions = events.typeName.eq("DATA_POINT");
        if(from != null){
            eventsConditions = DSL.and(eventsConditions, events.activeTs.greaterOrEqual(from));
        }
        if(to != null){
            eventsConditions = DSL.and(eventsConditions, events.activeTs.lessThan(to));
        }

        SelectJoinStep<Record> innerSelect = this.create.select(innerSelectFields)
                .from(events);
        innerSelect = joinPermissions(innerSelect, conditions, user);
        Table<?> innerSelectTable = innerSelect
                .where(eventsConditions)
                .groupBy(events.typeRef1, events.typeRef2, events.message, events.alarmLevel).asTable(events);

        SelectConnectByStep<Record> afterWhere;
        if(countQuery) {
            return this.create.selectCount().from(innerSelectTable)
                    .leftJoin(dataPoints).on(dataPoints.id.eq(events.typeRef1))
                    .leftOuterJoin(joinSelectTable)
                    .on(dataPointTags.dataPointId.eq(dataPoints.id))
                    .where(conditions.getCondition());
        }else {
            afterWhere = this.create.select(outerSelectFields).from(innerSelectTable)
                    .leftJoin(dataPoints).on(dataPoints.id.eq(events.typeRef1))
                    .leftOuterJoin(joinSelectTable)
                    .on(dataPointTags.dataPointId.eq(dataPoints.id))
                    .where(conditions.getCondition());

            SelectLimitStep<Record> afterSort = conditions.getSort() == null ? afterWhere : afterWhere.orderBy(conditions.getSort());
            Select<Record> offsetStep = afterSort;
            if (conditions.getLimit() != null) {
                if (conditions.getOffset() != null) {
                    offsetStep = afterSort.limit(conditions.getOffset(), conditions.getLimit());
                } else {
                    offsetStep = afterSort.limit(conditions.getLimit());
                }
            }
            return offsetStep;
        }
    }
}
