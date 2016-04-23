/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.web.mvc.spring.MangoWebSocketConfiguration;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
public class EventHandlerDao extends AbstractDao<AbstractEventHandlerVO<?>>{

	public static final EventHandlerDao instance = new EventHandlerDao();
	
	/**
	 * @param handler
	 * @param typeName
	 */
	protected EventHandlerDao() {
		super(MangoWebSocketConfiguration.eventHandlerHandler, AuditEventType.TYPE_EVENT_HANDLER);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getXidPrefix()
	 */
	@Override
	protected String getXidPrefix() {
		return AbstractEventHandlerVO.XID_PREFIX;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractDao#getNewVo()
	 */
	@Override
	public AbstractEventHandlerVO<?> getNewVo() {
		throw new ShouldNeverHappenException("Not Supported");
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getTableName()
	 */
	@Override
	protected String getTableName() {
		return SchemaDefinition.EVENT_HANDLER_TABLE;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#voToObjectArray(com.serotonin.m2m2.vo.AbstractBasicVO)
	 */
	@Override
	protected Object[] voToObjectArray(AbstractEventHandlerVO<?> vo) {
		throw new ShouldNeverHappenException("Not yet supported, use different save method.");
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertyTypeMap()
	 */
	@Override
	protected LinkedHashMap<String, Integer> getPropertyTypeMap() {
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
		map.put("id", Types.INTEGER);
		map.put("xid", Types.VARCHAR);
		map.put("alias", Types.VARCHAR);
		map.put("eventHandlerType", Types.VARCHAR);
		map.put("eventTypeName", Types.VARCHAR);
		map.put("eventSubtypeName", Types.VARCHAR);
		map.put("eventTypeRef1", Types.INTEGER);
		map.put("eventTypeRef2", Types.INTEGER);
		map.put("data", Types.BLOB);
		return map;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<AbstractEventHandlerVO<?>> getRowMapper() {
		return new EventHandlerRowMapper();
	}

    //
    //
    // Event handlers
    //

    public EventType getEventHandlerType(int handlerId) {
        return queryForObject(
                "select eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2 from eventHandlers where id=?",
                new Object[] { handlerId }, new RowMapper<EventType>() {
                    @Override
                    public EventType mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return EventDao.createEventType(rs, 1);
                    }
                });
    }

    private static final String EVENT_HANDLER_SELECT = "select id, xid, alias, eventHandlerType, data from eventHandlers ";

    public List<AbstractEventHandlerVO<?>> getEventHandlers(EventType type) {
        return getEventHandlers(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(),
                type.getReferenceId2());
    }

    public List<AbstractEventHandlerVO<?>> getEventHandlers(EventTypeVO type) {
        return getEventHandlers(type.getType(), type.getSubtype(), type.getTypeRef1(), type.getTypeRef2());
    }

    public List<AbstractEventHandlerVO<?>> getEventHandlers() {
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

    private List<AbstractEventHandlerVO<?>> getEventHandlers(String typeName, String subtypeName, int ref1, int ref2) {
        if (subtypeName == null)
            return query(EVENT_HANDLER_SELECT_NULLSUB, new Object[] { typeName, ref1, ref2 },
                    new EventHandlerRowMapper());
        return query(EVENT_HANDLER_SELECT_SUB, new Object[] { typeName, subtypeName, ref1, ref2 },
                new EventHandlerRowMapper());
    }

    public AbstractEventHandlerVO<?> getEventHandler(int eventHandlerId) {
        return queryForObject(EVENT_HANDLER_SELECT + "where id=?", new Object[] { eventHandlerId },
                new EventHandlerRowMapper());
    }

    public AbstractEventHandlerVO<?> getEventHandler(String xid) {
        return queryForObject(EVENT_HANDLER_SELECT + "where xid=?", new Object[] { xid }, new EventHandlerRowMapper(),
                null);
    }

    class EventHandlerRowMapper implements RowMapper<AbstractEventHandlerVO<?>> {
        @Override
        public AbstractEventHandlerVO<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
        	AbstractEventHandlerVO<?> h = (AbstractEventHandlerVO<?>) SerializationHelper.readObjectInContext(rs.getBinaryStream(5));
            h.setId(rs.getInt(1));
            h.setXid(rs.getString(2));
            h.setAlias(rs.getString(3));
            h.setDefinition(ModuleRegistry.getEventHandlerDefinition(rs.getString(4)));
            return h;
        }
    }

    public AbstractEventHandlerVO<?> saveEventHandler(final EventType type, final AbstractEventHandlerVO<?> handler) {
        if (type == null)
            return saveEventHandler("?", null, 0, 0, handler);
        return saveEventHandler(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(),
                type.getReferenceId2(), handler);
    }

    public AbstractEventHandlerVO<?> saveEventHandler(final EventTypeVO type, final AbstractEventHandlerVO<?> handler) {
        if (type == null)
            return saveEventHandler("?", null, 0, 0, handler);
        return saveEventHandler(type.getType(), type.getSubtype(), type.getTypeRef1(), type.getTypeRef2(), handler);
    }

    private AbstractEventHandlerVO<?> saveEventHandler(final String typeName, final String subtypeName, final int typeRef1,
            final int typeRef2, final AbstractEventHandlerVO<?> handler) {
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

    void insertEventHandler(String typeName, String subtypeName, int typeRef1, int typeRef2, AbstractEventHandlerVO<?> handler) {
        handler.setId(doInsert("insert into eventHandlers " //
                + "  (xid, alias, eventHandlerType, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2, data) " //
                + "values (?,?,?,?,?,?,?,?)", new Object[] { handler.getXid(), handler.getAlias(), handler.getDefinition().getEventHandlerTypeName(), typeName, subtypeName,
                typeRef1, typeRef2, SerializationHelper.writeObject(handler) }, new int[] { Types.VARCHAR,
                Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.BINARY }));
        AuditEventType.raiseAddedEvent(AuditEventType.TYPE_EVENT_HANDLER, handler);
    }

    void updateEventHandler(AbstractEventHandlerVO<?> handler) {
    	AbstractEventHandlerVO<?> old = getEventHandler(handler.getId());
        ejt.update("update eventHandlers set xid=?, alias=?, data=? where id=?", new Object[] { handler.getXid(),
                handler.getAlias(), SerializationHelper.writeObject(handler), handler.getId() }, new int[] {
                Types.VARCHAR, Types.VARCHAR, Types.BINARY, Types.INTEGER });
        AuditEventType.raiseChangedEvent(AuditEventType.TYPE_EVENT_HANDLER, old, handler);
    }

    public void deleteEventHandler(final int handlerId) {
    	AbstractEventHandlerVO<?> handler = getEventHandler(handlerId);
        ejt.update("delete from eventHandlers where id=?", new Object[] { handlerId });
        AuditEventType.raiseDeletedEvent(AuditEventType.TYPE_EVENT_HANDLER, handler);
    }
	
}
