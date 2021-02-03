/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
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
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infiniteautomation.mango.db.tables.Publishers;
import com.infiniteautomation.mango.db.tables.records.PublishersRecord;
import com.infiniteautomation.mango.spring.MangoRuntimeContextConfiguration;
import com.infiniteautomation.mango.util.LazyInitSupplier;
import com.infiniteautomation.mango.util.usage.AggregatePublisherUsageStatistics;
import com.infiniteautomation.mango.util.usage.PublisherPointsUsageStatistics;
import com.infiniteautomation.mango.util.usage.PublisherUsageStatistics;
import com.serotonin.ModuleNotLoadedException;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.module.ModuleRegistry;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.publish.PublishedPointVO;
import com.serotonin.m2m2.vo.publish.PublisherVO;
import com.serotonin.util.SerializationHelper;

/**
 * @author Matthew Lohbihler
 */
@Repository()
public class PublisherDao extends AbstractVoDao<PublisherVO<? extends PublishedPointVO>, PublishersRecord, Publishers> {

    private static final LazyInitSupplier<PublisherDao> springInstance = new LazyInitSupplier<>(() -> {
        return Common.getRuntimeContext().getBean(PublisherDao.class);
    });

    static final Log LOG = LogFactory.getLog(PublisherDao.class);

    @Autowired
    private PublisherDao(@Qualifier(MangoRuntimeContextConfiguration.DAO_OBJECT_MAPPER_NAME)ObjectMapper mapper,
            ApplicationEventPublisher publisher){
        super(AuditEventType.TYPE_PUBLISHER,
                Publishers.PUBLISHERS,
                new TranslatableMessage("internal.monitor.PUBLISHER_COUNT"),
                mapper, publisher);
    }

    /**
     * Get cached instance from Spring Context
     * @return
     */
    public static PublisherDao getInstance() {
        return springInstance.get();
    }

    private static final String PUBLISHER_SELECT = "select id, xid, publisherType, data from publishers ";

    public static class PublisherNameComparator implements Comparator<PublisherVO<?>> {
        @Override
        public int compare(PublisherVO<?> p1, PublisherVO<?> p2) {
            if (StringUtils.isBlank(p1.getName()))
                return -1;
            return p1.getName().compareTo(p2.getName());
        }
    }

    /**
     * Delete all publishers of a given type
     * @param publisherType
     */
    public void deletePublisherType(final String publisherType) {
        List<Integer> pubIds = queryForList("SELECT id FROM publishers WHERE publisherType=?",
                new Object[] { publisherType }, Integer.class);
        for (Integer pubId : pubIds)
            delete(pubId);
    }

    public Object getPersistentData(int id) {
        return query("select rtdata from publishers where id=?", new Object[] { id },
                new ResultSetExtractor<Serializable>() {
            @Override
            public Serializable extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (!rs.next())
                    return null;

                InputStream in = rs.getBinaryStream(1);
                if (in == null)
                    return null;

                return (Serializable) SerializationHelper.readObjectInContext(in);
            }
        });
    }

    public void savePersistentData(int id, Object data) {
        ejt.update("update publishers set rtdata=? where id=?", new Object[] { SerializationHelper.writeObject(data),
                id }, new int[] { Types.BINARY, Types.INTEGER });
    }

    public int countPointsForPublisherType(String publisherType, int excludeId) {
        try (Stream<Record> stream = create.select(table.fields()).from(table).where(
                table.publisherType.equal(publisherType),
                table.id.notEqual(excludeId)).stream()) {

            return stream.map(record -> {
                try {
                    return this.mapRecord(record);
                } catch (ShouldNeverHappenException e) {
                    // If the module was removed but there are still records in the database, this exception will be
                    // thrown. Check the inner exception to confirm.
                    if (e.getCause() instanceof ModuleNotLoadedException) {
                        // Yep. Log the occurrence and continue.
                        LOG.error("Publisher with type '" + record.get(table.publisherType) + "' and xid '" +
                                record.get(table.xid) + "' could not be loaded. Is its module missing?", e);
                    } else {
                        LOG.error(e.getMessage(), e);
                    }
                }
                return null;
            }).filter(Objects::nonNull)
                    .map(p -> p.getPoints().size()).reduce(0, Integer::sum);
        }
    }

    /**
     * Get the count of data sources per type
     * @return
     */
    public AggregatePublisherUsageStatistics getUsage() {
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

        List<PublisherPointsUsageStatistics> publisherPointsUsageStatistics = new ArrayList<>();
        for(PublisherUsageStatistics stats : publisherUsageStatistics) {
            PublisherPointsUsageStatistics pointStats = new PublisherPointsUsageStatistics();
            pointStats.setPublisherType(stats.getPublisherType());
            pointStats.setCount(countPointsForPublisherType(stats.getPublisherType(), -1));
            publisherPointsUsageStatistics.add(pointStats);
        }
        AggregatePublisherUsageStatistics usage = new AggregatePublisherUsageStatistics();
        usage.setPublisherUsageStatistics(publisherUsageStatistics);
        usage.setPublisherPointsUsageStatistics(publisherPointsUsageStatistics);
        return usage;
    }

    @Override
    protected String getXidPrefix() {
        return PublisherVO.XID_PREFIX;
    }

    @Override
    protected Record voToObjectArray(PublisherVO<? extends PublishedPointVO> vo) {
        Record record = table.newRecord();
        record.set(table.xid, vo.getXid());
        record.set(table.publisherType, vo.getDefinition().getPublisherTypeName());
        record.set(table.data, SerializationHelper.writeObjectToArray(vo));
        return record;
    }

    @Override
    public PublisherVO<? extends PublishedPointVO> mapRecord(Record record) {
        PublisherVO<? extends PublishedPointVO> p = (PublisherVO<? extends PublishedPointVO>) SerializationHelper
            .readObjectInContextFromArray(record.get(table.data));
        p.setId(record.get(table.id));
        p.setXid(record.get(table.xid));
        p.setDefinition(ModuleRegistry.getPublisherDefinition(record.get(table.publisherType)));
        return p;
    }

    @Override
    public void savePreRelationalData(PublisherVO<?> existing, PublisherVO<?> vo) {
        vo.getDefinition().savePreRelationalData(existing, vo);
    }

    @Override
    public void saveRelationalData(PublisherVO<?> existing, PublisherVO<?> vo) {
        vo.getDefinition().saveRelationalData(existing, vo);
    }

    @Override
    public void loadRelationalData(PublisherVO<?> vo) {
        vo.getDefinition().loadRelationalData(vo);
    }

    @Override
    public void deleteRelationalData(PublisherVO<?> vo) {
        ejt.update("delete from eventHandlersMapping where eventTypeName=? and eventTypeRef1=?", new Object[] {
                EventType.EventTypeNames.PUBLISHER, vo.getId()});
        vo.getDefinition().deleteRelationalData(vo);
    }

    @Override
    public void deletePostRelationalData(PublisherVO<?> vo) {
        vo.getDefinition().deletePostRelationalData(vo);
    }

}
