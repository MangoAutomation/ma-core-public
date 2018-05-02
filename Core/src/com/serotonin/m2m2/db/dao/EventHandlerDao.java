/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.infiniteautomation.mango.db.query.JoinClause;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
public class EventHandlerDao extends AbstractDao<AbstractEventHandlerVO<?>>{

	public static final EventHandlerDao instance = new EventHandlerDao();
	
	private static final boolean H2_SYNTAX;
	private static final boolean MYSQL_SYNTAX;
	static {
	    if(Common.databaseProxy.getType() == DatabaseProxy.DatabaseType.H2) {
	        H2_SYNTAX = true;
	        MYSQL_SYNTAX = false;
	    } else if(Common.databaseProxy.getType() == DatabaseProxy.DatabaseType.MYSQL) {
	        H2_SYNTAX = false;
            MYSQL_SYNTAX = true;
	    } else {
	        H2_SYNTAX = false;
            MYSQL_SYNTAX = false;
	    }
	}
	
	protected EventHandlerDao() {
		super(ModuleRegistry.getWebSocketHandlerDefinition("EVENT_HANDLER"), AuditEventType.TYPE_EVENT_HANDLER, "eh", new String[0], false, new TranslatableMessage("internal.monitor.EVENT_HANDLER_COUNT"));
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
		return new Object[]{
				vo.getXid(),
				vo.getAlias(),
				vo.getDefinition().getEventHandlerTypeName(),
				SerializationHelper.writeObject(vo)
		};
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
		map.put("data", Types.BLOB);
		return map;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getPropertiesMap()
	 */
	@Override
	protected Map<String, IntStringPair> getPropertiesMap() {
	    HashMap<String, IntStringPair> map = new HashMap<String, IntStringPair>();
        map.put("eventTypeName", new IntStringPair(Types.VARCHAR, "ehm.eventTypeName"));
        map.put("eventSubtypeName", new IntStringPair(Types.VARCHAR, "ehm.eventSubtypeName"));
        map.put("eventTypeRef1", new IntStringPair(Types.INTEGER, "ehm.eventTypeRef1"));
        map.put("eventTypeRef2", new IntStringPair(Types.INTEGER, "ehm.eventTypeRef2"));
        return map;
	}
	
	@Override
    protected List<JoinClause> getJoins() {
        List<JoinClause> joins = new ArrayList<JoinClause>();
        //This causes multiple rows per ID if there are > 1 detector to a handler
        //joins.add(new JoinClause(LEFT_JOIN, SchemaDefinition.EVENT_HANDLER_MAPPING_TABLE, "ehm", "ehm.eventHandlerId=eh.id"));
        return joins;
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#getRowMapper()
	 */
	@Override
	public RowMapper<AbstractEventHandlerVO<?>> getRowMapper() {
		return new EventHandlerRowMapper();
	}
	
    private static final String EVENT_HANDLER_SELECT = "SELECT id, xid, alias, eventHandlerType, data FROM eventHandlers ";
    /**
     * Note: eventHandlers.eventTypeRef[1,2] match on both the given ref and 0. This is to allow a single set of event
     * handlers to be defined for user login events, rather than have to individually define them for each user.
     */
    private static final String EVENT_HANDLER_SELECT_BY_TYPE_SUB = "SELECT eh.id, eh.xid, eh.alias, eh.eventHandlerType, " +
            "eh.data FROM eventHandlersMapping ehm INNER JOIN eventHandlers eh ON eh.id=ehm.eventHandlerId WHERE ehm.eventTypeName=? AND " +
            "ehm.eventSubtypeName=? AND (ehm.eventTypeRef1=? OR ehm.eventTypeRef1=0) AND (ehm.eventTypeRef2=? or ehm.eventTypeRef2=0)";
    private static final String EVENT_HANDLER_SELECT_BY_TYPE_NULLSUB = "SELECT eh.id, eh.xid, eh.alias, eh.eventHandlerType, eh.data " +
            "FROM eventHandlersMapping ehm INNER JOIN eventHandlers eh ON eh.id=ehm.eventHandlerId WHERE ehm.eventTypeName=? AND " +
            "ehm.eventSubtypeName='' AND (ehm.eventTypeRef1=? OR ehm.eventTypeRef1=0) AND (ehm.eventTypeRef2=? OR ehm.eventTypeRef2=0)";

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
    
    public List<EventType> getEventTypesForHandler(int handlerId) {
        return ejt.query("SELECT eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2 FROM eventHandlersMapping WHERE eventHandlerid="+handlerId, new RowMapper<EventType>() {
            @Override
            public EventType mapRow(ResultSet rs, int rowNum) throws SQLException {
                return EventDao.createEventType(rs, 1);
            }
        });
    }
    
    private static final String EVENT_HANDLER_XID_SELECT_SUB = "SELECT eh.xid FROM eventHandlersMapping ehm INNER JOIN eventHandlers eh ON eh.id=ehm.eventHandlerId WHERE " +
            "ehm.eventTypeName=? AND ehm.eventSubtypeName=? AND (ehm.eventTypeRef1=? OR ehm.eventTypeRef1=0) AND (ehm.eventTypeRef2=? OR ehm.eventTypeRef2=0)";
    private static final String EVENT_HANDLER_XID_SELECT_NULLSUB = "select eh.xid FROM eventHandlersMapping ehm INNER JOIN eventHandlers eh ON eh.id=ehm.eventHandlerId WHERE " +
            "ehm.eventTypeName=? AND ehm.eventSubtypeName='' AND (ehm.eventTypeRef1=? OR ehm.eventTypeRef1=0) AND (ehm.eventTypeRef2=? OR ehm.eventTypeRef2=0)";
    public List<String> getEventHandlerXids(EventTypeVO type) {
        if(type.getSubtype() == null)
            return queryForList(EVENT_HANDLER_XID_SELECT_NULLSUB, new Object[] { type.getType(), 
                    type.getTypeRef1(), type.getTypeRef2()}, String.class);
        return queryForList(EVENT_HANDLER_XID_SELECT_SUB, new Object[] { type.getType(), type.getSubtype(),
                type.getTypeRef1(), type.getTypeRef2()}, String.class);
    }

    private List<AbstractEventHandlerVO<?>> getEventHandlers(String typeName, String subtypeName, int ref1, int ref2) {
        if (subtypeName == null)
            return query(EVENT_HANDLER_SELECT_BY_TYPE_NULLSUB, new Object[] { typeName, ref1, ref2 },
                    new EventHandlerRowMapper());
        return query(EVENT_HANDLER_SELECT_BY_TYPE_SUB, new Object[] { typeName, subtypeName, ref1, ref2 },
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

    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#saveRelationalData(com.serotonin.m2m2.vo.AbstractBasicVO, boolean)
     */
    @Override
    public void saveRelationalData(AbstractEventHandlerVO<?> vo, boolean insert) {
        if(vo.getAddedEventTypes() != null) {
            for(EventType type : vo.getAddedEventTypes())
                addEventHandlerMappingIfMissing(vo.getId(), type);
        }
    }
    
    /* (non-Javadoc)
     * @see com.serotonin.m2m2.db.dao.AbstractBasicDao#delete(com.serotonin.m2m2.vo.AbstractBasicVO)
     */
    @Override
    public void delete(AbstractEventHandlerVO<?> vo, String initiatorId) {
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                deleteEventHandlerMappings(vo.getId());
                EventHandlerDao.super.delete(vo, initiatorId);
            }
        });
    }

    public void addEventHandlerMappingIfMissing(int handlerId, EventType type) {
        if(H2_SYNTAX) {
            if(type.getEventSubtype() == null)
                ejt.update("MERGE INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventTypeRef1, eventTypeRef2) KEY (eventHandlerId, eventTypeName, eventTypeRef1, eventTypeRef2) VALUES (?, ?, ?, ?)", 
                        new Object[] {handlerId, type.getEventType(), type.getReferenceId1(), type.getReferenceId2()}, 
                        new int[] {Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
            else
                ejt.update("MERGE INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) KEY (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) VALUES (?, ?, ?, ?, ?)", 
                    new Object[] {handlerId, type.getEventType(), type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()}, 
                    new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
        }
        else if(MYSQL_SYNTAX) {
            if(type.getEventSubtype() == null)
                ejt.update("REPLACE INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventTypeRef1, eventTypeRef2) values (?, ?, ?, ?)", 
                        new Object[] {handlerId, type.getEventType(), type.getReferenceId1(), type.getReferenceId2()}, 
                        new int[] {Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
            else
                ejt.update("REPLACE INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) values (?, ?, ?, ?, ?)", 
                    new Object[] {handlerId, type.getEventType(), type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()}, 
                    new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
        }
        else {
            getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                    deleteEventHandlerMapping(handlerId, type);
                    ejt.doInsert("INSERT INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) values (?, ?, ?, ?, ?)", 
                            new Object[] {handlerId, type.getEventType(), type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()}, 
                            new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
                }
            });
        }
    }
    
	private void deleteEventHandlerMappings(int eventHandlerId) {
	    ejt.update("DELETE FROM eventHandlersMapping WHERE eventHandlerId=?", new Object[] {eventHandlerId},
                new int[] {Types.INTEGER});
	}
	
    public void deleteEventHandlerMapping(int eventHandlerId, EventType type) {
        if(type.getEventSubtype() != null)
            ejt.update("DELETE FROM eventHandlersMapping WHERE eventHandlerId=? AND eventTypeName=? AND eventSubtypeName='' AND eventTypeRef1=? AND eventTypeRef2=?",
                    new Object[] {eventHandlerId, type.getEventType(), type.getReferenceId1(), type.getReferenceId2()},
                    new int[] {Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
        else
            ejt.update("DELETE FROM eventHandlersMapping WHERE eventHandlerId=? AND eventTypeName=? AND eventSubtypeName=? AND eventTypeRef1=? AND eventTypeRef2=?",
                new Object[] {eventHandlerId, type.getEventType(), type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()},
                new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
    }
}
