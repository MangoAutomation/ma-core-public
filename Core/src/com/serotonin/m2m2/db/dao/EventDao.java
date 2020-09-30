/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Field;
import org.jooq.UpdateConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.infiniteautomation.mango.spring.db.EventInstanceTableDefinition;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.json.JsonException;
import com.serotonin.m2m2.Common;
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
import com.serotonin.m2m2.rt.event.type.MissingEventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.util.JsonSerializableUtility;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.event.audit.AuditEventInstanceVO;

/**
 * This class is used for runtime management of events sort an eventRT but could
 *  be merged with the EventInstanceDao which is used for historical access and querying
 *
 * @author Terry Packer
 */
@Repository
public class EventDao extends BaseDao {
    private static final Log LOG = LogFactory.getLog(EventDao.class);

    private static final LazyInitSupplier<EventDao> instance = new LazyInitSupplier<>(() -> Common.getBean(EventDao.class));

    private final AuditEventDao auditEventDao;
    private final UserCommentDao userCommentDao;
    private final EventInstanceTableDefinition table;

    @Autowired
    private EventDao(AuditEventDao auditEventDao,
            UserCommentDao userCommentDao,
            EventInstanceTableDefinition table) {
        this.auditEventDao = auditEventDao;
        this.userCommentDao = userCommentDao;
        this.table = table;
    }

    public static EventDao getInstance() {
        return instance.get();
    }

    public void saveEvent(EventInstance event) {
        if (event.getEventType().getEventType().equals(EventType.EventTypeNames.AUDIT)) {
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

    private static final String EVENT_INSERT = //
            "insert into events (typeName, subtypeName, typeRef1, typeRef2, activeTs, rtnApplicable, rtnTs, rtnCause, " //
            + "  alarmLevel, message, ackTs) " //
            + "values (?,?,?,?,?,?,?,?,?,?,?)";
    private static final int[] EVENT_INSERT_TYPES = { Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
            Types.BIGINT, Types.CHAR, Types.BIGINT, Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.BIGINT };

    private void insertEvent(EventInstance event) {
        EventType type = event.getEventType();

        Object[] args = new Object[11];
        args[0] = type.getEventType();
        args[1] = type.getEventSubtype();
        args[2] = type.getReferenceId1();
        args[3] = type.getReferenceId2();
        args[4] = event.getActiveTimestamp();
        args[5] = boolToChar(event.isRtnApplicable());
        if (event.isRtnApplicable() && !event.isActive()) {
            args[6] = event.getRtnTimestamp();
            args[7] = event.getRtnCause().value();
        }
        args[8] = event.getAlarmLevel().value();
        args[9] = writeTranslatableMessage(event.getMessage());
        event.setId(doInsert(EVENT_INSERT, args, EVENT_INSERT_TYPES));
    }

    private static final String EVENT_UPDATE = "update events set rtnTs=?, rtnCause=? where id=?";

    private void updateEvent(EventInstance event) {
        if (event.isRtnApplicable()) {
            ejt.update(EVENT_UPDATE, new Object[] { event.getRtnTimestamp(), event.getRtnCause().value(), event.getId() });
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
        values.put(this.table.getField("rtnTs"), timestamp);
        values.put(this.table.getField("rtnCause"), cause.value());

        for(List<Integer> batch : batchInParameters(eventIds)) {
            UpdateConditionStep<?> update = this.create.update(this.table.getTable()).set(values).where(this.table.getField("rtnApplicable").eq(boolToChar(true)).and(this.table.getIdField().in(batch)));
            String sql = update.getSQL();
            List<Object> args = update.getBindValues();
            ejt.update(sql, args.toArray(new Object[args.size()]));
        }
    }

    private static final String EVENT_ACK = "update events set ackTs=?, ackUserId=?, alternateAckSource=? where id=? and ackTs is null";

    public boolean ackEvent(int eventId, long time, int userId, TranslatableMessage alternateAckSource) {
        // Ack the event
        int count = ejt.update(EVENT_ACK,
                new Object[] { time, userId == 0 ? null : userId, writeTranslatableMessage(alternateAckSource), eventId },
                new int[] { Types.BIGINT, Types.INTEGER, Types.CLOB, Types.INTEGER });
        return count > 0;
    }

    private static final String BASIC_EVENT_SELECT = //
            "select e.id, e.typeName, e.subtypeName, e.typeRef1, e.typeRef2, e.activeTs, e.rtnApplicable, e.rtnTs, " //
            + "  e.rtnCause, e.alarmLevel, e.message, e.ackTs, e.ackUserId, u.username, e.alternateAckSource, " //
            + "  (select count(1) from userComments where commentType=" + UserCommentVO.TYPE_EVENT //
            + "     and typeKey=e.id) as cnt " //
            + "from events e " //
            + "  left join users u on e.ackUserId=u.id ";

    public List<EventInstance> getActiveEvents() {
        List<EventInstance> results = query(BASIC_EVENT_SELECT + "where e.rtnApplicable=? and e.rtnTs is null",
                new Object[] { boolToChar(true) }, new EventInstanceRowMapper());
        attachRelationalInfo(results);
        return results;
    }

    public EventInstance get(int eventId){
        return queryForObject(BASIC_EVENT_SELECT + " where e.id = ?", new Object[]{ eventId }, new EventInstanceRowMapper(), null);
    }

    private EventInstance getEventInstance(int eventId) {
        return queryForObject(BASIC_EVENT_SELECT + "where e.id=?", new Object[] { eventId },
                new EventInstanceRowMapper());
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
            long ackTs = rs.getLong(12);
            if (!rs.wasNull()) {
                event.setAcknowledgedTimestamp(ackTs);
                event.setAcknowledgedByUserId(rs.getInt(13));
                if (!rs.wasNull())
                    event.setAcknowledgedByUsername(rs.getString(14));
                event.setAlternateAckSource(BaseDao.readTranslatableMessage(rs, 15));
            }
            event.setHasComments(rs.getInt(16) > 0);

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
        if (typeName.equals(EventType.EventTypeNames.DATA_POINT))
            type = new DataPointEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.DATA_SOURCE))
            type = new DataSourceEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.SYSTEM))
            type = new SystemEventType(subtypeName, rs.getInt(offset + 2));
        else if (typeName.equals(EventType.EventTypeNames.PUBLISHER))
            type = new PublisherEventType(rs.getInt(offset + 2), rs.getInt(offset + 3));
        else if (typeName.equals(EventType.EventTypeNames.AUDIT))
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

    private void attachRelationalInfo(List<EventInstance> list) {
        for (EventInstance e : list)
            attachRelationalInfo(e);
    }

    private static final String EVENT_COMMENT_SELECT = UserCommentDao.USER_COMMENT_SELECT //
            + "where uc.commentType= " + UserCommentVO.TYPE_EVENT //
            + " and uc.typeKey=? " //
            + "order by uc.ts";

    void attachRelationalInfo(EventInstance event) {
        if (event.isHasComments())
            event.setEventComments(query(EVENT_COMMENT_SELECT, new Object[] { event.getId() },
                    userCommentDao.getRowMapper()));
    }

    public EventInstance insertEventComment(UserCommentVO comment) {
        userCommentDao.insert(comment);
        return getEventInstance(comment.getReferenceId());
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

                return count;
            }
        });


        return count;
    }

    public int getEventCount() {
        return ejt.queryForInt("select count(*) from events", null, 0);
    }
}
