/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import static com.serotonin.m2m2.db.dao.DataPointTagsDao.DATA_POINT_TAGS_PIVOT_ALIAS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.infiniteautomation.mango.db.query.*;
import com.serotonin.log.LogStopWatch;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.tables.DataPointTags;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.Events;
import com.infiniteautomation.mango.db.tables.MintermsRoles;
import com.infiniteautomation.mango.db.tables.PermissionsMinterms;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.EventInstanceTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.db.UserCommentTableDefinition;
import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.spring.service.EventInstanceService.AlarmPointTagCount;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.MappedRowCallback;
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
public class EventInstanceDao extends AbstractVoDao<EventInstanceVO, EventInstanceTableDefinition> {

    private static final LazyInitSupplier<EventInstanceDao> springInstance = new LazyInitSupplier<>(
            () -> Common.getRuntimeContext().getBean(EventInstanceDao.class));

    private final UserTableDefinition userTable;
    private final UserCommentTableDefinition userCommentTable;
    private final DataPointTagsDao dataPointTagsDao;
    private final PermissionService permissionService;

    @Autowired
    private EventInstanceDao(EventInstanceTableDefinition table,
            UserTableDefinition userTable,
            UserCommentTableDefinition userCommentTable,
            DataPointTagsDao dataPointTagsDao,
            PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(null, table, null, mapper, publisher);
        this.userTable = userTable;
        this.userCommentTable = userCommentTable;
        this.dataPointTagsDao = dataPointTagsDao;
        this.permissionService = permissionService;
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
    protected Object[] voToObjectArray(EventInstanceVO event) {
        EventType type = event.getEventType();
        if (event.isRtnApplicable() && !event.isActive()) {
            return new Object[] {
                    type.getEventType(),
                    type.getEventSubtype(),
                    type.getReferenceId1(),
                    type.getReferenceId2(),
                    event.getActiveTimestamp(),
                    boolToChar(event.isRtnApplicable()),
                    event.getRtnTimestamp(),
                    event.getRtnCause().value(),
                    event.getAlarmLevel().value(),
                    writeTranslatableMessage(event.getMessage()),
                    null,
                    null,
                    null,
                    event.getReadPermission().getId()
            };
        }else {
            return new Object[] {
                    type.getEventType(),
                    type.getEventSubtype(),
                    type.getReferenceId1(),
                    type.getReferenceId2(),
                    event.getActiveTimestamp(),
                    boolToChar(event.isRtnApplicable()),
                    null,
                    null,
                    event.getAlarmLevel().value(),
                    writeTranslatableMessage(event.getMessage()),
                    null,
                    null,
                    null,
                    event.getReadPermission().getId()
            };
        }
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {

        select = select.leftJoin(userTable.getTableAsAlias()).on(userTable.getAlias("id").eq(table.getAlias("ackUserId")));

        if (conditions instanceof ConditionSortLimitWithTagKeys) {
            Map<String, Name> tagKeyToColumn = ((ConditionSortLimitWithTagKeys) conditions).getTagKeyToColumn();
            if (!tagKeyToColumn.isEmpty()) {
                // TODO Mango 4.0 throw exception or don't join if event type is not restricted to DATA_POINT
                Table<Record> pivotTable = dataPointTagsDao.createTagPivotSql(tagKeyToColumn).asTable().as(DATA_POINT_TAGS_PIVOT_ALIAS);
                select = select.leftJoin(pivotTable).on(DataPointTagsDao.PIVOT_ALIAS_DATA_POINT_ID.eq(this.table.getAlias("typeRef1")));
            }
        }

        return select;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select,
            ConditionSortLimit conditions, PermissionHolder user) {

        if(!permissionService.hasAdminRole(user)) {

            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);

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
                            EventInstanceTableDefinition.READ_PERMISSION_ALIAS));

        }
        return select;

    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(userTable.getAlias("username"));
        Field<?> hasComments = this.create.selectCount().from(userCommentTable.getTableAsAlias())
                .where(userCommentTable.getAlias("commentType").eq(UserCommentVO.TYPE_EVENT), userCommentTable.getAlias("typeKey").eq(this.table.getAlias("id"))).asField("cnt");
        fields.add(hasComments);
        return fields;
    }

    @Override
    public RowMapper<EventInstanceVO> getRowMapper() {
        return new EventInstanceVORowMapper();
    }

    public static class EventInstanceVORowMapper implements RowMapper<EventInstanceVO> {
        @Override
        public EventInstanceVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventInstanceVO event = new EventInstanceVO();
            event.setId(rs.getInt(1));

            EventType type = createEventType(rs, 2);
            event.setEventType(type);
            event.setActiveTimestamp(rs.getLong(6));
            event.setRtnApplicable(charToBool(rs.getString(7)));
            event.setAlarmLevel(AlarmLevels.fromValue(rs.getInt(10)));
            TranslatableMessage message = BaseDao.readTranslatableMessage(rs, 11);
            if(message == null)
                event.setMessage(new TranslatableMessage("common.noMessage"));
            else
                event.setMessage(message);

            //Set the Return to normal
            long rtnTs = rs.getLong(8);
            if (!rs.wasNull()){
                //if(event.isActive()){ Probably don't need this
                event.setRtnTimestamp(rtnTs);
                event.setRtnCause(ReturnCause.fromValue(rs.getInt(9)));
                //}
            }

            MangoPermission read = new MangoPermission(rs.getInt(15));
            event.supplyReadPermission(() -> read);

            long ackTs = rs.getLong(12);
            if (!rs.wasNull()) {
                //Compute total time
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
            vo.setEventComments(query(EVENT_COMMENT_SELECT, new Object[] { vo.getId() },
                    UserCommentDao.getInstance().getRowMapper()));
        }

        MangoPermission read = vo.getReadPermission();
        vo.supplyReadPermission(() -> permissionService.get(read.getId()));
    }

    @Override
    public void deletePostRelationalData(EventInstanceVO vo) {
        MangoPermission readPermission = vo.getReadPermission();
        permissionService.deletePermissions(readPermission);
    }

    private static final String EVENT_COMMENT_SELECT = UserCommentDao.USER_COMMENT_SELECT //
            + "where uc.commentType= " + UserCommentVO.TYPE_EVENT //
            + " and uc.typeKey=? " //
            + "order by uc.ts";

    public static EventType createEventType(ResultSet rs, int offset) throws SQLException {
        String typeName = rs.getString(offset);
        String subtypeName = rs.getString(offset + 1);
        EventType type;
        if (typeName.equals(EventType.EventTypeNames.DATA_POINT))
            type = new DataPointEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.DATA_SOURCE))
            type = new DataSourceEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.SYSTEM))
            type = new SystemEventType(subtypeName, rs.getInt(offset + 2));
        else if (typeName.equals(EventType.EventTypeNames.PUBLISHER))
            type = new PublisherEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.AUDIT))
            throw new ShouldNeverHappenException("AUDIT events should not exist here. Consider running the SQL: DELETE FROM events WHERE typeName='AUDIT';");
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
        Map<String, Function<Object, Object>> myConverters = new HashMap<>();
        myConverters.put("alarmLevel", value -> {
            if (value instanceof String) {
                return AlarmLevels.fromName((String)value).value();
            }else if(value instanceof AlarmLevels) {
                return ((AlarmLevels)value).value();
            }
            return value;
        });
        return combine(converters, myConverters);
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
     * @param conditions
     * @param from - all events >= this time (can be null)
     * @param to - all events < this time (can be null)
     * @param user
     * @param callback
     */
    public void queryDataPointEventCountsByRQL(ConditionSortLimit conditions,
            Long from,
            Long to,
            PermissionHolder user,
            MappedRowCallback<AlarmPointTagCount> callback) {

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

                            callback.row(new AlarmPointTagCount(xid, name, deviceName, message, level, count, tagMap), rowNum);

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
     * @param conditions
     * @param from
     * @param to
     * @param user
     * @return
     */
    public int countDataPointEventCountsByRQL(ConditionSortLimit conditions,
        Long from,
        Long to,
        PermissionHolder user) {
        final Select<?> query = createDataPointEventCountsQuery(conditions, from, to, true, user);
        return query.fetchOneInto(Integer.class);
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
        Events events = Events.EVENTS.as("evt");
        DataPoints dataPoints = DataPoints.DATA_POINTS.as("dp");
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
        }

        //Inner Select
        List<Field<?>> innerSelectFields = new ArrayList<>();
        innerSelectFields.add(events.typeRef1);
        innerSelectFields.add(events.message);
        innerSelectFields.add(DSL.max(events.alarmLevel).as(events.alarmLevel));
        Field<?> count = DSL.count(events.typeRef1).as("count");
        innerSelectFields.add(count);

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