/*
 * Copyright (C) 2021 RadixIot LLC. All rights reserved.
 */

package com.serotonin.m2m2.db.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.infiniteautomation.mango.db.query.ConditionSortLimit;
import com.infiniteautomation.mango.db.query.ConditionSortLimitWithTagKeys;
import com.infiniteautomation.mango.db.query.RQLSubSelectCondition;
import com.infiniteautomation.mango.db.query.RQLToCondition;
import com.infiniteautomation.mango.db.query.RQLToConditionWithTagKeys;
import com.infiniteautomation.mango.db.tables.DataPoints;
import com.infiniteautomation.mango.db.tables.PublishedPoints;
import com.infiniteautomation.mango.db.tables.Publishers;
import com.infiniteautomation.mango.db.tables.records.PublishedPointsRecord;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.spring.events.DaoEvent;
import com.infiniteautomation.mango.spring.events.DaoEventType;
import com.infiniteautomation.mango.spring.events.StateChangeEvent;
import com.infiniteautomation.mango.spring.events.audit.ToggleAuditEvent;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.PublisherPointsUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.module.PublisherDefinition;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.util.ILifecycleState;

@Repository
public class PublishedPointDao extends AbstractVoDao<PublishedPointVO, PublishedPointsRecord, PublishedPoints> {

    private final Publishers publishers;
    private final DataPoints dataPoints;
    private final DataPointTagsDao dataPointTagsDao;

    private static final LazyInitSupplier<PublishedPointDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(PublishedPointDao.class);
    });

    private PublishedPointDao(DaoDependencies dependencies,
                              DataPointTagsDao dataPointTagsDao) {
        super(dependencies, AuditEventType.TYPE_PUBLISHED_POINT, PublishedPoints.PUBLISHED_POINTS,
                new TranslatableMessage("internal.monitor.PUBLISHED_POINT_COUNT"));
        this.publishers = Publishers.PUBLISHERS;
        this.dataPoints = DataPoints.DATA_POINTS;
        this.dataPointTagsDao = dataPointTagsDao;
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static PublishedPointDao getInstance() {
        return springInstance.get();
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
        vo.setEnabled(charToBool(record.get(table.enabled)));
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
       select = select.join(publishers).on(publishers.id.eq(table.publisherId));
        if (conditions instanceof ConditionSortLimitWithTagKeys) {
            Map<String, Field<String>> tagFields = ((ConditionSortLimitWithTagKeys) conditions).getTagFields();
            select = dataPointTagsDao.joinTags(select, table.dataPointId, tagFields);
        }
        return select.join(dataPoints).on(dataPoints.id.eq(table.dataPointId));
    }

    @Override
    public void loadRelationalData(PublishedPointVO vo) {
        vo.supplyDataPointTags(() -> dataPointTagsDao.getTagsForDataPointId(vo.getDataPointId()));
    }

    @Override
    protected RQLToCondition createRqlToCondition(Map<String, RQLSubSelectCondition> subSelectMap, Map<String, Field<?>> fieldMap,
                                                  Map<String, Function<Object, Object>> converterMap) {
        return new RQLToConditionWithTagKeys(fieldMap, converterMap);
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
     * Get the published points for a given publisher.
     *  - NOTE this returns both enabled and disabled points
     * @param publisherId
     * @return
     */
    public List<PublishedPointVO> getPublishedPoints(int publisherId) {
        return getJoinedSelectQuery()
                .where(table.publisherId.eq(publisherId))
                .fetch(this::mapRecordLoadRelationalData);
    }

    /**
     * Get only the enabled points for a publisher
     * @param publisherId
     * @return
     */
    public List<PublishedPointVO> getEnabledPublishedPoints(int publisherId) {
        return getJoinedSelectQuery()
                .where(table.publisherId.eq(publisherId), table.enabled.eq(boolToChar(true)))
                .fetch(this::mapRecordLoadRelationalData);
    }

    /**
     * Replace points for a given publisher
     * @param id
     * @param pointVos
     */
    public void replacePoints(int id, List<PublishedPointVO> pointVos) {
        doInTransaction(txStatus -> {
            this.create.deleteFrom(table).where(table.publisherId.eq(id)).execute();
            for(PublishedPointVO vo : pointVos) {
                this.insert(vo);
            }
        });
    }

    /**
     * Count the points for a type of publisher (used for metrics reporting)
     * @return
     */
    public  List<PublisherPointsUsageStatistics> getUsage() {
        Field<Integer> count = DSL.count(table.id);
        return create.select(publishers.publisherType, count)
                .from(table)
                .join(publishers).on(publishers.id.eq(table.publisherId))
                .groupBy(publishers.publisherType)
                .fetch()
                .map(record -> {
                    PublisherPointsUsageStatistics usage = new PublisherPointsUsageStatistics();
                    usage.setPublisherType(record.get(publishers.publisherType));
                    usage.setCount(record.get(count));
                    return usage;
                });
    }

    /**
     * Update the enabled column
     *
     * @param pp point for which to update the enabled column
     */
    public void saveEnabledColumn(PublishedPointVO pp) {
        PublishedPointVO existing = get(pp.getId());
        saveEnabledColumn(existing, pp.isEnabled());
    }

    /**
     * Update the enabled column
     *
     * @param existing point for which to update the enabled column
     * @param enabled if the point should be enabled or disabled
     * @return updated publshed point
     */
    public PublishedPointVO saveEnabledColumn(PublishedPointVO existing, boolean enabled) {
        create.update(table)
                .set(table.enabled, boolToChar(enabled))
                .where(table.id.eq(existing.getId()))
                .execute();

        PublishedPointVO result = existing.copy();
        result.setEnabled(enabled);
        this.publishEvent(new DaoEvent<>(this, DaoEventType.UPDATE, result, existing));
        this.publishAuditEvent(new ToggleAuditEvent<>(this.auditEventType, Common.getUser(), result));
        return result;
    }

    /**
     * Is a published point enabled, returns false if point is disabled or DNE.
     * @param id
     * @return
     */
    public boolean isEnabled(int id) {
        String enabled = create.select(table.enabled)
                .from(table)
                .where(table.id.eq(id))
                .fetchSingle()
                .value1();
        return charToBool(enabled);
    }

    /**
     * Publish a notification of a runtime state change
     * @param vo
     * @param state
     */
    public void notifyStateChanged(PublishedPointVO vo, ILifecycleState state) {
        eventPublisher.publishEvent(new StateChangeEvent<>(this, state, vo));
    }

    /**
     * Send a notification when a data point has cascade deleted
     *  a running published point
     * @param vo
     */
    public void notifyPointDeleted(PublishedPointVO vo) {
        publishEvent(createDaoEvent(DaoEventType.DELETE, vo, null));
    }
}
