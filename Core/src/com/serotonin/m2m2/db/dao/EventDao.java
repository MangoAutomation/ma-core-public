/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.db.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.spring.ExtendedJdbcTemplate;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.i18n.Translations;
import com.serotonin.m2m2.module.EventTypeDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.DataPointEventType;
import com.serotonin.m2m2.rt.event.type.DataSourceEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.PublisherEventType;
import com.serotonin.m2m2.rt.event.type.SystemEventType;
import com.serotonin.m2m2.vo.UserComment;
import com.serotonin.m2m2.vo.event.EventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.dwr.EventsDwr;
import com.serotonin.util.SerializationHelper;

public class EventDao extends BaseDao {
    private static final int MAX_PENDING_EVENTS = 100;

    public void saveEvent(EventInstance event) {
        if (event.getId() == Common.NEW_ID)
            insertEvent(event);
        else
            updateEvent(event);
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
        if (!event.isActive()) {
            args[6] = event.getRtnTimestamp();
            args[7] = event.getRtnCause();
        }
        args[8] = event.getAlarmLevel();
        args[9] = writeTranslatableMessage(event.getMessage());
        if (!event.isAlarm()) {
            event.setAcknowledgedTimestamp(event.getActiveTimestamp());
            args[10] = event.getAcknowledgedTimestamp();
        }
        event.setId(doInsert(EVENT_INSERT, args, EVENT_INSERT_TYPES));
        event.setEventComments(new LinkedList<UserComment>());
    }

    private static final String EVENT_UPDATE = "update events set rtnTs=?, rtnCause=? where id=?";

    private void updateEvent(EventInstance event) {
        ejt.update(EVENT_UPDATE, new Object[] { event.getRtnTimestamp(), event.getRtnCause(), event.getId() });
    }

    private static final String EVENT_BULK_RTN = "update events set rtnTs=?, rtnCause=? where id in ";
    public void returnEventsToNormal(List<Integer> eventIds, long timestamp, long cause){
    	if(eventIds.size() == 0)
    		throw new ShouldNeverHappenException("Not enough Ids!");
    	StringBuilder inClause = new StringBuilder();
    	inClause.append("(");
    	final String comma = ",";
    	Iterator<Integer> it = eventIds.iterator();
    	while(it.hasNext()){
    		inClause.append(it.next());
    		if(it.hasNext())
    			inClause.append(comma);
    	}
    	inClause.append(")");
    	ejt.update( EVENT_BULK_RTN + inClause.toString(), new Object[]{timestamp, cause});
    }
    
    private static final String EVENT_ACK = "update events set ackTs=?, ackUserId=?, alternateAckSource=? where id=? and ackTs is null";
    private static final String USER_EVENT_ACK = "update userEvents set silenced=? where eventId=?";

    public void ackEvent(int eventId, long time, int userId, TranslatableMessage alternateAckSource) {
        // Ack the event
        ejt.update(EVENT_ACK, new Object[] { time, userId == 0 ? null : userId,
                writeTranslatableMessage(alternateAckSource), eventId }, new int[] { Types.BIGINT, Types.INTEGER,
                Types.CLOB, Types.INTEGER });
        // Silence the user events
        ejt.update(USER_EVENT_ACK, new Object[] { boolToChar(true), eventId });

    }

    private static final String USER_EVENTS_INSERT = "insert into userEvents (eventId, userId, silenced) values (?,?,?)";

