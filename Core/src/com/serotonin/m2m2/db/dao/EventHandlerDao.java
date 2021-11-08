/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.tables.EventHandlers;
import com.infiniteautomation.mango.db.tables.EventHandlersMapping;
import com.infiniteautomation.mango.db.tables.records.EventHandlersRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventTypeMatcher;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 * @author Jared Wiltshire
 */
@Repository()
public class EventHandlerDao extends AbstractVoDao<AbstractEventHandlerVO, EventHandlersRecord, EventHandlers> {

    private static final LazyInitSupplier<EventHandlerDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(EventHandlerDao.class);
    });

    private final EventHandlersMapping handlerMapping;

    @Autowired
    private EventHandlerDao(DaoDependencies dependencies) {
        super(dependencies, AuditEventType.TYPE_EVENT_HANDLER,
                EventHandlers.EVENT_HANDLERS,
                new TranslatableMessage("internal.monitor.EVENT_HANDLER_COUNT"));

        this.handlerMapping = EventHandlersMapping.EVENT_HANDLERS_MAPPING;
    }

    /**
     * Get cached instance from Spring Context
     */
    public static EventHandlerDao getInstance() {
        return springInstance.get();
    }

    @Override
    protected String getXidPrefix() {
        return AbstractEventHandlerVO.XID_PREFIX;
    }

    @Override
    protected Record toRecord(AbstractEventHandlerVO vo) {
        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.alias, vo.getName());
        record.set(table.eventHandlerType, vo.getDefinition().getEventHandlerTypeName());
        record.set(table.data, SerializationHelper.writeObjectToArray(vo));
        record.set(table.readPermissionId, vo.getReadPermission().getId());
        record.set(table.editPermissionId, vo.getEditPermission().getId());
        return record;
    }

    @Override
    public @NonNull AbstractEventHandlerVO mapRecord(@NonNull Record record) {
        AbstractEventHandlerVO h = (AbstractEventHandlerVO) SerializationHelper.readObjectInContextFromArray(record.get(table.data));
        h.setId(record.get(table.id));
        h.setXid(record.get(table.xid));
        h.setAlias(record.get(table.alias));
        h.setDefinition(ModuleRegistry.getEventHandlerDefinition(record.get(table.eventHandlerType)));

        MangoPermission read = new MangoPermission(record.get(table.readPermissionId));
        h.supplyReadPermission(() -> read);

        MangoPermission edit = new MangoPermission(record.get(table.editPermissionId));
        h.supplyEditPermission(() -> edit);

        return h;
    }

    public List<AbstractEventHandlerVO> getEventHandlers(EventType type) {
        return getEventHandlers(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(),
                type.getReferenceId2());
    }

    public List<AbstractEventHandlerVO> getEventHandlersByType(String typeName) {
        List<AbstractEventHandlerVO> handlers = getJoinedSelectQuery().where(table.eventHandlerType.equal(typeName)).fetch(this::mapRecord);
        for (AbstractEventHandlerVO handler : handlers) {
            loadRelationalData(handler);
        }
        return handlers;
    }

    private List<EventTypeMatcher> getEventTypesForHandler(int handlerId) {
        return this.create.select(handlerMapping.fields())
                .from(handlerMapping)
                .where(handlerMapping.eventHandlerId.equal(handlerId))
                .fetch(this::mapEventType);
    }

    private EventTypeMatcher mapEventType(Record record) {
        String typeName = record.get(handlerMapping.eventTypeName);
        String subtypeName = record.get(handlerMapping.eventSubtypeName);
        Integer typeRef1 = record.get(handlerMapping.eventTypeRef1);
        Integer typeRef2 = record.get(handlerMapping.eventTypeRef2);
        return new EventTypeMatcher(typeName, subtypeName, typeRef1, typeRef2);
    }

    private Select<Record1<Integer>> whereMappingExists(String typeName, String subtypeName, Integer ref1, Integer ref2) {
        SelectConditionStep<Record1<Integer>> select = DSL.select(DSL.one()).from(handlerMapping)
                .where(table.id.eq(handlerMapping.eventHandlerId),
                        handlerMapping.eventTypeName.equal(typeName),
                        handlerMapping.eventSubtypeName.equal(subtypeName == null ? "" : subtypeName));

        if (ref1 != null && ref2 != null) {
            select = select.and(DSL.and(
                DSL.or(handlerMapping.eventTypeRef1.equal(0), handlerMapping.eventTypeRef1.equal(ref1)),
                DSL.or(handlerMapping.eventTypeRef2.equal(0), handlerMapping.eventTypeRef2.equal(ref2))
            ));
        }

        return select;
    }

    public List<String> getEventHandlerXids(EventType type) {
        return create.select(table.xid).from(table)
                .whereExists(whereMappingExists(type.getEventType(), type.getEventSubtype(), type.getReferenceId1(), type.getReferenceId2()))
                .fetch(table.xid);
    }

    private List<AbstractEventHandlerVO> getEventHandlers(String typeName, String subtypeName, int ref1, int ref2) {
        return getJoinedSelectQuery()
                .whereExists(whereMappingExists(typeName, subtypeName, ref1, ref2))
                .fetch(this::mapRecord);
    }

    public List<AbstractEventHandlerVO> enabledHandlersForType(String typeName, String subtypeName) {
        try (Stream<Record> stream = getJoinedSelectQuery()
                .whereExists(whereMappingExists(typeName, subtypeName, null, null))
                .stream()) {

            return stream.map(this::mapRecordLoadRelationalData)
                    .filter(Objects::nonNull)
                    .filter(AbstractEventHandlerVO::isEnabled) // cant add to SQL as its contained in serialized data
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void savePreRelationalData(AbstractEventHandlerVO existing, AbstractEventHandlerVO vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission());
        vo.setEditPermission(editPermission);

        vo.getDefinition().savePreRelationalData(existing, vo);
    }

    @Override
    public void saveRelationalData(AbstractEventHandlerVO existing, AbstractEventHandlerVO vo) {
        if (existing != null) {
            // Delete old mappings
            deleteEventHandlerMappings(vo.getId());
        }

        List<EventTypeMatcher> types = vo.getEventTypes();
        if (types.size() > 0) {
            BatchBindStep batchTypes = create.batch(create.insertInto(handlerMapping)
                    .columns(handlerMapping.eventHandlerId, handlerMapping.eventTypeName,
                            handlerMapping.eventSubtypeName, handlerMapping.eventTypeRef1,
                            handlerMapping.eventTypeRef2).values((Integer) null, null, null, null, null));
            for (EventTypeMatcher type : types) {
                String subType = type.getEventSubtype();
                subType = subType != null ? subType : "";
                batchTypes.bind(vo.getId(), type.getEventType(), subType, type.getReferenceId1(), type.getReferenceId2());
            }
            batchTypes.execute();
        }
        vo.getDefinition().saveRelationalData(existing, vo);

        if (existing != null) {
            if(!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if(!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionService.deletePermissions(existing.getEditPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(AbstractEventHandlerVO vo) {
        vo.setEventTypes(getEventTypesForHandler(vo.getId()));
        vo.getDefinition().loadRelationalData(vo);
        //Populate permissions
        MangoPermission read = vo.getReadPermission();
        vo.supplyReadPermission(() -> permissionService.get(read.getId()));
        MangoPermission edit = vo.getEditPermission();
        vo.supplyEditPermission(() -> permissionService.get(edit.getId()));

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
        permissionService.deletePermissions(readPermission, editPermission);
    }

    private void deleteEventHandlerMappings(int eventHandlerId) {
        create.deleteFrom(handlerMapping)
                .where(handlerMapping.eventHandlerId.eq(eventHandlerId))
                .execute();
    }

    /**
     * Add a mapping for an existing event handler for an event type,
     * @param eventHandlerXid event handler xid
     * @param type type to add mapping for
     */
    public void saveEventHandlerMapping(String eventHandlerXid, EventType type) {
        Integer id = getIdByXid(eventHandlerXid);
        Objects.requireNonNull(id, "Event Handler with xid: " + eventHandlerXid + " does not exist, can't create mapping.");
        saveEventHandlerMapping(id, type);
    }

    /**
     * Add a mapping for an existing event handler for an event type,
     * @param eventHandlerId event handler id
     * @param type type to add mapping for
     */
    public void saveEventHandlerMapping(int eventHandlerId, EventType type) {
        String subType = type.getEventSubtype();
        subType = subType != null ? subType : "";
        switch (create.dialect()) {
            case MYSQL:
            case MARIADB:
                // the @Supports annotation on mergeInto claims that it supports MySQL, however it does not
                // translate/emulate the merge using "on duplicate key update" so it fails
                create.insertInto(handlerMapping)
                        .columns(handlerMapping.eventHandlerId, handlerMapping.eventTypeName,
                                handlerMapping.eventSubtypeName, handlerMapping.eventTypeRef1,
                                handlerMapping.eventTypeRef2)
                        .values(eventHandlerId, type.getEventType(), subType,
                                type.getReferenceId1(), type.getReferenceId2())
                        .onDuplicateKeyUpdate()
                        .set(handlerMapping.eventHandlerId, eventHandlerId)
                        .set(handlerMapping.eventTypeName, type.getEventType())
                        .set(handlerMapping.eventSubtypeName, subType)
                        .set(handlerMapping.eventTypeRef1, type.getReferenceId1())
                        .set(handlerMapping.eventTypeRef2, type.getReferenceId2())
                        .execute();
                break;
            default:
                create.mergeInto(handlerMapping)
                        .using(DSL.selectOne())
                        .on(handlerMapping.eventHandlerId.eq(eventHandlerId),
                                handlerMapping.eventTypeName.eq(type.getEventType()),
                                handlerMapping.eventSubtypeName.eq(subType),
                                handlerMapping.eventTypeRef1.eq(type.getReferenceId1()),
                                handlerMapping.eventTypeRef2.eq(type.getReferenceId2()))
                        .whenMatchedThenUpdate()
                        .set(handlerMapping.eventHandlerId, eventHandlerId)
                        .set(handlerMapping.eventTypeName, type.getEventType())
                        .set(handlerMapping.eventSubtypeName, subType)
                        .set(handlerMapping.eventTypeRef1, type.getReferenceId1())
                        .set(handlerMapping.eventTypeRef2, type.getReferenceId2())
                        .whenNotMatchedThenInsert(handlerMapping.eventHandlerId,
                                handlerMapping.eventTypeName, handlerMapping.eventSubtypeName,
                                handlerMapping.eventTypeRef1, handlerMapping.eventTypeRef2)
                        .values(eventHandlerId, type.getEventType(), subType,
                                type.getReferenceId1(), type.getReferenceId2())
                        .execute();
                break;
        }
    }

    /**
     * Delete all mappings for this event type
     * @param type type to delete mappings for
     */
    public void deleteEventHandlerMappings(EventType type) {
        String subType = type.getEventSubtype();
        subType = subType != null ? subType : "";
        create.deleteFrom(handlerMapping)
                .where(handlerMapping.eventTypeName.eq(type.getEventType()))
                .and(handlerMapping.eventSubtypeName.eq(subType))
                .and(handlerMapping.eventTypeRef1.eq(type.getReferenceId1()))
                .and(handlerMapping.eventTypeRef2.eq(type.getReferenceId2()))
                .execute();
    }

    public void deleteEventHandlerMapping(int eventHandlerId, EventType type) {
        String subType = type.getEventSubtype();
        subType = subType != null ? subType : "";
        create.deleteFrom(handlerMapping)
                .where(handlerMapping.eventTypeName.eq(type.getEventType()))
                .and(handlerMapping.eventSubtypeName.eq(subType))
                .and(handlerMapping.eventTypeRef1.eq(type.getReferenceId1()))
                .and(handlerMapping.eventTypeRef2.eq(type.getReferenceId2()))
                .and(handlerMapping.eventHandlerId.eq(eventHandlerId))
                .execute();
    }
}
