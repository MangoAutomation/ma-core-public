/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
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

import net.jazdw.rql.parser.ASTNode;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class EventInstanceDao extends AbstractDao<EventInstanceVO> {

    public static final Name ALIAS = DSL.name("evt");
    public static final Table<? extends Record> TABLE = DSL.table(SchemaDefinition.EVENTS_TABLE);

    private static final LazyInitSupplier<EventInstanceDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(EventInstanceDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (EventInstanceDao)o;
    });
    
    @Autowired
    private EventInstanceDao(@Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(null,
                TABLE, ALIAS,
                new Field<?>[]{
                        DSL.field(DSL.name("U").append("username")),
                        DSL.field(DSL.name("UE").append("silenced"))}, 
                null, mapper, publisher);
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
    protected Object[] voToObjectArray(EventInstanceVO vo) {
        return new Object[]{
                vo.getId(),

        };
    }
    
    @Override
    protected LinkedHashMap<String,Integer> getPropertyTypeMap(){
        LinkedHashMap<String,Integer> map = new LinkedHashMap<String,Integer>();
        map.put("id", Types.INTEGER);
        map.put("typeName", Types.VARCHAR);
        map.put("subtypeName", Types.VARCHAR);
        map.put("typeRef1", Types.INTEGER);
        map.put("typeRef2", Types.INTEGER);
        map.put("activeTs", Types.BIGINT);
        map.put("rtnApplicable", Types.CHAR);
        map.put("rtnTs", Types.BIGINT);
        map.put("rtnCause", Types.INTEGER);
        map.put("alarmLevel", Types.INTEGER);
        map.put("message", Types.LONGVARCHAR);
        map.put("ackTs", Types.BIGINT);
        map.put("ackUserId", Types.INTEGER);
        map.put("alternateAckSource", Types.LONGVARCHAR);

        return map;
    }

    @Override
    protected Map<String, IntStringPair> getPropertiesMap() {
        Map<String,IntStringPair> map = new HashMap<String,IntStringPair>();
        map.put("activeTimestamp", new IntStringPair(Types.BIGINT, "activeTs"));
        map.put("activeTimestampString", new IntStringPair(Types.BIGINT, "activeTs"));
        map.put("rtnTimestampString", new IntStringPair(Types.BIGINT, "rtnTs"));

        /*
         * IF(evt.rtnTs=null,
         * 		IF(evt.rtnApplicable='Y',
         * 			(NOW() - evt.activeTs),
         * 			-1),
         * 		IF(evt.rtnApplicable='Y',
         * 			(evt.rtnTs - evt.activeTs),
         * 			-1)
         *  )
         */
        switch(Common.databaseProxy.getType()){
            case MYSQL:
            case MSSQL:
                map.put("totalTimeString", new IntStringPair(Types.BIGINT, "IF(evt.rtnTs is null,IF(evt.rtnApplicable='Y',(? - evt.activeTs),-1),IF(evt.rtnApplicable='Y',(evt.rtnTs - evt.activeTs),-1))"));
                break;
            case H2:
                map.put("totalTimeString", new IntStringPair(Types.BIGINT, "CASE WHEN evt.rtnTs IS NULL THEN "
                        + "CASE WHEN evt.rtnApplicable='Y' THEN (? - evt.activeTs) ELSE -1 END "
                        + "ELSE CASE WHEN evt.rtnApplicable='Y' THEN (evt.rtnTs - evt.activeTs) ELSE -1 END END"));
                break;
            default:
                throw new ShouldNeverHappenException("Unsupported database for Alarms.");
        }
        map.put("messageString", new IntStringPair(Types.VARCHAR, "message"));
        map.put("rtnTimestampString", new IntStringPair(Types.BIGINT, "rtnTs"));
        map.put("userNotified", new IntStringPair(Types.CHAR, "silenced"));
        map.put("acknowledged", new IntStringPair(Types.BIGINT, "ackTs"));
        map.put("userId", new IntStringPair(Types.INTEGER, "ue.userId")); //Mapping for user
        return map;
    }
    
    @Override
    public SelectJoinStep<Record> getSelectQuery() {
        Field<?> hasComments = this.create.selectCount().from(SchemaDefinition.USER_COMMENTS_TABLE)
                .where(DSL.field("commentType").eq(UserCommentVO.TYPE_EVENT), DSL.field("typeKey").eq(this.propertyToField.get("id"))).asField("cnt");
        List<Field<?>> fields = new ArrayList<>(this.fields);
        fields.add(hasComments);
        SelectJoinStep<Record> query = this.create.select(fields)
                .from(this.table.as(tableAlias));
        return joinTables(query);
    }
    
    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select) {
        select = select.join(SchemaDefinition.USERS_TABLE).on(DSL.field(DSL.name("U").append("id")).eq(this.propertyToField.get("ackUserId")));
        return select.join(SchemaDefinition.USER_EVENTS_TABLE).on(DSL.field(DSL.name("UE").append("eventId")).eq(this.propertyToField.get("id")));
    }

    @Override
    public RowMapper<EventInstanceVO> getRowMapper() {
        return new UserEventInstanceVORowMapper();
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
                if(event.isRtnApplicable()){
                    event.setTotalTime(rtnTs - event.getActiveTimestamp());
                }else{
                    event.setTotalTime(-1L);
                }
            }else{
                if(event.isRtnApplicable()){
                    //Has not been acknowledged yet
                    Date now = new Date();
                    event.setTotalTime(now.getTime() -event.getActiveTimestamp());
                }else{
                    //Won't ever be
                    event.setTotalTime(-1L);
                }
            }

            long ackTs = rs.getLong(12);
            if (!rs.wasNull()) {
                //Compute total time
                event.setAcknowledgedTimestamp(ackTs);
                event.setAcknowledgedByUserId(rs.getInt(13));
                if (!rs.wasNull())
                    event.setAcknowledgedByUsername(rs.getString(15));
                event.setAlternateAckSource(BaseDao.readTranslatableMessage(rs, 14));
            }
            event.setHasComments(rs.getInt(16) > 0);

            //This makes another query!
            this.attachRelationalInfo(event);


            return event;
        }

        private static final String EVENT_COMMENT_SELECT = UserCommentDao.USER_COMMENT_SELECT //
                + "where uc.commentType= " + UserCommentVO.TYPE_EVENT //
                + " and uc.typeKey=? " //
                + "order by uc.ts";

        void attachRelationalInfo(EventInstanceVO event) {
            if (event.isHasComments())
                event.setEventComments(EventInstanceDao.getInstance().query(EVENT_COMMENT_SELECT, new Object[] { event.getId() },
                        UserCommentDao.getInstance().getRowMapper()));
        }


    }

    class UserEventInstanceVORowMapper extends EventInstanceVORowMapper {
        @Override
        public EventInstanceVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventInstanceVO event = super.mapRow(rs, rowNum);
            event.setSilenced(charToBool(rs.getString(17)));
            if (!rs.wasNull())
                event.setUserNotified(true);
            return event;
        }
    }

    /**
     * @param userId
     * @param level
     * @return
     */
    public EventInstanceVO getHighestUnsilencedEvent(int userId, AlarmLevels level) {
        return ejt.queryForObject(getSelectQuery().getSQL()
                + "where ue.silenced=? and ue.userId=? and evt.alarmLevel=? ORDER BY evt.activeTs DESC LIMIT 1", new Object[] { boolToChar(false), userId, level.value() },getRowMapper(), null);
    }

    /**
     * @param userId
     * @return
     */
    public List<EventInstanceVO> getUnsilencedEvents(int userId) {
        return ejt.query(getSelectQuery().getSQL()
                + "where ue.silenced=? and ue.userId=?", new Object[] { boolToChar(false), userId },getRowMapper());

    }

    static EventType createEventType(ResultSet rs, int offset) throws SQLException {
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
     * @param lifeSafety
     * @return
     */
    public int countUnsilencedEvents(int userId, AlarmLevels level) {
        return ejt.queryForInt(getCountQuery().getSQL() + " where ue.silenced=? and ue.userId=? and evt.alarmLevel=?", new Object[] { boolToChar(false), userId, level.value() }, 0);
    }
    
    @Override
    public ConditionSortLimit rqlToCondition(ASTNode rql) {
        RQLToEventInstanceConditions rqlToSelect = new RQLToEventInstanceConditions(this.propertyToField, this.valueConverterMap);
        return rqlToSelect.visit(rql);
    }
    
    @Override
    protected Map<String, Function<Object, Object>> createValueConverterMap() {
        Map<String, Function<Object, Object>> map = new HashMap<>(super.createValueConverterMap());
        map.put("alarmLevel", value -> {
            if (value instanceof String) {
                return AlarmLevels.fromName((String)value).value();
            }else if(value instanceof AlarmLevels) {
                return ((AlarmLevels)value).value();
            }
            return value;
        });
        return map;
    }

    @Override
    protected Map<String, Field<Object>> createPropertyToField() {
        Map<String, Field<Object>> map = super.createPropertyToField();
        map.put("eventType", map.get("typeName"));
        map.put("referenceId1", map.get("typeRef1"));
        map.put("referenceId2", map.get("typeRef1"));
        map.put("activeTimestamp", map.get("activeTs"));
        map.put("acknowledged", map.get("ackTs"));
        map.put("active", map.get("rtnTs"));
        return map;
    }
    
    public static class RQLToEventInstanceConditions extends RQLToCondition {
        
        public RQLToEventInstanceConditions(Map<String, Field<Object>> fieldMapping, Map<String, Function<Object, Object>> valueConverterMap) {
            super(fieldMapping, valueConverterMap);
        }
        
        @Override
        protected Condition visitConditionNode(ASTNode node) {
            String property = (String) node.getArgument(0);
            switch(property) {
                case "acknowledged":
                    Field<Object> ackField = getField(property);
                    Function<Object, Object> ackValueConverter = getValueConverter(ackField);
                    Object ackFirstArg = ackValueConverter.apply(node.getArgument(1));
                    switch (node.getName().toLowerCase()) {
                        case "eq":
                            if(ackFirstArg == null) {
                                return ackField.isNull();
                            }else {
                                return (Boolean)ackFirstArg ? ackField.isNotNull() : ackField.isNull();
                            }
                        case "ne":
                            if(ackFirstArg == null) {
                                return ackField.isNotNull();
                            }else {
                                return (Boolean)ackFirstArg ? ackField.isNull() : ackField.isNotNull();
                            }
                        default:
                            return super.visitConditionNode(node);
                    }
                case "active":
                    Field<Object> activeField = getField(property);
                    Function<Object, Object> activeValueConverter = getValueConverter(activeField);
                    Object activeFirstArg = activeValueConverter.apply(node.getArgument(1));
                    Condition rtnApplicable = getField("rtnApplicable").eq("Y");
                    switch (node.getName().toLowerCase()) {
                        case "eq":
                            if(activeFirstArg == null) {
                                return activeField.isNull();
                            }else {
                                return (Boolean)activeFirstArg ? activeField.isNull().and(rtnApplicable) : activeField.isNotNull().and(rtnApplicable);
                            }
                        case "ne":
                            if(activeFirstArg == null) {
                                return activeField.isNull().and(rtnApplicable);
                            }else {
                                return (Boolean)activeFirstArg ? activeField.isNotNull().and(rtnApplicable) : activeField.isNull().and(rtnApplicable);
                            }
                        default:
                            return super.visitConditionNode(node);
                    }
                default:
                    return super.visitConditionNode(node);
            }
        }
    }
}
