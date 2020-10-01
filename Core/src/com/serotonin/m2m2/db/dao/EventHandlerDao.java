/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.db.EventHandlerTableDefinition;
import com.infiniteautomation.mango.spring.db.RoleTableDefinition;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.DatabaseProxy;
import com.serotonin.m2m2.db.dao.tables.MintermMappingTable;
import com.serotonin.m2m2.db.dao.tables.PermissionMappingTable;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
@Repository()
public class EventHandlerDao extends AbstractVoDao<AbstractEventHandlerVO, EventHandlerTableDefinition>{

    private static final LazyInitSupplier<EventHandlerDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(EventHandlerDao.class);
    });

    private static final boolean H2_SYNTAX;
    private static final boolean MYSQL_SYNTAX;

    private final PermissionService permissionService;

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

    @Autowired
    private EventHandlerDao(EventHandlerTableDefinition table,
            PermissionService permissionService,
            @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(AuditEventType.TYPE_EVENT_HANDLER,
                table,
                new TranslatableMessage("internal.monitor.EVENT_HANDLER_COUNT"),
                mapper, publisher);
        this.permissionService = permissionService;
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static EventHandlerDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected String getXidPrefix() {
        return AbstractEventHandlerVO.XID_PREFIX;
    }

    @Override
    protected Object[] voToObjectArray(AbstractEventHandlerVO vo) {
        return new Object[]{
                vo.getXid(),
                vo.getName(),
                vo.getDefinition().getEventHandlerTypeName(),
                SerializationHelper.writeObjectToArray(vo),
                vo.getReadPermission().getId(),
                vo.getEditPermission().getId()
        };
    }

    @Override
    public RowMapper<AbstractEventHandlerVO> getRowMapper() {
        return new EventHandlerRowMapper();
    }

    private static final String EVENT_HANDLER_SELECT = "SELECT id, xid, alias, eventHandlerType, data, readPermissionId, editPermissionId FROM eventHandlers ";
    /**
     * Note: eventHandlers.eventTypeRef[1,2] match on both the given ref and 0. This is to allow a single set of event
     * handlers to be defined for user login events, rather than have to individually define them for each user.
     */
    private static final String EVENT_HANDLER_SELECT_BY_TYPE_SUB = "SELECT eh.id, eh.xid, eh.alias, eh.eventHandlerType, " +
            "eh.data, eh.readPermissionId, eh.editPermissionId FROM eventHandlersMapping ehm INNER JOIN eventHandlers eh ON eh.id=ehm.eventHandlerId WHERE ehm.eventTypeName=? AND " +
            "ehm.eventSubtypeName=? AND (ehm.eventTypeRef1=? OR ehm.eventTypeRef1=0) AND (ehm.eventTypeRef2=? or ehm.eventTypeRef2=0)";
    private static final String EVENT_HANDLER_SELECT_BY_TYPE_NULLSUB = "SELECT eh.id, eh.xid, eh.alias, eh.eventHandlerType, eh.data, eh.readPermissionId, eh.editPermissionId " +
            "FROM eventHandlersMapping ehm INNER JOIN eventHandlers eh ON eh.id=ehm.eventHandlerId WHERE ehm.eventTypeName=? AND " +
            "ehm.eventSubtypeName='' AND (ehm.eventTypeRef1=? OR ehm.eventTypeRef1=0) AND (ehm.eventTypeRef2=? OR ehm.eventTypeRef2=0)";

    public List<AbstractEventHandlerVO> getEventHandlers(EventType type) {
        return getEventHandlers(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(),
                type.getReferenceId2());
    }

    public List<AbstractEventHandlerVO> getEventHandlersByType(String typeName) {
        return query(EVENT_HANDLER_SELECT + " WHERE eventHandlerType=?", new Object[] {typeName}, new EventHandlerWithRelationalDataRowMapper());
    }

    private List<EventType> getEventTypesForHandler(int handlerId) {
        return ejt.query("SELECT eventTypeName, eventSubtypeName, eventTypeRef1, eventTypeRef2 FROM eventHandlersMapping WHERE eventHandlerId=?", new Object[] {handlerId}, new RowMapper<EventType>() {
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

    private List<AbstractEventHandlerVO> getEventHandlers(String typeName, String subtypeName, int ref1, int ref2) {
        if (subtypeName == null)
            return query(EVENT_HANDLER_SELECT_BY_TYPE_NULLSUB, new Object[] { typeName, ref1, ref2 },
                    new EventHandlerRowMapper());
        return query(EVENT_HANDLER_SELECT_BY_TYPE_SUB, new Object[] { typeName, subtypeName, ref1, ref2 },
                new EventHandlerRowMapper());
    }

    class EventHandlerRowMapper implements RowMapper<AbstractEventHandlerVO> {
        @Override
        public AbstractEventHandlerVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            AbstractEventHandlerVO h = (AbstractEventHandlerVO) SerializationHelper.readObjectInContext(rs.getBinaryStream(5));
            h.setId(rs.getInt(1));
            h.setXid(rs.getString(2));
            h.setAlias(rs.getString(3));
            h.setDefinition(ModuleRegistry.getEventHandlerDefinition(rs.getString(4)));
            h.setReadPermission(new MangoPermission(rs.getInt(6)));
            h.setEditPermission(new MangoPermission(rs.getInt(7)));
            return h;
        }
    }

    /**
     * For use in the legacy query methods
     *
     * @author Terry Packer
     */
    class EventHandlerWithRelationalDataRowMapper extends EventHandlerRowMapper {
        @Override
        public AbstractEventHandlerVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            AbstractEventHandlerVO h = super.mapRow(rs, rowNum);
            loadRelationalData(h);
            return h;
        }
    }

    @Override
    public void savePreRelationalData(AbstractEventHandlerVO existing, AbstractEventHandlerVO vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission().getRoles());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission().getRoles());
        vo.setEditPermission(editPermission);

        vo.getDefinition().savePreRelationalData(existing, vo);
    }

    @Override
    public void saveRelationalData(AbstractEventHandlerVO existing, AbstractEventHandlerVO vo) {
        if (existing == null) {
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
            vo.getDefinition().saveRelationalData(existing, vo);
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
            vo.getDefinition().saveRelationalData(existing, vo);

            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.permissionDeleted(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionService.permissionDeleted(existing.getEditPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(AbstractEventHandlerVO vo) {
        vo.setEventTypes(getEventTypesForHandler(vo.getId()));
        vo.getDefinition().loadRelationalData(vo);
        //Populate permissions
        vo.setReadPermission(permissionService.get(vo.getReadPermission().getId()));
        vo.setEditPermission(permissionService.get(vo.getEditPermission().getId()));
    }

    @Override
    public void deleteRelationalData(AbstractEventHandlerVO vo) {
        deleteEventHandlerMappings(vo.getId());
        vo.getDefinition().deleteRelationalData(vo);
    }

    @Override
    public void deletePostRelationalData(AbstractEventHandlerVO vo) {
        vo.getDefinition().deletePostRelationalData(vo);
        //Clean permissions
        MangoPermission readPermission = vo.getReadPermission();
        MangoPermission editPermission = vo.getEditPermission();
        permissionService.permissionDeleted(readPermission, editPermission);
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinPermissions(SelectJoinStep<R> select, ConditionSortLimit conditions,
            PermissionHolder user) {
        if(!permissionService.hasAdminRole(user)) {
            List<Integer> roleIds = permissionService.getAllInheritedRoles(user).stream().map(r -> r.getId()).collect(Collectors.toList());

            Condition roleIdsIn = RoleTableDefinition.roleIdField.in(roleIds);

            Table<?> mintermsGranted = this.create.select(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .from(MintermMappingTable.MINTERMS_MAPPING)
                    .groupBy(MintermMappingTable.MINTERMS_MAPPING.mintermId)
                    .having(DSL.count().eq(DSL.count(
                            DSL.case_().when(roleIdsIn, DSL.inline(1))
                            .else_(DSL.inline((Integer)null))))).asTable("mintermsGranted");

            Table<?> permissionsGranted = this.create.selectDistinct(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId)
                    .from(PermissionMappingTable.PERMISSIONS_MAPPING)
                    .join(mintermsGranted).on(mintermsGranted.field(MintermMappingTable.MINTERMS_MAPPING.mintermId).eq(PermissionMappingTable.PERMISSIONS_MAPPING.mintermId))
                    .asTable("permissionsGranted");

            select = select.join(permissionsGranted).on(
                    permissionsGranted.field(PermissionMappingTable.PERMISSIONS_MAPPING.permissionId).in(
                            EventHandlerTableDefinition.READ_PERMISSION_ALIAS));

        }
        return select;
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