    public void insertUserEvents(final int eventId, final List<Integer> userIds, final boolean alarm) {
        ejt.batchUpdate(USER_EVENTS_INSERT, new BatchPreparedStatementSetter() {
            @Override
            public int getBatchSize() {
                return userIds.size();
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, eventId);
                ps.setInt(2, userIds.get(i));
                ps.setString(3, boolToChar(!alarm));
            }
        });


    }

    private static final String BASIC_EVENT_SELECT = //
    "select e.id, e.typeName, e.subtypeName, e.typeRef1, e.typeRef2, e.activeTs, e.rtnApplicable, e.rtnTs, " //
            + "  e.rtnCause, e.alarmLevel, e.message, e.ackTs, e.ackUserId, u.username, e.alternateAckSource, " //
            + "  (select count(1) from userComments where commentType=" + UserComment.TYPE_EVENT //
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
    	return queryForObject(BASIC_EVENT_SELECT + " where e.id = ?", new Object[]{ eventId }, new EventInstanceRowMapper());
    }
    
    private static final String EVENT_SELECT_WITH_USER_DATA = //
    "select e.id, e.typeName, e.subtypeName, e.typeRef1, e.typeRef2, e.activeTs, e.rtnApplicable, e.rtnTs, " //
            + "  e.rtnCause, e.alarmLevel, e.message, e.ackTs, e.ackUserId, u.username, e.alternateAckSource, " //
            + "  (select count(1) from userComments where commentType=" + UserComment.TYPE_EVENT //
            + "     and typeKey=e.id) as cnt, " //
            + "  ue.silenced " //
            + "from events e " //
            + "  left join users u on e.ackUserId=u.id " //
            + "  left join userEvents ue on e.id=ue.eventId ";

    public List<EventInstance> getEventsForDataPoint(int dataPointId, int userId) {
        List<EventInstance> results = query(EVENT_SELECT_WITH_USER_DATA //
                + "where e.typeName=? " //
                + "  and e.typeRef1=? " //
                + "  and ue.userId=? " //
                + "order by e.activeTs desc",
                new Object[] { EventType.EventTypeNames.DATA_POINT, dataPointId, userId },
                new UserEventInstanceRowMapper());
        attachRelationalInfo(results);
        return results;
    }



    public List<EventInstance> getPendingEventsForDataSource(int dataSourceId, int userId) {
        return getPendingEvents(EventType.EventTypeNames.DATA_SOURCE, dataSourceId, userId);
    }

    public List<EventInstance> getPendingEventsForPublisher(int publisherId, int userId) {
        return getPendingEvents(EventType.EventTypeNames.PUBLISHER, publisherId, userId);
    }

    List<EventInstance> getPendingEvents(String typeName, int typeRef1, int userId) {
        Object[] params;
        StringBuilder sb = new StringBuilder();
        sb.append(EVENT_SELECT_WITH_USER_DATA);
        sb.append("where e.typeName=?");

        if (typeRef1 == -1) {
            params = new Object[] { typeName, userId, boolToChar(true) };
        }
        else {
            sb.append("  and e.typeRef1=?");
            params = new Object[] { typeName, typeRef1, userId, boolToChar(true) };
        }
        sb.append("  and ue.userId=? ");
        sb.append("  and (e.ackTs is null or (e.rtnApplicable=? and e.rtnTs is null and e.alarmLevel > 0)) ");
        sb.append("order by e.activeTs desc");

        List<EventInstance> results = query(sb.toString(), params, new UserEventInstanceRowMapper());
        attachRelationalInfo(results);
        return results;
    }

    public List<EventInstance> getAllUnsilencedEvents(int userId) {

        StringBuilder sb = new StringBuilder();
        sb.append(EVENT_SELECT_WITH_USER_DATA);

        sb.append(" where ue.userId=? ");
        sb.append(" and ue.silenced=? ");
        sb.append("order by e.activeTs desc");

        List<EventInstance> results = query(sb.toString(), new Object[] { userId, boolToChar(false) }, new UserEventInstanceRowMapper());
        attachRelationalInfo(results);
        return results;
    }
    
    public List<EventInstance> getPendingEvents(int userId) {
        List<EventInstance> results = Common.databaseProxy.doLimitQuery(this, EVENT_SELECT_WITH_USER_DATA
                + "where ue.userId=? and e.ackTs is null order by e.activeTs desc", new Object[] { userId },
                new UserEventInstanceRowMapper(), MAX_PENDING_EVENTS);
        attachRelationalInfo(results);
        return results;
    }

    private EventInstance getEventInstance(int eventId) {
        return queryForObject(BASIC_EVENT_SELECT + "where e.id=?", new Object[] { eventId },
                new EventInstanceRowMapper());
    }

    public static class EventInstanceRowMapper implements RowMapper<EventInstance> {
        @Override
        public EventInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventType type = createEventType(rs, 2);
            EventInstance event = new EventInstance(type, rs.getLong(6), charToBool(rs.getString(7)), rs.getInt(10),
                    BaseDao.readTranslatableMessage(rs, 11), null);
            event.setId(rs.getInt(1));
            long rtnTs = rs.getLong(8);
            if (!rs.wasNull())
                event.returnToNormal(rtnTs, rs.getInt(9));
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

    class UserEventInstanceRowMapper extends EventInstanceRowMapper {
        @Override
        public EventInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventInstance event = super.mapRow(rs, rowNum);
            event.setSilenced(charToBool(rs.getString(17)));
            if (!rs.wasNull())
                event.setUserNotified(true);
            return event;
        }
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
            type = new AuditEventType(subtypeName, rs.getInt(offset + 2));
        else {
            EventTypeDefinition def = ModuleRegistry.getEventTypeDefinition(typeName);
            if (def == null)
                throw new ShouldNeverHappenException("Unknown event type: " + typeName + ", are you missing a module?");
            type = def.createEventType(subtypeName, rs.getInt(offset + 2), rs.getInt(offset + 3));
            if (type == null)
                throw new ShouldNeverHappenException("Unknown event type: " + typeName + ", are you missing a module?");
        }
        return type;
    }

    private void attachRelationalInfo(List<EventInstance> list) {
        for (EventInstance e : list)
            attachRelationalInfo(e);
    }

    private static final String EVENT_COMMENT_SELECT = UserCommentRowMapper.USER_COMMENT_SELECT //
            + "where uc.commentType= " + UserComment.TYPE_EVENT //
            + " and uc.typeKey=? " //
            + "order by uc.ts";

    void attachRelationalInfo(EventInstance event) {
        if (event.isHasComments())
            event.setEventComments(query(EVENT_COMMENT_SELECT, new Object[] { event.getId() },
                    new UserCommentRowMapper()));
    }

    public EventInstance insertEventComment(int eventId, UserComment comment) {
        new UserDao().insertUserComment(UserComment.TYPE_EVENT, eventId, comment);
        return getEventInstance(eventId);
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
            	int count = ejt2.queryForInt("select count (*) from events", new Object[0], 0);
                ejt2.update("delete from userevents");
                ejt2.update("delete from events");
                ejt2.update("delete from userComments where commentType=" + UserComment.TYPE_EVENT
                        + "  and typeKey not in (select id from events)");
                return count;
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
    public int purgeEventsBefore(final long time, final int alarmLevel) {
        // Find a list of event ids with no remaining acknowledgments pending.
        final ExtendedJdbcTemplate ejt2 = ejt;
        int count = getTransactionTemplate().execute(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus status) {
            	
                int count = ejt2.update("delete from events where activeTs<? and alarmLevel=?", new Object[] { time, alarmLevel});

                // Delete orphaned user comments.
                ejt2.update("delete from userComments where commentType=" + UserComment.TYPE_EVENT
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
                ejt2.update("delete from userComments where commentType=" + UserComment.TYPE_EVENT
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
                ejt2.update("delete from userComments where commentType=" + UserComment.TYPE_EVENT
                        + "  and typeKey not in (select id from events)");

                return count;
            }
        });


        return count;
    }

    public int getEventCount() {
        return ejt.queryForInt("select count(*) from events");
    }

    public List<EventInstance> search(int eventId, String eventType, String status, int alarmLevel,
            final String[] keywords, long dateFrom, long dateTo, int userId, final Translations translations,
            final int from, final int to, final Date date) {
        List<String> where = new ArrayList<String>();
        List<Object> params = new ArrayList<Object>();

        StringBuilder sql = new StringBuilder();
        sql.append(EVENT_SELECT_WITH_USER_DATA);
        sql.append("where ue.userId=?");
        params.add(userId);

        if (eventId != 0) {
            where.add("e.id=?");
            params.add(eventId);
        }

        if (!StringUtils.isBlank(eventType)) {
            where.add("e.typeName=?");
            params.add(eventType);
        }

        if (EventsDwr.STATUS_ACTIVE.equals(status)) {
            where.add("e.rtnApplicable=? and e.rtnTs is null");
            params.add(boolToChar(true));
        }
        else if (EventsDwr.STATUS_RTN.equals(status)) {
            where.add("e.rtnApplicable=? and e.rtnTs is not null");
            params.add(boolToChar(true));
        }
        else if (EventsDwr.STATUS_NORTN.equals(status)) {
            where.add("e.rtnApplicable=?");
            params.add(boolToChar(false));
        }

        if (alarmLevel != -1) {
            where.add("e.alarmLevel=?");
            params.add(alarmLevel);
        }

        if (dateFrom != -1) {
            where.add("activeTs>=?");
            params.add(dateFrom);
        }

        if (dateTo != -1) {
            where.add("activeTs<?");
            params.add(dateTo);
        }

        if (!where.isEmpty()) {
            for (String s : where) {
                sql.append(" and ");
                sql.append(s);
            }
        }
        sql.append(" order by e.activeTs desc");

        final List<EventInstance> results = new ArrayList<EventInstance>();
        final UserEventInstanceRowMapper rowMapper = new UserEventInstanceRowMapper();

        final int[] data = new int[2];

        ejt.query(sql.toString(), params.toArray(), new ResultSetExtractor<Object>() {
            @Override
            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                int row = 0;
                long dateTs = date == null ? -1 : date.getTime();
                int startRow = -1;

                while (rs.next()) {
                    EventInstance e = rowMapper.mapRow(rs, 0);
                    attachRelationalInfo(e);
                    boolean add = true;

                    if (keywords != null) {
                        // Do the text search. If the instance has a match, put it in the result. Otherwise ignore.
                        StringBuilder text = new StringBuilder();
                        text.append(e.getMessage().translate(translations));
                        if (e.isHasComments()) {
                            for (UserComment comment : e.getEventComments())
                                text.append(' ').append(comment.getComment());
                        }

                        String[] values = text.toString().split("\\s+");

                        for (String keyword : keywords) {
                            if (keyword.startsWith("-")) {
                                if (com.serotonin.util.StringUtils.globWhiteListMatchIgnoreCase(values,
                                        keyword.substring(1))) {
                                    add = false;
                                    break;
                                }
                            }
                            else {
                                if (!com.serotonin.util.StringUtils.globWhiteListMatchIgnoreCase(values, keyword)) {
                                    add = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (add) {
                        if (date != null) {
                            if (e.getActiveTimestamp() <= dateTs && results.size() < to - from) {
                                if (startRow == -1)
                                    startRow = row;
                                results.add(e);
                            }
                        }
                        else if (row >= from && row < to)
                            results.add(e);

                        row++;
                    }
                }

                data[0] = row;
                data[1] = startRow;

                return null;
            }
        });

        searchRowCount = data[0];
        startRow = data[1];

        return results;
    }

    private int searchRowCount;
    private int startRow;

    public int getSearchRowCount() {
        return searchRowCount;
    }

    public int getStartRow() {
        return startRow;
    }

    //
    //
    // Event handlers
    //
    public String generateUniqueXid() {
        return generateUniqueXid(EventHandlerVO.XID_PREFIX, "eventHandlers");
    }

    public boolean isXidUnique(String xid, int excludeId) {
        return isXidUnique(xid, excludeId, "eventHandlers");
    }

    public EventType getEventHandlerType(int handlerId) {
        return queryForObject(
                "select eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2 from eventHandlers where id=?",
                new Object[] { handlerId }, new RowMapper<EventType>() {
                    @Override
                    public EventType mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return createEventType(rs, 1);
                    }
                });
    }

    private static final String EVENT_HANDLER_SELECT = "select id, xid, alias, data from eventHandlers ";

    public List<EventHandlerVO> getEventHandlers(EventType type) {
        return getEventHandlers(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(),
                type.getReferenceId2());
    }

    public List<EventHandlerVO> getEventHandlers(EventTypeVO type) {
        return getEventHandlers(type.getType(), type.getSubtype(), type.getTypeRef1(), type.getTypeRef2());
    }

    public List<EventHandlerVO> getEventHandlers() {
        return query(EVENT_HANDLER_SELECT, new EventHandlerRowMapper());
    }

    /**
     * Note: eventHandlers.eventTypeRef[1,2] match on both the given ref and 0. This is to allow a single set of event
     * handlers to be defined for user login events, rather than have to individually define them for each user.
     */
    private final static String EVENT_HANDLER_SELECT_SUB = EVENT_HANDLER_SELECT //
            + "where eventTypeName=? and eventSubtypeName=? " //
            + "  and (eventTypeRef1=? or eventTypeRef1=0)" //
            + "  and (eventTypeRef2=? or eventTypeRef2=0)";

    private final static String EVENT_HANDLER_SELECT_NULLSUB = EVENT_HANDLER_SELECT //
            + "where eventTypeName=? and eventSubtypeName is null " //
            + "  and (eventTypeRef1=? or eventTypeRef1=0)" //
            + "  and (eventTypeRef2=? or eventTypeRef2=0)";

    private List<EventHandlerVO> getEventHandlers(String typeName, String subtypeName, int ref1, int ref2) {
        if (subtypeName == null)
            return query(EVENT_HANDLER_SELECT_NULLSUB, new Object[] { typeName, ref1, ref2 },
                    new EventHandlerRowMapper());
        return query(EVENT_HANDLER_SELECT_SUB, new Object[] { typeName, subtypeName, ref1, ref2 },
                new EventHandlerRowMapper());
    }

    public EventHandlerVO getEventHandler(int eventHandlerId) {
        return queryForObject(EVENT_HANDLER_SELECT + "where id=?", new Object[] { eventHandlerId },
                new EventHandlerRowMapper());
    }

    public EventHandlerVO getEventHandler(String xid) {
        return queryForObject(EVENT_HANDLER_SELECT + "where xid=?", new Object[] { xid }, new EventHandlerRowMapper(),
                null);
    }

    class EventHandlerRowMapper implements RowMapper<EventHandlerVO> {
        @Override
        public EventHandlerVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            EventHandlerVO h = (EventHandlerVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(4));
            h.setId(rs.getInt(1));
            h.setXid(rs.getString(2));
            h.setAlias(rs.getString(3));
            return h;
        }
    }

    public EventHandlerVO saveEventHandler(final EventType type, final EventHandlerVO handler) {
        if (type == null)
            return saveEventHandler("?", null, 0, 0, handler);
        return saveEventHandler(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(),
                type.getReferenceId2(), handler);
    }

    public EventHandlerVO saveEventHandler(final EventTypeVO type, final EventHandlerVO handler) {
        if (type == null)
            return saveEventHandler("?", null, 0, 0, handler);
        return saveEventHandler(type.getType(), type.getSubtype(), type.getTypeRef1(), type.getTypeRef2(), handler);
    }

    private EventHandlerVO saveEventHandler(final String typeName, final String subtypeName, final int typeRef1,
            final int typeRef2, final EventHandlerVO handler) {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                if (handler.getId() == Common.NEW_ID)
                    insertEventHandler(typeName, subtypeName, typeRef1, typeRef2, handler);
                else
                    updateEventHandler(handler);
            }
        });
        return getEventHandler(handler.getId());
    }

    void insertEventHandler(String typeName, String subtypeName, int typeRef1, int typeRef2, EventHandlerVO handler) {
        handler.setId(doInsert("insert into eventHandlers " //
                + "  (xid, alias, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2, data) " //
                + "values (?,?,?,?,?,?,?)", new Object[] { handler.getXid(), handler.getAlias(), typeName, subtypeName,
                typeRef1, typeRef2, SerializationHelper.writeObject(handler) }, new int[] { Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BINARY }));
        AuditEventType.raiseAddedEvent(AuditEventType.TYPE_EVENT_HANDLER, handler);
    }

    void updateEventHandler(EventHandlerVO handler) {
        EventHandlerVO old = getEventHandler(handler.getId());
        ejt.update("update eventHandlers set xid=?, alias=?, data=? where id=?", new Object[] { handler.getXid(),
                handler.getAlias(), SerializationHelper.writeObject(handler), handler.getId() }, new int[] {
                Types.VARCHAR, Types.VARCHAR, Types.BINARY, Types.INTEGER });
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_EVENT_HANDLER, old, handler);
    }

    public void deleteEventHandler(final int handlerId) {
        EventHandlerVO handler = getEventHandler(handlerId);
        ejt.update("delete from eventHandlers where id=?", new Object[] { handlerId });
        AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_EVENT_HANDLER, handler);
    }

    //
    //
    // User alarms
    //
    private static final String SILENCED_SELECT = "select ue.silenced " //
            + "from events e " //
            + "  join userEvents ue on e.id=ue.eventId " //
            + "where e.id=? " //
            + "  and ue.userId=? " //
            + "  and e.ackTs is null";

    public boolean toggleSilence(int eventId, int userId) {
        String result = ejt.queryForObject(SILENCED_SELECT, new Object[] { eventId, userId }, String.class, null);
        if (result == null)
            return true;

        boolean silenced = !charToBool(result);
        ejt.update("update userEvents set silenced=? where eventId=? and userId=?", new Object[] {
                boolToChar(silenced), eventId, userId });
        return silenced;
    }

    public int getHighestUnsilencedAlarmLevel(int userId) {
        return ejt.queryForInt("select max(e.alarmLevel) from userEvents u " + "  join events e on u.eventId=e.id "
                + "where u.silenced=? and u.userId=?", new Object[] { boolToChar(false), userId });
    }



}
