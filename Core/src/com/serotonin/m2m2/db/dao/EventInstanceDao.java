/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
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
 * This is used for querying events from the database
 *
 * @author Terry Packer
 *
 */
@Repository()
public class EventInstanceDao extends AbstractDao<EventInstanceVO, EventInstanceTableDefinition> {

    private static final LazyInitSupplier<EventInstanceDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(EventInstanceDao.class);
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
        return select.leftJoin(userTable.getTableAsAlias()).on(userTable.getAlias("id").eq(table.getAlias("ackUserId")));
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

            return event;
        }
    }

    @Override
    public void loadRelationalData(EventInstanceVO vo) {
        if (vo.isHasComments())
            vo.setEventComments(EventInstanceDao.getInstance().query(EVENT_COMMENT_SELECT, new Object[] { vo.getId() },
                    UserCommentDao.getInstance().getRowMapper()));
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
     * @param lifeSafety
     * @return
     */
    public int countUnsilencedEvents(int userId, AlarmLevels level) {
        return ejt.queryForInt(getCountQuery().getSQL() + " where ue.silenced=? and ue.userId=? and evt.alarmLevel=?", new Object[] { boolToChar(false), userId, level.value() }, 0);
    }

    @Override
    public ConditionSortLimit rqlToCondition(ASTNode rql, Map<String, Field<?>> fieldMap, Map<String, Function<Object, Object>> valueConverters) {
        Map<String, Function<Object, Object>> fullMap;
        if(valueConverters == null) {
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
