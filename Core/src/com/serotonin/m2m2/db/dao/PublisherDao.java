/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.db.dao;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.infiniteautomation.mango.db.tables.EventHandlersMapping;
import com.infiniteautomation.mango.db.tables.Publishers;
import com.infiniteautomation.mango.db.tables.records.PublishersRecord;
import com.infiniteautomation.mango.spring.DaoDependencies;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.PublisherUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType.EventTypeNames;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Matthew Lohbihler
 */
@Repository()
public class PublisherDao extends AbstractVoDao<PublisherVO, PublishersRecord, Publishers> {

    private static final LazyInitSupplier<PublisherDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(PublisherDao.class);
    });

    static final Logger LOG = LoggerFactory.getLogger(PublisherDao.class);

    private final DataPointDao dataPointDao;
    private final EventHandlersMapping handlerMapping;

    @Autowired
    private PublisherDao(DaoDependencies dependencies, DataPointDao dataPointDao) {
        super(dependencies, AuditEventType.TYPE_PUBLISHER,
                Publishers.PUBLISHERS,
                new TranslatableMessage("internal.monitor.PUBLISHER_COUNT"));
        this.dataPointDao = dataPointDao;
        this.handlerMapping = EventHandlersMapping.EVENT_HANDLERS_MAPPING;
    }

    /**
     * Get cached instance from Spring Context
     */
    public static PublisherDao getInstance() {
        return springInstance.get();
    }

    private static final String PUBLISHER_SELECT = "select id, xid, publisherType, data from publishers ";

    /**
     * Delete all publishers of a given type
     */
    public void deletePublisherType(final String publisherType) {
        create.select(table.id)
                .from(table)
                .where(table.publisherType.eq(publisherType))
                .fetch()
                .forEach(record -> {
                    int id = record.get(table.id);
                    delete(id);
                });
    }

    public Object getPersistentData(int id) {
        return create.select(table.rtdata)
                .from(table)
                .where(table.id.eq(id))
                .fetchSingle(record -> {
                    byte[] rtData = record.get(table.rtdata);
                    return SerializationHelper.readObjectFromArray(rtData);
                });
    }

    public void savePersistentData(int id, Object data) {
        create.update(table)
                .set(table.rtdata, SerializationHelper.writeObjectToArray(data))
                .where(table.id.eq(id))
                .execute();
    }

    @Override
    protected void handleMappingException(Exception e, Record record) {
        if (e.getCause() instanceof ModuleNotLoadedException) {
            LOG.error("Publisher with xid '" + record.get(table.xid) +
                    "' could not be loaded. Is its module missing?", e.getCause());
        } else {
            LOG.error("Error mapping publisher with xid '" + record.get(table.xid) +
                    "' from SQL record", e.getCause());
        }
    }

    /**
     * Get the count of data sources per type
     */
    public List<PublisherUsageStatistics> getUsage() {
        Field<Integer> count = DSL.count(table.publisherType);
        List<PublisherUsageStatistics> publisherUsageStatistics = create.select(table.publisherType, count)
                .from(table)
                .groupBy(table.publisherType)
                .fetch()
                .map(record -> {
                    PublisherUsageStatistics usage = new PublisherUsageStatistics();
                    usage.setPublisherType(record.get(table.publisherType));
                    usage.setCount(record.get(count));
                    return usage;
                });
        return publisherUsageStatistics;
    }

    @Override
    protected String getXidPrefix() {
        return PublisherVO.XID_PREFIX;
    }

    @Override
    protected Record toRecord(PublisherVO vo) {
        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.publisherType, vo.getDefinition().getPublisherTypeName());
        record.set(table.data, SerializationHelper.writeObjectToArray(vo));
        return record;
    }

    @Override
    public PublisherVO mapRecord(Record record) {
        PublisherVO p = (PublisherVO) SerializationHelper
            .readObjectInContextFromArray(record.get(table.data));
        p.setId(record.get(table.id));
        p.setXid(record.get(table.xid));
        p.setDefinition(ModuleRegistry.getPublisherDefinition(record.get(table.publisherType)));
        return p;
    }

    @Override
    public void savePreRelationalData(PublisherVO existing, PublisherVO vo) {
        vo.getDefinition().savePreRelationalData(existing, vo);
    }

    @Override
    public void saveRelationalData(PublisherVO existing, PublisherVO vo) {
        vo.getDefinition().saveRelationalData(existing, vo);
    }

    @Override
    public void loadRelationalData(PublisherVO vo) {
        vo.getDefinition().loadRelationalData(vo);
    }

    @Override
    public void deleteRelationalData(PublisherVO vo) {
        create.deleteFrom(handlerMapping)
                .where(handlerMapping.eventTypeName.eq(EventTypeNames.PUBLISHER))
                .and(handlerMapping.eventTypeRef1.eq(vo.getId()))
                .execute();
        vo.getDefinition().deleteRelationalData(vo);
    }

    @Override
    public void deletePostRelationalData(PublisherVO vo) {
        vo.getDefinition().deletePostRelationalData(vo);
    }

}
