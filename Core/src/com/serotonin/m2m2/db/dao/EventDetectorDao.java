/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 *
 * @author Terry Packer
 */
package com.serotonin.m2m2.db.dao;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.DataSources;
import com.infiniteautomation.mango.db.tables.EventDetectors;
import com.infiniteautomation.mango.db.tables.records.EventDetectorsRecord;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.EventDetectorDefinition;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.definitions.event.detectors.PointEventDetectorDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.event.AbstractEventHandlerVO;
import com.serotonin.m2m2.vo.event.EventTypeVO;
import com.serotonin.m2m2.vo.event.detector.AbstractEventDetectorVO;
import com.serotonin.m2m2.vo.event.detector.AbstractPointEventDetectorVO;

/**
 * @author Terry Packer
 */
@Repository()
public class EventDetectorDao extends AbstractVoDao<AbstractEventDetectorVO, EventDetectorsRecord, EventDetectors> {

    private static final LazyInitSupplier<EventDetectorDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(EventDetectorDao.class);
    });

    /* Map of Source Type to Source ID Column Names */
    private final DataPoints dataPointTable;
    private final DataSources dataSourceTable;

    private final PermissionService permissionService;
    private final Map<String, Field<Integer>> sourceTypeToField;

    @Autowired
    private EventDetectorDao(PermissionService permissionService,
                             @Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME) ObjectMapper mapper,
                             ApplicationEventPublisher publisher) {
        super(AuditEventType.TYPE_EVENT_DETECTOR,
                EventDetectors.EVENT_DETECTORS,
                new TranslatableMessage("internal.monitor.EVENT_DETECTOR_COUNT"),
                mapper, publisher);
        this.dataPointTable = DataPoints.DATA_POINTS;
        this.dataSourceTable = DataSources.DATA_SOURCES;
        this.permissionService = permissionService;
        //Build our ordered column set from the Module Registry

        this.sourceTypeToField = ModuleRegistry.getEventDetectorDefinitions()
                .stream()
                .collect(Collectors.toMap(EventDetectorDefinition::getSourceTypeName,
                        EventDetectorDefinition::getSourceIdColumnName, (a, b) -> a));
    }

    /**
     * Get cached instance from Spring Context
     *
     * @return
     */
    public static EventDetectorDao getInstance() {
        return springInstance.get();
    }

    private String writeValueAsString(AbstractEventDetectorVO value) throws JsonException, IOException {
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(Common.JSON_CONTEXT, stringWriter);
        writer.writeObject(value);
        return stringWriter.toString();
    }

    @Override
    protected Record voToObjectArray(AbstractEventDetectorVO vo) {
        String data = null;
        try {
            data = writeValueAsString(vo);
        } catch (JsonException | IOException e) {
            LOG.error(e.getMessage(), e);
        }

        //Find the index of our sourceIdColumn
        Field<Integer> sourceId = sourceTypeToField.get(vo.getDetectorSourceType());

        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.sourceTypeName, vo.getDetectorSourceType());
        record.set(table.typeName, vo.getDetectorType());
        record.set(table.jsonData, convertData(vo.getData()));
        record.set(table.data, data);
        record.set(table.readPermissionId, vo.getReadPermission().getId());
        record.set(table.editPermissionId, vo.getEditPermission().getId());

        // null out other columns
        for (Field<Integer> field : sourceTypeToField.values()) {
            record.set(field, null);
        }
        // update the correct source id column
        record.set(sourceId, vo.getSourceId());

        return record;
    }

    @Override
    public AbstractEventDetectorVO mapRecord(Record record) {
        String type = record.get(table.typeName);
        EventDetectorDefinition<?> definition = ModuleRegistry.getEventDetectorDefinition(type);
        if (definition == null)
            throw new IllegalStateException("Event detector definition of type '" + type + "' not found.");

        //Compute the index of this source id
        Field<Integer> sourceIdField = sourceTypeToField.get(definition.getSourceTypeName());
        int sourceId = record.get(sourceIdField);

        AbstractEventDetectorVO vo = definition.baseCreateEventDetectorVO(sourceId);
        readRecordIntoEventDetector(record, vo);
        return vo;
    }

    private void readRecordIntoEventDetector(Record record, AbstractEventDetectorVO vo) {
        vo.setId(record.get(table.id));
        vo.setXid(record.get(table.xid));
        vo.setData(extractDataFromObject(record.get(table.jsonData)));

        //Read Into Detector
        JsonTypeReader typeReader = new JsonTypeReader(record.get(table.data));
        try {
            JsonValue value = typeReader.read();
            JsonObject root = value.toJsonObject();
            JsonReader reader = new JsonReader(Common.JSON_CONTEXT, root);
            root.remove("handlers");
            reader.readInto(vo);
        } catch (ClassCastException | IOException | JsonException e) {
            LOG.error(e.getMessage(), e);
        }

        MangoPermission read = new MangoPermission(record.get(table.readPermissionId));
        vo.supplyReadPermission(() -> read);
        MangoPermission edit = new MangoPermission(record.get(table.editPermissionId));
        vo.supplyEditPermission(() -> edit);
    }

    @Override
    protected String getXidPrefix() {
        return AbstractEventDetectorVO.XID_PREFIX;
    }

    @Override
    public void savePreRelationalData(AbstractEventDetectorVO existing, AbstractEventDetectorVO vo) {
        MangoPermission readPermission = permissionService.findOrCreate(vo.getReadPermission());
        vo.setReadPermission(readPermission);

        MangoPermission editPermission = permissionService.findOrCreate(vo.getEditPermission());
        vo.setEditPermission(editPermission);

        vo.getDefinition().savePreRelationalData(existing, vo);
    }

    @Override
    public void saveRelationalData(AbstractEventDetectorVO existing, AbstractEventDetectorVO vo) {
        EventTypeVO et = vo.getEventType();
        EventHandlerDao eventHandlerDao = EventHandlerDao.getInstance();
        if (vo.getAddedEventHandlers() != null) {
            if (existing != null) {
                for (AbstractEventHandlerVO ehVo : vo.getAddedEventHandlers()) {
                    eventHandlerDao.addEventHandlerMappingIfMissing(ehVo.getId(), et.getEventType());
                }
            } else {
                for (AbstractEventHandlerVO ehVo : vo.getAddedEventHandlers()) {
                    eventHandlerDao.saveEventHandlerMapping(ehVo.getId(), et.getEventType());
                }
            }
        } else if (vo.getEventHandlerXids() != null) {
            //Remove all mappings if we are updating the detector
            if (existing != null) {
                eventHandlerDao.deleteEventHandlerMappings(et.getEventType());
            }
            //Add mappings
            for (String xid : vo.getEventHandlerXids()) {
                eventHandlerDao.saveEventHandlerMapping(xid, et.getEventType());
            }
        }
        if (existing != null) {
            if (!existing.getReadPermission().equals(vo.getReadPermission())) {
                permissionService.deletePermissions(existing.getReadPermission());
            }
            if (!existing.getEditPermission().equals(vo.getEditPermission())) {
                permissionService.deletePermissions(existing.getEditPermission());
            }
        }
    }

    @Override
    public void loadRelationalData(AbstractEventDetectorVO vo) {
        vo.supplyEventHandlerXids(() -> EventHandlerDao.getInstance().getEventHandlerXids(vo.getEventType().getEventType()));

        //Populate permissions
        MangoPermission read = vo.getReadPermission();
        vo.supplyReadPermission(() -> permissionService.get(read.getId()));
        MangoPermission edit = vo.getEditPermission();
        vo.supplyEditPermission(() -> permissionService.get(edit.getId()));

        vo.getDefinition().loadRelationalData(vo);
    }

    @Override
    public void deleteRelationalData(AbstractEventDetectorVO vo) {
        //Also update the Event Handlers
        ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1=? and eventTypeRef2=?",
                vo.getEventType().getEventType().getEventType(), vo.getSourceId(), vo.getId());
    }

    @Override
    public void deletePostRelationalData(AbstractEventDetectorVO vo) {
        //Clean permissions
        MangoPermission readPermission = vo.getReadPermission();
        MangoPermission editPermission = vo.getEditPermission();
        permissionService.deletePermissions(readPermission, editPermission);
        vo.getDefinition().deletePostRelationalData(vo);
    }

    /**
     * Get all data point event detectors with the corresponding sourceId AND the point loaded into it
     * Ordered by detector id.
     *
     * @param sourceId
     * @return
     */
    public List<AbstractPointEventDetectorVO> getWithSource(int sourceId, DataPointVO dp) {
        Field<Integer> sourceIdColumnName = sourceTypeToField.get(EventType.EventTypeNames.DATA_POINT);

        return getJoinedSelectQuery()
                .where(sourceIdColumnName.eq(sourceId))
                .orderBy(getIdField())
                .fetch(r -> {
                    AbstractPointEventDetectorVO result = mapPointEventDetector(r, dp);
                    loadRelationalData(result);
                    return result;
                });
    }

    /**
     * Get the id for a given row
     *
     * @param xid
     * @return
     */
    public int getId(String xid, int dpId) {
        return this.create.select(table.id)
                .from(table)
                .where(table.xid.equal(xid), table.dataPointId.equal(dpId))
                .fetchOptional(table.id).orElse(Common.NEW_ID);
    }

    /**
     * Get all point event detectors with the data point loaded
     *
     * @return
     */
    public List<AbstractPointEventDetectorVO> getAllPointEventDetectors() {
        List<Field<?>> fields = new ArrayList<>(getSelectFields());
        fields.addAll(Arrays.asList(dataPointTable.fields()));
        fields.add(dataSourceTable.name);
        fields.add(dataSourceTable.xid);
        fields.add(dataSourceTable.dataSourceType);

        Select<Record> select = joinTables(getSelectQuery(fields), null)
                .leftOuterJoin(dataPointTable)
                .on(dataPointTable.id.eq(table.dataPointId))
                .join(dataSourceTable)
                .on(dataSourceTable.id.eq(dataPointTable.dataSourceId))
                .where(table.sourceTypeName.eq(EventTypeNames.DATA_POINT));

        DataPointDao dataPointDao = DataPointDao.getInstance();
        return select.fetch(record -> {
            DataPointVO dataPoint = dataPointDao.mapRecord(record);
            dataPointDao.loadRelationalData(dataPoint);
            return mapPointEventDetector(record, dataPoint);
        });
    }

    public AbstractPointEventDetectorVO mapPointEventDetector(Record record, DataPointVO dataPoint) {
        String type = record.get(table.typeName);
        if (type == null) {
            return null;
        }
        PointEventDetectorDefinition<?> definition = ModuleRegistry.getEventDetectorDefinition(type);
        AbstractPointEventDetectorVO detector = definition.baseCreateEventDetectorVO(dataPoint);
        readRecordIntoEventDetector(record, detector);
        return detector;
    }
}
