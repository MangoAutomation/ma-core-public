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
import java.util.Objects;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.infiniteautomation.mango.db.query.JoinClause;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.db.pair.IntStringPair;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class EventHandlerDao<T extends AbstractEventHandlerVO<?>> extends AbstractDao<T>{

    @SuppressWarnings("unchecked")
    private static final LazyInitSupplier<EventHandlerDao<AbstractEventHandlerVO<?>>> springInstance = new LazyInitSupplier<>(() -> {
        Object o = Common.getRuntimeContext().getBean(EventHandlerDao.class);
        if(o == null)
            throw new ShouldNeverHappenException("DAO not initialized in Spring Runtime Context");
        return (EventHandlerDao<AbstractEventHandlerVO<?>>)o;
    });

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

    private EventHandlerDao() {
        super(AuditEventType.TYPE_EVENT_HANDLER, "eh", new String[0], false, new TranslatableMessage("internal.monitor.EVENT_HANDLER_COUNT"));
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static EventHandlerDao<AbstractEventHandlerVO<?>> getInstance() {
        return springInstance.get();
    }

    @Override
    protected String getXidPrefix() {
        return AbstractEventHandlerVO.XID_PREFIX;
    }

    @Override
    protected String getTableName() {
        return SchemaDefinition.EVENT_HANDLER_TABLE;
    }

    @Override
    protected Object[] voToObjectArray(T vo) {
        return new Object[]{
                vo.getXid(),
                vo.getName(),
                vo.getDefinition().getEventHandlerTypeName(),
                SerializationHelper.writeObject(vo)
        };
    }

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

    @Override
    public RowMapper<T> getRowMapper() {
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

    public List<T> getEventHandlers(EventType type) {
        return getEventHandlers(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(),
                type.getReferenceId2());
    }

    public List<T> getEventHandlersByType(String typeName) {
        return query(EVENT_HANDLER_SELECT + " WHERE eventHandlerType=?", new Object[] {typeName}, new EventHandlerRowMapper());
    }

    public List<T> getEventHandlers() {
        return query(EVENT_HANDLER_SELECT, new EventHandlerRowMapper());
    }

    public List<EventType> getEventTypesForHandler(int handlerId) {
        return ejt.query("SELECT eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2 FROM eventHandlersMapping WHERE eventHandlerid=?", new Object[] {handlerId}, new RowMapper<EventType>() {
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

    public List<String> getEventHandlerXids(EventType type) {
        if(type.getEventSubtype() == null)
            return queryForList(EVENT_HANDLER_XID_SELECT_NULLSUB, new Object[] { type.getEventType(),
                    type.getReferenceId1(), type.getReferenceId2()}, String.class);
        return queryForList(EVENT_HANDLER_XID_SELECT_SUB, new Object[] { type.getEventType(), type.getEventSubtype(),
                type.getReferenceId1(), type.getReferenceId2()}, String.class);
    }

    //    public List<String> getEventHandlerXids(EventTypeVO type) {
    //        return this.getEventHandlerXids(type.getEventType());
    //    }

    private List<T> getEventHandlers(String typeName, String subtypeName, int ref1, int ref2) {
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

    public T getEventHandler(String xid) {
        return queryForObject(EVENT_HANDLER_SELECT + "where xid=?", new Object[] { xid }, new EventHandlerRowMapper(),
                null);
    }
    
    

    class EventHandlerRowMapper implements RowMapper<T> {
        @Override
        public T mapRow(ResultSet rs, int rowNum) throws SQLException {
            @SuppressWarnings("unchecked")
            T h = (T) SerializationHelper.readObjectInContext(rs.getBinaryStream(5));
            h.setId(rs.getInt(1));
            h.setXid(rs.getString(2));
            h.setAlias(rs.getString(3));
            h.setDefinition(ModuleRegistry.getEventHandlerDefinition(rs.getString(4)));
            return h;
        }
    }

    @Override
    public void saveRelationalData(T vo, boolean insert) {
        if (insert) {
            if(vo.getEventTypes() != null) {
                for (EventType type : vo.getEventTypes()) {
                    ejt.doInsert(
                            "INSERT INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) values (?, ?, ?, ?, ?)",
                            new Object[] {vo.getId(), type.getEventType(), type.getEventSubtype() != null ? type.getEventSubtype() : "",
                                    type.getReferenceId1(), type.getReferenceId2()},
                            new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                                    Types.INTEGER});
                }
            }
            vo.getDefinition().saveRelationalData(vo, insert);
        } else {
            // Replace all mappings
            deleteEventHandlerMappings(vo.getId());
            if(vo.getEventTypes() != null) {
                for (EventType type : vo.getEventTypes()) {
                    ejt.doInsert(
                            "INSERT INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) values (?, ?, ?, ?, ?)",
                            new Object[] {vo.getId(), type.getEventType(), type.getEventSubtype() != null ? type.getEventSubtype() : "",
                                    type.getReferenceId1(), type.getReferenceId2()},
                            new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                                    Types.INTEGER});
    
                }
            }
            vo.getDefinition().saveRelationalData(vo, insert);
        }
    }
    
    @Override
    public void loadRelationalData(T vo) {
        vo.getDefinition().loadRelationalData(vo);
    }

    @Override
    public void delete(T vo, String initiatorId) {
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
                            new Object[] {handlerId, type.getEventType(), type.getEventSubtype() == null ? "" : type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()},
                            new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
                }
            });
        }
    }

    private void deleteEventHandlerMappings(int eventHandlerId) {
        ejt.update("DELETE FROM eventHandlersMapping WHERE eventHandlerId=?", new Object[] {eventHandlerId},
                new int[] {Types.INTEGER});
    }

    /**
     * Add a mapping for an existing event handler for an event type and if it already exists replace it
     * @param eventHandlerXid
     * @param type
     */
    public void saveEventHandlerMapping(String eventHandlerXid, EventType type) {
        Integer id = getIdByXid(eventHandlerXid);
        Objects.requireNonNull(id, "Event Handler with xid: " + eventHandlerXid + " does not exist, can't create mapping.");
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                    deleteEventHandlerMapping(id, type);
                    ejt.doInsert(
                            "INSERT INTO eventHandlersMapping (eventHandlerId, eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2) values (?, ?, ?, ?, ?)",
                            new Object[] {id, type.getEventType(), type.getEventSubtype() != null ? type.getEventSubtype() : "",
                                    type.getReferenceId1(), type.getReferenceId2()},
                            new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                                    Types.INTEGER});
                }
        });
    }
    
    /**
     * Delete all mappings for this event type
     * @param type
     */
    public void deleteEventHandlerMappings(EventType type) {
        if(type.getEventSubtype() == null)
            ejt.update("DELETE FROM eventHandlersMapping WHERE eventTypeName=? AND eventSubtypeName='' AND eventTypeRef1=? AND eventTypeRef2=?",
                    new Object[] {type.getEventType(), type.getReferenceId1(), type.getReferenceId2()},
                    new int[] {Types.VARCHAR, Types.INTEGER, Types.INTEGER});
        else
            ejt.update("DELETE FROM eventHandlersMapping WHERE AND eventTypeName=? AND eventSubtypeName=? AND eventTypeRef1=? AND eventTypeRef2=?",
                    new Object[] {type.getEventType(), type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()},
                    new int[] {Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER});

    }
    
    public void deleteEventHandlerMapping(int eventHandlerId, EventType type) {
        if(type.getEventSubtype() == null)
            ejt.update("DELETE FROM eventHandlersMapping WHERE eventHandlerId=? AND eventTypeName=? AND eventSubtypeName='' AND eventTypeRef1=? AND eventTypeRef2=?",
                    new Object[] {eventHandlerId, type.getEventType(), type.getReferenceId1(), type.getReferenceId2()},
                    new int[] {Types.INTEGER, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
        else
            ejt.update("DELETE FROM eventHandlersMapping WHERE eventHandlerId=? AND eventTypeName=? AND eventSubtypeName=? AND eventTypeRef1=? AND eventTypeRef2=?",
                    new Object[] {eventHandlerId, type.getEventType(), type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()},
                    new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER});
    }
}
