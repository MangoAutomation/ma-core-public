/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jooq.Condition;
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
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.EventInstanceTableDefinition;
import com.infiniteautomation.mango.spring.db.UserCommentTableDefinition;
import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
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
public class EventInstanceDao extends AbstractDao<EventInstanceVO, EventInstanceTableDefinition> {

    private static final LazyInitSupplier<EventInstanceDao> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(EventInstanceDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (EventInstanceDao)o;
    });

    private final UserTableDefinition userTable;
    private final UserCommentTableDefinition userCommentTable;

    @Autowired
    private EventInstanceDao(EventInstanceTableDefinition table,
            UserTableDefinition userTable,
            UserCommentTableDefinition userCommentTable,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(null, table, null, mapper, publisher);
        this.userTable = userTable;
        this.userCommentTable = userCommentTable;
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
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select,
            ConditionSortLimit conditions) {
        select = select.leftJoin(userTable.getTableAsAlias()).on(userTable.getAlias("id").eq(table.getAlias("ackUserId")));
        return select.leftJoin(EventInstanceTableDefinition.USER_EVENTS_TABLE.as(EventInstanceTableDefinition.USER_EVENTS_ALIAS))
                .on(DSL.field(EventInstanceTableDefinition.USER_EVENTS_ALIAS.append("eventId")).eq(this.table.getAlias("id")));
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(userTable.getAlias("username"));
        fields.add(DSL.field(DSL.name(EventInstanceTableDefinition.USER_EVENTS_ALIAS).append("silenced")));
        Field<?> hasComments = this.create.selectCount().from(userCommentTable.getTableAsAlias())
                .where(userCommentTable.getAlias("commentType").eq(UserCommentVO.TYPE_EVENT), userCommentTable.getAlias("typeKey").eq(this.table.getAlias("id"))).asField("cnt");
        fields.add(hasComments);
        return fields;
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
            event.setHasComments(rs.getInt(17) > 0);

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
        return ejt.queryForObject(getJoinedSelectQuery().getSQL()
                + "where ue.silenced=? and ue.userId=? and evt.alarmLevel=? ORDER BY evt.activeTs DESC LIMIT 1", new Object[] { boolToChar(false), userId, level.value() },getRowMapper(), null);
    }

    /**
     * @param userId
     * @return
     */
    public List<EventInstanceVO> getUnsilencedEvents(int userId) {
        return ejt.query(getJoinedSelectQuery().getSQL()
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
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters) {
        Map<String, Function<Object, Object>> fullMap;
        if(valueConverterMap == null) {
            fullMap = new HashMap<>(this.valueConverterMap);
        }else {
            fullMap = new HashMap<>(this.valueConverterMap);
            fullMap.putAll(valueConverters);
        }

        Map<String, Field<?>> fullFields;
        if(fieldMap == null) {
            fullFields = new HashMap<>(this.table.getAliasMap());
        }else {
            fullFields = new HashMap<>(this.table.getAliasMap());
            fullFields.putAll(fieldMap);
        }

        RQLToEventInstanceConditions rqlToSelect = new RQLToEventInstanceConditions(fullFields, fullMap);
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

    public static class RQLToEventInstanceConditions extends RQLToCondition {

        public RQLToEventInstanceConditions(Map<String, Field<?>> fieldMapping, Map<String, Function<Object, Object>> valueConverterMap) {
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
