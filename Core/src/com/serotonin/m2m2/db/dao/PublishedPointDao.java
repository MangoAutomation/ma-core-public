/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.db.tables.Publishers;
import com.infiniteautomation.mango.db.tables.records.PublishedPointsRecord;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;

@Repository
public class PublishedPointDao extends AbstractVoDao<PublishedPointVO, PublishedPointsRecord, PublishedPoints> {

    private final Publishers publishers;
    private final DataPoints dataPoints;

    private PublishedPointDao(DaoDependencies dependencies) {
        super(dependencies, AuditEventType.TYPE_PUBLISHED_POINT, PublishedPoints.PUBLISHED_POINTS,
                new TranslatableMessage("internal.monitor.PUBLISHED_POINT_COUNT"));
        this.publishers = Publishers.PUBLISHERS;
        this.dataPoints = DataPoints.DATA_POINTS;
    }

    @Override
    protected Record toRecord(PublishedPointVO vo) {
        PublishedPointsRecord record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.name, vo.getName());
        record.set(table.enabled, boolToChar(vo.isEnabled()));
        record.set(table.publisherId, vo.getPublisherId());
        record.set(table.dataPointId, vo.getDataPointId());
        //JSON Data for UI customizations
        record.set(table.jsonData, convertData(vo.getJsonData()));

        PublisherDefinition<?> def = ModuleRegistry.getPublisherDefinition(vo.getPublisherTypeName());
        if(def == null) {
            throw new RuntimeException(new ModuleNotLoadedException(vo.getPublisherTypeName(), vo.getClass().getName(), new Exception("Publisher definition not found")));
        }

        try {
            record.set(table.data, def.createPublishedPointDbData(vo));
        } catch (JsonProcessingException e) {
            LOG.error("Failed to write published point JSON for point {} ", vo.getXid(), e);
        }

        return record;
    }

    @Override
    public @NonNull PublishedPointVO mapRecord(@NonNull Record record) {
        String publisherType = record.get(publishers.publisherType);
        PublisherDefinition<?> def = ModuleRegistry.getPublisherDefinition(publisherType);
        if(def == null) {
            throw new RuntimeException(new ModuleNotLoadedException(publisherType, "published point", new Exception("Publisher definition not found")));
        }

        PublishedPointVO vo = def.createPublishedPointVO(record.get(table.publisherId), record.get(table.dataPointId));
        vo.setId(record.get(table.id));
        vo.setXid(record.get(table.xid));
        vo.setName(record.get(table.name));
        vo.setJsonData(extractData(record.get(table.jsonData)));

        // Publisher information from join
        vo.setPublisherXid(record.get(publishers.xid));
        vo.setPublisherTypeName(publisherType);
        // Data point information from join
        vo.setDataPointXid(record.get(dataPoints.xid));

        String data =  record.get(table.data);
        if(data != null) {
            try {
                def.mapPublishedPointDbData(vo, data);
            } catch (JsonProcessingException e) {
                LOG.error("Failed to read published point JSON for point {}", vo.getXid(), e);
            }
        }

        return vo;
    }

    @Override
    public List<Field<?>> getSelectFields() {
        List<Field<?>> fields = new ArrayList<>(super.getSelectFields());
        fields.add(publishers.publisherType);
        fields.add(publishers.xid);
        fields.add(dataPoints.xid);
        return fields;
    }

    @Override
    protected String getXidPrefix() {
        return PublishedPointVO.XID_PREFIX;
    }

    @Override
    public <R extends Record> SelectJoinStep<R> joinTables(SelectJoinStep<R> select, ConditionSortLimit conditions) {
        SelectJoinStep<R> s = select.join(publishers).on(publishers.id.eq(table.publisherId));
        return s.join(dataPoints).on(dataPoints.id.eq(table.dataPointId));
    }

    @Override
    protected void handleMappingException(Exception e, Record record) {
        if (e.getCause() instanceof ModuleNotLoadedException) {
            LOG.error("Published point with xid '" + record.get(table.xid) +
                    "' could not be loaded. Is its module missing?", e.getCause());
        } else {
            LOG.error("Error mapping published point with xid '" + record.get(table.xid) +
                    "' from SQL record", e.getCause());
        }
    }

    /**
     * Get the published points for a given publisher
     * @param publisherId
     * @return
     */
    public List<PublishedPointVO> getPublishedPoints(int publisherId) {
        return getJoinedSelectQuery()
                .where(table.publisherId.eq(publisherId))
                .fetch(this::mapRecordLoadRelationalData);
    }
}
